package com.logbook.service;

import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, ResetTokenEntry> tokens = new ConcurrentHashMap<>();
    private final Clock clock;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public String requestPasswordReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        purgeExpiredTokens();

        userRepository.findByEmailIgnoreCaseAndActiveTrue(normalizedEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            Instant expiresAt = clock.instant().plus(TOKEN_TTL);
            tokens.put(token, new ResetTokenEntry(user.getId(), expiresAt, false));
            log.info("Token reset kata sandi dibuat untuk User ID {} ({}): {} (berlaku {} menit)",
                    user.getId(), user.getEmail(), token, TOKEN_TTL.toMinutes());
        });

        return "Jika email terdaftar dan aktif, token reset kata sandi telah diproses.";
    }

    @Transactional
    public String confirmPasswordReset(String token, String newPassword) {
        purgeExpiredTokens();

        ResetTokenEntry entry = tokens.get(token);
        if (entry == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token reset kata sandi tidak valid atau sudah kedaluwarsa.");
        }

        synchronized (entry) {
            if (entry.used || clock.instant().isAfter(entry.expiresAt)) {
                tokens.remove(token);
                throw new ApiException(HttpStatus.BAD_REQUEST, "Token reset kata sandi tidak valid atau sudah kedaluwarsa.");
            }

            User user = userRepository.findByIdAndActiveTrue(entry.userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Pengguna tidak ditemukan atau tidak aktif."));

            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setMustChangePassword(false);
            userRepository.save(user);

            entry.used = true;
            tokens.remove(token);
            log.info("Reset kata sandi berhasil untuk User ID {}", user.getId());
        }

        return "Kata sandi berhasil direset. Silakan login menggunakan kata sandi baru.";
    }

    private void purgeExpiredTokens() {
        Instant now = clock.instant();
        tokens.entrySet().removeIf(e -> e.getValue().used || now.isAfter(e.getValue().expiresAt));
    }

    private static final class ResetTokenEntry {
        private final Long userId;
        private final Instant expiresAt;
        private boolean used;

        private ResetTokenEntry(Long userId, Instant expiresAt, boolean used) {
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.used = used;
        }
    }
}