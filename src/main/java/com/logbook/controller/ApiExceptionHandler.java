package com.logbook.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    // Validasi dan ResponseStatusException (termasuk ApiException) sengaja
    // ditangani GlobalExceptionHandler agar semua error memakai envelope
    // standar: code, status, message, timestamp, dan requestId.

    private String currentRequestId(HttpServletRequest request) {
        String mdcRequestId = MDC.get("requestId");
        if (mdcRequestId != null && !mdcRequestId.isBlank()) {
            return mdcRequestId;
        }
        String headerRequestId = request != null ? request.getHeader("X-Request-Id") : null;
        return headerRequestId != null ? headerRequestId : "";
    }

    private Map<String, Object> errorBody(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("status", status.name());
        body.put("message", message);
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("requestId", currentRequestId(request));
        return body;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        log.warn("Parameter request hilang: {}", exception.getParameterName());
        return ResponseEntity.badRequest()
                .body(errorBody(
                        HttpStatus.BAD_REQUEST,
                        "Parameter " + exception.getParameterName() + " wajib diisi",
                        request
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.warn("Nilai parameter '{}' tidak valid", exception.getName());
        return ResponseEntity.badRequest()
                .body(errorBody(
                        HttpStatus.BAD_REQUEST,
                        "Nilai parameter " + exception.getName() + " tidak valid",
                        request
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        log.warn("Akses ditolak: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(
                        HttpStatus.UNAUTHORIZED,
                        "Silakan login terlebih dahulu",
                        request
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn("Akses ditolak (Forbidden): {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(
                        HttpStatus.FORBIDDEN,
                        "Anda tidak memiliki akses ke resource ini",
                        request
                ));
    }
}