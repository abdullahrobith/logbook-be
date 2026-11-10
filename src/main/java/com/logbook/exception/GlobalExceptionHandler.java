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
import java.util.Map;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        
        String fieldName = (fieldError != null) ? fieldError.getField() : "unknown";
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Validasi gagal";

        log.warn("Validasi gagal pada field '{}': {}", fieldName, errorMessage);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "field", fieldName, 
            "message", errorMessage
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex) {
        log.error("Error Bisnis: {} - {}", ex.getStatus().value(), ex.getMessage());
        
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "code", ex.getStatus().value(),
            "status", ex.getStatus().name(),
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        log.error("Terjadi Error Sistem: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "code", 500,
            "status", "INTERNAL_SERVER_ERROR",
            "message", "Terjadi kesalahan pada server. Silakan hubungi admin."
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParseError(HttpMessageNotReadableException ex) {
        log.warn("Format JSON tidak valid: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "message", "Format request body tidak valid atau tipe data salah."
        ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleSpringValidationException(HandlerMethodValidationException ex) {
        log.warn("Validasi Spring gagal: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "code", 400,
            "status", "BAD_REQUEST",
            "message", "Data yang dikirim tidak valid."
        ));
    }
}