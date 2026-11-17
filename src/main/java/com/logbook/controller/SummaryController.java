package com.logbook.controller;

import com.logbook.dto.SummaryResponse;
import com.logbook.service.AuthService;
import com.logbook.service.SummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/summary")
@Tag(name = "Talent API", description = "Rekap mingguan/bulanan Ringkasanku untuk talent sendiri")
public class SummaryController {

    private final SummaryService summaryService;
    private final AuthService authService;

    public SummaryController(SummaryService summaryService, AuthService authService) {
        this.summaryService = summaryService;
        this.authService = authService;
    }

    @GetMapping
    public SummaryResponse getSummary(
            @RequestParam String period,
            @RequestParam(required = false) String date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request GET /api/summary period={} date={} untuk User ID {}",
                period, date, userId);

        SummaryResponse response = summaryService.getSummary(userId, period, date);

        log.info("Berhasil mengirim rekap {} untuk User ID {}", period, userId);
        return response;
    } 
}
