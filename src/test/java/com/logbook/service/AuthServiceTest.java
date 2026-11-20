package com.logbook.service;

import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.UserRepository;
import com.logbook.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;
    private User user;
    private MockHttpSession session;
    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleService, passwordEncoder);
        session = new MockHttpSession();
        httpRequest = new MockHttpServletRequest();
        httpRequest.setSession(session);

        user = new User();
        user.setId(1L);
        user.setEmail("rina.talent@contoh.id");
        user.setFullName("Rina Talent");
        user.setPasswordHash("$2a$10$demoHash");
        user.setMustChangePassword(false);
        user.setActive(true);
    }

    @Test
    void loginWithCorrectPasswordCreatesSession() {
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("rina.talent@contoh.id"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPasswordHash())).thenReturn(true);
        when(roleService.rolesOf(user)).thenReturn(Set.of(Role.TALENT));

        var response = authService.login("rina.talent@contoh.id", "secret", session, httpRequest);

        assertEquals(1L, response.id());
        assertEquals(1L, session.getAttribute(AuthService.SESSION_USER_ID));
        assertEquals("rina.talent@contoh.id", session.getAttribute("email"));
    }

    @Test
    void loginWithWrongPasswordThrowsGenericUnauthorized() {
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("rina.talent@contoh.id"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.login("rina.talent@contoh.id", "salah", session, httpRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Email atau kata sandi salah", exception.getMessage());
        verify(roleService, never()).rolesOf(user);
    }

    @Test
    void loginWithMissingPasswordHashThrowsUnauthorized() {
        user.setPasswordHash(null);
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("rina.talent@contoh.id"))
                .thenReturn(Optional.of(user));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.login("rina.talent@contoh.id", "secret", session, httpRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void loginWithUnknownEmailThrowsUnauthorized() {
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("unknown@contoh.id"))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.login("unknown@contoh.id", "secret", session, httpRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Email atau kata sandi salah", exception.getMessage());
    }
}
