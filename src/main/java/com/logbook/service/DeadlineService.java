package com.logbook.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Menentukan batas hari sebuah Daily Log.
 *
 * <p>Deadline sebuah tanggal adalah awal hari berikutnya pada zona waktu yang
 * dikonfigurasi ({@code logbook.zona-waktu}). Setelah deadline terlewati, hari
 * tersebut dianggap sudah berlalu sehingga tidak dapat ditambah, diubah, atau
 * dikirim. Hari yang belum tiba juga terkunci: hanya Daily Log tanggal hari ini
 * yang dapat diisi. Aturan ini dapat dimatikan melalui
 * {@code logbook.batas-hari.aktif=false}.</p>
 */
@Service
public class DeadlineService {

    private static final String STATUS_DRAFT = "DRAF";
    private static final String STATUS_REJECTED = "DITOLAK";
    private static final String STATUS_MISSED = "TERLEWAT";

    private final boolean active;
    private final ZoneId zoneId;
    private final Clock clock;

    @Autowired
    public DeadlineService(
            @Value("${logbook.batas-hari.aktif:true}") boolean active,
            @Value("${logbook.zona-waktu:Asia/Jakarta}") String zoneId
    ) {
        this(active, ZoneId.of(zoneId), Clock.system(ZoneId.of(zoneId)));
    }

    /**
     * Konstruktor untuk pengujian agar waktu dapat dipatok.
     */
    DeadlineService(boolean active, ZoneId zoneId, Clock clock) {
        this.active = active;
        this.zoneId = zoneId;
        this.clock = clock;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Awal hari berikutnya pada zona waktu aplikasi.
     */
    public OffsetDateTime deadlineFor(LocalDate logDate) {
        return logDate.plusDays(1)
                .atStartOfDay(zoneId)
                .toOffsetDateTime();
    }

    /**
     * True bila hari tersebut sudah melewati deadline. Saat aturan dimatikan,
     * method ini selalu false agar pemanggil tidak perlu memeriksa flag lagi.
     */
    public boolean isPastDeadline(LocalDate logDate) {
        if (!active || logDate == null) {
            return false;
        }

        return !OffsetDateTime.now(clock).isBefore(deadlineFor(logDate));
    }

    /**
     * Tanggal hari ini pada zona waktu aplikasi.
     */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Waktu sekarang menurut jam server pada zona waktu aplikasi. Dipakai
     * tugas berkala agar stempel waktu konsisten dengan penentuan batas hari.
     */
    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    /**
     * True bila tanggal tersebut belum tiba. Saat aturan dimatikan, method ini
     * selalu false agar semua tanggal dapat diisi.
     */
    public boolean isFutureDay(LocalDate logDate) {
        if (!active || logDate == null) {
            return false;
        }

        return logDate.isAfter(today());
    }

    /**
     * True bila tanggal tersebut adalah hari ini, atau bila aturan dimatikan.
     */
    public boolean isToday(LocalDate logDate) {
        return !active || (logDate != null && logDate.equals(today()));
    }

    /**
     * Daily Log dapat disunting hanya jika berstatus DRAF atau DITOLAK dan
     * tanggalnya hari ini. Hari yang sudah lewat maupun yang belum tiba tetap
     * terkunci. Status DITOLAK ikut dapat disunting agar talent dapat
     * memperbaiki lalu mengirim ulang pada hari yang sama (LBK-KRM-01).
     *
     * <p>Saat aturan batas hari dimatikan, hari TERLEWAT boleh diisi kembali
     * dan akan menjadi DRAF (OCD 5.3.6).</p>
     */
    public boolean isEditable(String status, LocalDate logDate) {
        boolean editableStatus = STATUS_DRAFT.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || (!active && STATUS_MISSED.equalsIgnoreCase(status));

        return editableStatus && isToday(logDate);
    }

    /**
     * Zona waktu yang dipakai untuk menentukan hari ini dan batas hari.
     */
    public ZoneId getZoneId() {
        return zoneId;
    }
}
