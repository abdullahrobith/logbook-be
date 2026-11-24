package com.logbook.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void testHandleMissingParameter() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("file", "MultipartFile");
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<?> response = handler.handleMissingParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400, body.get("code"));
        assertEquals("BAD_REQUEST", body.get("status"));
        assertEquals("Parameter file wajib diisi", body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.containsKey("requestId"));
    }

    @Test
    void testHandleTypeMismatch() {
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", parameter, new IllegalArgumentException());
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<?> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400, body.get("code"));
        assertEquals("BAD_REQUEST", body.get("status"));
        assertEquals("Nilai parameter id tidak valid", body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.containsKey("requestId"));
    }
}