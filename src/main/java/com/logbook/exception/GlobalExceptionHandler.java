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

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        
        String fieldName = (fieldError != null) ? fieldError.getField() : "unknown";
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Validasi gagal";

        // Semua field yang gagal dikumpulkan agar frontend bisa menandai kolom
        // form yang salah (kontrak yang dibaca apiClient.ts).
        Map<String, String> fieldErrors = new java.util.LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validasi gagal pada field '{}': {}", fieldName, errorMessage);
        
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", 400);
        body.put("status", "BAD_REQUEST");
        body.put("field", fieldName);
        body.put("message", errorMessage);
        body.put("fieldErrors", fieldErrors);
        body.put("timestamp", OffsetDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex) {
        log.error("Error Bisnis: {} - {}", ex.getStatus().value(), ex.getMessage());
        
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "code", ex.getStatus().value(),
            "status", ex.getStatus().name(),
            "message", ex.getMessage(),
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        log.error("Terjadi Error Sistem: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "code", 500,
            "status", "INTERNAL_SERVER_ERROR",
            "message", "Terjadi kesalahan pada server. Silakan hubungi admin.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParseError(HttpMessageNotReadableException ex) {
        log.warn("Format JSON tidak valid: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "message", "Format request body tidak valid atau tipe data salah.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleSpringValidationException(HandlerMethodValidationException ex) {
        log.warn("Validasi Spring gagal: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "message", "Data yang dikirim tidak valid.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

     @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Endpoint tidak ditemukan: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "code", 404,
            "status", "NOT_FOUND",
            "message", "Endpoint API tidak ditemukan. Pastikan URL sudah benar.",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        log.warn("Error Status: {} - {}", ex.getStatusCode().value(), ex.getReason());
        
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "code", ex.getStatusCode().value(),
            "status", ex.getStatusCode().toString(),
            "message", ex.getReason() != null ? ex.getReason() : "Terjadi kesalahan pada permintaan.",
            "timestamp", java.time.OffsetDateTime.now().toString()
        ));
    }
}