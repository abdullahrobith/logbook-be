package com.logbook.controller;

import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.TeamMemberResponse;
import com.logbook.entity.User;
import com.logbook.service.DailyLogService;
import com.logbook.service.RoleService;
import com.logbook.service.TeamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API halaman Tim untuk atasan dan admin.
 */
@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;
    private final RoleService roleService;
    private final DailyLogService dailyLogService;

    public TeamController(
            TeamService teamService,
            RoleService roleService,
            DailyLogService dailyLogService
    ) {
        this.teamService = teamService;
        this.roleService = roleService;
        this.dailyLogService = dailyLogService;
    }

    @GetMapping
    public List<TeamMemberResponse> getTeam(HttpSession session) {
        User actor = roleService.requireSupervisorOrAdmin(session);
        return teamService.getTeam(actor);
    }

    @GetMapping("/{userId}/daily-logs/{date}")
    public DailyLogResponse getTeamMemberDailyLog(
            @PathVariable Long userId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        User actor = roleService.requireSupervisorOrAdmin(session);
        User member = roleService.requireCanViewTalent(actor, userId);
        return dailyLogService.getDailyLog(member.getId(), date);
    }
}
