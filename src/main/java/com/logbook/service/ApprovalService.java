package com.logbook.service;

import com.logbook.dto.DailyLogResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

/**
 * Keputusan atas Daily Log: setujui, tolak, ajukan perbaikan, dan setujui
 * perbaikan.
 *
 * <p>Aturan yang ditegakkan:</p>
 * <ul>
 *   <li>Hanya atasan langsung atau admin yang boleh memutuskan Daily Log
 *       (lihat {@link RoleService#canDecideTalent(User, User)}).</li>
 *   <li>Siapa pun, termasuk atasan dan admin, tidak boleh memutuskan Daily Log
 *       miliknya sendiri.</li>
 *   <li>Alasan wajib diisi saat menolak dan saat mengajukan perbaikan.</li>
 *   <li>Setiap keputusan mengisi decided_by_user_id/decided_at dan dicatat pada
 *       log aplikasi level INFO berisi pelaku serta tanggal Daily Log.</li>
 * </ul>
 */
@Slf4j
@Service
public class ApprovalService {

    private static final String STATUS_DRAFT = "DRAF";
    private static final String STATUS_WAITING = "MENUNGGU";
    private static final String STATUS_APPROVED = "DISETUJUI";
    private static final String STATUS_REVISION_REQUESTED = "PERBAIKAN_DIAJUKAN";

    private static final int MAX_REASON_LENGTH = 1000;

    private final DailyLogRepository dailyLogRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final DeadlineService deadlineService;

    public ApprovalService(
            DailyLogRepository dailyLogRepository,
            UserRepository userRepository,
            RoleService roleService,
            DeadlineService deadlineService
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.deadlineService = deadlineService;
    }

    /**
     * Atasan langsung atau admin menyetujui Daily Log yang berstatus MENUNGGU.
     */
    @Transactional
    public DailyLogResponse approve(User actor, Long dailyLogId) {
        DailyLog dailyLog = requireDecidableLog(actor, dailyLogId);
        requireStatus(dailyLog, STATUS_WAITING, "disetujui");

        dailyLog.approve(actor.getId(), OffsetDateTime.now());
        DailyLog saved = dailyLogRepository.save(dailyLog);

        log.info(
                "Daily Log ID {} tanggal {} milik User ID {} disetujui oleh User ID {} ({}).",
                saved.getId(),
                saved.getLogDate(),
                saved.getUserId(),
                actor.getId(),
                actor.getEmail()
        );

        return toResponse(saved);
    }

    /**
     * Atasan langsung atau admin menolak Daily Log yang berstatus MENUNGGU.
     * Alasan penolakan wajib diisi.
     */
    @Transactional
    public DailyLogResponse reject(User actor, Long dailyLogId, String reason) {
        DailyLog dailyLog = requireDecidableLog(actor, dailyLogId);
        requireStatus(dailyLog, STATUS_WAITING, "ditolak");

        String normalizedReason = requireReason(reason);

        dailyLog.reject(actor.getId(), normalizedReason, OffsetDateTime.now());
        DailyLog saved = dailyLogRepository.save(dailyLog);

        log.info(
                "Daily Log ID {} tanggal {} milik User ID {} ditolak oleh User ID {} ({}): {}",
                saved.getId(),
                saved.getLogDate(),
                saved.getUserId(),
                actor.getId(),
                actor.getEmail(),
                normalizedReason
        );

        return toResponse(saved);
    }

    /**
     * Talent mengajukan perbaikan atas Daily Log miliknya yang sudah
     * disetujui. Alasan pengajuan wajib diisi.
     */
    @Transactional
    public DailyLogResponse requestRevision(User actor, Long dailyLogId, String reason) {
        DailyLog dailyLog = requireOwnLog(actor, dailyLogId);
        requireStatus(dailyLog, STATUS_APPROVED, "diajukan perbaikan");

        String normalizedReason = requireReason(reason);

        dailyLog.requestRevision(normalizedReason, OffsetDateTime.now());
        DailyLog saved = dailyLogRepository.save(dailyLog);

        log.info(
                "Daily Log ID {} tanggal {} milik User ID {} diajukan perbaikan oleh User ID {} ({}): {}",
                saved.getId(),
                saved.getLogDate(),
                saved.getUserId(),
                actor.getId(),
                actor.getEmail(),
                normalizedReason
        );

        return toResponse(saved);
    }

    /**
     * Atasan langsung atau admin menyetujui pengajuan perbaikan sehingga hari
     * itu dibuka kembali sebagai DRAF dan dapat disunting talent.
     */
    @Transactional
    public DailyLogResponse approveRevision(User actor, Long dailyLogId) {
        DailyLog dailyLog = requireDecidableLog(actor, dailyLogId);
        requireStatus(dailyLog, STATUS_REVISION_REQUESTED, "disetujui perbaikannya");

        dailyLog.approveRevision(actor.getId(), OffsetDateTime.now());
        DailyLog saved = dailyLogRepository.save(dailyLog);

        log.info(
                "Perbaikan Daily Log ID {} tanggal {} milik User ID {} disetujui oleh User ID {} ({}); "
                        + "status kembali {}.",
                saved.getId(),
                saved.getLogDate(),
                saved.getUserId(),
                actor.getId(),
                actor.getEmail(),
                STATUS_DRAFT
        );

        return toResponse(saved);
    }

    /**
     * Memuat Daily Log yang boleh diputuskan pelaku. Pemilik Daily Log selalu
     * ditolak lebih dahulu agar pesan galatnya jelas.
     */
    private DailyLog requireDecidableLog(User actor, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findById(dailyLogId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily Log tidak ditemukan"
                ));

        if (dailyLog.getUserId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tidak boleh memutuskan Daily Log milik sendiri"
            );
        }

        User owner = userRepository.findByIdAndActiveTrue(dailyLog.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pengguna tidak ditemukan"
                ));

        if (!roleService.canDecideTalent(actor, owner)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tidak berwenang memutuskan Daily Log pengguna ini"
            );
        }

        return dailyLog;
    }

    private DailyLog requireOwnLog(User actor, Long dailyLogId) {
        DailyLog dailyLog = dailyLogRepository.findById(dailyLogId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily Log tidak ditemukan"
                ));

        if (!dailyLog.getUserId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Hanya pemilik Daily Log yang dapat mengajukan perbaikan"
            );
        }

        return dailyLog;
    }

    private DailyLogResponse toResponse(DailyLog dailyLog) {
        return DailyLogResponse.from(
                dailyLog,
                deadlineService.isEditable(dailyLog.getStatus(), dailyLog.getLogDate()),
                deadlineService.deadlineFor(dailyLog.getLogDate())
        );
    }

    private void requireStatus(DailyLog dailyLog, String expectedStatus, String action) {
        if (!expectedStatus.equalsIgnoreCase(dailyLog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Daily Log tidak dapat " + action + " pada status "
                            + dailyLog.getStatus() + "."
            );
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Alasan wajib diisi"
            );
        }

        String normalizedReason = reason.trim();

        if (normalizedReason.length() > MAX_REASON_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Alasan maksimal " + MAX_REASON_LENGTH + " karakter"
            );
        }

        return normalizedReason;
    }
}
