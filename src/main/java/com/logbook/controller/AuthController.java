package com.logbook.controller;

import com.logbook.dto.LoginRequest;
import com.logbook.dto.UserResponse;
import com.logbook.service.AuthService;
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
    private final String sessionCookieName;

    public AuthController(
            AuthService authService,
            @Value("${server.servlet.session.cookie.name:LOGBOOK_SESSION}") String sessionCookieName
    ) {
        this.authService = authService;
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
