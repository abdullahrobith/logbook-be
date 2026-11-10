package com.logbook.dto;

import java.time.LocalDate;

/**
 * Ringkasan satu anggota tim untuk halaman Tim atasan/admin.
 *
 * <p>Status dan total jam hanya mewakili Daily Log hari ini. Bila anggota
 * belum memiliki Daily Log hari ini, statusnya BELUM_MULAI dan totalnya 0.
 * pendingDays menghitung seluruh Daily Log berstatus MENUNGGU milik anggota.</p>
 */
public record TeamMemberResponse(
        Long userId,
        String fullName,
        String email,
        String jenis,
        LocalDate date,
        Long dailyLogId,
        String status,
        Integer totalDurationMinutes,
        long pendingDays
) {
}
