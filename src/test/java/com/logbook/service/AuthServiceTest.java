package com.logbook.service;

import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.exception.PasswordChangeRequiredException;
import com.logbook.repository.UserRepository;
import com.logbook.security.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import static org.mockito.Mockito.mock;
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

    @Mock
    private RateLimitService rateLimitService;

    private AuthService authService;
    private User user;
    private MockHttpSession session;
    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleService, passwordEncoder, rateLimitService);
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

    @Test
    void loginReturnsMustChangePasswordFlag() {
        user.setMustChangePassword(true);
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("rina.talent@contoh.id"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPasswordHash())).thenReturn(true);
        when(roleService.rolesOf(user)).thenReturn(Set.of(Role.TALENT));

        var response = authService.login("rina.talent@contoh.id", "secret", session, httpRequest);

        assertEquals(true, response.mustChangePassword());
    }

    @Test
    void changePasswordSavesNewHashAndClearsFlag() {
        user.setMustChangePassword(true);
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        when(userRepository.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");
        when(roleService.rolesOf(user)).thenReturn(Set.of(Role.TALENT));

        var response = authService.changePassword(session, "oldPassword", "newPassword", httpRequest);

        assertEquals("newHash", user.getPasswordHash());
        assertEquals(false, user.isMustChangePassword());
        assertEquals(false, response.mustChangePassword());
        assertEquals(user.getId(), session.getAttribute(AuthService.SESSION_USER_ID));
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        when(userRepository.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.changePassword(session, "salah", "newPassword", httpRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePasswordRejectsSamePassword() {
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        when(userRepository.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("samePassword", user.getPasswordHash())).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.changePassword(session, "samePassword", "samePassword", httpRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void requireUserIdRejectsUserWhoMustChangePassword() {
        user.setMustChangePassword(true);
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        when(userRepository.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));

        assertThrows(
                PasswordChangeRequiredException.class,
                () -> authService.requireUserId(session)
        );
    }

    @Test
    void meRemainsAvailableWhilePasswordChangeIsRequired() {
        user.setMustChangePassword(true);
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        when(userRepository.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));
        when(roleService.rolesOf(user)).thenReturn(Set.of(Role.TALENT));

        var response = authService.me(session);

        assertEquals(true, response.mustChangePassword());
    }

    @Test
    void loginRotatesSessionIdBeforeWritingAttributes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue("rina.talent@contoh.id"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPasswordHash())).thenReturn(true);
        when(roleService.rolesOf(user)).thenReturn(Set.of(Role.TALENT));

        authService.login("rina.talent@contoh.id", "secret", session, request);

        verify(request).changeSessionId();
        assertEquals(user.getId(), session.getAttribute(AuthService.SESSION_USER_ID));
    }

    @Test
    void changePasswordRotatesSessionId() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        when(userRepository.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");
        when(roleService.rolesOf(user)).thenReturn(Set.of(Role.TALENT));

        authService.changePassword(session, "oldPassword", "newPassword", request);

        verify(request).changeSessionId();
        assertEquals("newHash", user.getPasswordHash());
    }

    @Test
    void logoutInvalidatesSession() {
        HttpSession logoutSession = mock(HttpSession.class);
        when(logoutSession.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(user.getId());

        authService.logout(logoutSession);

        verify(logoutSession).invalidate();
    }
}