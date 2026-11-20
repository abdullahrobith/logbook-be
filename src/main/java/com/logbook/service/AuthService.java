package com.logbook.service;

import com.logbook.dto.UserResponse;
import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    public static final String SESSION_USER_ID = "userId";

    private static final String INVALID_CREDENTIALS = "Email atau kata sandi salah";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse login(String email, String password, HttpSession session, HttpServletRequest httpRequest) {
        User user = userRepository
                .findByEmailIgnoreCaseAndActiveTrue(email.trim())
                .orElseThrow(() -> {
                    log.warn("Percobaan login gagal untuk email: {}", email.trim());
                    return invalidCredentials();
                });

        verifyPassword(user, password);

        httpRequest.changeSessionId();
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute("email", user.getEmail());
        log.info("Login berhasil untuk User ID {} ({})", user.getId(), user.getEmail());

        return UserResponse.from(user, roleService.rolesOf(user));
    }

    private void verifyPassword(User user, String rawPassword) {
        String storedHash = user.getPasswordHash();

        if (storedHash == null || storedHash.isBlank() || !storedHash.startsWith("$2")) {
            log.warn("Percobaan login gagal: User ID {} belum memiliki hash kata sandi yang valid", user.getId());
            throw invalidCredentials();
        }

        try {
            if (!passwordEncoder.matches(rawPassword, storedHash)) {
                throw invalidCredentials();
            }
        } catch (IllegalArgumentException exception) {
            log.warn("Percobaan login gagal: hash kata sandi User ID {} tidak dikenali", user.getId());
            throw invalidCredentials();
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }

    public Long requireUserId(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Long userId)) {
            log.debug("Request tanpa sesi login yang valid");
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Silakan login terlebih dahulu"
            );
        }
        return userId;
    }

    @Transactional(readOnly = true)
    public UserResponse me(HttpSession session) {
        Long userId = requireUserId(session);

        User user = userRepository
                .findByIdAndActiveTrue(userId)
                .orElseThrow(() -> {
                    log.warn("Sesi tidak valid: User ID {} tidak ditemukan atau tidak aktif", userId);
                    return new ApiException(
                            HttpStatus.UNAUTHORIZED,
                            "Sesi tidak valid"
                    );
                });

        return UserResponse.from(user, roleService.rolesOf(user));
    }

    public void logout(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_ID);
        session.invalidate();

        if (value instanceof Long userId) {
            log.info("Logout berhasil untuk User ID {}", userId);
        } else {
            log.info("Logout untuk sesi tanpa User ID aktif");
        }
    }
}