package com.logbook.service;

import com.logbook.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginRateLimitService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(15);

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginRateLimitService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        Instant now = clock.instant();

        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }

        synchronized (state) {
            if (state.lockedUntil != null) {
                if (now.isBefore(state.lockedUntil)) {
                    long remainingMinutes = Math.max(
                            1,
                            Duration.between(now, state.lockedUntil).toMinutes() + 1
                    );
                    throw new ApiException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Terlalu banyak percobaan login gagal. Coba lagi dalam " + remainingMinutes + " menit."
                    );
                }
                attempts.remove(key, state);
                return;
            }

            if (state.windowStartedAt != null && Duration.between(state.windowStartedAt, now).compareTo(WINDOW_DURATION) > 0) {
                attempts.remove(key, state);
            }
        }
    }

    public void recordFailure(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        Instant now = clock.instant();

        AttemptState state = attempts.compute(key, (k, existing) -> {
            if (existing == null) {
                AttemptState fresh = new AttemptState();
                fresh.windowStartedAt = now;
                fresh.failedCount = 1;
                return fresh;
            }

            synchronized (existing) {
                if (existing.lockedUntil != null && now.isBefore(existing.lockedUntil)) {
                    return existing;
                }

                if (existing.windowStartedAt == null
                        || Duration.between(existing.windowStartedAt, now).compareTo(WINDOW_DURATION) > 0) {
                    existing.windowStartedAt = now;
                    existing.failedCount = 1;
                    existing.lockedUntil = null;
                    return existing;
                }

                existing.failedCount += 1;
                if (existing.failedCount >= MAX_FAILED_ATTEMPTS) {
                    existing.lockedUntil = now.plus(LOCK_DURATION);
                    log.warn("Akun/IP terkena rate limit login selama {} menit untuk key: {}", LOCK_DURATION.toMinutes(), key);
                }
                return existing;
            }
        });

        log.debug("Percobaan login gagal ke-{} untuk key: {}", state.failedCount, key);
    }

    public void recordSuccess(String email, HttpServletRequest request) {
        String key = buildKey(email, request);
        attempts.remove(key);
    }

    private String buildKey(String email, HttpServletRequest request) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String ip = extractClientIp(request);
        return normalizedEmail + "|" + ip;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static final class AttemptState {
        private Instant windowStartedAt;
        private int failedCount;
        private Instant lockedUntil;
    }
}