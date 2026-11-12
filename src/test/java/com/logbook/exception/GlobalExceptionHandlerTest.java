package com.logbook.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpSession session;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void testHandleApiException() {
        when(session.getAttribute("email")).thenReturn("user@example.com");
        ApiException ex = new ApiException(HttpStatus.NOT_FOUND, "Resource tidak ditemukan");

        ResponseEntity<?> response = exceptionHandler.handleApiException(ex, session);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(404, body.get("code"));
        assertEquals("NOT_FOUND", body.get("status"));
        assertEquals("Resource tidak ditemukan", body.get("message"));

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("user@example.com")));
    }

    @Test
    void testHandleAllExceptions() {
        when(session.getAttribute("email")).thenReturn(null);
        when(session.getAttribute("userEmail")).thenReturn("user2@example.com");
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<?> response = exceptionHandler.handleAllExceptions(ex, session);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(500, body.get("code"));
        assertEquals("INTERNAL_SERVER_ERROR", body.get("status"));

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("user2@example.com")));
    }

    @Test
    void testHandleValidationExceptionsWithFieldError() {
        when(session.getAttribute("email")).thenReturn(null);
        when(session.getAttribute("userEmail")).thenReturn(null);

        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "Email tidak valid");
        when(bindingResult.getFieldError()).thenReturn(fieldError);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<?> response = exceptionHandler.handleValidationExceptions(ex, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400, body.get("code"));
        assertEquals("BAD_REQUEST", body.get("status"));
        assertEquals("email", body.get("field"));
        assertEquals("Email tidak valid", body.get("message"));

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains("Anonymous")));
    }

    @Test
    void testHandleValidationExceptionsWithoutFieldError() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldError()).thenReturn(null);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<?> response = exceptionHandler.handleValidationExceptions(ex, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("unknown", body.get("field"));
        assertEquals("Validasi gagal", body.get("message"));
    }

    @Test
    void testHandleJsonParseError() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Malformed JSON");

        ResponseEntity<?> response = exceptionHandler.handleJsonParseError(ex, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400, body.get("code"));
        assertEquals("Format request body tidak valid atau tipe data salah.", body.get("message"));
    }

    @Test
    void testHandleSpringValidationException() {
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getMessage()).thenReturn("Validation error");

        ResponseEntity<?> response = exceptionHandler.handleSpringValidationException(ex, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400, body.get("code"));
        assertEquals("Data yang dikirim tidak valid.", body.get("message"));
    }

    @Test
    void testHandleNotFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("Not found");

        ResponseEntity<?> response = exceptionHandler.handleNotFound(ex, session);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(404, body.get("code"));
    }

    @Test
    void testHandleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses dilarang");

        ResponseEntity<?> response = exceptionHandler.handleResponseStatusException(ex, session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(403, body.get("code"));
        assertEquals("Akses dilarang", body.get("message"));
    }
}
