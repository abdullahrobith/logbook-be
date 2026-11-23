package com.logbook.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // Validasi dan ResponseStatusException (termasuk ApiException) sengaja
    // ditangani GlobalExceptionHandler agar semua error memakai envelope
    // standar: code, status, message, timestamp, dan requestId.

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception
    ) {
        log.warn("Parameter request hilang: {}", exception.getParameterName());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "Parameter " + exception.getParameterName() + " wajib diisi"
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        log.warn("Nilai parameter '{}' tidak valid", exception.getName());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "Nilai parameter " + exception.getName() + " tidak valid"
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        log.warn("Akses ditolak: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("Silakan login terlebih dahulu"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Akses ditolak (Forbidden): {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("Anda tidak memiliki akses ke resource ini"));
    }

    public record ApiErrorResponse(String message) {
    }
}