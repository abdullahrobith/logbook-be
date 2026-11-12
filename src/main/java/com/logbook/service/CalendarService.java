package com.logbook.service;

import com.logbook.dto.DayInfoResponse;
import com.logbook.entity.Holiday;
import com.logbook.entity.Leave;
import com.logbook.repository.HolidayRepository;
import com.logbook.repository.LeaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Menyatukan data libur nasional dan cuti menjadi keterangan hari kerja.
 *
 * <p>Dipakai untuk dua hal: memberi label Libur/Cuti pada tampilan, dan
 * memastikan tugas berkala tidak menandai hari libur, cuti, atau akhir pekan
 * sebagai TERLEWAT.</p>
 */
@Service
public class CalendarService {

    private static final int MAX_RANGE_DAYS = 62;

    private final HolidayRepository holidayRepository;
    private final LeaveRepository leaveRepository;

    public CalendarService(
            HolidayRepository holidayRepository,
            LeaveRepository leaveRepository
    ) {
        this.holidayRepository = holidayRepository;
        this.leaveRepository = leaveRepository;
    }

    @Transactional(readOnly = true)
    public DayInfoResponse describe(Long userId, LocalDate date) {
        String holidayName = holidayRepository.findByHolidayDate(date)
                .map(Holiday::getName)
                .orElse(null);

        boolean onLeave = leaveRepository.countCovering(userId, date) > 0;

        return build(date, holidayName, onLeave);
    }

    /**
     * Keterangan hari untuk satu rentang tanggal. Data libur dan cuti diambil
     * sekali agar tidak menjadi satu kueri per tanggal.
     */
    @Transactional(readOnly = true)
    public List<DayInfoResponse> describeRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }

        LocalDate lastDate = endDate;
        if (startDate.plusDays(MAX_RANGE_DAYS).isBefore(lastDate)) {
            lastDate = startDate.plusDays(MAX_RANGE_DAYS);
        }

        Map<LocalDate, String> holidays = new HashMap<>();
        for (Holiday holiday : holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDate(startDate, lastDate)) {
            holidays.put(holiday.getHolidayDate(), holiday.getName());
        }

        Set<LocalDate> leaveDates = new HashSet<>();
        for (Leave leave : leaveRepository.findOverlappingForUser(userId, startDate, lastDate)) {
            leaveDates.addAll(expand(leave, startDate, lastDate));
        }

        List<DayInfoResponse> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            days.add(build(date, holidays.get(date), leaveDates.contains(date)));
        }

        return days;
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByHolidayDate(date);
    }

    @Transactional(readOnly = true)
    public boolean isOnLeave(Long userId, LocalDate date) {
        return leaveRepository.countCovering(userId, date) > 0;
    }

    /**
     * Hari kerja menurut OCD: Senin-Jumat, bukan libur nasional, dan bukan cuti
     * pengguna tersebut.
     */
    @Transactional(readOnly = true)
    public boolean isWorkingDay(Long userId, LocalDate date) {
        return isWorkingDay(date, isHoliday(date), isOnLeave(userId, date));
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Versi tanpa kueri untuk pemanggil yang sudah memuat data libur/cuti.
     */
    public static boolean isWorkingDay(
            LocalDate date,
            boolean holiday,
            boolean onLeave
    ) {
        return !isWeekend(date) && !holiday && !onLeave;
    }

    private DayInfoResponse build(
            LocalDate date,
            String holidayName,
            boolean onLeave
    ) {
        boolean weekend = isWeekend(date);
        boolean workingDay = !weekend && holidayName == null && !onLeave;

        return new DayInfoResponse(date, holidayName, onLeave, weekend, workingDay);
    }

    private Set<LocalDate> expand(Leave leave, LocalDate startDate, LocalDate endDate) {
        LocalDate from = leave.getStartDate().isBefore(startDate)
                ? startDate
                : leave.getStartDate();
        LocalDate to = leave.getEndDate().isAfter(endDate)
                ? endDate
                : leave.getEndDate();

        Set<LocalDate> dates = new HashSet<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }
}
