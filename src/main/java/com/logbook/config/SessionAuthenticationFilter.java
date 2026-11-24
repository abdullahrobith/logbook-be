package com.logbook.config;

import com.logbook.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Menjembatani sesi aplikasi (atribut {@code userId}) ke SecurityContext
 * Spring Security. Filter ini bisa dipakai di test slice yang tidak memuat
 * konfigurasi Security penuh, jadi tidak bergantung pada bean tambahan.
 */
@Slf4j
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(AuthService.SESSION_USER_ID);
        log.debug("SessionAuthenticationFilter uri={} session={} userId={} existingAuth={}",
                request.getRequestURI(), session != null, userId,
                SecurityContextHolder.getContext().getAuthentication() != null);

        if (session != null && SecurityContextHolder.getContext().getAuthentication() == null && userId != null) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    AuthorityUtils.NO_AUTHORITIES
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
        }

        filterChain.doFilter(request, response);
    }
}
