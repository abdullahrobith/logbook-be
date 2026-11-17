package com.logbook.controller;

import com.logbook.dto.DayDto;
import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.DayTaskRequest;
import com.logbook.dto.SubmitCheckResponse;
import com.logbook.service.AuthService;
import com.logbook.service.DailyLogService;
import com.logbook.service.DayTaskService;
import com.logbook.service.SubmitCheckService;
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
    private final SubmitCheckService submitCheckService;

    public DayController(
            DailyLogService dailyLogService,
            DayTaskService dayTaskService,
            AuthService authService,
            SubmitCheckService submitCheckService
    ) {
        this.dailyLogService = dailyLogService;
        this.dayTaskService = dayTaskService;
        this.authService = authService;
        this.submitCheckService = submitCheckService;
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

    /**
     * Mode pemeriksaan sebelum pengiriman (LBK-KUA-02): mengembalikan saran
     * tanpa mengubah status Daily Log.
     */
    @PostMapping(value = "/{date}/submit", params = "check=true")
    public SubmitCheckResponse checkSubmit(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request pemeriksaan submit tanggal {} untuk User ID {}", date, userId);
        return submitCheckService.checkOn(userId, date);
    }

    /**
     * Mengirim Daily Log tanggal tersebut, sesuai SRS LBK-KRM-01.
     */
    @PostMapping("/{date}/submit")
    public DailyLogResponse submitDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request POST submit tanggal {} untuk User ID {}", date, userId);
        return dailyLogService.submitDailyLogOn(userId, date);
    }
}
