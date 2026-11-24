package com.logbook.interceptor;

import com.logbook.exception.CsrfHeaderRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Menegakkan SRS LBK-AUT-04: setiap permintaan API selain GET wajib membawa
 * header {@code X-Requested-With: XMLHttpRequest}.
 *
 * <p>OPTIONS dikecualikan karena dipakai untuk preflight CORS sehingga tidak
 * membawa header kustom dari browser. GET diizinkan tanpa header karena bersifat
 * aman dan dipakai untuk navigasi serta bootstrap sesi.</p>
 */
public class CsrfInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Requested-With";
    public static final String HEADER_VALUE = "XMLHttpRequest";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String header = request.getHeader(HEADER_NAME);
        if (header != null && HEADER_VALUE.equalsIgnoreCase(header.trim())) {
            return true;
        }

        throw new CsrfHeaderRequiredException();
    }
}
