package com.logbook.service;

import com.logbook.dto.AdminDailyLogResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDailyLogServiceTest {

    private DailyLogRepository dailyLogRepository;
    private UserRepository userRepository;
    private AdminDailyLogService service;

    @BeforeEach
    void setUp() {
        dailyLogRepository = mock(DailyLogRepository.class);
        userRepository = mock(UserRepository.class);
        service = new AdminDailyLogService(dailyLogRepository, userRepository);
    }

    @Test
    void listWithoutFiltersPassesNullsToRepository() {
        when(dailyLogRepository.findForAdmin(null, null, null)).thenReturn(List.of());

        List<AdminDailyLogResponse> result = service.list(null, null, null);

        assertEquals(List.of(), result);
        verify(dailyLogRepository).findForAdmin(null, null, null);
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void listNormalizesStatusAndMapsOwnerAndUnionTotal() {
        DailyLog dailyLog = dailyLog(
                3L,
                1L,
                LocalDate.of(2026, 10, 29),
                "MENUNGGU",
                List.of(
                        task(LocalTime.of(9, 0), (short) 60),
                        task(LocalTime.of(9, 30), (short) 60),
                        task(LocalTime.of(11, 0), (short) 30)
                )
        );
        User owner = user(1L, "rina.talent@contoh.id", "Rina Talent", "MAGANG");

        when(dailyLogRepository.findForAdmin(LocalDate.of(2026, 10, 29), 1L, "MENUNGGU"))
                .thenReturn(List.of(dailyLog));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(owner));

        AdminDailyLogResponse response = service
                .list(LocalDate.of(2026, 10, 29), 1L, "menunggu")
                .get(0);

        assertEquals(3L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("Rina Talent", response.fullName());
        assertEquals("rina.talent@contoh.id", response.email());
        assertEquals("MAGANG", response.jenis());
        assertEquals("MENUNGGU", response.status());
        assertEquals(3, response.taskCount());
        assertEquals(120, response.totalDurationMinutes());
    }

    @Test
    void listKeepsRowWhenOwnerIsMissing() {
        DailyLog dailyLog = dailyLog(4L, 99L, LocalDate.of(2026, 10, 29), "DRAF", List.of());

        when(dailyLogRepository.findForAdmin(null, 99L, null)).thenReturn(List.of(dailyLog));
        when(userRepository.findAllById(List.of(99L))).thenReturn(List.of());

        AdminDailyLogResponse response = service.list(null, 99L, null).get(0);

        assertNull(response.fullName());
        assertNull(response.email());
        assertNull(response.jenis());
        assertEquals(0, response.taskCount());
        assertEquals(0, response.totalDurationMinutes());
    }

    @Test
    void blankStatusMeansNoFilter() {
        assertNull(service.normalizeStatus(null));
        assertNull(service.normalizeStatus(""));
        assertNull(service.normalizeStatus("   "));
    }

    @Test
    void unknownStatusIsRejected() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.normalizeStatus("SELESAI")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void knownStatusIsAcceptedInAnyCase() {
        assertEquals("PERBAIKAN_DIAJUKAN", service.normalizeStatus(" perbaikan_diajukan "));
        assertEquals("DRAF_DIBUKA_KEMBALI", service.normalizeStatus("draf_dibuka_kembali"));
    }

    private DailyLog dailyLog(
            Long id,
            Long userId,
            LocalDate logDate,
            String status,
            List<LogTask> tasks
    ) {
        DailyLog dailyLog = mock(DailyLog.class);
        when(dailyLog.getId()).thenReturn(id);
        when(dailyLog.getUserId()).thenReturn(userId);
        when(dailyLog.getLogDate()).thenReturn(logDate);
        when(dailyLog.getStatus()).thenReturn(status);
        when(dailyLog.getTasks()).thenReturn(tasks);
        return dailyLog;
    }

    private LogTask task(LocalTime startTime, short durationMinutes) {
        LogTask task = mock(LogTask.class);
        when(task.getStartTime()).thenReturn(startTime);
        when(task.getDurationMinutes()).thenReturn(durationMinutes);
        return task;
    }

    private User user(Long id, String email, String fullName, String jenis) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getFullName()).thenReturn(fullName);
        when(user.getJenis()).thenReturn(jenis);
        return user;
    }
}
