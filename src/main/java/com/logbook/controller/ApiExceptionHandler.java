package com.logbook.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse("Data yang dikirim tidak valid.");

        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse(message, fieldErrors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception
    ) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() == null
                ? "Permintaan tidak dapat diproses."
                : exception.getReason();

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(message));
    }

    public record ValidationErrorResponse(
            String message,
            Map<String, String> fieldErrors
    ) {
    }

    public record ApiErrorResponse(String message) {
    }
}
