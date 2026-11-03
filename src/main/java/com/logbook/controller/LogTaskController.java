package com.logbook.controller;

import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.LogTaskRequest;
import com.logbook.service.AuthService;
import com.logbook.service.LogTaskService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        Long userId = authService.requireUserId(session);
        DailyLogResponse.TaskResponse response =
                logTaskService.createTask(userId, dailyLogId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
