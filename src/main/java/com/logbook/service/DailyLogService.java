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
                    DailyLog dailyLog = new DailyLog(
                            userId,
                            date,
                            DEFAULT_WORK_SCHEDULE_ID,
                            STATUS_DRAFT,
                            now,
                            now
                    );
                    return DailyLogResponse.from(dailyLogRepository.save(dailyLog));
                });
    }
}