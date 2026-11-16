
package com.logbook.dto;

import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DailyLogResponseTest {

    private LogTask createTask(
            Long id,
            Long projectId,
            int startMinute,
            short duration
    ) {
        LogTask task = mock(LogTask.class);

        when(task.getId()).thenReturn(id);
        when(task.getProjectId()).thenReturn(projectId);
        when(task.getTaskTypeId()).thenReturn((short) 1);
        when(task.getWorkStatusId()).thenReturn((short) 1);
        when(task.getTicket()).thenReturn("ABC-" + id);
        when(task.getStartMinute()).thenReturn(startMinute);
        when(task.getDurationMinutes()).thenReturn(duration);
        when(task.getSummary()).thenReturn("Testing task " + id);
        when(task.getObstacle()).thenReturn(null);

        return task;
    }

    private DailyLog createDailyLog(List<LogTask> tasks) {
        DailyLog dailyLog = mock(DailyLog.class);

        when(dailyLog.getId()).thenReturn(3L);
        when(dailyLog.getUserId()).thenReturn(1L);
        when(dailyLog.getLogDate()).thenReturn(
                LocalDate.of(2026, 10, 29)
        );
        when(dailyLog.getStatus()).thenReturn("DRAF");
        when(dailyLog.getTasks()).thenReturn(tasks);

        return dailyLog;
    }

    private DailyLogResponse response(List<LogTask> tasks) {
        return DailyLogResponse.from(
                createDailyLog(tasks),
                true,
                OffsetDateTime.of(2026, 10, 30, 0, 0, 0, 0, ZoneOffset.ofHours(7))
        );
    }

    @Test
    void shouldCalculateTotalDurationWithoutDoubleCountingParallelTasks() {
        List<LogTask> tasks = List.of(
                createTask(1L, 1L, 480, (short) 120), // 08:00
                createTask(2L, 1L, 540, (short) 60),  // 09:00
                createTask(3L, 1L, 540, (short) 120)  // 09:00
        );

        DailyLogResponse response = response(tasks);

        // Rentang gabungan 08:00 - 11:00 = 180 menit.
        assertEquals(180, response.totalDurationMinutes());
    }

    @Test
    void shouldCalculateSubtotalForEachProject() {
        List<LogTask> tasks = List.of(
                createTask(1L, 1L, 480, (short) 120), // 08:00
                createTask(2L, 1L, 540, (short) 60),  // 09:00
                createTask(3L, 2L, 600, (short) 120), // 10:00
                createTask(4L, 2L, 780, (short) 90)   // 13:00
        );

        DailyLogResponse response = response(tasks);

        assertEquals(2, response.projects().size());

        // Project 1: 08:00 - 10:00 = 120 menit.
        assertEquals(
                120,
                response.projects().get(0).subtotalMinutes()
        );

        // Project 2: 10:00 - 12:00 dan 13:00 - 14:30 = 210 menit.
        assertEquals(
                210,
                response.projects().get(1).subtotalMinutes()
        );
    }

    @Test
    void shouldCalculateTotalAcrossDifferentProjectsWithoutDoubleCounting() {
        List<LogTask> tasks = List.of(
                createTask(1L, 1L, 480, (short) 120), // 08:00
                createTask(2L, 1L, 540, (short) 60),  // 09:00
                createTask(3L, 2L, 540, (short) 120)  // 09:00
        );

        DailyLogResponse response = response(tasks);

        // Semua task digabung menjadi satu rentang 08:00 - 11:00.
        assertEquals(180, response.totalDurationMinutes());
    }

    @Test
    void shouldCalculateSequentialTasksCorrectly() {
        List<LogTask> tasks = List.of(
                createTask(1L, 1L, 480, (short) 60), // 08:00
                createTask(2L, 1L, 540, (short) 60), // 09:00
                createTask(3L, 1L, 600, (short) 60)  // 10:00
        );

        DailyLogResponse response = response(tasks);

        assertEquals(180, response.totalDurationMinutes());
    }

    @Test
    void shouldReturnZeroWhenThereAreNoTasks() {
        DailyLogResponse response = response(List.of());

        assertEquals(0, response.totalDurationMinutes());
        assertTrue(response.projects().isEmpty());
        assertTrue(response.tasks().isEmpty());
    }

    @Test
    void shouldCountIdenticalTaskIntervalsOnlyOnce() {
        List<LogTask> tasks = List.of(
                createTask(1L, 1L, 480, (short) 120), // 08:00
                createTask(2L, 2L, 480, (short) 120)  // 08:00
        );

        DailyLogResponse response = response(tasks);

        assertEquals(120, response.totalDurationMinutes());

        // Masing-masing proyek tetap memiliki subtotal sendiri.
        assertEquals(120, response.projects().get(0).subtotalMinutes());
        assertEquals(120, response.projects().get(1).subtotalMinutes());
    }

    @Test
    void shouldSupportCrossMidnightIntervals() {
        List<LogTask> tasks = List.of(
                createTask(1L, 1L, 1380, (short) 120), // 23:00 -> 01:00 (hari berikutnya, menit 1500)
                createTask(2L, 1L, 1440, (short) 30)   // 00:00 hari berikutnya
        );

        DailyLogResponse response = response(tasks);

        // Union 1380-1500 = 120 menit.
        assertEquals(120, response.totalDurationMinutes());
    }

    @Test
    void shouldExposeEditableAndDeadline() {
        DailyLogResponse response = response(List.of());

        assertTrue(response.editable());
        assertEquals(
                OffsetDateTime.of(2026, 10, 30, 0, 0, 0, 0, ZoneOffset.ofHours(7)),
                response.deadline()
        );
    }
}
