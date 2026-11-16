package com.logbook.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
                540, // 09:00
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
                null, 540, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void taskTypeIdMustNotBeNull() {
        LogTaskRequest request = new LogTaskRequest(
                1L, null, (short) 1,
                null, 540, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void workStatusIdMustNotBeNull() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, null,
                null, 540, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void invalidTicketFormatShouldFail() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                "ABC154", 540, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void startMinuteMustNotBeNull() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, null, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void startMinuteMustSupportCrossMidnightRange() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, 1500, (short) 120, // 25:00 (lintas tengah malam)
                "Mengerjakan fitur", null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void startMinuteAboveRangeShouldFail() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, 2880, (short) 120,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void durationMustBePositive() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, 540, (short) 0,
                "Mengerjakan fitur", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void summaryMustNotBeBlank() {
        LogTaskRequest request = new LogTaskRequest(
                1L, (short) 1, (short) 1,
                null, 540, (short) 120,
                "   ", null
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}