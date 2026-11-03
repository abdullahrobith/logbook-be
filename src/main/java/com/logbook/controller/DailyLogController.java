package com.logbook.controller;

import com.logbook.dto.DailyLogResponse;
import com.logbook.service.DailyLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/daily-logs")
public class DailyLogController {

    private final DailyLogService dailyLogService;

    public DailyLogController(DailyLogService dailyLogService) {
        this.dailyLogService = dailyLogService;
    }

    @GetMapping
    public DailyLogResponse getDailyLog(
            @RequestParam Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return dailyLogService.getDailyLog(userId, date);
    }
}