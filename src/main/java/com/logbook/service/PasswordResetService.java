package com.logbook.service;

import com.logbook.entity.PasswordResetToken;
import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.PasswordResetTokenRepository;
import com.logbook.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Alur reset kata sandi. Token plaintext dikembalikan ke pemanggil untuk
 * keperluan demo lokal (lihat AuthController), tetapi yang disimpan di database
 * hanyalah hash SHA-256. Token tidak pernah dicatat ke log aplikasi.
 */
@Slf4j
@Service
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final String GENERIC_MESSAGE =
            "Jika email terdaftar dan aktif, token reset kata sandi telah diproses.";
    private static final String SUCCESS_MESSAGE =
            "Kata sandi berhasil direset. Silakan login menggunakan kata sandi baru.";
    private static final String INVALID_TOKEN_MESSAGE =
            "Token reset kata sandi tidak valid atau sudah kedaluwarsa.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            RateLimitService rateLimitService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimitService = rateLimitService;
        this.clock = clock;
    }

    public record PasswordResetRequestResult(String message, String token) {
    }

    @Transactional
    public PasswordResetRequestResult requestPasswordReset(String email, HttpServletRequest request) {
        String normalizedEmail = normalize(email);

        rateLimitService.checkAllowed(RateLimitService.ACTION_FORGOT_PASSWORD, normalizedEmail, request);
        rateLimitService.recordFailure(RateLimitService.ACTION_FORGOT_PASSWORD, normalizedEmail, request);
        tokenRepository.deleteByExpiresAtBefore(toOffsetDateTime(clock.instant()));

        Optional<User> user = userRepository.findByEmailIgnoreCaseAndActiveTrue(normalizedEmail);
        if (user.isEmpty()) {
            return new PasswordResetRequestResult(GENERIC_MESSAGE, null);
        }

        tokenRepository.findByUserIdAndUsedAtIsNull(user.get().getId()).forEach(tokenRepository::delete);

        String token = generateToken();
        Instant now = clock.instant();
        PasswordResetToken entity = new PasswordResetToken();
        entity.setUserId(user.get().getId());
        entity.setTokenHash(hashToken(token));
        entity.setCreatedAt(toOffsetDateTime(now));
        entity.setExpiresAt(toOffsetDateTime(now.plus(TOKEN_TTL)));
        tokenRepository.save(entity);

        log.info("Token reset kata sandi dibuat untuk User ID {} (token tidak dicatat)", user.get().getId());
        return new PasswordResetRequestResult(GENERIC_MESSAGE, token);
    }

    @Transactional
    public String confirmPasswordReset(String token, String newPassword, HttpServletRequest request) {
        rateLimitService.checkAllowed(RateLimitService.ACTION_RESET_PASSWORD, "", request);
        rateLimitService.recordFailure(RateLimitService.ACTION_RESET_PASSWORD, "", request);

        String tokenHash = hashToken(token == null ? "" : token.trim());
        Instant now = clock.instant();
        PasswordResetToken entry = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);

        if (entry.getUsedAt() != null || now.isAfter(entry.getExpiresAt().toInstant())) {
            tokenRepository.delete(entry);
            throw invalidToken();
        }

        User user = userRepository.findByIdAndActiveTrue(entry.getUserId())
                .orElseThrow(this::inactiveUser);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        entry.setUsedAt(toOffsetDateTime(now));
        tokenRepository.save(entry);
        rateLimitService.recordSuccess(RateLimitService.ACTION_RESET_PASSWORD, "", request);

        log.info("Reset kata sandi berhasil untuk User ID {}", user.getId());
        return SUCCESS_MESSAGE;
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, INVALID_TOKEN_MESSAGE);
    }

    private ApiException inactiveUser() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Pengguna tidak ditemukan atau tidak aktif.");
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 tidak tersedia", exception);
        }
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, clock.getZone());
    }
}
