package com.logbook.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import jakarta.servlet.http.HttpSession;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler {

    // Fungsi bantuan untuk narik email dari session
    private String getUserEmail(HttpSession session) {
        if (session != null) {
            Object emailAttr = session.getAttribute("email");
            if (emailAttr == null) {
                emailAttr = session.getAttribute("userEmail");
            }
            if (emailAttr != null) {
                return emailAttr.toString();
            }
        }
        return "Anonymous";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, HttpSession session) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        
        String fieldName = (fieldError != null) ? fieldError.getField() : "unknown";
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Validasi gagal";
        String userEmail = getUserEmail(session);

        // 1. Ambil semua field errors (Punya temenmu)
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        // 2. LOG WARN dengan format [User: email] (Punyamu)
        log.warn("[User: {}] Validasi gagal pada field '{}': {}", userEmail, fieldName, errorMessage);
        
        // 3. Gabungin response body-nya
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 400);
        body.put("status", "BAD_REQUEST");
        body.put("field", fieldName);
        body.put("message", errorMessage);
        body.put("fieldErrors", fieldErrors); // Ini buat Frontend
        body.put("timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.error("[User: {}] Error Bisnis: {} - {}", userEmail, ex.getStatus().value(), ex.getMessage());
        
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "code", ex.getStatus().value(),
            "status", ex.getStatus().name(),
            "message", ex.getMessage(),
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.error("[User: {}] Terjadi Error Sistem: {}", userEmail, ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "code", 500,
            "status", "INTERNAL_SERVER_ERROR",
            "message", "Terjadi kesalahan pada server. Silakan hubungi admin.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParseError(HttpMessageNotReadableException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Format JSON tidak valid: {}", userEmail, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "message", "Format request body tidak valid atau tipe data salah.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleSpringValidationException(HandlerMethodValidationException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Validasi Spring gagal: {}", userEmail, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "message", "Data yang dikirim tidak valid.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Endpoint tidak ditemukan: {}", userEmail, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "code", 404,
            "status", "NOT_FOUND",
            "message", "Endpoint API tidak ditemukan. Pastikan URL sudah benar.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Error Status: {} - {}", userEmail, ex.getStatusCode().value(), ex.getReason());
        
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "code", ex.getStatusCode().value(),
            "status", ex.getStatusCode().toString(),
            "message", ex.getReason() != null ? ex.getReason() : "Terjadi kesalahan pada permintaan.",
            "timestamp", java.time.OffsetDateTime.now().toString()
        ));
    }
}