package com.logbook.controller;

import com.logbook.dto.AdminDailyLogResponse;
import com.logbook.entity.User;
import com.logbook.service.AdminDailyLogService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API admin untuk menelusuri Daily Log seluruh talent.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/daily-logs")
public class AdminDailyLogController {

    private final AdminDailyLogService adminDailyLogService;
    private final RoleService roleService;

    public AdminDailyLogController(
            AdminDailyLogService adminDailyLogService,
            RoleService roleService
    ) {
        this.adminDailyLogService = adminDailyLogService;
        this.roleService = roleService;
    }

    @GetMapping
    public List<AdminDailyLogResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            Long userId,
            @RequestParam(required = false)
            String status,
            HttpSession session
    ) {
        User actor = roleService.requireAdmin(session);
        log.info("Menerima request GET daftar Daily Log admin dari User ID {} ({}) dengan filter date: {}, userId: {}, status: {}",
                actor.getId(), actor.getEmail(), date, userId, status);
        return adminDailyLogService.list(date, userId, status);
    }
}
