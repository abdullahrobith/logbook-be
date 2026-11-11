package com.logbook.controller;

import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.DecisionRequest;
import com.logbook.entity.User;
import com.logbook.service.ApprovalService;
import com.logbook.service.AuthService;
import com.logbook.service.DailyLogService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/daily-logs")
public class DailyLogController {

    private final DailyLogService dailyLogService;
    private final ApprovalService approvalService;
    private final AuthService authService;
    private final RoleService roleService;

    public DailyLogController(
            DailyLogService dailyLogService,
            ApprovalService approvalService,
            AuthService authService,
            RoleService roleService
    ) {
        this.dailyLogService = dailyLogService;
        this.approvalService = approvalService;
        this.authService = authService;
        this.roleService = roleService;
    }

    @GetMapping
    public DailyLogResponse getDailyLog(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request GET Daily Log tanggal {} untuk User ID {}", date, userId);
        return dailyLogService.getDailyLog(userId, date);
    }

    /**
     * Daily Log milik pengguna lain: talent hanya boleh membaca miliknya,
     * atasan hanya timnya, dan admin boleh membaca semua.
     */
    @GetMapping("/users/{userId}")
    public DailyLogResponse getTalentDailyLog(
            @PathVariable Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        User actor = roleService.requireCurrentUser(session);
        User talent = roleService.requireCanViewTalent(actor, userId);
        log.info("Menerima request GET Daily Log User ID {} tanggal {} oleh User ID {} ({})",
                talent.getId(), date, actor.getId(), actor.getEmail());
        return dailyLogService.getDailyLog(talent.getId(), date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public DailyLogResponse createDailyLog(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request POST untuk membuat Daily Log tanggal {} untuk User ID {}", date, userId);
        return dailyLogService.createDailyLog(userId, date);
    }

    @PostMapping("/{dailyLogId}/submit")
    public DailyLogResponse submitDailyLog(
            @PathVariable Long dailyLogId,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request POST submit Daily Log ID: {} oleh User ID {}", dailyLogId, userId);
        return dailyLogService.submitDailyLog(userId, dailyLogId);
    }

    @PostMapping("/{dailyLogId}/withdraw")
    public DailyLogResponse withdrawDailyLog(
            @PathVariable Long dailyLogId,
            HttpSession session
    ) {
        Long userId = authService.requireUserId(session);
        log.info("Menerima request POST withdraw Daily Log ID: {} oleh User ID {}", dailyLogId, userId);
        return dailyLogService.withdrawDailyLog(userId, dailyLogId);
    }

    /**
     * Atasan langsung atau admin menyetujui Daily Log anggota timnya. Pelaku
     * tidak boleh memutuskan Daily Log miliknya sendiri.
     */
    @PostMapping("/{dailyLogId}/approve")
    public DailyLogResponse approveDailyLog(
            @PathVariable Long dailyLogId,
            HttpSession session
    ) {
        User actor = roleService.requireCurrentUser(session);
        log.info("Menerima request POST approve Daily Log ID: {} oleh User ID {}", dailyLogId, actor.getId());
        return approvalService.approve(actor, dailyLogId);
    }

    /**
     * Atasan langsung atau admin menolak Daily Log anggota timnya. Alasan
     * penolakan wajib diisi.
     */
    @PostMapping("/{dailyLogId}/reject")
    public DailyLogResponse rejectDailyLog(
            @PathVariable Long dailyLogId,
            @RequestBody(required = false) DecisionRequest request,
            HttpSession session
    ) {
        User actor = roleService.requireCurrentUser(session);
        log.info("Menerima request POST reject Daily Log ID: {} oleh User ID {}", dailyLogId, actor.getId());
        return approvalService.reject(
                actor,
                dailyLogId,
                request == null ? null : request.reason()
        );
    }

    /**
     * Talent mengajukan perbaikan atas Daily Log miliknya yang sudah
     * disetujui. Alasan pengajuan wajib diisi.
     */
    @PostMapping("/{dailyLogId}/revision-request")
    public DailyLogResponse requestRevision(
            @PathVariable Long dailyLogId,
            @RequestBody(required = false) DecisionRequest request,
            HttpSession session
    ) {
        User actor = roleService.requireCurrentUser(session);
        log.info("Menerima request POST pengajuan perbaikan Daily Log ID: {} oleh User ID {}", dailyLogId, actor.getId());
        return approvalService.requestRevision(
                actor,
                dailyLogId,
                request == null ? null : request.reason()
        );
    }

    /**
     * Atasan langsung atau admin menyetujui pengajuan perbaikan sehingga hari
     * itu dibuka kembali sebagai DRAF.
     */
    @PostMapping("/{dailyLogId}/approve-revision")
    public DailyLogResponse approveRevision(
            @PathVariable Long dailyLogId,
            HttpSession session
    ) {
        User actor = roleService.requireCurrentUser(session);
        log.info("Menerima request POST persetujuan perbaikan Daily Log ID: {} oleh User ID {}", dailyLogId, actor.getId());
        return approvalService.approveRevision(actor, dailyLogId);
    }
}
