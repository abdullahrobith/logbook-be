package com.logbook.controller;

import com.logbook.dto.TaskDto;
import com.logbook.service.AuthService;
import com.logbook.service.ContinueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/continue")
@Tag(name = "Talent API", description = "Endpoint untuk melanjutkan task dari hari terakhir yang berisi catatan")
public class ContinueController {

    private final ContinueService continueService;
    private final AuthService authService;

    public ContinueController(ContinueService continueService, AuthService authService) {
        this.continueService = continueService;
        this.authService = authService;
    }

    @GetMapping
    public List<TaskDto> getContinueTasks(HttpSession session) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request GET /api/continue untuk User ID {}", userId);

        List<TaskDto> tasks = continueService.findContinueTasks(userId);

        log.info("Berhasil mengirim {} task Lanjutkan untuk User ID {}", tasks.size(), userId);
        return tasks;
    }
}
