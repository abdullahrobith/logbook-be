package com.logbook.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Rekap mingguan/bulanan untuk talent sendiri (SRS LBK-KUA-04).
 *
 * <p>{@code totalMinutes} adalah jumlah durasi union per hari, sedangkan
 * {@code perType} dan {@code perProject} memakai jumlah durasi penuh task.
 * Hari libur/cuti tidak dihitung sebagai hari kerja, tetapi catatan yang tetap
 * dibuat pada hari tersebut masih mengisi rekap.</p>
 */
public record SummaryResponse(
        int totalMinutes,
        int daysRecorded,
        int workdays,
        List<TypeSummary> perType,
        List<ProjectSummary> perProject,
        List<TaskSummary> done,
        List<TaskSummary> ongoing,
        List<ObstacleSummary> obstacles,
        List<ShortSummaryDay> shortSummaryDays
) {

    public SummaryResponse {
        perType = perType == null ? List.of() : List.copyOf(perType);
        perProject = perProject == null ? List.of() : List.copyOf(perProject);
        done = done == null ? List.of() : List.copyOf(done);
        ongoing = ongoing == null ? List.of() : List.copyOf(ongoing);
        obstacles = obstacles == null ? List.of() : List.copyOf(obstacles);
        shortSummaryDays = shortSummaryDays == null
                ? List.of()
                : List.copyOf(shortSummaryDays);
    }

    public record TypeSummary(String name, int minutes) {
    }

    public record ProjectSummary(String name, String color, int minutes) {
    }

    public record TaskSummary(
            LocalDate date,
            Long taskId,
            String project,
            String color,
            String ticket,
            String type,
            String summary,
            String workStatus,
            Integer startMinute,
            Integer durationMinutes
    ) {
    }

    public record ObstacleSummary(
            LocalDate date,
            Long taskId,
            String project,
            String ticket,
            String obstacle
    ) {
    }

    /** Hari dengan jumlah ringkasan singkat, disebut secara netral (tanpa skor). */
    public record ShortSummaryDay(LocalDate date, int count) {
    }
}
