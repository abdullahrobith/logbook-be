package com.logbook.service;

import com.logbook.entity.DailyLog;
import com.logbook.entity.Holiday;
import com.logbook.entity.Leave;
import com.logbook.entity.LogTask;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.HolidayRepository;
import com.logbook.repository.LeaveRepository;
import com.logbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyLogAutomationServiceTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    // Rabu, 11 November 2026 pukul 10:00 WIB.
    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    private static final short WORK_SCHEDULE_ID = 1;

    private DailyLogRepository dailyLogRepository;
    private UserRepository userRepository;
    private HolidayRepository holidayRepository;
    private LeaveRepository leaveRepository;

    @BeforeEach
    void setUp() {
        dailyLogRepository = mock(DailyLogRepository.class);
        userRepository = mock(UserRepository.class);
        holidayRepository = mock(HolidayRepository.class);
        leaveRepository = mock(LeaveRepository.class);

        when(userRepository.findByActiveTrueOrderByFullNameAsc()).thenReturn(List.of());
        when(dailyLogRepository.findDraftsUpTo(any())).thenReturn(List.of());
        when(dailyLogRepository.findByLogDateBetween(any(), any())).thenReturn(List.of());
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any()))
                .thenReturn(List.of());
        when(leaveRepository.findOverlapping(any(), any())).thenReturn(List.of());
    }

    private DailyLogAutomationService service(boolean scheduleEnabled) {
        return service(scheduleEnabled, 31);
    }

    private DailyLogAutomationService service(boolean scheduleEnabled, int historyDays) {
        return new DailyLogAutomationService(
                dailyLogRepository,
                userRepository,
                holidayRepository,
                leaveRepository,
                new DeadlineService(true, JAKARTA, Clock.fixed(NOW, JAKARTA)),
                scheduleEnabled,
                historyDays
        );
    }

    private DailyLog draftLog(Long id, Long userId, LocalDate logDate, List<LogTask> tasks) {
        DailyLog dailyLog = mock(DailyLog.class);
        when(dailyLog.getId()).thenReturn(id);
        when(dailyLog.getUserId()).thenReturn(userId);
        when(dailyLog.getLogDate()).thenReturn(logDate);
        when(dailyLog.getTasks()).thenReturn(tasks);
        return dailyLog;
    }

    private User user(Long id, boolean admin, LocalDate joinedDate) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.isAdmin()).thenReturn(admin);
        when(user.getCreatedAt()).thenReturn(
                joinedDate.atTime(8, 0).atOffset(ZoneOffset.ofHours(7))
        );
        return user;
    }

    @Test
    void pastDraftWithTasksShouldBeAutoSubmitted() {
        DailyLog draft = draftLog(10L, 1L, YESTERDAY, List.of(mock(LogTask.class)));
        when(dailyLogRepository.findDraftsUpTo(TODAY)).thenReturn(List.of(draft));

        service(true).runScheduled();

        verify(draft).submitAutomatically(any());
        verify(dailyLogRepository).save(draft);
        verify(dailyLogRepository, never()).delete(any(DailyLog.class));
    }

    @Test
    void emptyPastDraftOnWorkingDayShouldBeMarkedMissed() {
        DailyLog draft = draftLog(10L, 1L, YESTERDAY, List.of());
        when(dailyLogRepository.findDraftsUpTo(TODAY)).thenReturn(List.of(draft));

        service(true).runScheduled();

        verify(draft).markMissed(any());
        verify(dailyLogRepository).save(draft);
        verify(dailyLogRepository, never()).delete(any(DailyLog.class));
    }

    @Test
    void emptyPastDraftOnWeekendShouldBeDeleted() {
        LocalDate saturday = LocalDate.of(2026, 11, 7);
        DailyLog draft = draftLog(10L, 1L, saturday, List.of());
        when(dailyLogRepository.findDraftsUpTo(TODAY)).thenReturn(List.of(draft));

        service(true).runScheduled();

        verify(draft, never()).markMissed(any());
        verify(dailyLogRepository).delete(draft);
    }

    @Test
    void pastDraftThatIsNotPastDeadlineShouldBeIgnored() {
        DailyLog draft = draftLog(10L, 1L, TODAY, List.of(mock(LogTask.class)));
        when(dailyLogRepository.findDraftsUpTo(TODAY)).thenReturn(List.of(draft));

        service(true).runScheduled();

        verify(draft, never()).submitAutomatically(any());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void missedDaysShouldSkipWeekendHolidayLeaveExistingLogsAndAdmin() {
        User talent = user(1L, false, LocalDate.of(2026, 11, 1));
        User admin = user(2L, true, LocalDate.of(2026, 11, 1));
        when(userRepository.findByActiveTrueOrderByFullNameAsc())
                .thenReturn(List.of(talent, admin));

        // 31 hari ke belakang dari 11 November: 7 November (Sabtu) hingga
        // 10 November (Selasa). Libur 9 November, cuti 10 November.
        Holiday holiday = mock(Holiday.class);
        when(holiday.getHolidayDate()).thenReturn(LocalDate.of(2026, 11, 9));
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any()))
                .thenReturn(List.of(holiday));

        Leave leave = mock(Leave.class);
        when(leave.getUserId()).thenReturn(1L);
        when(leave.covers(LocalDate.of(2026, 11, 10))).thenReturn(true);
        when(leaveRepository.findOverlapping(any(), any())).thenReturn(List.of(leave));

        // Rentang empat hari agar pengujian hanya mencakup 7-10 November:
        // Sabtu, Minggu, libur Senin, cuti Selasa.
        service(true, 4).runScheduled();

        verify(dailyLogRepository, never()).insertDraftIfAbsent(
                anyLong(), any(), any(), any(), any()
        );
    }

    @Test
    void missedDaysShouldOnlyCreateLogsForWorkingDaysWithoutExistingLog() {
        User talent = user(1L, false, LocalDate.of(2026, 11, 9));
        when(userRepository.findByActiveTrueOrderByFullNameAsc())
                .thenReturn(List.of(talent));

        // 10 November sudah punya Daily Log.
        DailyLog existing = draftLog(20L, 1L, LocalDate.of(2026, 11, 10), List.of());
        when(dailyLogRepository.findByLogDateBetween(any(), any()))
                .thenReturn(List.of(existing));
        when(dailyLogRepository.insertDraftIfAbsent(anyLong(), any(), any(), any(), any()))
                .thenReturn(1);

        service(true).runScheduled();

        // Hanya Senin 9 November yang belum berisi; 10 November dilewati.
        verify(dailyLogRepository, times(1)).insertDraftIfAbsent(
                eq(1L),
                eq(LocalDate.of(2026, 11, 9)),
                eq(WORK_SCHEDULE_ID),
                eq("TERLEWAT"),
                any()
        );
        verify(dailyLogRepository, never()).insertDraftIfAbsent(
                eq(1L),
                eq(LocalDate.of(2026, 11, 10)),
                any(),
                any(),
                any()
        );
    }

    @Test
    void userJoinedTodayShouldNotHavePastDaysMarked() {
        User talent = user(1L, false, TODAY);
        when(userRepository.findByActiveTrueOrderByFullNameAsc())
                .thenReturn(List.of(talent));

        service(true).runScheduled();

        verify(dailyLogRepository, never()).insertDraftIfAbsent(
                anyLong(), any(), any(), any(), any()
        );
    }

    @Test
    void disabledScheduleShouldDoNothing() {
        service(false).runScheduled();

        verify(dailyLogRepository, never()).findDraftsUpTo(any());
        verify(userRepository, never()).findByActiveTrueOrderByFullNameAsc();
    }

    @Test
    void inactiveDeadlineRuleShouldDoNothing() {
        DailyLogAutomationService service = new DailyLogAutomationService(
                dailyLogRepository,
                userRepository,
                holidayRepository,
                leaveRepository,
                new DeadlineService(false, JAKARTA, Clock.fixed(NOW, JAKARTA)),
                true,
                31
        );

        service.runScheduled();

        verify(dailyLogRepository, never()).findDraftsUpTo(any());
        verify(userRepository, never()).findByActiveTrueOrderByFullNameAsc();
    }

    @Test
    void timestampShouldUseServerTime() {
        // Memastikan OffsetDateTime yang dikirim ke entitas berasal dari jam
        // server, bukan nilai tetap dari konfigurasi.
        DailyLog draft = draftLog(10L, 1L, YESTERDAY, List.of(mock(LogTask.class)));
        when(dailyLogRepository.findDraftsUpTo(TODAY)).thenReturn(List.of(draft));

        service(true).runScheduled();

        verify(draft).submitAutomatically(any(OffsetDateTime.class));
    }
}
