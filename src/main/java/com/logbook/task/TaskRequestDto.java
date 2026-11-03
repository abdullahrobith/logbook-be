package com.logbook.task;

public record TaskRequestDto(
    Long project_id,
    Long task_type_id,
    Long work_status_id,
    String ticket,
    String start_time,
    Integer duration_minutes,
    String summary,
    String obstacle
) {}