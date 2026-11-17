package com.logbook.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Cuti seorang talent pada rentang tanggal tertentu.
 *
 * <p>Tanggal cuti tidak dihitung sebagai hari kerja sehingga tidak pernah
 * ditandai TERLEWAT, tetapi tetap boleh diisi bila talent bekerja.</p>
 */
@Entity
@Table(name = "leaves")
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tambahkan @JsonProperty agar JSON-nya tetap "user_id"
    @JsonProperty("user_id")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Tambahkan @JsonProperty agar JSON-nya tetap "start_date"
    @JsonProperty("start_date")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Tambahkan @JsonProperty agar JSON-nya tetap "end_date"
    @JsonProperty("end_date")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Tambahkan @JsonProperty agar JSON-nya tetap "note"
    @JsonProperty("note")
    @Column(name = "note") 
    private String reason;

    // Tambahkan @JsonProperty agar JSON-nya tetap "created_at"
    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Leave() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * True bila tanggal tersebut berada di dalam rentang cuti.
     */
    public boolean covers(LocalDate date) {
        return date != null
                && !date.isBefore(startDate)
                && !date.isAfter(endDate);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}