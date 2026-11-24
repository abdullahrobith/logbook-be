package com.logbook.service;

import com.logbook.entity.RateLimitAttempt;
import com.logbook.exception.ApiException;
import com.logbook.repository.RateLimitAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T01:00:00Z");
    private static final String EMAIL = "rina.talent@contoh.id";

    @Mock
    private RateLimitAttemptRepository repository;

    private final Map<String, RateLimitAttempt> store = new HashMap<>();
    private final AtomicReference<Instant> now = new AtomicReference<>(NOW);
    private Clock clock;
    private MockHttpServletRequest request;
    private RateLimitService service;

    @BeforeEach
    void setUp() {
        now.set(NOW);
        store.clear();

        clock = org.mockito.Mockito.mock(Clock.class);
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        when(repository.findByBucketKeyForUpdate(anyString())).thenAnswer(
                invocation -> Optional.ofNullable(store.get(invocation.getArgument(0, String.class)))
        );
        when(repository.saveAndFlush(any(RateLimitAttempt.class))).thenAnswer(invocation -> {
            RateLimitAttempt attempt = invocation.getArgument(0);
            store.put(attempt.getBucketKey(), attempt);
            return attempt;
        });
        doAnswer(invocation -> {
            RateLimitAttempt attempt = invocation.getArgument(0);
            store.remove(attempt.getBucketKey(), attempt);
            return null;
        }).when(repository).delete(any(RateLimitAttempt.class));
        doAnswer(invocation -> {
            store.remove(invocation.getArgument(0, String.class));
            return null;
        }).when(repository).deleteByBucketKey(anyString());

        request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        service = new RateLimitService(repository, clock, false);
    }

    @Test
    void checkAllowedPassesWhenThereIsNoState() {
        assertDoesNotThrow(() -> service.checkAllowed(EMAIL, request));
        assertTrue(store.isEmpty());
    }

    @Test
    void fiveFailuresLockTheBucket() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(EMAIL, request);
        }

        RateLimitAttempt state = store.get(loginKey());
        assertEquals(5, state.getFailedCount());
        assertNotNull(state.getLockedUntil());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.checkAllowed(EMAIL, request)
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        assertTrue(exception.getMessage().contains("Coba lagi"));
    }

    @Test
    void expiredLockIsRemovedAndBucketCanBeUsedAgain() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(EMAIL, request);
        }
        now.set(NOW.plus(Duration.ofMinutes(16)));

        assertDoesNotThrow(() -> service.checkAllowed(EMAIL, request));
        assertTrue(store.isEmpty());
    }

    @Test
    void expiredWindowResetsFailureCount() {
        service.recordFailure(EMAIL, request);
        service.recordFailure(EMAIL, request);
        assertEquals(2, store.get(loginKey()).getFailedCount());

        now.set(NOW.plus(Duration.ofMinutes(16)));
        service.recordFailure(EMAIL, request);

        RateLimitAttempt state = store.get(loginKey());
        assertEquals(1, state.getFailedCount());
        assertNull(state.getLockedUntil());
    }

    @Test
    void successClearsState() {
        service.recordFailure(EMAIL, request);
        service.recordFailure(EMAIL, request);
        service.recordSuccess(EMAIL, request);

        assertTrue(store.isEmpty());
        assertDoesNotThrow(() -> service.checkAllowed(EMAIL, request));
    }

    @Test
    void forwardedHeaderIsIgnoredWhenProxyIsNotTrusted() {
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        service.recordFailure(EMAIL, request);

        String key = store.keySet().iterator().next();
        assertTrue(key.contains("203.0.113.10"), key);
        assertFalse(key.contains("198.51.100.7"), key);
    }

    @Test
    void forwardedHeaderIsUsedWhenProxyIsTrusted() {
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        RateLimitService trustedService = new RateLimitService(repository, clock, true);
        trustedService.recordFailure(EMAIL, request);

        String key = store.keySet().iterator().next();
        assertTrue(key.contains("198.51.100.7"), key);
    }

    @Test
    void loginAndForgotPasswordUseSeparateBuckets() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(EMAIL, request);
        }

        assertThrows(ApiException.class, () -> service.checkAllowed(EMAIL, request));
        assertDoesNotThrow(() -> service.checkAllowed(RateLimitService.ACTION_FORGOT_PASSWORD, EMAIL, request));
    }

    @Test
    void failuresOnForgotPasswordDoNotLockLogin() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(RateLimitService.ACTION_FORGOT_PASSWORD, EMAIL, request);
        }

        assertThrows(
                ApiException.class,
                () -> service.checkAllowed(RateLimitService.ACTION_FORGOT_PASSWORD, EMAIL, request)
        );
        assertDoesNotThrow(() -> service.checkAllowed(EMAIL, request));
    }

    private String loginKey() {
        return RateLimitService.ACTION_LOGIN + "|" + EMAIL + "|" + request.getRemoteAddr();
    }
}
