package com.logbook.controller;

import com.logbook.dto.DayDto;
import com.logbook.dto.TeamMemberResponse;
import com.logbook.entity.User;
import com.logbook.service.DailyLogService;
import com.logbook.service.RoleService;
import com.logbook.service.TeamService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("Menerima request GET daftar tim oleh User ID {} ({})", actor.getId(), actor.getEmail());
        return teamService.getTeam(actor);
    }

    /**
     * Path lama tetap tersedia, tetapi responsnya disamakan dengan
     * {@code GET /api/team/{userId}/days/{date}} dan {@code GET /api/days/{date}}.
     */
    @GetMapping("/{userId}/daily-logs/{date}")
    public DayDto getTeamMemberDailyLog(
            @PathVariable Long userId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        User actor = roleService.requireSupervisorOrAdmin(session);
        User member = roleService.requireCanViewTalent(actor, userId);
        log.info("Menerima request GET DayDto anggota User ID {} tanggal {} oleh User ID {} ({})",
                member.getId(), date, actor.getId(), actor.getEmail());
        return dailyLogService.getDay(member.getId(), date);
    }

    @GetMapping("/{userId}/days/{date}")
    public DayDto getTeamMemberDay(
            @PathVariable Long userId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        User actor = roleService.requireSupervisorOrAdmin(session);
        User member = roleService.requireCanViewTalent(actor, userId);
        log.info("Menerima request GET DayDto User ID {} tanggal {} oleh User ID {} ({})",
                member.getId(), date, actor.getId(), actor.getEmail());
        return dailyLogService.getDay(member.getId(), date);
    }
}
