package com.logbook.service;

import com.logbook.dto.AdminDailyLogResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Daftar Daily Log seluruh talent untuk admin.
 *
 * <p>Semua filter bersifat opsional dan dapat digabung: tanggal, pengguna, dan
 * status. Status yang dikirim dinormalisasi ke huruf besar agar nilai seperti
 * "menunggu" tetap diterima, tetapi nilai di luar status workflow ditolak
 * dengan 400.</p>
 */
@Slf4j
@Service
public class AdminDailyLogService {

    /** Status yang diizinkan oleh check constraint daily_logs_status_check. */
    public static final Set<String> VALID_STATUSES = new LinkedHashSet<>(List.of(
            "DRAF",
            "MENUNGGU",
            "DISETUJUI",
            "DITOLAK",
            "TERLEWAT",
            "PERBAIKAN_DIAJUKAN",
            "DRAF_DIBUKA_KEMBALI"
    ));

    private final DailyLogRepository dailyLogRepository;
    private final UserRepository userRepository;

    public AdminDailyLogService(
            DailyLogRepository dailyLogRepository,
            UserRepository userRepository
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDailyLogResponse> list(LocalDate date, Long userId, String status) {
        String normalizedStatus = normalizeStatus(status);

        List<DailyLog> dailyLogs = dailyLogRepository.findForAdmin(
                date,
                userId,
                normalizedStatus
        );

        if (dailyLogs.isEmpty()) {
            log.info("Tidak ada Daily Log yang cocok untuk filter date: {}, userId: {}, status: {}",
                    date, userId, normalizedStatus);
            return List.of();
        }

        List<Long> userIds = dailyLogs.stream()
                .map(DailyLog::getUserId)
                .distinct()
                .toList();

        Map<Long, User> users = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AdminDailyLogResponse> responses = dailyLogs.stream()
                .map(dailyLog -> AdminDailyLogResponse.from(
                        dailyLog,
                        users.get(dailyLog.getUserId())
                ))
                .toList();

        log.info("Berhasil mengambil {} Daily Log untuk filter date: {}, userId: {}, status: {}",
                responses.size(), date, userId, normalizedStatus);
        return responses;
    }

    /**
     * Mengubah status menjadi bentuk kanonik. Nilai kosong dianggap tanpa
     * filter; nilai yang tidak dikenal ditolak agar kesalahan ketik tidak
     * diam-diam mengembalikan daftar kosong.
     */
    public String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalized)) {
            log.warn("Status filter tidak valid: {}", status);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status tidak valid. Pilihan: " + String.join(", ", VALID_STATUSES)
            );
        }

        return normalized;
    }
}
