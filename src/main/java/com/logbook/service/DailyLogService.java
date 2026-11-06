package com.logbook.service;

import com.logbook.dto.DailyLogResponse;
import com.logbook.entity.DailyLog;
import com.logbook.repository.DailyLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class DailyLogService {

    private static final short DEFAULT_WORK_SCHEDULE_ID = 1;
    private static final String STATUS_DRAFT = "DRAF";
    private static final String STATUS_SUBMITTED = "TERKIRIM";

    private final DailyLogRepository dailyLogRepository;

    public DailyLogService(DailyLogRepository dailyLogRepository) {
        this.dailyLogRepository = dailyLogRepository;
    }

    @Transactional(readOnly = true)
    public DailyLogResponse getDailyLog(Long userId, LocalDate date) {
        return dailyLogRepository
                .findByUserIdAndLogDate(userId, date)
                .map(DailyLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily log tidak ditemukan"
                ));
    }

    @Transactional
    public DailyLogResponse createDailyLog(Long userId, LocalDate date) {
        return dailyLogRepository.findByUserIdAndLogDate(userId, date)
                .map(DailyLogResponse::from)
                .orElseGet(() -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    dailyLogRepository.insertDraftIfAbsent(
                            userId,
                            date,
                            DEFAULT_WORK_SCHEDULE_ID,
                            STATUS_DRAFT,
                            now
                    );

                    return dailyLogRepository.findByUserIdAndLogDate(userId, date)
                            .map(DailyLogResponse::from)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Daily log gagal dibuat"
                            ));
                });
    }

    @Transactional
    public DailyLogResponse submitDailyLog(Long userId, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findByIdAndUserId(dailyLogId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily log tidak ditemukan"
                ));

        if (STATUS_SUBMITTED.equalsIgnoreCase(dailyLog.getStatus())) {
            return DailyLogResponse.from(dailyLog);
        }

        if (!STATUS_DRAFT.equalsIgnoreCase(dailyLog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Daily log tidak dapat dikirim pada status saat ini"
            );
        }

        if (dailyLog.getTasks() == null || dailyLog.getTasks().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Daily log belum memiliki task untuk dikirim"
            );
        }

        dailyLog.submit(OffsetDateTime.now());
        return DailyLogResponse.from(dailyLogRepository.save(dailyLog));
    }

    @Transactional
    public DailyLogResponse withdrawDailyLog(Long userId, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findByIdAndUserId(dailyLogId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily log tidak ditemukan"
                ));

        if (STATUS_DRAFT.equalsIgnoreCase(dailyLog.getStatus())) {
            return DailyLogResponse.from(dailyLog);
        }

        if (!STATUS_SUBMITTED.equalsIgnoreCase(dailyLog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Daily log tidak dapat ditarik pada status saat ini"
            );
        }

        dailyLog.withdraw(OffsetDateTime.now());
        return DailyLogResponse.from(dailyLogRepository.save(dailyLog));
    }
}