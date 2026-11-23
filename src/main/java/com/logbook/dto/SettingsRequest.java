package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalTime;

public record SettingsRequest(
        @JsonFormat(pattern = "HH:mm")
        LocalTime workStart,
        @JsonFormat(pattern = "HH:mm")
        LocalTime workEnd,
        Boolean onboarded
) {
    /**
     * Jam kerja boleh diisi sebagian (partial update). Jika keduanya dikirim,
     * jam mulai harus lebih awal dari jam selesai.
     */
    @AssertTrue(message = "Jam mulai harus lebih awal dari jam selesai")
    public boolean isWorkHoursValid() {
        return workStart == null || workEnd == null || workStart.isBefore(workEnd);
    }
}
