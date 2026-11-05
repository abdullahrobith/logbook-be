package com.logbook.controller;

import com.logbook.dto.DailyLogResponse;
import com.logbook.service.AuthService;
import com.logbook.service.DailyLogService;
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

    public DailyLogController(DailyLogService dailyLogService, AuthService authService) {
        this.dailyLogService = dailyLogService;
        this.authService = authService;
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
}
