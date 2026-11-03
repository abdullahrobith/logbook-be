package com.logbook.service;

import com.logbook.dto.UserResponse;
import com.logbook.entity.User;
import com.logbook.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse login(String email, HttpSession session) {
        User user = userRepository
                .findByEmailIgnoreCaseAndActiveTrue(email.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email tidak terdaftar atau tidak aktif"
                ));

        session.setAttribute(SESSION_USER_ID, user.getId());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Belum login");
        }

        return userRepository
                .findByIdAndActiveTrue(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Sesi tidak valid"
                ));
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
