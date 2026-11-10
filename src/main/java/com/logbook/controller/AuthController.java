package com.logbook.controller;

import com.logbook.dto.LoginRequest;
import com.logbook.dto.RoleResponse;
import com.logbook.dto.UserResponse;
import com.logbook.service.AuthService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RoleService roleService;
    private final String sessionCookieName;

    public AuthController(
            AuthService authService,
            RoleService roleService,
            @Value("${server.servlet.session.cookie.name:LOGBOOK_SESSION}") String sessionCookieName
    ) {
        this.authService = authService;
        this.roleService = roleService;
        this.sessionCookieName = sessionCookieName;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return authService.login(request.email(), session);
    }

    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        return authService.me(session);
    }

    /**
     * Informasi peran pengguna saat ini, dihitung dari data pada setiap
     * permintaan. Frontend memakai endpoint ini untuk menyusun menu per peran.
     */
    @GetMapping("/roles")
    public RoleResponse roles(HttpSession session) {
        return roleService.currentRoles(session);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session, HttpServletResponse response) {
        authService.logout(session);

        ResponseCookie expiredCookie = ResponseCookie.from(sessionCookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }
}
