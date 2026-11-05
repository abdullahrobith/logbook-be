package com.logbook.service;

import com.logbook.entity.DailyLog;
import com.logbook.repository.DailyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    void createDailyLogShouldUseDefaultScheduleAndDraftStatus() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        when(dailyLogRepository.findByUserIdAndLogDate(1L, date))
                .thenReturn(Optional.empty());
        when(dailyLogRepository.save(any(DailyLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createDailyLog(1L, date);

        var savedLog = org.mockito.ArgumentCaptor.forClass(DailyLog.class);
        verify(dailyLogRepository).save(savedLog.capture());
        assertEquals(1L, savedLog.getValue().getUserId());
        assertEquals(date, savedLog.getValue().getLogDate());
        assertEquals((short) 1, savedLog.getValue().getWorkScheduleId());
        assertEquals("DRAF", savedLog.getValue().getStatus());
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
}
