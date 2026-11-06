package com.logbook.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_logs")
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "work_schedule_id", nullable = false)
    private Short workScheduleId;

    @Column(nullable = false)
    private String status;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "dailyLog", fetch = FetchType.LAZY)
    @OrderBy("startTime ASC")
    private List<LogTask> tasks = new ArrayList<>();

    public DailyLog() {
    }

    public DailyLog(
            Long userId,
            LocalDate logDate,
            Short workScheduleId,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.userId = userId;
        this.logDate = logDate;
        this.workScheduleId = workScheduleId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void submit(OffsetDateTime now) {
        this.status = "TERKIRIM";
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public void withdraw(OffsetDateTime now) {
        this.status = "DRAF";
        this.submittedAt = null;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getLogDate() { return logDate; }
    public Short getWorkScheduleId() { return workScheduleId; }
    public String getStatus() { return status; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<LogTask> getTasks() { return tasks; }
}