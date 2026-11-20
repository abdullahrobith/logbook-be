package com.logbook.interceptor;

import com.logbook.exception.PasswordChangeRequiredException;
import com.logbook.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Menegakkan flag wajib ganti kata sandi untuk seluruh endpoint API.
 *
 * <p>Endpoint yang dipakai untuk login, melihat profil sendiri, ganti kata
 * sandi, dan logout sengaja dikecualikan agar pengguna masih bisa menyelesaikan
 * alur ganti kata sandi.</p>
 */
public class PasswordChangeInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/me",
            "/api/auth/change-password",
            "/api/auth/logout"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (ALLOWED_PATHS.contains(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null
                || !Boolean.TRUE.equals(session.getAttribute(AuthService.PASSWORD_CHANGE_REQUIRED))) {
            return true;
        }

        throw new PasswordChangeRequiredException();
    }
}
