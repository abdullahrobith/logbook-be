package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.logbook.entity.UserSettings;

import java.time.LocalTime;

public record SettingsResponse(
        @JsonFormat(pattern = "HH:mm")
        LocalTime workStart,
        @JsonFormat(pattern = "HH:mm")
        LocalTime workEnd,
        boolean onboarded
) {
    public static SettingsResponse from(UserSettings settings) {
        return new SettingsResponse(
                settings.getWorkStart(),
                settings.getWorkEnd(),
                settings.isOnboarded()
        );
    }
}
