package com.logbook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifikasi keamanan sesi memakai Tomcat sungguhan. MockMvc tidak selalu
 * merekam pergantian ID dari HttpServletRequest.changeSessionId().
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "logbook.jadwal.aktif=false")
class SessionSecurityIntegrationTest {

    private static final String COOKIE_NAME = "LOGBOOK_SESSION";
    private static final String LOGIN_BODY = """
            {"email":"rina.talent@contoh.id","password":"Logbook@2026"}
            """;
    private static final String CHANGE_PASSWORD_BODY = """
            {"currentPassword":"Logbook@2026","newPassword":"Logbook@2026Baru"}
            """;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String originalPasswordHash;
    private boolean originalMustChangePassword;

    @BeforeEach
    void captureOriginalPassword() {
        originalPasswordHash = jdbcTemplate.queryForObject(
                "select password_hash from users where id = 1", String.class);
        originalMustChangePassword = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select must_change_password from users where id = 1", Boolean.class));
    }

    @AfterEach
    void restoreOriginalPassword() {
        if (originalPasswordHash != null) {
            jdbcTemplate.update(
                    "update users set password_hash = ?, must_change_password = ? where id = 1",
                    originalPasswordHash, originalMustChangePassword);
        }
    }

    @Test
    void loginCookieUsesHardenedAttributes() {
        ApiResponse response = login();
        String setCookie = response.setCookie();
        String lower = setCookie.toLowerCase(Locale.ROOT);

        assertEquals(200, response.status(), response.body());
        assertTrue(cookiePair(setCookie).startsWith(COOKIE_NAME + "="));
        assertTrue(lower.contains("httponly"));
        assertTrue(lower.contains("samesite=lax"));
        assertFalse(lower.contains("secure"), "Profil default HTTP lokal tidak boleh memakai cookie Secure");
    }

    @Test
    void loginRotatesSessionIdAndOldIdStopsWorking() {
        ApiResponse first = login();
        String oldId = sessionId(first.setCookie());

        ApiResponse second = send("/api/auth/login", "POST", LOGIN_BODY, cookiePair(first.setCookie()));
        assertEquals(200, second.status(), second.body());
        assertNotNull(second.setCookie(), "Login ulang harus mengirim cookie session ID baru");
        String newId = sessionId(second.setCookie());
        assertNotEquals(oldId, newId);

        ApiResponse oldSession = send("/api/auth/me", "GET", null, COOKIE_NAME + "=" + oldId);
        assertEquals(401, oldSession.status(), oldSession.body());
    }

    @Test
    void parallelRequestsAfterLoginDoNotInvalidateSessionCookie() {
        ApiResponse login = login();
        String sessionCookie = cookiePair(login.setCookie());

        List<CompletableFuture<ApiResponse>> futures = IntStream.range(0, 5)
                .mapToObj(index -> CompletableFuture.supplyAsync(
                        () -> send("/api/auth/me", "GET", null, sessionCookie)))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        for (CompletableFuture<ApiResponse> future : futures) {
            ApiResponse response = future.join();
            assertEquals(200, response.status(), response.body());
            assertNull(response.setCookie(),
                    "Request setelah login tidak boleh mengganti cookie sesi yang masih berlaku");
        }
    }

    @Test
    void changePasswordRotatesSessionId() {
        ApiResponse login = login();
        String before = sessionId(login.setCookie());

        ApiResponse changed = send(
                "/api/auth/change-password", "POST", CHANGE_PASSWORD_BODY, cookiePair(login.setCookie()));
        assertEquals(200, changed.status(), changed.body());
        assertNotNull(changed.setCookie(), "Ganti kata sandi harus mengirim cookie session ID baru");
        assertNotEquals(before, sessionId(changed.setCookie()));
    }

    @Test
    void logoutExpiresCookieAndInvalidatesSession() {
        ApiResponse login = login();
        String sessionCookie = cookiePair(login.setCookie());

        ApiResponse logout = send("/api/auth/logout", "POST", null, sessionCookie);
        assertEquals(204, logout.status(), logout.body());
        assertTrue(logout.setCookies().stream()
                        .anyMatch(cookie -> cookie.toLowerCase(Locale.ROOT).contains("max-age=0")),
                "Logout harus mengirim cookie dengan Max-Age=0");

        ApiResponse afterLogout = send("/api/auth/me", "GET", null, sessionCookie);
        assertEquals(401, afterLogout.status(), afterLogout.body());
    }

    private ApiResponse login() {
        ApiResponse response = send("/api/auth/login", "POST", LOGIN_BODY, null);
        assertEquals(200, response.status(), response.body());
        assertNotNull(response.setCookie(), "Login harus menghasilkan header Set-Cookie");
        return response;
    }

    private ApiResponse send(String path, String method, String body, String cookiePair) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("X-Requested-With", "XMLHttpRequest")
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) builder.header("Content-Type", "application/json");
        if (cookiePair != null) builder.header("Cookie", cookiePair);

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(
                    response.statusCode(),
                    response.headers().firstValue("Set-Cookie").orElse(null),
                    response.headers().allValues("Set-Cookie"),
                    response.body());
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal memanggil " + path, exception);
        }
    }

    private static String cookiePair(String setCookie) {
        return setCookie.substring(0, setCookie.indexOf(';')).trim();
    }

    private static String sessionId(String setCookie) {
        String pair = cookiePair(setCookie);
        assertTrue(pair.startsWith(COOKIE_NAME + "="));
        return pair.substring((COOKIE_NAME + "=").length());
    }

    private record ApiResponse(int status, String setCookie, List<String> setCookies, String body) {
    }
}
