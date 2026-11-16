package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LogTaskRequest {

    @NotNull(message = "Project ID wajib diisi")
    @Positive(message = "Project ID harus lebih dari 0")
    private final Long projectId;

    @NotNull(message = "Task type ID wajib diisi")
    @Positive(message = "Task type ID harus lebih dari 0")
    private final Short taskTypeId;

    @NotNull(message = "Work status ID wajib diisi")
    private final Short workStatusId;

    @Pattern(
            regexp = "^(?:[A-Za-z][A-Za-z0-9]*-[0-9]+)?$",
            message = "Format tiket tidak valid. Contoh: ABC-154"
    )
    private final String ticket;

    @NotNull(message = "Menit mulai wajib diisi")
    @Min(value = 0, message = "Menit mulai minimal 0")
    @Max(value = 2879, message = "Menit mulai maksimal 2879")
    private final Integer startMinute;

    @NotNull(message = "Durasi wajib diisi")
    @Min(value = 5, message = "Durasi minimal 5 menit")
    @Max(value = 1440, message = "Durasi maksimal 1440 menit")
    private final Short durationMinutes;

    @NotBlank(message = "Ringkasan wajib diisi")
    private final String summary;

    private final String obstacle;

    @JsonCreator
    public LogTaskRequest(
            @JsonProperty("projectId") Long projectId,
            @JsonProperty("taskTypeId") Short taskTypeId,
            @JsonProperty("workStatusId") Short workStatusId,
            @JsonProperty("ticket") String ticket,
            @JsonProperty("startMinute") @JsonAlias("start_minute") Integer startMinute,
            @JsonProperty("startTime") @JsonAlias({"start_time"}) Object startTime,
            @JsonProperty("durationMinutes") @JsonAlias("duration_minutes") Short durationMinutes,
            @JsonProperty("summary") String summary,
            @JsonProperty("obstacle") String obstacle
    ) {
        this.projectId = projectId;
        this.taskTypeId = taskTypeId;
        this.workStatusId = workStatusId;
        this.ticket = ticket;
        this.startMinute = resolveStartMinute(startMinute, startTime);
        this.durationMinutes = durationMinutes;
        this.summary = summary;
        this.obstacle = obstacle;
    }

    // Convenience constructor for programmatic/test usage (tanpa startTime legacy)
    public LogTaskRequest(
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            String ticket,
            Integer startMinute,
            Short durationMinutes,
            String summary,
            String obstacle
    ) {
        this(projectId, taskTypeId, workStatusId, ticket, startMinute, null, durationMinutes, summary, obstacle);
    }

    private static Integer resolveStartMinute(Integer startMinute, Object startTime) {
        if (startMinute != null) {
            return startMinute;
        }
        if (startTime == null) {
            return null;
        }
        if (startTime instanceof Number n) {
            return n.intValue();
        }
        String s = startTime.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        // Support "HH:mm" or "HH:mm:ss"
        try {
            LocalTime t = LocalTime.parse(s.length() <= 5 ? s : s.substring(0, 8), 
                    s.length() <= 5 ? DateTimeFormatter.ofPattern("H:mm") : DateTimeFormatter.ofPattern("H:mm:ss"));
            return t.getHour() * 60 + t.getMinute();
        } catch (DateTimeParseException ignored) {
        }
        try {
            // Try ISO local time (HH:mm / HH:mm:ss)
            LocalTime t = LocalTime.parse(s);
            return t.getHour() * 60 + t.getMinute();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null; // will fail @NotNull validation with clear message
        }
    }

    public Long projectId() { return projectId; }
    public Short taskTypeId() { return taskTypeId; }
    public Short workStatusId() { return workStatusId; }
    public String ticket() { return ticket; }
    public Integer startMinute() { return startMinute; }
    public Short durationMinutes() { return durationMinutes; }
    public String summary() { return summary; }
    public String obstacle() { return obstacle; }
}
