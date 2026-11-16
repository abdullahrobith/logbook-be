package com.logbook.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.logbook.dto.DayDto;
import com.logbook.dto.DayTaskRequest;
import com.logbook.dto.LogTaskRequest;
import com.logbook.service.AuthService;
import com.logbook.service.DayTaskService;
import com.logbook.service.LogTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Talent API", description = "Endpoint untuk manajemen Task Log Book")
public class TaskController {

    private final DayTaskService dayTaskService;
    private final LogTaskService logTaskService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public TaskController(
            DayTaskService dayTaskService,
            LogTaskService logTaskService,
            AuthService authService,
            Validator validator
    ) {
        this.dayTaskService = dayTaskService;
        this.logTaskService = logTaskService;
        this.authService = authService;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
        this.validator = validator;
    }

    @PutMapping("/{id}")
    public Object updateTask(
            @PathVariable Long id,
            @RequestBody String rawBody,
            HttpSession session
    ) throws Exception {
        Long userId = authService.requireUserId(session);
        JsonNode body = objectMapper.readTree(rawBody);

        // Deteksi payload legacy (FE lama): memiliki projectId/taskTypeId/workStatusId
        if (body.has("projectId") || body.has("taskTypeId") || body.has("workStatusId")) {
            log.info("Menerima request PUT (legacy) untuk update Task ID: {} oleh User ID {}", id, userId);
            LogTaskRequest request = objectMapper.treeToValue(body, LogTaskRequest.class);
            validate(request);
            return logTaskService.updateTask(userId, id, request);
        }

        log.info("Menerima request PUT (SRS) untuk update Task ID: {} oleh User ID {}", id, userId);
        DayTaskRequest request = objectMapper.treeToValue(body, DayTaskRequest.class);
        validate(request);
        return dayTaskService.updateTask(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public DayDto deleteTask(@PathVariable Long id, HttpSession session) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request DELETE untuk Task ID: {} oleh User ID {}", id, userId);
        // Mengembalikan DayDto terbaru (sesuai SRS); klien legacy yang mengabaikan body tetap kompatibel.
        return dayTaskService.deleteTask(userId, id);
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
