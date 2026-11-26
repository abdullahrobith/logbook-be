package com.logbook.filter;

import com.logbook.repository.UserRepository; 
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor // Ini buat inject repository lewat constructor
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private final UserRepository userRepository; // Inject Repository

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put("requestId", requestId);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                Object principal = auth.getPrincipal();
                String emailUser = null;

                // JIKA PRINCIPAL ADALAH LONG (ID User)
                if (principal instanceof Long) {
                    Long userId = (Long) principal;
                    // Cari email di database berdasarkan ID
                    emailUser = userRepository.findById(userId)
                            .map(user -> user.getEmail()) // Ambil email
                            .orElse(auth.getName());      // Kalau gak nemu, balikin ke ID
                } 
                // Kalau ternyata principal adalah String (kadang beberapa JWT config begini)
                else if (principal instanceof String) {
                    emailUser = auth.getName(); // Atau sesuaikan dengan logika JWT kamu
                }

                // Masukkan ke MDC
                MDC.put("email", emailUser != null ? emailUser : auth.getName());
            }

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            String uri = request.getRequestURI()
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString());

            log.info("{} {} selesai dengan status {} dalam {} ms",
                    request.getMethod(), uri, response.getStatus(), elapsedMs);

            MDC.remove("requestId");
            MDC.remove("email");
        }
    }

    private String resolveRequestId(String header) {
        if (header != null && VALID_REQUEST_ID.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}