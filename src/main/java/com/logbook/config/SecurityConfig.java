package com.logbook.config;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            SessionAuthenticationFilter sessionAuthenticationFilter,
            Clock clock
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/tasks/master-data/**").authenticated()
                .requestMatchers("/api/examples/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // Rotasi sudah dilakukan eksplisit oleh AuthService saat login dan
                // ganti kata sandi. Rotasi tambahan oleh Spring Security pada
                // request pertama setelah login membuat request parallel saling
                // meng-invalidasi cookie sesi dan memicu logout spontan.
                .sessionFixation(fixation -> fixation.none())
            )
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, clock, HttpStatus.UNAUTHORIZED, "Silakan login terlebih dahulu"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeError(response, clock, HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke resource ini"))
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * Respons Security mengikuti envelope standar API supaya konsisten dengan
     * GlobalExceptionHandler (code, status, message, timestamp, requestId).
     */
    private void writeError(
            HttpServletResponse response,
            Clock clock,
            HttpStatus status,
            String message
    ) throws IOException {
        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = "";
        }

        String body = String.format(
                "{\"code\":%d,\"status\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"requestId\":\"%s\"}",
                status.value(),
                status.name(),
                message,
                OffsetDateTime.now(clock),
                requestId
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }
}
