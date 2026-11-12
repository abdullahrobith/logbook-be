package com.logbook.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import com.logbook.controller.AdminController;
import com.logbook.filter.RequestIdFilter;
import com.logbook.service.RoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingTest {

    @InjectMocks
    private RequestIdFilter requestIdFilter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RoleService roleService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private HttpSession session;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger rootLogger;

    @BeforeEach
    void setUp() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        listAppender = new ListAppender<>();
        listAppender.setContext(context);
        listAppender.start();
        rootLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(listAppender);
        listAppender.stop();
        MDC.clear();
    }

    @Test
    void testLogbackConfigurationAppenders() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        Appender<ILoggingEvent> consoleAppender = root.getAppender("CONSOLE");
        assertNotNull(consoleAppender, "Appender CONSOLE harus terdaftar pada root logger");
    }

    @Test
    void testRequestIdFilterInjectsAndCleansMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedRequestId.set(MDC.get("requestId"));
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        requestIdFilter.doFilter(request, response, filterChain);

        assertNotNull(capturedRequestId.get(), "MDC requestId harus terisi saat request berjalan");
        assertEquals(8, capturedRequestId.get().length(), "MDC requestId harus terdiri dari 8 karakter");
        assertNull(MDC.get("requestId"), "MDC requestId harus dibersihkan setelah request selesai");

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLoggerName().equals(RequestIdFilter.class.getName())
                        && event.getFormattedMessage().contains("GET /api/test selesai dengan status 200")));
    }

    @Test
    void testRequestIdFilterCleansMdcOnException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/fail");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new RuntimeException("Simulated error"))
                .when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        assertThrows(RuntimeException.class, () -> requestIdFilter.doFilter(request, response, filterChain));
        assertNull(MDC.get("requestId"), "MDC requestId harus tetap dibersihkan meskipun terjadi exception");
    }

    @Test
    void testAdminControllerLogsInfoAndWarn() {
        AdminController adminController = new AdminController(jdbcTemplate, roleService);

        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        assertThrows(ResponseStatusException.class, () -> adminController.importUsers(emptyFile, session));

        assertThrows(ResponseStatusException.class, () -> adminController.addHoliday(Map.of(), session));

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLoggerName().equals(AdminController.class.getName())
                        && event.getFormattedMessage().contains("Menerima request import CSV")));

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLoggerName().equals(AdminController.class.getName())
                        && event.getFormattedMessage().contains("Validasi gagal: Tanggal atau Nama libur kosong")));
    }
}
