package com.logbook.service;

import com.logbook.entity.UserSettings;
import com.logbook.repository.UserSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Menentukan batas hari sebuah Daily Log berdasarkan jam kerja pengguna.
 *
 * <p>Aturan (SRS LBK-KRM-02):</p>
 * <ul>
 *   <li>Batas normal tanggal D adalah pukul 23.59 pada tanggal D.</li>
 *   <li>Bila {@code work_end} lebih kecil dari {@code work_start} (shift malam
 *       yang melewati tengah malam), batas tanggal D adalah {@code work_end}
 *       pada tanggal D+1.</li>
 * </ul>
 *
 * <p>Jam kerja diambil dari {@code user_settings}; pengguna tanpa pengaturan
 * memakai bawaan 08.00-17.00. Zona waktu mengikuti
 * {@code logbook.zona-waktu}, dan seluruh waktu memakai {@link Clock} server
 * yang dapat dipatok dalam pengujian.</p>
 *
 * <p>Hari aktif pengguna (dipakai untuk menentukan hari yang boleh diisi) ikut
 * bergeser pada shift malam: sebelum {@code work_end}, hari aktif masih tanggal
 * mulai shift. Dengan begitu Daily Log shift malam tetap dapat diisi setelah
 * tengah malam sampai jam kerjanya berakhir. Aturan ini dapat dimatikan melalui
 * {@code logbook.batas-hari.aktif=false}.</p>
 */
@Service
public class DeadlineService {

    private static final String STATUS_DRAFT = "DRAF";
    private static final String STATUS_REJECTED = "DITOLAK";
    private static final String STATUS_MISSED = "TERLEWAT";

    /** Batas normal sebuah tanggal. */
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    private static final LocalTime DEFAULT_WORK_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_WORK_END = LocalTime.of(17, 0);
    private static final WorkHours DEFAULT_WORK_HOURS =
            new WorkHours(DEFAULT_WORK_START, DEFAULT_WORK_END);

    private final boolean active;
    private final ZoneId zoneId;
    private final Clock clock;
    private final UserSettingsRepository userSettingsRepository;

    @Autowired
    public DeadlineService(
            @Value("${logbook.batas-hari.aktif:true}") boolean active,
            Clock clock,
            ZoneId applicationZoneId,
            UserSettingsRepository userSettingsRepository
    ) {
        this(active, applicationZoneId, clock, userSettingsRepository);
    }

    /**
     * Konstruktor untuk pengujian agar waktu dapat dipatok. Tanpa repository,
     * jam kerja yang dipakai adalah bawaan 08.00-17.00.
     */
    DeadlineService(boolean active, ZoneId zoneId, Clock clock) {
        this(active, zoneId, clock, null);
    }

    /**
     * Konstruktor untuk pengujian jam kerja pengguna (mis. shift malam).
     */
    DeadlineService(
            boolean active,
            ZoneId zoneId,
            Clock clock,
            UserSettingsRepository userSettingsRepository
    ) {
        this.active = active;
        this.zoneId = zoneId;
        this.clock = clock;
        this.userSettingsRepository = userSettingsRepository;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Batas hari tanggal {@code logDate} menurut jam kerja {@code userId}:
     * 23.59 pada tanggal tersebut, atau {@code work_end} tanggal berikutnya
     * untuk shift malam.
     */
    public OffsetDateTime deadlineFor(Long userId, LocalDate logDate) {
        WorkHours workHours = workHoursOf(userId);

        if (workHours.crossesMidnight()) {
            return logDate.plusDays(1)
                    .atTime(workHours.end())
                    .atZone(zoneId)
                    .toOffsetDateTime();
        }

        return logDate.atTime(END_OF_DAY)
                .atZone(zoneId)
                .toOffsetDateTime();
    }

    /**
     * True bila hari tersebut sudah melewati deadline pengguna. Saat aturan
     * dimatikan, method ini selalu false agar pemanggil tidak perlu memeriksa
     * flag lagi.
     */
    public boolean isPastDeadline(Long userId, LocalDate logDate) {
        if (!active || logDate == null) {
            return false;
        }

        return !OffsetDateTime.now(clock).isBefore(deadlineFor(userId, logDate));
    }

    /**
     * Tanggal hari ini pada zona waktu aplikasi (tanggal kalender).
     */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Hari aktif {@code userId}. Pada shift malam, sebelum jam kerja berakhir
     * hari aktif masih tanggal mulai shift sehingga Daily Log semalam tetap
     * dapat diisi sampai {@code work_end}.
     */
    public LocalDate activeDay(Long userId) {
        LocalDate calendarToday = LocalDate.now(clock);
        WorkHours workHours = workHoursOf(userId);

        if (workHours.crossesMidnight() && LocalTime.now(clock).isBefore(workHours.end())) {
            return calendarToday.minusDays(1);
        }

        return calendarToday;
    }

    /**
     * Waktu sekarang menurut jam server pada zona waktu aplikasi. Dipakai
     * tugas berkala agar stempel waktu konsisten dengan penentuan batas hari.
     */
    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    /**
     * True bila tanggal tersebut belum tiba bagi pengguna. Saat aturan
     * dimatikan, method ini selalu false agar semua tanggal dapat diisi.
     */
    public boolean isFutureDay(Long userId, LocalDate logDate) {
        if (!active || logDate == null) {
            return false;
        }

        return logDate.isAfter(activeDay(userId));
    }

    /**
     * True bila tanggal tersebut adalah hari aktif pengguna, atau bila aturan
     * dimatikan.
     */
    public boolean isToday(Long userId, LocalDate logDate) {
        return !active || (logDate != null && logDate.equals(activeDay(userId)));
    }

    /**
     * Daily Log dapat disunting hanya jika berstatus DRAF atau DITOLAK dan
     * tanggalnya masih hari aktif pengguna. Hari yang sudah lewat maupun yang
     * belum tiba tetap terkunci, termasuk pada shift malam. Status DITOLAK ikut
     * dapat disunting agar talent dapat memperbaiki lalu mengirim ulang pada
     * hari yang sama (LBK-KRM-01).
     *
     * <p>Saat aturan batas hari dimatikan, hari TERLEWAT boleh diisi kembali
     * dan akan menjadi DRAF (OCD 5.3.6).</p>
     */
    public boolean isEditable(Long userId, String status, LocalDate logDate) {
        boolean editableStatus = STATUS_DRAFT.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || (!active && STATUS_MISSED.equalsIgnoreCase(status));

        return editableStatus && isToday(userId, logDate);
    }

    /**
     * Zona waktu yang dipakai untuk menentukan hari ini dan batas hari.
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    private WorkHours workHoursOf(Long userId) {
        if (userSettingsRepository == null || userId == null) {
            return DEFAULT_WORK_HOURS;
        }

        Optional<UserSettings> settings = userSettingsRepository.findById(userId);
        if (settings.isEmpty()) {
            return DEFAULT_WORK_HOURS;
        }

        UserSettings userSettings = settings.get();
        LocalTime start = userSettings.getWorkStart() == null
                ? DEFAULT_WORK_START
                : userSettings.getWorkStart();
        LocalTime end = userSettings.getWorkEnd() == null
                ? DEFAULT_WORK_END
                : userSettings.getWorkEnd();

        return new WorkHours(start, end);
    }

    /**
     * Jam kerja pengguna. Shift malam bila jam selesai lebih awal daripada jam
     * mulai, sehingga {@code work_end} jatuh pada tanggal berikutnya.
     */
    private record WorkHours(LocalTime start, LocalTime end) {

        boolean crossesMidnight() {
            return end.isBefore(start);
        }
    }
}
