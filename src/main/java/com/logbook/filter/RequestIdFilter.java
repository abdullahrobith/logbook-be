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
import java.util.regex.Pattern;

/**
 * Menempelkan requestId pada MDC sehingga seluruh baris log selama satu
 * permintaan dapat ditelusuri, kemudian mencatat ringkasan permintaan
 * (metode, URI, status, dan durasi) sebagai log akses.
 *
 * ID dari klien dipakai jika valid; jika tidak, filter membuat ID sendiri.
 * ID yang dipakai selalu dikirim balik lewat header X-Request-Id, termasuk
 * untuk respons galat 4xx/5xx.
 */
@Slf4j
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

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
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            String uri = request.getRequestURI()
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString());

            log.info("{} {} selesai dengan status {} dalam {} ms",
                    request.getMethod(), uri, response.getStatus(), elapsedMs);

            MDC.remove("requestId");
        }
    }

    private String resolveRequestId(String header) {
        if (header != null && VALID_REQUEST_ID.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
