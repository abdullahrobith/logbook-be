package com.logbook.dto;

import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Baris daftar Daily Log seluruh talent untuk admin.
 *
 * <p>Memuat identitas pemilik, status workflow, dan ringkasan hari tanpa
 * mengirim seluruh task. totalDurationMinutes memakai aturan union yang sama
 * dengan Daily Log milik talent.</p>
 */
public record AdminDailyLogResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String jenis,
        LocalDate logDate,
        String status,
        OffsetDateTime submittedAt,
        String rejectionReason,
        String revisionReason,
        Long decidedByUserId,
        OffsetDateTime decidedAt,
        boolean autoSubmitted,
        int taskCount,
        int totalDurationMinutes
) {
    public static AdminDailyLogResponse from(DailyLog dailyLog, User user) {
        List<LogTask> tasks = dailyLog.getTasks();

        return new AdminDailyLogResponse(
                dailyLog.getId(),
                dailyLog.getUserId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getJenis(),
                dailyLog.getLogDate(),
                dailyLog.getStatus(),
                dailyLog.getSubmittedAt(),
                dailyLog.getRejectionReason(),
                dailyLog.getRevisionReason(),
                dailyLog.getDecidedByUserId(),
                dailyLog.getDecidedAt(),
                dailyLog.isAutoSubmitted(),
                tasks == null ? 0 : tasks.size(),
                DailyLogResponse.calculateUnionDuration(tasks)
        );
    }
}
