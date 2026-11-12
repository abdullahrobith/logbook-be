package com.logbook.service;

import com.logbook.entity.DailyLog;
import com.logbook.entity.Holiday;
import com.logbook.entity.Leave;
import com.logbook.entity.LogTask;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.HolidayRepository;
import com.logbook.repository.LeaveRepository;
import com.logbook.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tugas berkala yang menjaga status Daily Log tetap benar tanpa campur tangan
 * pengguna (OCD 5.2, 5.3.6, dan skenario 5):
 *
 * <ul>
 *   <li>Draf yang sudah melewati batas hari dikirim otomatis apa adanya dan
 *       ditandai {@code auto_submitted}.</li>
 *   <li>Hari kerja tanpa isi yang sudah melewati batas hari ditandai
 *       TERLEWAT, paling jauh {@code logbook.terlewat.hari-ke-belakang} hari.
 *       Akhir pekan, libur nasional, cuti, dan akun admin tidak pernah
 *       ditandai TERLEWAT.</li>
 * </ul>
 *
 * <p>Seluruh pemeriksaan dilewati bila {@code logbook.batas-hari.aktif=false}
 * atau {@code logbook.jadwal.aktif=false}. Tugas berjalan setiap 15 menit pada
 * zona waktu aplikasi, sehingga perubahan konfigurasi cukup dengan restart.</p>
 */
@Slf4j
@Service
public class DailyLogAutomationService {

    private static final String STATUS_DRAFT = "DRAF";
    private static final String STATUS_MISSED = "TERLEWAT";
    private static final short DEFAULT_WORK_SCHEDULE_ID = 1;

    private final DailyLogRepository dailyLogRepository;
    private final UserRepository userRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRepository leaveRepository;
    private final DeadlineService deadlineService;
    private final boolean scheduleEnabled;
    private final int historyDays;

    public DailyLogAutomationService(
            DailyLogRepository dailyLogRepository,
            UserRepository userRepository,
            HolidayRepository holidayRepository,
            LeaveRepository leaveRepository,
            DeadlineService deadlineService,
            @Value("${logbook.jadwal.aktif:true}") boolean scheduleEnabled,
            @Value("${logbook.terlewat.hari-ke-belakang:31}") int historyDays
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.userRepository = userRepository;
        this.holidayRepository = holidayRepository;
        this.leaveRepository = leaveRepository;
        this.deadlineService = deadlineService;
        this.scheduleEnabled = scheduleEnabled;
        this.historyDays = Math.max(historyDays, 1);
    }

    @Scheduled(
            cron = "${logbook.jadwal.cron:0 */15 * * * *}",
            zone = "${logbook.zona-waktu:Asia/Jakarta}"
    )
    @Transactional
    public void runScheduled() {
        if (!scheduleEnabled) {
            log.info("Tugas berkala Daily Log dilewati: logbook.jadwal.aktif=false");
            return;
        }

        if (!deadlineService.isActive()) {
            log.info("Tugas berkala Daily Log dilewati: batas hari nonaktif");
            return;
        }

        LocalDate today = deadlineService.today();
        OffsetDateTime now = deadlineService.now();

        DraftResult drafts = autoSubmitDrafts(today, now);
        int markedMissed = drafts.markedMissed() + markMissedDays(today, now);

        log.info(
                "Tugas berkala Daily Log selesai untuk {}: {} Draf dikirim otomatis, "
                        + "{} hari ditandai TERLEWAT",
                today,
                drafts.autoSubmitted(),
                markedMissed
        );
    }

    /**
     * Draf dengan isi dikirim otomatis. Draf kosong pada hari kerja menjadi
     * TERLEWAT; draf kosong pada hari libur/cuti/akhir pekan dihapus agar hari
     * itu kembali Belum mulai.
     */
    private DraftResult autoSubmitDrafts(LocalDate today, OffsetDateTime now) {
        List<DailyLog> drafts = dailyLogRepository.findDraftsUpTo(today);
        int autoSubmitted = 0;
        int markedMissed = 0;

        for (DailyLog dailyLog : drafts) {
            if (!deadlineService.isPastDeadline(dailyLog.getLogDate())) {
                continue;
            }

            List<LogTask> tasks = dailyLog.getTasks();
            boolean hasTasks = tasks != null && !tasks.isEmpty();

            if (hasTasks) {
                dailyLog.submitAutomatically(now);
                dailyLogRepository.save(dailyLog);
                autoSubmitted++;

                log.info(
                        "Daily Log ID {} tanggal {} milik User ID {} dikirim otomatis",
                        dailyLog.getId(),
                        dailyLog.getLogDate(),
                        dailyLog.getUserId()
                );
                continue;
            }

            if (isWorkingDay(dailyLog.getUserId(), dailyLog.getLogDate())) {
                dailyLog.markMissed(now);
                dailyLogRepository.save(dailyLog);
                markedMissed++;

                log.info(
                        "Daily Log ID {} tanggal {} milik User ID {} kosong dan ditandai TERLEWAT",
                        dailyLog.getId(),
                        dailyLog.getLogDate(),
                        dailyLog.getUserId()
                );
            } else {
                dailyLogRepository.delete(dailyLog);

                log.info(
                        "Draf kosong Daily Log ID {} tanggal {} milik User ID {} dihapus "
                                + "(bukan hari kerja)",
                        dailyLog.getId(),
                        dailyLog.getLogDate(),
                        dailyLog.getUserId()
                );
            }
        }

        return new DraftResult(autoSubmitted, markedMissed);
    }

