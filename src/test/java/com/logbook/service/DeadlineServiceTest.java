package com.logbook.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlineServiceTest {

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    // 11 November 2026 pukul 10:00 WIB.
    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    private DeadlineService service(boolean active) {
        return new DeadlineService(
                active,
                JAKARTA,
                Clock.fixed(NOW, JAKARTA)
        );
    }

    @Test
    void deadlineShouldBeMidnightOfTheNextDayInConfiguredZone() {
        OffsetDateTime deadline = service(true).deadlineFor(TODAY);

        assertEquals(
                OffsetDateTime.of(2026, 11, 12, 0, 0, 0, 0, ZoneOffset.ofHours(7)),
                deadline
        );
    }

    @Test
    void yesterdayShouldBePastDeadline() {
        assertTrue(service(true).isPastDeadline(YESTERDAY));
    }

    @Test
    void todayAndTomorrowShouldNotBePastDeadline() {
        DeadlineService service = service(true);

        assertFalse(service.isPastDeadline(TODAY));
        assertFalse(service.isPastDeadline(TOMORROW));
    }

    @Test
    void onlyTomorrowShouldBeFutureDay() {
        DeadlineService service = service(true);

        assertFalse(service.isFutureDay(TODAY));
        assertFalse(service.isFutureDay(YESTERDAY));
        assertTrue(service.isFutureDay(TOMORROW));
    }

    @Test
    void todayShouldBeTheOnlyToday() {
        DeadlineService service = service(true);

        assertTrue(service.isToday(TODAY));
        assertFalse(service.isToday(YESTERDAY));
        assertFalse(service.isToday(TOMORROW));
    }

    @Test
    void draftTodayShouldBeEditable() {
        assertTrue(service(true).isEditable("DRAF", TODAY));
    }

    @Test
    void draftYesterdayShouldNotBeEditable() {
        assertFalse(service(true).isEditable("DRAF", YESTERDAY));
    }

    @Test
    void draftTomorrowShouldNotBeEditable() {
        assertFalse(service(true).isEditable("DRAF", TOMORROW));
    }

    @Test
    void rejectedTodayShouldBeEditable() {
        assertTrue(service(true).isEditable("DITOLAK", TODAY));
    }

    @Test
    void rejectedOtherDaysShouldNotBeEditable() {
        assertFalse(service(true).isEditable("DITOLAK", YESTERDAY));
        assertFalse(service(true).isEditable("DITOLAK", TOMORROW));
    }

    @Test
    void nonDraftStatusShouldNotBeEditable() {
        assertFalse(service(true).isEditable("MENUNGGU", TODAY));
        assertFalse(service(true).isEditable("DISETUJUI", TODAY));
    }

    @Test
    void disabledRuleShouldAllowPastDays() {
        DeadlineService service = service(false);

        assertFalse(service.isPastDeadline(YESTERDAY));
        assertTrue(service.isEditable("DRAF", YESTERDAY));
    }

    @Test
    void missedDayShouldOnlyBeEditableWhenRuleIsDisabled() {
        assertFalse(service(true).isEditable("TERLEWAT", TODAY));
        assertFalse(service(true).isEditable("TERLEWAT", YESTERDAY));
        assertTrue(service(false).isEditable("TERLEWAT", YESTERDAY));
    }

    @Test
    void disabledRuleShouldAllowFutureDays() {
        DeadlineService service = service(false);

        assertFalse(service.isFutureDay(TOMORROW));
        assertTrue(service.isToday(TOMORROW));
        assertTrue(service.isEditable("DRAF", TOMORROW));
        assertTrue(service.isEditable("DITOLAK", YESTERDAY));
    }
}
