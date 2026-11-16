package com.logbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "work_start", nullable = false)
    private LocalTime workStart;

    @Column(name = "work_end", nullable = false)
    private LocalTime workEnd;

    @Column(name = "onboarded_at")
    private OffsetDateTime onboardedAt;

    protected UserSettings() {
    }

    public UserSettings(User user, LocalTime workStart, LocalTime workEnd) {
        this.user = user;
        this.userId = user.getId();
        this.workStart = workStart;
        this.workEnd = workEnd;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public void setWorkStart(LocalTime workStart) {
        this.workStart = workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public void setWorkEnd(LocalTime workEnd) {
        this.workEnd = workEnd;
    }

    public OffsetDateTime getOnboardedAt() {
        return onboardedAt;
    }

    public void setOnboardedAt(OffsetDateTime onboardedAt) {
        this.onboardedAt = onboardedAt;
    }

    public boolean isOnboarded() {
        return onboardedAt != null;
    }
}
