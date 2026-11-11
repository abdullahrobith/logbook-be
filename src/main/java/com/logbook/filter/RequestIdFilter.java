package com.logbook.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Menempelkan requestId pada MDC sehingga seluruh baris log selama satu
 * permintaan dapat ditelusuri, kemudian mencatat ringkasan permintaan
 * (metode, URI, status, dan durasi) sebagai log akses.
 */
@Slf4j
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            String uri = request.getRequestURI()
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString());

            log.info("{} {} selesai dengan status {} dalam {} ms",
                    request.getMethod(), uri, response.getStatus(), elapsedMs);

            MDC.remove("requestId");
        }
    }
}
