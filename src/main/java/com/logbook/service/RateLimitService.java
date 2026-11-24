package com.logbook.service;

import com.logbook.entity.RateLimitAttempt;
import com.logbook.exception.ApiException;
import com.logbook.repository.RateLimitAttemptRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Pembatasan laju permintaan berbasis database. State tidak lagi disimpan di
 * memori agar tetap konsisten saat aplikasi restart atau berjalan dalam beberapa
 * instance. Method dua argumen dipakai untuk login; method tiga argumen dipakai
 * oleh alur reset kata sandi dengan bucket yang terpisah.
 */
@Slf4j
@Service
public class RateLimitService {

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_FORGOT_PASSWORD = "forgot-password";
    public static final String ACTION_RESET_PASSWORD = "reset-password";

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(15);

    private final RateLimitAttemptRepository repository;
    private final Clock clock;
    private final boolean trustForwardedHeaders;

    public RateLimitService(
            RateLimitAttemptRepository repository,
            Clock clock,
            @Value("${logbook.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders
    ) {
        this.repository = repository;
        this.clock = clock;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Transactional
    public void checkAllowed(String email, HttpServletRequest request) {
        checkAllowed(ACTION_LOGIN, email, request);
    }

    @Transactional
    public void checkAllowed(String action, String identity, HttpServletRequest request) {
        String key = buildKey(action, identity, request);
        Instant now = clock.instant();
        RateLimitAttempt state = repository.findByBucketKeyForUpdate(key).orElse(null);
        if (state == null) {
            return;
        }

        OffsetDateTime lockedUntil = state.getLockedUntil();
        if (lockedUntil != null) {
            if (now.isBefore(lockedUntil.toInstant())) {
                long remainingMinutes = Math.max(
                        1,
                        Duration.between(now, lockedUntil.toInstant()).toMinutes() + 1
                );
                throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Terlalu banyak percobaan. Coba lagi dalam " + remainingMinutes + " menit."
                );
            }
            repository.delete(state);
            return;
        }

        OffsetDateTime windowStartedAt = state.getWindowStartedAt();
        if (windowStartedAt != null
                && Duration.between(windowStartedAt.toInstant(), now).compareTo(WINDOW_DURATION) > 0) {
            repository.delete(state);
        }
    }

    /**
     * Mencatat satu percobaan yang dihitung terhadap kuota. Untuk login ini
     * berarti percobaan gagal; untuk forgot/reset password setiap permintaan
     * dihitung agar endpoint tidak bisa dipakai untuk brute force token.
     */
    @Transactional
    public void recordFailure(String email, HttpServletRequest request) {
        recordFailure(ACTION_LOGIN, email, request);
    }

    @Transactional
    public void recordFailure(String action, String identity, HttpServletRequest request) {
        String key = buildKey(action, identity, request);
        Instant now = clock.instant();
        RateLimitAttempt state = repository.findByBucketKeyForUpdate(key).orElse(null);

        if (state == null) {
            RateLimitAttempt fresh = new RateLimitAttempt();
            fresh.setBucketKey(key);
            fresh.setWindowStartedAt(toOffsetDateTime(now));
            fresh.setFailedCount(1);
            try {
                state = repository.saveAndFlush(fresh);
            } catch (DataIntegrityViolationException exception) {
                state = repository.findByBucketKeyForUpdate(key).orElseThrow(() -> exception);
            }
        } else {
            OffsetDateTime lockedUntil = state.getLockedUntil();
            if (lockedUntil != null && now.isBefore(lockedUntil.toInstant())) {
                log.debug("Bucket {} masih terkunci, percobaan tidak dihitung", key);
                return;
            }

            OffsetDateTime windowStartedAt = state.getWindowStartedAt();
            if (windowStartedAt == null
                    || Duration.between(windowStartedAt.toInstant(), now).compareTo(WINDOW_DURATION) > 0) {
                state.setWindowStartedAt(toOffsetDateTime(now));
                state.setFailedCount(1);
                state.setLockedUntil(null);
            } else {
                state.setFailedCount(state.getFailedCount() + 1);
                if (state.getFailedCount() >= MAX_FAILED_ATTEMPTS) {
                    state.setLockedUntil(toOffsetDateTime(now.plus(LOCK_DURATION)));
                    log.warn("Pembatasan laju aktif untuk aksi {} selama {} menit", action, LOCK_DURATION.toMinutes());
                }
            }
        }

        log.debug("Percobaan ke-{} untuk aksi {}", state.getFailedCount(), action);
    }

    @Transactional
    public void recordSuccess(String email, HttpServletRequest request) {
        recordSuccess(ACTION_LOGIN, email, request);
    }

    @Transactional
    public void recordSuccess(String action, String identity, HttpServletRequest request) {
        repository.deleteByBucketKey(buildKey(action, identity, request));
    }

    private String buildKey(String action, String identity, HttpServletRequest request) {
        String normalizedIdentity = identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
        return action + "|" + normalizedIdentity + "|" + extractClientIp(request);
    }

    /**
     * Header X-Forwarded-For hanya dipercaya bila aplikasi memang berada di
     * belakang reverse proxy tepercaya. Jika tidak, klien bisa memalsukannya
     * untuk mendapatkan bucket rate limit yang baru.
     */
    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        if (trustForwardedHeaders) {
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
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, clock.getZone());
    }
}
