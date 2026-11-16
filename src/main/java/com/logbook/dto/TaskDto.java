package com.logbook.dto;

public record TaskDto(
        Long id,
        String project,
        String color,
        String ticket,
        Integer startMinute,
        Short durationMinutes,
        String type,
        String summary,
        String workStatus,
        String obstacle
) {
}
