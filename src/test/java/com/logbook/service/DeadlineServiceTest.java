package com.logbook.service;

import com.logbook.entity.UserSettings;
import com.logbook.repository.UserSettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeadlineServiceTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final ZoneOffset JAKARTA_OFFSET = ZoneOffset.ofHours(7);

    // 11 November 2026 pukul 10:00 WIB.
    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    private static final Long USER_ID = 1L;

    // 12 November 2026 pukul 02:00 WIB, masih di dalam shift malam 22.00-06.00.
    private static final Instant NIGHT_SHIFT_AFTER_MIDNIGHT =
            Instant.parse("2026-11-11T19:00:00Z");

    // 12 November 2026 pukul 07:00 WIB, shift malam sudah berakhir.
    private static final Instant NIGHT_SHIFT_AFTER_WORK_END =
            Instant.parse("2026-11-12T00:00:00Z");

    private DeadlineService service(boolean active) {
        return new DeadlineService(
                active,
                JAKARTA,
                Clock.fixed(NOW, JAKARTA)
        );
    }

    private DeadlineService serviceWithWorkHours(
            Instant now,
            LocalTime workStart,
            LocalTime workEnd
    ) {
        UserSettingsRepository repository = mock(UserSettingsRepository.class);
        UserSettings settings = mock(UserSettings.class);
        when(settings.getWorkStart()).thenReturn(workStart);
        when(settings.getWorkEnd()).thenReturn(workEnd);
        when(repository.findById(USER_ID)).thenReturn(Optional.of(settings));

        return new DeadlineService(
                true,
                JAKARTA,
                Clock.fixed(now, JAKARTA),
                repository
        );
    }

    private DeadlineService nightShift(Instant now) {
        return serviceWithWorkHours(now, LocalTime.of(22, 0), LocalTime.of(6, 0));
    }

    @Test
    void deadlineShouldBe2359OnTheSameDayInConfiguredZone() {
        OffsetDateTime deadline = service(true).deadlineFor(USER_ID, TODAY);

        assertEquals(
                OffsetDateTime.of(2026, 11, 11, 23, 59, 0, 0, JAKARTA_OFFSET),
                deadline
        );
    }

    @Test
    void nightShiftDeadlineIsWorkEnd() {
        OffsetDateTime deadline = nightShift(NOW).deadlineFor(USER_ID, TODAY);

        assertEquals(
                OffsetDateTime.of(2026, 11, 12, 6, 0, 0, 0, JAKARTA_OFFSET),
                deadline
        );
    }

    @Test
    void yesterdayShouldBePastDeadline() {
        assertTrue(service(true).isPastDeadline(USER_ID, YESTERDAY));
    }

    @Test
    void todayAndTomorrowShouldNotBePastDeadline() {
        DeadlineService service = service(true);

        assertFalse(service.isPastDeadline(USER_ID, TODAY));
        assertFalse(service.isPastDeadline(USER_ID, TOMORROW));
    }

    @Test
    void onlyTomorrowShouldBeFutureDay() {
        DeadlineService service = service(true);

        assertFalse(service.isFutureDay(USER_ID, TODAY));
        assertFalse(service.isFutureDay(USER_ID, YESTERDAY));
        assertTrue(service.isFutureDay(USER_ID, TOMORROW));
    }

    @Test
    void todayShouldBeTheOnlyToday() {
        DeadlineService service = service(true);

        assertTrue(service.isToday(USER_ID, TODAY));
        assertFalse(service.isToday(USER_ID, YESTERDAY));
        assertFalse(service.isToday(USER_ID, TOMORROW));
    }

    @Test
    void draftTodayShouldBeEditable() {
        assertTrue(service(true).isEditable(USER_ID, "DRAF", TODAY));
    }

    @Test
    void draftYesterdayShouldNotBeEditable() {
        assertFalse(service(true).isEditable(USER_ID, "DRAF", YESTERDAY));
    }

    @Test
    void draftTomorrowShouldNotBeEditable() {
        assertFalse(service(true).isEditable(USER_ID, "DRAF", TOMORROW));
    }

    @Test
    void rejectedTodayShouldBeEditable() {
        assertTrue(service(true).isEditable(USER_ID, "DITOLAK", TODAY));
    }

    @Test
    void rejectedOtherDaysShouldNotBeEditable() {
        assertFalse(service(true).isEditable(USER_ID, "DITOLAK", YESTERDAY));
        assertFalse(service(true).isEditable(USER_ID, "DITOLAK", TOMORROW));
    }

    @Test
    void nonDraftStatusShouldNotBeEditable() {
        assertFalse(service(true).isEditable(USER_ID, "MENUNGGU", TODAY));
        assertFalse(service(true).isEditable(USER_ID, "DISETUJUI", TODAY));
    }

    @Test
    void disabledRuleShouldAllowPastDays() {
        DeadlineService service = service(false);

        assertFalse(service.isPastDeadline(USER_ID, YESTERDAY));
        assertTrue(service.isEditable(USER_ID, "DRAF", YESTERDAY));
    }

    @Test
    void missedDayShouldOnlyBeEditableWhenRuleIsDisabled() {
        assertFalse(service(true).isEditable(USER_ID, "TERLEWAT", TODAY));
        assertFalse(service(true).isEditable(USER_ID, "TERLEWAT", YESTERDAY));
        assertTrue(service(false).isEditable(USER_ID, "TERLEWAT", YESTERDAY));
    }

    @Test
    void disabledRuleShouldAllowFutureDays() {
        DeadlineService service = service(false);

        assertFalse(service.isFutureDay(USER_ID, TOMORROW));
        assertTrue(service.isToday(USER_ID, TOMORROW));
        assertTrue(service.isEditable(USER_ID, "DRAF", TOMORROW));
        assertTrue(service.isEditable(USER_ID, "DITOLAK", YESTERDAY));
    }

    @Test
    void nightShiftAfterMidnightShouldKeepShiftDayActive() {
        DeadlineService service = nightShift(NIGHT_SHIFT_AFTER_MIDNIGHT);

        // Pukul 02.00 tanggal 12 masih bagian shift tanggal 11.
        assertEquals(TODAY, service.activeDay(USER_ID));
        assertTrue(service.isToday(USER_ID, TODAY));
        assertFalse(service.isFutureDay(USER_ID, TODAY));
        assertTrue(service.isFutureDay(USER_ID, TOMORROW));
        assertFalse(service.isPastDeadline(USER_ID, TODAY));
        assertTrue(service.isEditable(USER_ID, "DRAF", TODAY));
        assertFalse(service.isEditable(USER_ID, "DRAF", TOMORROW));
    }

    @Test
    void nightShiftAfterWorkEndShouldCloseShiftDay() {
        DeadlineService service = nightShift(NIGHT_SHIFT_AFTER_WORK_END);

        // Pukul 07.00 tanggal 12: shift tanggal 11 sudah lewat batas.
        assertEquals(TOMORROW, service.activeDay(USER_ID));
        assertTrue(service.isPastDeadline(USER_ID, TODAY));
        assertFalse(service.isEditable(USER_ID, "DRAF", TODAY));
        assertTrue(service.isEditable(USER_ID, "DRAF", TOMORROW));
    }

    @Test
    void normalShiftShouldKeepCalendarTodayAfterMidnight() {
        LocalDate nextDay = TOMORROW;
        DeadlineService service = serviceWithWorkHours(
                NIGHT_SHIFT_AFTER_MIDNIGHT,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );

        assertEquals(nextDay, service.activeDay(USER_ID));
        assertTrue(service.isPastDeadline(USER_ID, TODAY));
        assertFalse(service.isEditable(USER_ID, "DRAF", TODAY));
    }
}
