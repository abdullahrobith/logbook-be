package com.logbook.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "CookieAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Logbook API Padepokan HRMS 3.0")
                        .version("1.0")
                        .description("Dokumentasi API Logbook dengan autentikasi Sesi Cookie (LOGBOOK_SESSION). " +
                                     "Silakan login melalui endpoint POST /api/auth/login untuk mengaktifkan sesi browser."))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("LOGBOOK_SESSION")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .description("Cookie Sesi HTTP-only yang dihasilkan oleh endpoint POST /api/auth/login")));
    }
}
