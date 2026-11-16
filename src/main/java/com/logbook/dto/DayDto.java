package com.logbook.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DayDto(
        LocalDate date,
        String status,
        String statusLabel,
        boolean editable,
        OffsetDateTime deadline,
        String dayLabel,
        RangeDto range,
        int totalMinutes,
        List<PerProjectDto> perProject,
        List<TaskDto> tasks,
        String rejectReason
) {
    public record RangeDto(Integer startMinute, Integer endMinute) {}

    public record PerProjectDto(String name, String color, int minutes) {}
}
