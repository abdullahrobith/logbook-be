package com.logbook.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SESSION_COOKIE_SCHEME = "SessionCookieAuth";
    private static final String REQUESTED_WITH_SCHEME = "RequestedWithHeader";

    private final String sessionCookieName;

    public OpenApiConfig(
            @Value("${server.servlet.session.cookie.name:LOGBOOK_SESSION}") String sessionCookieName
    ) {
        this.sessionCookieName = sessionCookieName;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        String description = """
                Dokumentasi API Logbook dengan autentikasi berbasis Session Cookie (%s) \
                dan header wajib X-Requested-With: XMLHttpRequest.

                Alur penggunaan di Swagger UI / client:
                1. Panggil POST /api/auth/login dengan email dan password (browser/client akan menerima cookie %s).
                2. Sertakan header X-Requested-With: XMLHttpRequest pada permintaan yang membutuhkannya.
                3. Cookie %s otomatis dikirimkan oleh browser/HTTP client yang mendukung cookie pada endpoint terproteksi.
                """.formatted(sessionCookieName, sessionCookieName, sessionCookieName);

        return new OpenAPI()
                .info(new Info()
                        .title("Logbook API Padepokan HRMS 3.0")
                        .version("1.0")
                        .description(description))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SESSION_COOKIE_SCHEME)
                        .addList(REQUESTED_WITH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE_SCHEME,
                                new SecurityScheme()
                                        .name(sessionCookieName)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .description("Cookie sesi login bernama " + sessionCookieName + " yang di-set setelah POST /api/auth/login."))
                        .addSecuritySchemes(REQUESTED_WITH_SCHEME,
                                new SecurityScheme()
                                        .name("X-Requested-With")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Header wajib bernilai XMLHttpRequest.")));
    }
}