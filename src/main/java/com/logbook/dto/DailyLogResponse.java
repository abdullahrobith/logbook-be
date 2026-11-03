package com.logbook.dto;

import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DailyLogResponse(
        Long id,
        Long userId,
        LocalDate logDate,
        String status,
        List<TaskResponse> tasks
) {
    public static DailyLogResponse from(DailyLog dailyLog) {
        List<TaskResponse> tasks = dailyLog.getTasks()
                .stream()
                .map(TaskResponse::from)
                .toList();

        return new DailyLogResponse(
                dailyLog.getId(),
                dailyLog.getUserId(),
                dailyLog.getLogDate(),
                dailyLog.getStatus(),
                tasks
        );
    }

    public record TaskResponse(
            Long id,
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            String ticket,
            LocalTime startTime,
            Short durationMinutes,
            String summary
    ) {
        public static TaskResponse from(LogTask task) {
            return new TaskResponse(
                    task.getId(),
                    task.getProjectId(),
                    task.getTaskTypeId(),
                    task.getWorkStatusId(),
                    task.getTicket(),
                    task.getStartTime(),
                    task.getDurationMinutes(),
                    task.getSummary()
            );
        }
    }
}