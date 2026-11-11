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

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "revision_reason", length = 1000)
    private String revisionReason;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "auto_submitted", nullable = false)
    private boolean autoSubmitted;

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
        this.status = "MENUNGGU";
        this.submittedAt = now;
        this.autoSubmitted = false;
        // Daily Log kembali menunggu keputusan, jadi jejak keputusan dan alasan
        // dari putaran sebelumnya tidak ditampilkan lagi. Jejak audit tetap
        // tersimpan pada log aplikasi level INFO.
        this.rejectionReason = null;
        this.revisionReason = null;
        this.decidedByUserId = null;
        this.decidedAt = null;
        this.updatedAt = now;
    }

    public void withdraw(OffsetDateTime now) {
        this.status = "DRAF";
        this.submittedAt = null;
        this.updatedAt = now;
    }

    /**
     * Atasan atau admin menyetujui Daily Log yang sedang menunggu.
     */
    public void approve(Long decidedByUserId, OffsetDateTime now) {
        this.status = "DISETUJUI";
        this.decidedByUserId = decidedByUserId;
        this.decidedAt = now;
        this.updatedAt = now;
    }

    /**
     * Atasan atau admin menolak Daily Log disertai alasan wajib.
     */
    public void reject(Long decidedByUserId, String reason, OffsetDateTime now) {
        this.status = "DITOLAK";
        this.rejectionReason = reason;
        this.decidedByUserId = decidedByUserId;
        this.decidedAt = now;
        this.updatedAt = now;
    }

    /**
     * Talent mengajukan perbaikan atas Daily Log yang sudah disetujui.
     */
    public void requestRevision(String reason, OffsetDateTime now) {
        this.status = "PERBAIKAN_DIAJUKAN";
        this.revisionReason = reason;
        this.updatedAt = now;
    }

    /**
     * Atasan atau admin menyetujui pengajuan perbaikan. Sesuai SRS 3.1, hari
     * itu dibuka kembali sebagai DRAF agar talent dapat menyunting lagi.
     */
    public void approveRevision(Long decidedByUserId, OffsetDateTime now) {
        this.status = "DRAF";
        this.decidedByUserId = decidedByUserId;
        this.decidedAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getLogDate() { return logDate; }
    public Short getWorkScheduleId() { return workScheduleId; }
    public String getStatus() { return status; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public String getRevisionReason() { return revisionReason; }
    public Long getDecidedByUserId() { return decidedByUserId; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public boolean isAutoSubmitted() { return autoSubmitted; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<LogTask> getTasks() { return tasks; }
}
