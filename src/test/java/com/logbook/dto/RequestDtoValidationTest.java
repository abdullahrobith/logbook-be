package com.logbook.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void decisionRequestAcceptsNonBlankReasonWithinLimit() {
        assertTrue(validator.validate(new DecisionRequest("Ringkasan kurang rinci")).isEmpty());
        assertTrue(validator.validate(new DecisionRequest("a".repeat(1000))).isEmpty());
    }

    @Test
    void decisionRequestRejectsBlankReason() {
        Set<String> messages = messages(validator.validate(new DecisionRequest("   ")));
        assertTrue(messages.contains("Alasan wajib diisi"));
    }

    @Test
    void decisionRequestRejectsReasonLongerThanColumn() {
        Set<String> messages = messages(validator.validate(new DecisionRequest("a".repeat(1001))));
        assertTrue(messages.contains("Alasan maksimal 1000 karakter"));
    }

    @Test
    void settingsRequestAcceptsPartialUpdate() {
        assertTrue(validator.validate(new SettingsRequest(null, null, true)).isEmpty());
        assertTrue(validator.validate(new SettingsRequest(LocalTime.of(9, 0), null, null)).isEmpty());
        assertTrue(validator.validate(new SettingsRequest(null, LocalTime.of(18, 0), false)).isEmpty());
    }

    @Test
    void settingsRequestAcceptsValidWorkHours() {
        assertTrue(validator.validate(
                new SettingsRequest(LocalTime.of(7, 30), LocalTime.of(16, 30), true)
        ).isEmpty());
    }

    @Test
    void settingsRequestRejectsWorkStartNotBeforeWorkEnd() {
        Set<String> after = messages(validator.validate(
                new SettingsRequest(LocalTime.of(18, 0), LocalTime.of(9, 0), null)
        ));
        assertTrue(after.contains("Jam mulai harus lebih awal dari jam selesai"));

        Set<String> same = messages(validator.validate(
                new SettingsRequest(LocalTime.of(9, 0), LocalTime.of(9, 0), null)
        ));
        assertFalse(same.isEmpty());
        assertTrue(same.contains("Jam mulai harus lebih awal dari jam selesai"));
    }
}
