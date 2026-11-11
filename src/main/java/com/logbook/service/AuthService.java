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
    private final RoleService roleService;

    public AuthService(UserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
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
        session.setAttribute("email", user.getEmail());
        return UserResponse.from(user, roleService.rolesOf(user));
    }

    public Long requireUserId(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Long userId)) {
            throw new ResponseStatusException(
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Sesi tidak valid"
                ));

        return UserResponse.from(user, roleService.rolesOf(user));
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
