package com.logbook.controller;

import com.logbook.dto.DailyLogResponse;
import com.logbook.entity.User;
import com.logbook.service.AuthService;
import com.logbook.service.DailyLogService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/daily-logs")
public class DailyLogController {

    private final DailyLogService dailyLogService;
    private final AuthService authService;
    private final RoleService roleService;

    public DailyLogController(
            DailyLogService dailyLogService,
            AuthService authService,
            RoleService roleService
    ) {
        this.dailyLogService = dailyLogService;
        this.authService = authService;
        this.roleService = roleService;
    }

    @GetMapping
    public DailyLogResponse getDailyLog(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        return dailyLogService.getDailyLog(userId, date);
    }

    /**
     * Daily Log milik pengguna lain: talent hanya boleh membaca miliknya,
     * atasan hanya timnya, dan admin boleh membaca semua.
     */
    @GetMapping("/users/{userId}")
    public DailyLogResponse getTalentDailyLog(
            @PathVariable Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        User actor = roleService.requireCurrentUser(session);
        User talent = roleService.requireCanViewTalent(actor, userId);
        return dailyLogService.getDailyLog(talent.getId(), date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public DailyLogResponse createDailyLog(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        return dailyLogService.createDailyLog(userId, date);
    }

    @PostMapping("/{dailyLogId}/submit")
    public DailyLogResponse submitDailyLog(
            @PathVariable Long dailyLogId,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        return dailyLogService.submitDailyLog(userId, dailyLogId);
    }

    @PostMapping("/{dailyLogId}/withdraw")
    public DailyLogResponse withdrawDailyLog(
            @PathVariable Long dailyLogId,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        return dailyLogService.withdrawDailyLog(userId, dailyLogId);
    }
}
