package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record SettingsRequest(
        @JsonFormat(pattern = "HH:mm")
        LocalTime workStart,
        @JsonFormat(pattern = "HH:mm")
        LocalTime workEnd,
        Boolean onboarded
) {
}
