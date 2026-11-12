package com.logbook.service;

import com.logbook.dto.DayInfoResponse;
import com.logbook.entity.Holiday;
import com.logbook.entity.Leave;
import com.logbook.repository.HolidayRepository;
import com.logbook.repository.LeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    private static final Long USER_ID = 7L;

    // Senin 9 November 2026 sampai Rabu 11 November 2026.
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 10);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 11);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 7);

    private HolidayRepository holidayRepository;
    private LeaveRepository leaveRepository;
    private CalendarService service;

    @BeforeEach
    void setUp() {
        holidayRepository = mock(HolidayRepository.class);
        leaveRepository = mock(LeaveRepository.class);

        when(holidayRepository.findByHolidayDate(any())).thenReturn(Optional.empty());
        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(any(), any()))
                .thenReturn(List.of());
        when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
        when(leaveRepository.countCovering(any(), any())).thenReturn(0L);
        when(leaveRepository.findOverlappingForUser(any(), any(), any())).thenReturn(List.of());

        service = new CalendarService(holidayRepository, leaveRepository);
    }

    private Holiday holiday(LocalDate date, String name) {
        Holiday holiday = mock(Holiday.class);
        when(holiday.getHolidayDate()).thenReturn(date);
        when(holiday.getName()).thenReturn(name);
        return holiday;
    }

    private Leave leave(LocalDate startDate, LocalDate endDate) {
        Leave leave = mock(Leave.class);
        when(leave.getUserId()).thenReturn(USER_ID);
        when(leave.getStartDate()).thenReturn(startDate);
        when(leave.getEndDate()).thenReturn(endDate);
        return leave;
    }

    @Test
    void weekdayShouldBeWorkingDay() {
        assertFalse(CalendarService.isWeekend(WEDNESDAY));
        assertTrue(CalendarService.isWorkingDay(WEDNESDAY, false, false));

        DayInfoResponse day = service.describe(USER_ID, WEDNESDAY);

        assertEquals(WEDNESDAY, day.date());
        assertNull(day.holidayName());
        assertFalse(day.onLeave());
        assertFalse(day.weekend());
        assertTrue(day.workingDay());
    }

    @Test
    void weekendShouldNotBeWorkingDay() {
        assertTrue(CalendarService.isWeekend(SATURDAY));
        assertFalse(CalendarService.isWorkingDay(SATURDAY, false, false));

        DayInfoResponse day = service.describe(USER_ID, SATURDAY);

        assertTrue(day.weekend());
        assertFalse(day.workingDay());
    }

    @Test
    void nationalHolidayShouldNotBeWorkingDay() {
        Holiday holiday = holiday(TUESDAY, "Hari Pahlawan");
        when(holidayRepository.findByHolidayDate(TUESDAY))
                .thenReturn(Optional.of(holiday));

        DayInfoResponse day = service.describe(USER_ID, TUESDAY);

        assertEquals("Hari Pahlawan", day.holidayName());
        assertFalse(day.workingDay());
    }

    @Test
    void leaveShouldNotBeWorkingDayForThatUserOnly() {
        when(leaveRepository.countCovering(USER_ID, MONDAY)).thenReturn(1L);

        DayInfoResponse day = service.describe(USER_ID, MONDAY);

        assertTrue(day.onLeave());
        assertFalse(day.workingDay());
        assertFalse(service.isWorkingDay(USER_ID, MONDAY));
        assertFalse(service.isOnLeave(USER_ID + 1, MONDAY));
    }

    @Test
    void describeRangeShouldCombineHolidayLeaveAndWeekend() {
        Holiday holiday = holiday(TUESDAY, "Hari Pahlawan");
        Leave leave = leave(MONDAY, MONDAY);

        when(holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(SATURDAY, WEDNESDAY))
                .thenReturn(List.of(holiday));
        when(leaveRepository.findOverlappingForUser(USER_ID, SATURDAY, WEDNESDAY))
                .thenReturn(List.of(leave));

        List<DayInfoResponse> days = service.describeRange(USER_ID, SATURDAY, WEDNESDAY);

        assertEquals(5, days.size());

        DayInfoResponse saturday = days.get(0);
        assertEquals(SATURDAY, saturday.date());
        assertTrue(saturday.weekend());

        DayInfoResponse monday = days.get(2);
        assertTrue(monday.onLeave());
        assertFalse(monday.workingDay());

        DayInfoResponse tuesday = days.get(3);
        assertEquals("Hari Pahlawan", tuesday.holidayName());
        assertFalse(tuesday.workingDay());

        DayInfoResponse wednesday = days.get(4);
        assertTrue(wednesday.workingDay());
    }

    @Test
    void describeRangeShouldClampToMaximumRange() {
        List<DayInfoResponse> days = service.describeRange(
                USER_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertEquals(63, days.size());
        assertEquals(LocalDate.of(2026, 1, 1), days.get(0).date());
        assertEquals(LocalDate.of(2026, 3, 4), days.get(days.size() - 1).date());
    }

    @Test
    void describeRangeShouldBeEmptyForInvalidRange() {
        assertTrue(service.describeRange(USER_ID, WEDNESDAY, MONDAY).isEmpty());
        assertTrue(service.describeRange(USER_ID, null, MONDAY).isEmpty());
    }

    @Test
    void isWorkingDayShouldRespectHolidayOnly() {
        when(holidayRepository.existsByHolidayDate(WEDNESDAY)).thenReturn(true);

        assertFalse(service.isWorkingDay(USER_ID, WEDNESDAY));
    }

    @Test
    void isHolidayShouldQueryExactDateOnly() {
        when(holidayRepository.existsByHolidayDate(eq(WEDNESDAY))).thenReturn(true);

        assertTrue(service.isHoliday(WEDNESDAY));
        assertFalse(service.isHoliday(TUESDAY));
    }
}
