
package com.logbook.dto;

import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record DailyLogResponse(
        Long id,
        Long userId,
        LocalDate logDate,
        String status,
        Integer totalDurationMinutes,
        List<ProjectResponse> projects,
        List<TaskResponse> tasks
) {

    public static DailyLogResponse from(DailyLog dailyLog) {
        List<LogTask> logTasks = dailyLog.getTasks();

        List<TaskResponse> tasks = logTasks.stream()
                .map(TaskResponse::from)
                .toList();

        // Mengelompokkan task berdasarkan proyek.
        Map<Long, List<LogTask>> groupedTasks = new TreeMap<>();

        for (LogTask task : logTasks) {
            groupedTasks
                    .computeIfAbsent(task.getProjectId(), key -> new ArrayList<>())
                    .add(task);
        }

        // Menghitung subtotal masing-masing proyek.
        List<ProjectResponse> projects = groupedTasks.entrySet()
                .stream()
                .map(entry -> new ProjectResponse(
                        entry.getKey(),
                        calculateUnionDuration(entry.getValue()),
                        entry.getValue().stream()
                                .map(TaskResponse::from)
                                .toList()
                ))
                .toList();

        // Menghitung total seluruh task tanpa menghitung
        // waktu yang bertumpang tindih lebih dari sekali.
        int totalDuration = calculateUnionDuration(logTasks);

        return new DailyLogResponse(
                dailyLog.getId(),
                dailyLog.getUserId(),
                dailyLog.getLogDate(),
                dailyLog.getStatus(),
                totalDuration,
                projects,
                tasks
        );
    }

    /**
     * Menghitung total durasi berdasarkan union rentang waktu.
     * Rentang waktu yang saling bertumpang tindih hanya dihitung sekali.
     */
    private static int calculateUnionDuration(List<LogTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }

        List<TimeInterval> intervals = tasks.stream()
                .map(task -> {
                    int start = task.getStartTime().toSecondOfDay() / 60;
                    int end = start + task.getDurationMinutes().intValue();

                    return new TimeInterval(start, end);
                })
                .sorted(Comparator.comparingInt(TimeInterval::start))
                .toList();

        int total = 0;
        int currentStart = intervals.get(0).start();
        int currentEnd = intervals.get(0).end();

        for (int i = 1; i < intervals.size(); i++) {
            TimeInterval next = intervals.get(i);

            if (next.start() <= currentEnd) {
                // Rentang bertumpang tindih atau bersambungan.
                currentEnd = Math.max(currentEnd, next.end());
            } else {
                total += currentEnd - currentStart;

                currentStart = next.start();
                currentEnd = next.end();
            }
        }

        // Menambahkan rentang terakhir.
        total += currentEnd - currentStart;

        return total;
    }

    private record TimeInterval(int start, int end) {
    }

    public record ProjectResponse(
            Long projectId,
            Integer subtotalMinutes,
            List<TaskResponse> tasks
    ) {
    }

    public record TaskResponse(
            Long id,
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            String ticket,
            LocalTime startTime,
            Short durationMinutes,
            String summary,
            String obstacle
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
                    task.getSummary(),
                    task.getObstacle()
            );
        }
    }
}
