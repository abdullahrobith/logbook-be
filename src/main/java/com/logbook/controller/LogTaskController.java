
package com.logbook.controller;

import com.logbook.dto.CreateLogTaskRequest;
import com.logbook.dto.DailyLogResponse;
import com.logbook.service.LogTaskService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/daily-logs/{dailyLogId}/tasks")
public class LogTaskController {

    private final LogTaskService logTaskService;

    public LogTaskController(LogTaskService logTaskService) {
        this.logTaskService = logTaskService;
    }

    @PostMapping
    public ResponseEntity<DailyLogResponse.TaskResponse> createTask(
            @PathVariable Long dailyLogId,
            @Valid @RequestBody CreateLogTaskRequest request
    ) {
        DailyLogResponse.TaskResponse response =
                logTaskService.createTask(dailyLogId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