    /**
     * Menandai hari kerja yang sudah lewat batas hari tetapi belum memiliki
     * Daily Log. Data libur dan cuti dimuat sekali untuk seluruh rentang.
     */
    private int markMissedDays(LocalDate today, OffsetDateTime now) {
        LocalDate firstDate = today.minusDays(historyDays);
        LocalDate lastDate = today.minusDays(1);

        if (lastDate.isBefore(firstDate)) {
            return 0;
        }

        List<User> users = userRepository.findByActiveTrueOrderByFullNameAsc();
        if (users.isEmpty()) {
            return 0;
        }

        Set<LocalDate> holidayDates = new HashSet<>();
        for (Holiday holiday : holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDate(firstDate, lastDate)) {
            holidayDates.add(holiday.getHolidayDate());
        }

        Map<Long, List<Leave>> leavesByUser = new HashMap<>();
        for (Leave leave : leaveRepository.findOverlapping(firstDate, lastDate)) {
            leavesByUser.computeIfAbsent(leave.getUserId(), key -> new ArrayList<>())
                    .add(leave);
        }

        Map<Long, Set<LocalDate>> existingDatesByUser = new HashMap<>();
        for (DailyLog dailyLog : dailyLogRepository.findByLogDateBetween(firstDate, lastDate)) {
            existingDatesByUser
                    .computeIfAbsent(dailyLog.getUserId(), key -> new HashSet<>())
                    .add(dailyLog.getLogDate());
        }

        int markedMissed = 0;

        for (User user : users) {
            if (user.isAdmin()) {
                continue;
            }

            LocalDate startDate = firstDate;
            if (user.getCreatedAt() != null) {
                LocalDate joinedDate = user.getCreatedAt()
                        .atZoneSameInstant(deadlineService.getZoneId())
                        .toLocalDate();
                if (joinedDate.isAfter(startDate)) {
                    startDate = joinedDate;
                }
            }

            if (startDate.isAfter(lastDate)) {
                continue;
            }

            Set<LocalDate> existingDates = existingDatesByUser.getOrDefault(
                    user.getId(),
                    Set.of()
            );
            List<Leave> userLeaves = leavesByUser.getOrDefault(user.getId(), List.of());

            for (LocalDate date = startDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
                if (existingDates.contains(date)) {
                    continue;
                }

                if (!isWorkingDay(date, holidayDates.contains(date), userLeaves)) {
                    continue;
                }

                // Native insert ... on conflict do nothing membuat tugas tetap
                // aman bila dijalankan bersamaan atau lebih dari sekali.
                markedMissed += dailyLogRepository.insertDraftIfAbsent(
                        user.getId(),
                        date,
                        DEFAULT_WORK_SCHEDULE_ID,
                        STATUS_MISSED,
                        now
                );

                log.info(
                        "Hari kerja {} tanpa isi ditandai TERLEWAT untuk User ID {}",
                        date,
                        user.getId()
                );
            }
        }

        return markedMissed;
    }

    private boolean isWorkingDay(Long userId, LocalDate date) {
        return CalendarService.isWorkingDay(
                date,
                holidayRepository.existsByHolidayDate(date),
                leaveRepository.countCovering(userId, date) > 0
        );
    }

    private boolean isWorkingDay(
            LocalDate date,
            boolean holiday,
            List<Leave> leaves
    ) {
        boolean onLeave = leaves.stream().anyMatch(leave -> leave.covers(date));
        return CalendarService.isWorkingDay(date, holiday, onLeave);
    }

    /**
     * Hasil satu putaran untuk draf: berapa yang dikirim otomatis dan berapa
     * draf kosong yang berubah menjadi TERLEWAT.
     */
    private record DraftResult(int autoSubmitted, int markedMissed) {
    }
}
