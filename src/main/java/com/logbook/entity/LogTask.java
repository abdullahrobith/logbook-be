
package com.logbook.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "log_tasks")
public class LogTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "task_type_id", nullable = false)
    private Short taskTypeId;

    @Column(name = "work_status_id", nullable = false)
    private Short workStatusId;

    private String ticket;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Column(nullable = false)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public LogTask() {
    }

    public LogTask(
            DailyLog dailyLog,
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            String ticket,
            LocalTime startTime,
            Short durationMinutes,
            String summary,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.dailyLog = dailyLog;
        this.projectId = projectId;
        this.taskTypeId = taskTypeId;
        this.workStatusId = workStatusId;
        this.ticket = ticket;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.summary = summary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Short getTaskTypeId() { return taskTypeId; }
    public Short getWorkStatusId() { return workStatusId; }
    public String getTicket() { return ticket; }
    public LocalTime getStartTime() { return startTime; }
    public Short getDurationMinutes() { return durationMinutes; }
    public String getSummary() { return summary; }
}
