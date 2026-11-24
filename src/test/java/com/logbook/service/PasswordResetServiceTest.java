package com.logbook.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.logbook.entity.PasswordResetToken;
import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.PasswordResetTokenRepository;
import com.logbook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T01:00:00Z");
    private static final String EMAIL = "rina.talent@contoh.id";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RateLimitService rateLimitService;

    private PasswordResetService service;
    private MockHttpServletRequest request;
    private User user;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Jakarta"));
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, rateLimitService, clock);
        request = new MockHttpServletRequest();

        user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setPasswordHash("$2a$10$oldHash");
        user.setMustChangePassword(true);
        user.setActive(true);

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.getLogger(PasswordResetService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void requestCreatesHashedTokenAndInvalidatesPreviousToken() {
        PasswordResetToken previous = token("previous-hash", NOW.minusSeconds(60), null);
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of(previous));

        var result = service.requestPasswordReset("  Rina.Talent@Contoh.id  ", request);

        assertNotNull(result.token());
        assertFalse(result.token().isBlank());
        assertEquals("Jika email terdaftar dan aktif, token reset kata sandi telah diproses.", result.message());

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(result.token(), saved.getTokenHash());
        assertEquals(hash(result.token()), saved.getTokenHash());
        assertEquals(OffsetDateTime.ofInstant(NOW.plusSeconds(900), ZoneId.of("Asia/Jakarta")), saved.getExpiresAt());
        assertNull(saved.getUsedAt());

        verify(tokenRepository).delete(previous);
        verify(tokenRepository).deleteByExpiresAtBefore(any(OffsetDateTime.class));
        verify(rateLimitService).checkAllowed(RateLimitService.ACTION_FORGOT_PASSWORD, EMAIL, request);
        verify(rateLimitService).recordFailure(RateLimitService.ACTION_FORGOT_PASSWORD, EMAIL, request);

        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
        assertFalse(logs.contains(result.token()), "Token plaintext tidak boleh muncul di log");
    }

    @Test
    void requestForUnknownEmailReturnsGenericMessageWithoutToken() {
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("tidak.ada@contoh.id"))
                .thenReturn(Optional.empty());

        var result = service.requestPasswordReset("tidak.ada@contoh.id", request);

        assertNull(result.token());
        assertNotNull(result.message());
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(rateLimitService).recordFailure(RateLimitService.ACTION_FORGOT_PASSWORD, "tidak.ada@contoh.id", request);
    }

    @Test
    void confirmWithValidTokenUpdatesPasswordAndMarksTokenUsed() {
        String token = "token-rahasia";
        PasswordResetToken entry = token(hash(token), NOW.plusSeconds(300), null);
        when(tokenRepository.findByTokenHash(hash(token))).thenReturn(Optional.of(entry));
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Reset@2026")).thenReturn("newHash");

        String message = service.confirmPasswordReset(token, "Reset@2026", request);

        assertEquals("Kata sandi berhasil direset. Silakan login menggunakan kata sandi baru.", message);
        assertEquals("newHash", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
        assertNotNull(entry.getUsedAt());
        verify(userRepository).save(user);
        verify(tokenRepository).save(entry);
        verify(rateLimitService).recordSuccess(RateLimitService.ACTION_RESET_PASSWORD, "", request);
    }

    @Test
    void confirmRejectsUsedToken() {
        String token = "token-terpakai";
        PasswordResetToken entry = token(hash(token), NOW.plusSeconds(300), NOW);
        when(tokenRepository.findByTokenHash(hash(token))).thenReturn(Optional.of(entry));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.confirmPasswordReset(token, "Reset@2026", request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).findByIdAndActiveTrue(any());
    }

    @Test
    void confirmRejectsExpiredTokenAndDeletesIt() {
        String token = "token-kedaluwarsa";
        PasswordResetToken entry = token(hash(token), NOW.minusSeconds(1), null);
        when(tokenRepository.findByTokenHash(hash(token))).thenReturn(Optional.of(entry));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.confirmPasswordReset(token, "Reset@2026", request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(tokenRepository).delete(entry);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void confirmRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.confirmPasswordReset("token-asing", "Reset@2026", request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Token reset kata sandi tidak valid atau sudah kedaluwarsa.", exception.getMessage());
    }

    @Test
    void confirmRejectsInactiveUser() {
        String token = "token-user-nonaktif";
        PasswordResetToken entry = token(hash(token), NOW.plusSeconds(300), null);
        when(tokenRepository.findByTokenHash(hash(token))).thenReturn(Optional.of(entry));
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.confirmPasswordReset(token, "Reset@2026", request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Pengguna tidak ditemukan atau tidak aktif.", exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
    }

    private PasswordResetToken token(String hash, Instant expiresAt, Instant usedAt) {
        PasswordResetToken entity = new PasswordResetToken();
        entity.setUserId(1L);
        entity.setTokenHash(hash);
        entity.setCreatedAt(OffsetDateTime.ofInstant(NOW, ZoneId.of("Asia/Jakarta")));
        entity.setExpiresAt(OffsetDateTime.ofInstant(expiresAt, ZoneId.of("Asia/Jakarta")));
        entity.setUsedAt(usedAt == null ? null : OffsetDateTime.ofInstant(usedAt, ZoneId.of("Asia/Jakarta")));
        return entity;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
