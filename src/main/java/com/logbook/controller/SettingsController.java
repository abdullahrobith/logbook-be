package com.logbook.controller;

import com.logbook.dto.SettingsRequest;
import com.logbook.dto.SettingsResponse;
import com.logbook.entity.User;
import com.logbook.service.RoleService;
import com.logbook.service.SettingsService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final RoleService roleService;

    public SettingsController(SettingsService settingsService, RoleService roleService) {
        this.settingsService = settingsService;
        this.roleService = roleService;
    }

    @GetMapping
    public SettingsResponse getSettings(HttpSession session) {
        User user = roleService.requireCurrentUser(session);
        log.info("Menerima request GET /api/settings untuk User ID {}", user.getId());
        return settingsService.getSettings(user);
    }

    @PutMapping
    public SettingsResponse updateSettings(
            @RequestBody(required = false) SettingsRequest request,
            HttpSession session
    ) {
        User user = roleService.requireCurrentUser(session);
        log.info("Menerima request PUT /api/settings untuk User ID {}", user.getId());
        return settingsService.updateSettings(user, request);
    }
}
