package com.logbook.controller;

import lombok.extern.slf4j.Slf4j;
import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.LogTaskRequest;
import com.logbook.service.AuthService;
import com.logbook.service.LogTaskService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/daily-logs/{dailyLogId}/tasks")
public class LogTaskController {

    private final LogTaskService logTaskService;
    private final AuthService authService;

    public LogTaskController(LogTaskService logTaskService, AuthService authService) {
        this.logTaskService = logTaskService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<DailyLogResponse.TaskResponse> createTask(
            @PathVariable Long dailyLogId,
            @Valid @RequestBody LogTaskRequest request,
            HttpSession session
    ) {
        log.info("Menerima request POST untuk membuat task baru di Daily Log ID: {}", dailyLogId);

        Long userId = authService.requireUserId(session);
        DailyLogResponse.TaskResponse response =
                logTaskService.createTask(userId, dailyLogId, request);

        log.info("Berhasil membuat task baru untuk Daily Log ID: {}", dailyLogId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}