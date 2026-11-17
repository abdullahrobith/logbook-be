package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.logbook.entity.UserSettings;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pengaturan pengguna. {@code activeDate} adalah hari aktif menurut jam kerja
 * pengguna: pada shift malam yang melewati tengah malam, nilainya masih tanggal
 * mulai shift sampai {@code workEnd} tiba. Frontend memakai nilai ini agar
 * tanggal yang dapat diisi sama dengan yang dihitung server.
 */
public record SettingsResponse(
        @JsonFormat(pattern = "HH:mm")
        LocalTime workStart,
        @JsonFormat(pattern = "HH:mm")
        LocalTime workEnd,
        boolean onboarded,
        LocalDate activeDate
) {
    public static SettingsResponse from(UserSettings settings, LocalDate activeDate) {
        return new SettingsResponse(
                settings.getWorkStart(),
                settings.getWorkEnd(),
                settings.isOnboarded(),
                activeDate
        );
    }
}
