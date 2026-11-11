package com.logbook.service;

import com.logbook.dto.DailyLogResponse;
import com.logbook.entity.DailyLog;
import com.logbook.repository.DailyLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class DailyLogService {

    private static final short DEFAULT_WORK_SCHEDULE_ID = 1;
    private static final String STATUS_DRAFT = "DRAF";
    private static final String STATUS_WAITING = "MENUNGGU";
    private static final String STATUS_REJECTED = "DITOLAK";

    private final DailyLogRepository dailyLogRepository;

    public DailyLogService(DailyLogRepository dailyLogRepository) {
        this.dailyLogRepository = dailyLogRepository;
    }

    @Transactional(readOnly = true)
    public DailyLogResponse getDailyLog(Long userId, LocalDate date) {
        return dailyLogRepository
                .findByUserIdAndLogDate(userId, date)
                .map(DailyLogResponse::from)
                .orElseThrow(() -> {
                    log.debug("Daily Log User ID {} tanggal {} tidak ditemukan", userId, date);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Daily log tidak ditemukan"
                    );
                });
    }

    @Transactional
    public DailyLogResponse createDailyLog(Long userId, LocalDate date) {
        return dailyLogRepository.findByUserIdAndLogDate(userId, date)
                .map(DailyLogResponse::from)
                .orElseGet(() -> {
                    log.info("Membuat Daily Log baru berstatus DRAF untuk User ID {} tanggal {}",
                            userId, date);

                    OffsetDateTime now = OffsetDateTime.now();
                    dailyLogRepository.insertDraftIfAbsent(
                            userId,
                            date,
                            DEFAULT_WORK_SCHEDULE_ID,
                            STATUS_DRAFT,
                            now
                    );

                    DailyLog created = dailyLogRepository.findByUserIdAndLogDate(userId, date)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Daily log gagal dibuat"
                            ));

                    log.info("Berhasil membuat Daily Log ID {} untuk User ID {} tanggal {}",
                            created.getId(), userId, date);
                    return DailyLogResponse.from(created);
                });
    }

    @Transactional
    public DailyLogResponse submitDailyLog(Long userId, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findByIdAndUserId(dailyLogId, userId)
                .orElseThrow(() -> {
                    log.warn("Gagal submit: Daily Log ID {} tidak ditemukan atau bukan milik User ID {}",
                            dailyLogId, userId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Daily log tidak ditemukan"
                    );
                });

        if (STATUS_WAITING.equalsIgnoreCase(dailyLog.getStatus())) {
            log.info("Daily Log ID {} sudah berstatus MENUNGGU, submit diabaikan", dailyLogId);
            return DailyLogResponse.from(dailyLog);
        }

        // DRAF dikirim pertama kali; DITOLAK dikirim ulang setelah diperbaiki
        // (LBK-KRM-01).
        if (!STATUS_DRAFT.equalsIgnoreCase(dailyLog.getStatus())
                && !STATUS_REJECTED.equalsIgnoreCase(dailyLog.getStatus())) {
            log.warn("Gagal submit: Daily Log ID {} berstatus {}, bukan DRAF/DITOLAK",
                    dailyLogId, dailyLog.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Daily log tidak dapat dikirim pada status saat ini"
            );
        }

        if (dailyLog.getTasks() == null || dailyLog.getTasks().isEmpty()) {
            log.warn("Gagal submit: Daily Log ID {} belum memiliki task", dailyLogId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Daily log belum memiliki task untuk dikirim"
            );
        }

        dailyLog.submit(OffsetDateTime.now());
        DailyLog saved = dailyLogRepository.save(dailyLog);

        log.info("Berhasil submit Daily Log ID {} tanggal {} milik User ID {}",
                saved.getId(), saved.getLogDate(), userId);
        return DailyLogResponse.from(saved);
    }

    @Transactional
    public DailyLogResponse withdrawDailyLog(Long userId, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findByIdAndUserId(dailyLogId, userId)
                .orElseThrow(() -> {
                    log.warn("Gagal withdraw: Daily Log ID {} tidak ditemukan atau bukan milik User ID {}",
                            dailyLogId, userId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Daily log tidak ditemukan"
                    );
                });

        if (STATUS_DRAFT.equalsIgnoreCase(dailyLog.getStatus())) {
            log.info("Daily Log ID {} sudah berstatus DRAF, withdraw diabaikan", dailyLogId);
            return DailyLogResponse.from(dailyLog);
        }

        if (!STATUS_WAITING.equalsIgnoreCase(dailyLog.getStatus())) {
            log.warn("Gagal withdraw: Daily Log ID {} berstatus {}, bukan MENUNGGU",
                    dailyLogId, dailyLog.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Daily log tidak dapat ditarik pada status saat ini"
            );
        }

        dailyLog.withdraw(OffsetDateTime.now());
        DailyLog saved = dailyLogRepository.save(dailyLog);

        log.info("Berhasil withdraw Daily Log ID {} tanggal {} milik User ID {}",
                saved.getId(), saved.getLogDate(), userId);
        return DailyLogResponse.from(saved);
    }
}
