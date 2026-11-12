package com.logbook.controller;

import com.logbook.dto.DayInfoResponse;
import com.logbook.entity.User;
import com.logbook.service.AuthService;
import com.logbook.service.CalendarService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Kalender pribadi pengguna: label Libur nasional, Cuti, dan akhir pekan untuk
 * satu rentang tanggal. Dipakai tampilan linimasa dan strip minggu agar hari
 * yang bukan hari kerja tidak tampil sebagai Terlewat.
 */
@Slf4j
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private static final int MAX_RANGE_DAYS = 62;

    private final CalendarService calendarService;
    private final AuthService authService;
    private final RoleService roleService;

    public CalendarController(
            CalendarService calendarService,
            AuthService authService,
            RoleService roleService
    ) {
        this.calendarService = calendarService;
        this.authService = authService;
        this.roleService = roleService;
    }

    @GetMapping
    public List<DayInfoResponse> getCalendar(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) Long userId,
            HttpSession session
    ) {
        authService.requireUserId(session);

        if (to.isBefore(from)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tanggal akhir tidak boleh mendahului tanggal mulai"
            );
        }

        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rentang kalender maksimal " + MAX_RANGE_DAYS + " hari"
            );
        }

        User target = resolveTarget(userId, session);
        log.info("Menerima request GET kalender {} sampai {} untuk User ID {}",
                from, to, target.getId());

        return calendarService.describeRange(target.getId(), from, to);
    }

    /**
     * Tanpa userId, kalender berisi cuti pengguna sendiri. Melihat kalender
     * pengguna lain mengikuti aturan akses yang sama dengan Daily Log.
     */
    private User resolveTarget(Long userId, HttpSession session) {
        User actor = roleService.requireCurrentUser(session);

        if (userId == null || userId.equals(actor.getId())) {
            return actor;
        }

        return roleService.requireCanViewTalent(actor, userId);
    }
}
