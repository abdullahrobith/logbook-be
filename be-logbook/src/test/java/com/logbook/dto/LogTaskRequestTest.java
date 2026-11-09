package com.logbook.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LogTaskRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private LogTaskRequest validRequest() {
        return new LogTaskRequest(
                1L,
                (short) 1,
                (short) 1,
                "ABC-154",
                LocalTime.of(9, 0),
                (short) 120,
                "Mengerjakan pengembangan fitur.",
                null
        );
    }

    @Test
    void validRequestShouldPass() {
        Set<?> violations = validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void projectIdMustBePositive() {
        LogTaskRequest request = new LogTaskRequest(
                0L, (short) 1, (short) 1,
                null, LocalTime.of(9, 0), (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void taskTypeIdMustNotBeNull() {
        LogTaskRequest request = new LogTaskRequest(
                1L, null, (short) 1,
                null, LocalTime.of(9, 0), (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void workStatusIdMustNotBeNull() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, null,
                null, LocalTime.of(9, 0), (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void invalidTicketFormatShouldFail() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                "ABC154", LocalTime.of(9, 0), (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void startTimeMustNotBeNull() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, null, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void durationMustBePositive() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, LocalTime.of(9, 0), (short) 0,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void summaryMustNotBeBlank() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, LocalTime.of(9, 0), (short) 120,
                "   ", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}