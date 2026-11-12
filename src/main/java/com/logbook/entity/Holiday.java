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
 * Libur nasional yang berlaku untuk semua pengguna.
 *
 * <p>Tanggal pada tabel ini tidak dihitung sebagai hari kerja sehingga tidak
 * pernah ditandai TERLEWAT. Hari libur tetap boleh diisi oleh talent.</p>
 */
@Entity
@Table(name = "holidays")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Holiday() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getName() {
        return name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
