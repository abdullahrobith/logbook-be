package com.logbook.controller;

import com.logbook.dto.DayDto;
import com.logbook.dto.DayTaskRequest;
import com.logbook.service.AuthService;
import com.logbook.service.DailyLogService;
import com.logbook.service.DayTaskService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/days")
public class DayController {

    private final DailyLogService dailyLogService;
    private final DayTaskService dayTaskService;
    private final AuthService authService;

    public DayController(DailyLogService dailyLogService, DayTaskService dayTaskService, AuthService authService) {
        this.dailyLogService = dailyLogService;
        this.dayTaskService = dayTaskService;
        this.authService = authService;
    }

    @GetMapping("/{date}")
    public DayDto getDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request GET DayDto tanggal {} untuk User ID {}", date, userId);
        return dailyLogService.getDay(userId, date);
    }

    @PostMapping("/{date}/tasks")
    @ResponseStatus(HttpStatus.OK)
    public DayDto createTask(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DayTaskRequest request,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request POST task (SRS) tanggal {} untuk User ID {}", date, userId);
        return dayTaskService.createTask(userId, date, request);
    }
}
