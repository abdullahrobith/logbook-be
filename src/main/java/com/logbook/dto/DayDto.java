package com.logbook.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detail satu Daily Log. Kontrak lama tetap dipertahankan; field identitas dan
 * metadata keputusan ditambahkan di akhir agar konsumen lama tidak berubah.
 */
public record DayDto(
        LocalDate date,
        String status,
        String statusLabel,
        boolean editable,
        OffsetDateTime deadline,
        String dayLabel,
        RangeDto range,
        int totalMinutes,
        List<PerProjectDto> perProject,
        List<TaskDto> tasks,
        String rejectReason,
        Long dailyLogId,
        Long userId,
        String fullName,
        String jenis,
        OffsetDateTime submittedAt,
        boolean autoSubmitted,
        String revisionReason,
        OffsetDateTime decidedAt,
        Long decidedByUserId,
        String decidedByName
) {
    /** Konstruktor lama untuk konsumen yang belum memakai field tambahan. */
    public DayDto(
            LocalDate date,
            String status,
            String statusLabel,
            boolean editable,
            OffsetDateTime deadline,
            String dayLabel,
            RangeDto range,
            int totalMinutes,
            List<PerProjectDto> perProject,
            List<TaskDto> tasks,
            String rejectReason
    ) {
        this(date, status, statusLabel, editable, deadline, dayLabel, range, totalMinutes,
                perProject, tasks, rejectReason,
                null, null, null, null, null, false, null, null, null, null);
    }

    public record RangeDto(Integer startMinute, Integer endMinute) {}

    public record PerProjectDto(String name, String color, int minutes) {}
}
