package com.logbook.task;

import lombok.extern.slf4j.Slf4j;
import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.LogTaskRequest;
import com.logbook.service.AuthService;
import com.logbook.service.LogTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Talent API", description = "Endpoint untuk manajemen Task Log Book")
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
