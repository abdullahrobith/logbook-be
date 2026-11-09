package com.logbook.service;

import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.repository.DailyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DailyLogServiceTest {

    private DailyLogRepository dailyLogRepository;
    private DailyLogService service;

    @BeforeEach
    void setUp() {
        dailyLogRepository = mock(DailyLogRepository.class);
        service = new DailyLogService(dailyLogRepository);
    }

    @Test
    void createDailyLogShouldInsertDraftWithDefaultScheduleWhenMissing() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog created = draftLog(13L, date, List.of());

        when(dailyLogRepository.findByUserIdAndLogDate(1L, date))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(dailyLogRepository.insertDraftIfAbsent(
                eq(1L),
                eq(date),
                eq((short) 1),
                eq("DRAF"),
                any()
        )).thenReturn(1);

        var response = service.createDailyLog(1L, date);

        assertEquals(13L, response.id());
        assertEquals("DRAF", response.status());
        verify(dailyLogRepository).insertDraftIfAbsent(
                eq(1L),
                eq(date),
                eq((short) 1),
                eq("DRAF"),
                any()
        );
    }

    @Test
    void createDailyLogShouldReturnExistingLogWithoutSavingDuplicate() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog existing = mock(DailyLog.class);
        when(existing.getId()).thenReturn(12L);
        when(existing.getUserId()).thenReturn(1L);
        when(existing.getLogDate()).thenReturn(date);
        when(existing.getStatus()).thenReturn("DRAF");
        when(existing.getTasks()).thenReturn(java.util.List.of());
        when(dailyLogRepository.findByUserIdAndLogDate(1L, date))
                .thenReturn(Optional.of(existing));

        var response = service.createDailyLog(1L, date);

        assertEquals(12L, response.id());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void submitDailyLogShouldMarkSubmittedWhenDraftHasTasks() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        LogTask task = mock(LogTask.class);
        when(task.getProjectId()).thenReturn(1L);
        when(task.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(task.getDurationMinutes()).thenReturn((short) 30);

        DailyLog dailyLog = draftLog(10L, date, List.of(task));
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(dailyLog));
        when(dailyLogRepository.save(dailyLog)).thenReturn(dailyLog);

        var response = service.submitDailyLog(1L, 10L);

        assertNotNull(response);
        verify(dailyLog).submit(any());
        verify(dailyLogRepository).save(dailyLog);
    }

    @Test
    void submitDailyLogShouldBeIdempotentWhenAlreadySubmitted() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog dailyLog = draftLog(10L, date, List.of());
        when(dailyLog.getStatus()).thenReturn("MENUNGGU");
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(dailyLog));

        var response = service.submitDailyLog(1L, 10L);

        assertEquals(10L, response.id());
        verify(dailyLog, never()).submit(any());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void submitDailyLogShouldFailWhenDraftHasNoTasks() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog dailyLog = draftLog(10L, date, List.of());
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(dailyLog));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.submitDailyLog(1L, 10L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(dailyLog, never()).submit(any());
    }

    @Test
    void submitDailyLogShouldFailWhenDailyLogIsNotFound() {
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.submitDailyLog(1L, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void withdrawDailyLogShouldMarkDraftWhenSubmitted() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog dailyLog = draftLog(10L, date, List.of());
        when(dailyLog.getStatus()).thenReturn("MENUNGGU");
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(dailyLog));
        when(dailyLogRepository.save(dailyLog)).thenReturn(dailyLog);

        var response = service.withdrawDailyLog(1L, 10L);

        assertEquals(10L, response.id());
        verify(dailyLog).withdraw(any());
        verify(dailyLogRepository).save(dailyLog);
    }

    @Test
    void withdrawDailyLogShouldBeIdempotentWhenAlreadyDraft() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog dailyLog = draftLog(10L, date, List.of());
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(dailyLog));

        var response = service.withdrawDailyLog(1L, 10L);

        assertEquals("DRAF", response.status());
        verify(dailyLog, never()).withdraw(any());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void withdrawDailyLogShouldFailWhenStatusCannotBeWithdrawn() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        DailyLog dailyLog = draftLog(10L, date, List.of());
        when(dailyLog.getStatus()).thenReturn("DITOLAK");
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(dailyLog));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.withdrawDailyLog(1L, 10L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(dailyLog, never()).withdraw(any());
    }

    @Test
    void withdrawDailyLogShouldFailWhenDailyLogIsNotFound() {
        when(dailyLogRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.withdrawDailyLog(1L, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private DailyLog draftLog(Long id, LocalDate date, List<LogTask> tasks) {
        DailyLog dailyLog = mock(DailyLog.class);
        when(dailyLog.getId()).thenReturn(id);
        when(dailyLog.getUserId()).thenReturn(1L);
        when(dailyLog.getLogDate()).thenReturn(date);
        when(dailyLog.getStatus()).thenReturn("DRAF");
        when(dailyLog.getTasks()).thenReturn(tasks);
        return dailyLog;
    }
}
