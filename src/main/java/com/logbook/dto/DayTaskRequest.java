package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DayTaskRequest {

    private final String project;
    private final String color;
    private final String ticket;

    @NotNull(message = "Menit mulai wajib diisi")
    @Min(value = 0, message = "Menit mulai minimal 0")
    @Max(value = 2879, message = "Menit mulai maksimal 2879")
    private final Integer startMinute;

    @NotNull(message = "Durasi wajib diisi")
    @Min(value = 5, message = "Durasi minimal 5 menit")
    @Max(value = 1440, message = "Durasi maksimal 1440 menit")
    private final Short durationMinutes;

    private final String type;

    @NotBlank(message = "Ringkasan wajib diisi")
    private final String summary;

    private final String workStatus;
    private final String obstacle;

    @JsonCreator
    public DayTaskRequest(
            @JsonProperty("project") String project,
            @JsonProperty("color") String color,
            @JsonProperty("ticket") String ticket,
            @JsonProperty("startMinute") @JsonAlias("start_minute") Integer startMinute,
            @JsonProperty("startTime") @JsonAlias({"start_time"}) Object startTime,
            @JsonProperty("durationMinutes") @JsonAlias("duration_minutes") Short durationMinutes,
            @JsonProperty("type") String type,
            @JsonProperty("summary") String summary,
            @JsonProperty("workStatus") @JsonAlias({"work_status"}) String workStatus,
            @JsonProperty("obstacle") String obstacle
    ) {
        this.project = project;
        this.color = color;
        this.ticket = ticket;
        this.startMinute = resolveStartMinute(startMinute, startTime);
        this.durationMinutes = durationMinutes;
        this.type = type;
        this.summary = summary;
        this.workStatus = workStatus;
        this.obstacle = obstacle;
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
        try {
            LocalTime t = LocalTime.parse(s.length() <= 5 ? s : s.substring(0, 8),
                    s.length() <= 5 ? DateTimeFormatter.ofPattern("H:mm") : DateTimeFormatter.ofPattern("H:mm:ss"));
            return t.getHour() * 60 + t.getMinute();
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalTime t = LocalTime.parse(s);
            return t.getHour() * 60 + t.getMinute();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String project() { return project; }
    public String color() { return color; }
    public String ticket() { return ticket; }
    public Integer startMinute() { return startMinute; }
    public Short durationMinutes() { return durationMinutes; }
    public String type() { return type; }
    public String summary() { return summary; }
    public String workStatus() { return workStatus; }
    public String obstacle() { return obstacle; }
}
