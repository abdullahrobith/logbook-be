package com.logbook.entity;

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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

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
}
