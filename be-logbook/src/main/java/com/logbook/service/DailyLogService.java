package com.logbook.service;

import com.logbook.dto.DailyLogResponse;
import com.logbook.repository.DailyLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Service
public class DailyLogService {

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
}