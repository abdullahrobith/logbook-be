package com.logbook.service;

import com.logbook.dto.SubmitCheckResponse;
import com.logbook.dto.SubmitCheckResponse.Hint;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.repository.DailyLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Menyusun saran sebelum pengiriman Daily Log (LBK-KUA-02).
 *
 * <p>Dua jenis saran yang diperiksa:</p>
 * <ul>
 *   <li>{@code RINGKASAN_PENDEK}: ringkasan task lebih pendek dari
 *       {@code logbook.saran.ringkasan-min} karakter.</li>
 *   <li>{@code TASK_PANJANG}: durasi task melebihi
 *       {@code logbook.saran.task-maks-menit} menit.</li>
 * </ul>
 *
 * <p>Batas bernilai nol atau negatif mematikan saran terkait. Pemeriksaan ini
 * tidak mengubah data dan tidak pernah menghalangi pengiriman; keputusan
 * melanjutkan tetap milik talent.</p>
 */
@Slf4j
@Service
public class SubmitCheckService {

    private final int ringkasanMin;
    private final int taskMaksMenit;
    private final DailyLogRepository dailyLogRepository;

    public SubmitCheckService(
            @Value("${logbook.saran.ringkasan-min:20}") int ringkasanMin,
            @Value("${logbook.saran.task-maks-menit:240}") int taskMaksMenit,
            DailyLogRepository dailyLogRepository
    ) {
        this.ringkasanMin = Math.max(ringkasanMin, 0);
        this.taskMaksMenit = Math.max(taskMaksMenit, 0);
        this.dailyLogRepository = dailyLogRepository;
    }

    /**
     * Pemeriksaan untuk Daily Log tertentu tanpa mengubah statusnya. Dipakai
     * {@code POST /api/daily-logs/{dailyLogId}/submit?check=true}.
     */
    @Transactional(readOnly = true)
    public SubmitCheckResponse check(Long userId, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findByIdAndUserId(dailyLogId, userId)
                .orElseThrow(() -> {
                    log.warn("Gagal memeriksa: Daily Log ID {} tidak ditemukan atau bukan milik User ID {}",
                            dailyLogId, userId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Daily log tidak ditemukan"
                    );
                });

        return evaluate(dailyLog);
    }

    /**
     * Pemeriksaan untuk tanggal tertentu, sesuai kontrak SRS LBK-KUA-02
     * ({@code POST /api/days/{date}/submit?check=true}).
     */
    @Transactional(readOnly = true)
    public SubmitCheckResponse checkOn(Long userId, LocalDate date) {
        DailyLog dailyLog = dailyLogRepository.findByUserIdAndLogDate(userId, date)
                .orElseThrow(() -> {
                    log.warn("Gagal memeriksa: Daily Log tanggal {} tidak ditemukan untuk User ID {}",
                            date, userId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Daily log tanggal " + date + " tidak ditemukan"
                    );
                });

        return evaluate(dailyLog);
    }

    public SubmitCheckResponse evaluate(DailyLog dailyLog) {
        List<LogTask> tasks = dailyLog.getTasks() == null ? List.of() : dailyLog.getTasks();
        List<Hint> hints = new ArrayList<>();

        for (LogTask task : tasks) {
            String summary = task.getSummary() == null ? "" : task.getSummary().trim();
            if (ringkasanMin > 0 && summary.length() < ringkasanMin) {
                hints.add(new Hint(
                        task.getId(),
                        SubmitCheckResponse.KIND_RINGKASAN_PENDEK,
                        "Ringkasan task baru " + summary.length() + " karakter. "
                                + "Disarankan minimal " + ringkasanMin
                                + " karakter agar hasil pekerjaan mudah dipahami."
                ));
            }

            int duration = task.getDurationMinutes() == null ? 0 : task.getDurationMinutes();
            if (taskMaksMenit > 0 && duration > taskMaksMenit) {
                hints.add(new Hint(
                        task.getId(),
                        SubmitCheckResponse.KIND_TASK_PANJANG,
                        "Durasi task " + duration + " menit melebihi " + taskMaksMenit
                                + " menit. Pertimbangkan memecahnya menjadi beberapa task."
                ));
            }
        }

        log.info("Pemeriksaan sebelum kirim Daily Log ID {} menghasilkan {} saran",
                dailyLog.getId(), hints.size());
        return new SubmitCheckResponse(hints);
    }
}
