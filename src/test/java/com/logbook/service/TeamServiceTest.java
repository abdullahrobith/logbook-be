package com.logbook.service;

import com.logbook.dto.TeamMemberResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.PendingDayCountProjection;
import com.logbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    private UserRepository userRepository;
    private DailyLogRepository dailyLogRepository;
    private TeamService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        dailyLogRepository = mock(DailyLogRepository.class);
        service = new TeamService(userRepository, dailyLogRepository);
    }

    @Test
    void supervisorGetsOnlyOwnTeam() {
        User supervisor = user(10L, "atasan@contoh.id", "Atasan", false);
        User member = user(1L, "rina@contoh.id", "Rina", false);

        when(userRepository.findByEmailAtasanIgnoreCaseAndActiveTrueOrderByFullNameAsc(
                "atasan@contoh.id"
        )).thenReturn(List.of(member));
        when(dailyLogRepository.findByUserIdInAndLogDate(List.of(1L), LocalDate.now()))
                .thenReturn(List.of());
        when(dailyLogRepository.countByUserIdInAndStatus(List.of(1L), "MENUNGGU"))
                .thenReturn(List.of());

        List<TeamMemberResponse> result = service.getTeam(supervisor);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());
        assertEquals("BELUM_MULAI", result.get(0).status());
        assertEquals(0, result.get(0).totalDurationMinutes());
        assertEquals(0L, result.get(0).pendingDays());
        verify(userRepository, never()).findByActiveTrueOrderByFullNameAsc();
    }

    @Test
    void adminGetsAllActiveUsers() {
        User admin = user(9L, "admin@contoh.id", "Admin", true);
        User first = user(1L, "rina@contoh.id", "Rina", false);
        User second = user(2L, "budi@contoh.id", "Budi", false);

        when(userRepository.findByActiveTrueOrderByFullNameAsc())
                .thenReturn(List.of(first, second));
        when(dailyLogRepository.findByUserIdInAndLogDate(
                List.of(1L, 2L),
                LocalDate.now()
        )).thenReturn(List.of());
        when(dailyLogRepository.countByUserIdInAndStatus(
                List.of(1L, 2L),
                "MENUNGGU"
        )).thenReturn(List.of());

        List<TeamMemberResponse> result = service.getTeam(admin);

        assertEquals(2, result.size());
        verify(userRepository).findByActiveTrueOrderByFullNameAsc();
    }

    @Test
    void todayStatusUsesUnionDurationAndPendingDays() {
        User supervisor = user(10L, "atasan@contoh.id", "Atasan", false);
        User member = user(1L, "rina@contoh.id", "Rina", false);
        DailyLog todayLog = dailyLog(4L, 1L, "MENUNGGU", List.of(
                task(LocalTime.of(9, 0), (short) 60),
                task(LocalTime.of(9, 30), (short) 60)
        ));
        PendingDayCountProjection pending = pending(1L, 2L);

        when(userRepository.findByEmailAtasanIgnoreCaseAndActiveTrueOrderByFullNameAsc(
                "atasan@contoh.id"
        )).thenReturn(List.of(member));
        when(dailyLogRepository.findByUserIdInAndLogDate(List.of(1L), LocalDate.now()))
                .thenReturn(List.of(todayLog));
        when(dailyLogRepository.countByUserIdInAndStatus(List.of(1L), "MENUNGGU"))
                .thenReturn(List.of(pending));

        TeamMemberResponse response = service.getTeam(supervisor).get(0);

        assertEquals(4L, response.dailyLogId());
        assertEquals("MENUNGGU", response.status());
        assertEquals(90, response.totalDurationMinutes());
        assertEquals(2L, response.pendingDays());
    }

    @Test
    void emptyTeamReturnsEmptyList() {
        User supervisor = user(10L, "atasan@contoh.id", "Atasan", false);

        when(userRepository.findByEmailAtasanIgnoreCaseAndActiveTrueOrderByFullNameAsc(
                "atasan@contoh.id"
        )).thenReturn(List.of());

        List<TeamMemberResponse> result = service.getTeam(supervisor);

        assertEquals(List.of(), result);
    }

    private User user(Long id, String email, String fullName, boolean admin) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getFullName()).thenReturn(fullName);
        when(user.isAdmin()).thenReturn(admin);
        return user;
    }

    private DailyLog dailyLog(Long id, Long userId, String status, List<LogTask> tasks) {
        DailyLog dailyLog = mock(DailyLog.class);
        when(dailyLog.getId()).thenReturn(id);
        when(dailyLog.getUserId()).thenReturn(userId);
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

    private PendingDayCountProjection pending(Long userId, long total) {
        PendingDayCountProjection projection = mock(PendingDayCountProjection.class);
        when(projection.getUserId()).thenReturn(userId);
        when(projection.getTotal()).thenReturn(total);
        return projection;
    }
}
