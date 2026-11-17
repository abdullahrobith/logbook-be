package com.logbook.service;

import com.logbook.dto.SubmitCheckResponse;
import com.logbook.dto.SubmitCheckResponse.Hint;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.repository.DailyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitCheckServiceTest {

    private static final int RINGKASAN_MIN = 20;
    private static final int TASK_MAKS_MENIT = 240;

    private DailyLogRepository dailyLogRepository;
    private SubmitCheckService service;

    @BeforeEach
    void setUp() {
        dailyLogRepository = mock(DailyLogRepository.class);
        service = new SubmitCheckService(RINGKASAN_MIN, TASK_MAKS_MENIT, dailyLogRepository);
    }

    @Test
    void shortSummaryHint() {
        DailyLog dailyLog = dailyLog(task(11L, 60, "Singkat"));

        List<Hint> hints = service.evaluate(dailyLog).hints();

        assertEquals(1, hints.size());
        assertEquals(11L, hints.get(0).taskId());
        assertEquals(SubmitCheckResponse.KIND_RINGKASAN_PENDEK, hints.get(0).kind());
        assertTrue(hints.get(0).message().contains("minimal 20"));
    }

    @Test
    void longTaskHint() {
        DailyLog dailyLog = dailyLog(task(12L, 300, ringkasanCukup()));

        List<Hint> hints = service.evaluate(dailyLog).hints();

        assertEquals(1, hints.size());
        assertEquals(12L, hints.get(0).taskId());
        assertEquals(SubmitCheckResponse.KIND_TASK_PANJANG, hints.get(0).kind());
        assertTrue(hints.get(0).message().contains("300 menit"));
        assertTrue(hints.get(0).message().contains("240"));
    }

    @Test
    void completeTasksProduceNoHints() {
        DailyLog dailyLog = dailyLog(
                task(13L, 60, ringkasanCukup()),
                task(14L, TASK_MAKS_MENIT, ringkasanCukup())
        );

        assertTrue(service.evaluate(dailyLog).hints().isEmpty());
    }

    @Test
    void oneTaskCanProduceBothHints() {
        DailyLog dailyLog = dailyLog(task(15L, 480, "Pendek"));

        List<Hint> hints = service.evaluate(dailyLog).hints();

        assertEquals(2, hints.size());
        assertEquals(SubmitCheckResponse.KIND_RINGKASAN_PENDEK, hints.get(0).kind());
        assertEquals(SubmitCheckResponse.KIND_TASK_PANJANG, hints.get(1).kind());
    }

    @Test
    void thresholdsFromConfig() {
        SubmitCheckService custom = new SubmitCheckService(5, 120, dailyLogRepository);
        DailyLog dailyLog = dailyLog(task(16L, 180, "Sepuluh kark"));

        List<Hint> hints = custom.evaluate(dailyLog).hints();

        assertEquals(1, hints.size());
        assertEquals(SubmitCheckResponse.KIND_TASK_PANJANG, hints.get(0).kind());
    }

    @Test
    void zeroThresholdDisablesItsHint() {
        SubmitCheckService disabled = new SubmitCheckService(0, 0, dailyLogRepository);
        DailyLog dailyLog = dailyLog(task(17L, 600, "Pendek"));

        assertTrue(disabled.evaluate(dailyLog).hints().isEmpty());
    }

    @Test
    void checkReturnsHintsWithoutSavingTheDailyLog() {
        DailyLog dailyLog = dailyLog(task(18L, 60, "Pendek"));
        ReflectionTestUtils.setField(dailyLog, "id", 42L);
        when(dailyLogRepository.findByIdAndUserId(42L, 1L)).thenReturn(Optional.of(dailyLog));

        SubmitCheckResponse response = service.check(1L, 42L);

        assertEquals(1, response.hints().size());
        assertEquals(SubmitCheckResponse.KIND_RINGKASAN_PENDEK, response.hints().get(0).kind());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void checkForUnknownDailyLogReturnsNotFound() {
        when(dailyLogRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.check(1L, 99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Daily log tidak ditemukan", error.getReason());
    }

    @Test
    void checkOnDateReturnsHintsForThatDay() {
        DailyLog dailyLog = dailyLog(task(19L, 60, "Pendek"));
        LocalDate date = LocalDate.of(2026, 11, 17);
        when(dailyLogRepository.findByUserIdAndLogDate(1L, date)).thenReturn(Optional.of(dailyLog));

        SubmitCheckResponse response = service.checkOn(1L, date);

        assertEquals(1, response.hints().size());
        assertEquals(19L, response.hints().get(0).taskId());
    }

    private static String ringkasanCukup() {
        return "Menyelesaikan validasi form pendaftaran pengguna.";
    }

    private DailyLog dailyLog(LogTask... tasks) {
        OffsetDateTime now = OffsetDateTime.parse("2026-11-17T09:00:00+07:00");
        DailyLog dailyLog = new DailyLog(
                1L,
                LocalDate.of(2026, 11, 17),
                (short) 1,
                "DRAF",
                now,
                now
        );
        ReflectionTestUtils.setField(dailyLog, "tasks", List.of(tasks));
        return dailyLog;
    }

    private LogTask task(Long id, int durationMinutes, String summary) {
        OffsetDateTime now = OffsetDateTime.parse("2026-11-17T09:00:00+07:00");
        LogTask task = new LogTask(
                null,
                1L,
                (short) 1,
                (short) 2,
                "ABC-153",
                480,
                (short) durationMinutes,
                summary,
                null,
                now,
                now
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
