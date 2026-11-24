package com.logbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

/**
 * State pembatasan laju permintaan yang tersimpan di database sehingga tetap
 * konsisten ketika aplikasi dijalankan dalam beberapa instance atau di-restart.
 */
@Entity
@Table(
        name = "rate_limit_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uk_rate_limit_bucket_key", columnNames = "bucket_key")
)
public class RateLimitAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bucket_key", nullable = false, length = 255)
    private String bucketKey;

    @Column(name = "window_started_at", nullable = false)
    private OffsetDateTime windowStartedAt;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String bucketKey) {
        this.bucketKey = bucketKey;
    }

    public OffsetDateTime getWindowStartedAt() {
        return windowStartedAt;
    }

    public void setWindowStartedAt(OffsetDateTime windowStartedAt) {
        this.windowStartedAt = windowStartedAt;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
