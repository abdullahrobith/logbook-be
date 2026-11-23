package com.logbook.exception;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

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

    // Sama dengan ID yang ditaruh RequestIdFilter di MDC dan di header respons.
    private String currentRequestId() {
        String requestId = MDC.get("requestId");
        return requestId == null ? "" : requestId;
    }

    private Map<String, Object> baseErrorBody(int code, String status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("status", status);
        body.put("message", message);
        body.put("timestamp", OffsetDateTime.now(clock).toString());
        body.put("requestId", currentRequestId());
        return body;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "param";
            String leafField = path.contains(".")
                    ? path.substring(path.lastIndexOf('.') + 1)
                    : path;
            fieldErrors.putIfAbsent(leafField, violation.getMessage());
        }

        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse("Validasi parameter gagal.");

        log.warn("[User: {}] ConstraintViolation: {}", userEmail, message);

        Map<String, Object> body = baseErrorBody(400, "BAD_REQUEST", message);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, HttpSession session) {
        FieldError fieldError = ex.getBindingResult().getFieldError();

        String fieldName = (fieldError != null) ? fieldError.getField() : "unknown";
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Validasi gagal";
        String userEmail = getUserEmail(session);

        // 1. Ambil semua field errors
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        // 2. LOG WARN dengan format [User: email]
        log.warn("[User: {}] Validasi gagal pada field '{}': {}", userEmail, fieldName, errorMessage);

        // 3. Gabungin response body-nya
        Map<String, Object> body = baseErrorBody(400, "BAD_REQUEST", errorMessage);
        body.put("field", fieldName);
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(MissingServletRequestParameterException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        String message = "Parameter " + ex.getParameterName() + " wajib diisi";
        log.warn("[User: {}] {}", userEmail, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseErrorBody(400, "BAD_REQUEST", message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        String message = "Nilai parameter " + ex.getName() + " tidak valid";
        log.warn("[User: {}] {}", userEmail, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseErrorBody(400, "BAD_REQUEST", message));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.error("[User: {}] Error Bisnis: {} - {}", userEmail, ex.getStatus().value(), ex.getMessage());

        Object code = ex instanceof PasswordChangeRequiredException passwordException
                ? passwordException.code()
                : ex instanceof CsrfHeaderRequiredException csrfException
                    ? csrfException.code()
                    : ex.getStatus().value();

        Map<String, Object> body = baseErrorBody(ex.getStatus().value(), ex.getStatus().name(), ex.getMessage());
        body.put("code", code);

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParseError(HttpMessageNotReadableException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Format JSON tidak valid: {}", userEmail, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseErrorBody(400, "BAD_REQUEST", "Format request body tidak valid atau tipe data salah."));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleSpringValidationException(HandlerMethodValidationException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Validasi Spring gagal: {}", userEmail, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseErrorBody(400, "BAD_REQUEST", "Data yang dikirim tidak valid."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(NoResourceFoundException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Endpoint tidak ditemukan: {}", userEmail, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(baseErrorBody(404, "NOT_FOUND", "Endpoint API tidak ditemukan. Pastikan URL sudah benar."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.warn("[User: {}] Error Status: {} - {}", userEmail, ex.getStatusCode().value(), ex.getReason());

        String message = ex.getReason() != null ? ex.getReason() : "Terjadi kesalahan pada permintaan.";
        return ResponseEntity.status(ex.getStatusCode())
                .body(baseErrorBody(ex.getStatusCode().value(), ex.getStatusCode().toString(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, HttpSession session) {
        String userEmail = getUserEmail(session);
        log.error("[User: {}] Terjadi Error Sistem: {}", userEmail, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(baseErrorBody(500, "INTERNAL_SERVER_ERROR", "Terjadi kesalahan pada server. Silakan hubungi admin."));
    }
}