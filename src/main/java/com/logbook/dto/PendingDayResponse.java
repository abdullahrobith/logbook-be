package com.logbook.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Satu hari menunggu persetujuan pada halaman Tim atasan/admin.
 *
 * <p>Menambah tautan yang sebelumnya hanya berupa angka {@code pendingDays}:
 * setiap entri membawa {@code dailyLogId} dan {@code date} sehingga frontend
 * dapat langsung membuka layar review untuk hari tersebut.</p>
 */
public record PendingDayResponse(
        Long userId,
        Long dailyLogId,
        LocalDate date,
        Integer totalDurationMinutes,
        OffsetDateTime submittedAt,
        boolean autoSubmitted
) {
}
