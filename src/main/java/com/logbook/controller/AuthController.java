package com.logbook.controller;

import com.logbook.dto.ChangePasswordRequest;
import com.logbook.dto.ForgotPasswordRequest;
import com.logbook.dto.LoginRequest;
import com.logbook.dto.ResetPasswordRequest;
import com.logbook.dto.RoleResponse;
import com.logbook.dto.UserResponse;
import com.logbook.service.AuthService;
import com.logbook.service.PasswordResetService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RoleService roleService;
    private final PasswordResetService passwordResetService;
    private final Environment environment;
    private final String sessionCookieName;
    private final boolean sessionCookieSecure;
    private final String sessionCookieSameSite;

    public AuthController(
            AuthService authService,
            RoleService roleService,
            PasswordResetService passwordResetService,
            Environment environment,
            @Value("${server.servlet.session.cookie.name:LOGBOOK_SESSION}") String sessionCookieName,
            @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure,
            @Value("${server.servlet.session.cookie.same-site:lax}") String sessionCookieSameSite
    ) {
        this.authService = authService;
        this.roleService = roleService;
        this.passwordResetService = passwordResetService;
        this.environment = environment;
        this.sessionCookieName = sessionCookieName;
        this.sessionCookieSecure = sessionCookieSecure;
        this.sessionCookieSameSite = sessionCookieSameSite;
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session,
            HttpServletRequest httpRequest
    ) {
        log.info("Menerima request POST /api/auth/login");
        return authService.login(request.email(), request.password(), session, httpRequest);
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Menerima request POST /api/auth/forgot-password");
        var result = passwordResetService.requestPasswordReset(request.email(), httpRequest);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", result.message());
        if (!environment.acceptsProfiles(Profiles.of("prod")) && result.token() != null) {
            body.put("devToken", result.token());
            log.warn("Token reset kata sandi dikirim di respons karena profil prod tidak aktif");
        }
        return body;
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Menerima request POST /api/auth/reset-password");
        String message = passwordResetService.confirmPasswordReset(
                request.token(),
                request.newPassword(),
                httpRequest
        );
        return Map.of("message", message);
    }

    @PostMapping("/change-password")
    public UserResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpSession session,
            HttpServletRequest httpRequest
    ) {
        log.info("Menerima request POST /api/auth/change-password");
        return authService.changePassword(
                session,
                request.currentPassword(),
                request.newPassword(),
                httpRequest
        );
    }

    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        log.info("Menerima request GET /api/auth/me");
        return authService.me(session);
    }

    /**
     * Informasi peran pengguna saat ini, dihitung dari data pada setiap
     * permintaan. Frontend memakai endpoint ini untuk menyusun menu per peran.
     */
    @GetMapping("/roles")
    public RoleResponse roles(HttpSession session) {
        log.info("Menerima request GET /api/auth/roles");
        return roleService.currentRoles(session);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session, HttpServletResponse response) {
        log.info("Menerima request POST /api/auth/logout");
        authService.logout(session);

        ResponseCookie expiredCookie = ResponseCookie.from(sessionCookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(sessionCookieSecure)
                .sameSite(sessionCookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }
}