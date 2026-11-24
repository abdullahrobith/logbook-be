package com.logbook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Memastikan profil produksi menghasilkan cookie sesi bertanda Secure.
 * Cookie dibaca langsung dari header karena pengujian memakai HTTP lokal.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=prod", "logbook.jadwal.aktif=false"})
class ProductionSessionCookieTest {

    private static final String LOGIN_BODY = """
            {"email":"rina.talent@contoh.id","password":"Logbook@2026"}
            """;

    @Value("${local.server.port}")
    private int port;

    @Test
    void loginCookieHasSecureAttributeInProdProfile() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Requested-With", "XMLHttpRequest")
                .POST(HttpRequest.BodyPublishers.ofString(LOGIN_BODY))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());

        String setCookie = response.headers().firstValue("Set-Cookie").orElse(null);
        assertNotNull(setCookie, "Login harus menghasilkan header Set-Cookie");
        String lower = setCookie.toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("secure"));
        assertTrue(lower.contains("httponly"));
        assertTrue(lower.contains("samesite=lax"));
    }
}
