package com.logbook.task;

import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.LogTaskRequest;
import com.logbook.service.AuthService;
import com.logbook.service.LogTaskService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final LogTaskService logTaskService;
    private final AuthService authService;

    public TaskController(LogTaskService logTaskService, AuthService authService) {
        this.logTaskService = logTaskService;
        this.authService = authService;
    }

    @PutMapping("/{id}")
    public DailyLogResponse.TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody LogTaskRequest request,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        return logTaskService.updateTask(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id, HttpSession session) {
        Long userId = authService.requireUserId(session);
        logTaskService.deleteTask(userId, id);
    }
}
