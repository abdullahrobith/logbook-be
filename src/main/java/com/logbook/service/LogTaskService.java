package com.logbook.service;

import lombok.extern.slf4j.Slf4j;
import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.LogTaskRequest;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.LogTaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;

@Slf4j
@Service
public class LogTaskService {

    private static final String STATUS_DRAF = "DRAF";
    private static final String STATUS_REJECTED = "DITOLAK";
    private static final short MIN_DURATION_MINUTES = 5;
    private static final short MAX_DURATION_MINUTES = 1440;
    private static final int MAX_CONCURRENT_TASKS = 3;

    private static final Set<Short> VALID_WORK_STATUS_IDS =
            Set.of((short) 1, (short) 2, (short) 3);

    private final DailyLogRepository dailyLogRepository;
    private final LogTaskRepository logTaskRepository;
    private final ProjectRepository projectRepository;
    private final DeadlineService deadlineService;

    public LogTaskService(
            DailyLogRepository dailyLogRepository,
            LogTaskRepository logTaskRepository,
            ProjectRepository projectRepository,
            DeadlineService deadlineService
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.logTaskRepository = logTaskRepository;
        this.projectRepository = projectRepository;
        this.deadlineService = deadlineService;
    }

    @Transactional
    public DailyLogResponse.TaskResponse createTask(
            Long userId,
            Long dailyLogId,
            LogTaskRequest request
    ) {
        log.info("Memproses pembuatan task baru untuk User ID: {} di Daily Log ID: {}", userId, dailyLogId);

        DailyLog dailyLog = dailyLogRepository.findById(dailyLogId)
                .orElseThrow(() -> {
                    log.warn("Gagal membuat task: Daily Log ID {} tidak ditemukan", dailyLogId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Daily log tidak ditemukan");
                });

        if (!dailyLog.getUserId().equals(userId)) {
            log.warn("Gagal membuat task: User ID {} bukan pemilik Daily Log ID {}", userId, dailyLogId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Daily log bukan milik pengguna ini");
        }

        requireEditableDay(dailyLog, "ditambah");
        requireEditableStatus(dailyLog);
        validateRequest(userId, request);
        validateConcurrentTaskLimit(dailyLog, request.startTime(), request.durationMinutes(), null);

        OffsetDateTime now = OffsetDateTime.now();
        LogTask logTask = new LogTask(
                dailyLog,
                request.projectId(),
                request.taskTypeId(),
                request.workStatusId(),
                normalizeTicket(request.ticket()),
                request.startTime(),
                request.durationMinutes(),
                request.summary().trim(),
                normalizeObstacle(request.obstacle()),
                now,
                now
        );

        LogTask savedTask = logTaskRepository.save(logTask);
        log.info("Berhasil membuat task baru dengan ID: {} untuk Daily Log ID: {}", savedTask.getId(), dailyLogId);
        
        return DailyLogResponse.TaskResponse.from(savedTask);
    }

    @Transactional
    public DailyLogResponse.TaskResponse updateTask(
            Long userId,
            Long taskId,
            LogTaskRequest request
    ) {
        log.info("Memproses update task ID: {} untuk User ID: {}", taskId, userId);
        
        LogTask logTask = findOwnedTask(userId, taskId);

        requireEditableDay(logTask.getDailyLog(), "diubah");
        requireEditableStatus(logTask.getDailyLog());
        validateRequest(userId, request);
        validateConcurrentTaskLimit(
                logTask.getDailyLog(),
                request.startTime(),
                request.durationMinutes(),
                logTask.getId()
        );

        logTask.update(
                request.projectId(),
                request.taskTypeId(),
                request.workStatusId(),
                normalizeTicket(request.ticket()),
                request.startTime(),
                request.durationMinutes(),
                request.summary().trim(),
                normalizeObstacle(request.obstacle()),
                OffsetDateTime.now()
        );

        LogTask updatedTask = logTaskRepository.save(logTask);
        log.info("Berhasil mengupdate task ID: {}", updatedTask.getId());
        
        return DailyLogResponse.TaskResponse.from(updatedTask);
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        log.info("Memproses penghapusan task ID: {} untuk User ID: {}", taskId, userId);
        
        LogTask logTask = findOwnedTask(userId, taskId);

        requireEditableDay(logTask.getDailyLog(), "dihapus");
        requireEditableStatus(logTask.getDailyLog());
        logTaskRepository.delete(logTask);
        
        log.info("Berhasil menghapus task ID: {}", taskId);
    }

    private LogTask findOwnedTask(Long userId, Long taskId) {
        return logTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> {
                    log.warn("Gagal: Task ID {} tidak ditemukan atau bukan milik User ID {}", taskId, userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Task tidak ditemukan");
                });
    }

    private void requireEditableStatus(DailyLog dailyLog) {
        String status = dailyLog.getStatus();
        if (!STATUS_DRAF.equalsIgnoreCase(status)
                && !STATUS_REJECTED.equalsIgnoreCase(status)) {
            log.warn("Validasi gagal: Daily Log ID {} berstatus {}, bukan DRAF/DITOLAK",
                    dailyLog.getId(), status);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Task hanya dapat diubah pada Daily Log berstatus DRAF atau DITOLAK"
            );
        }
    }

    private void requireEditableDay(DailyLog dailyLog, String action) {
        if (deadlineService.isPastDeadline(dailyLog.getLogDate())) {
            log.warn("Validasi gagal: batas hari Daily Log ID {} tanggal {} sudah lewat, task tidak dapat {}",
                    dailyLog.getId(), dailyLog.getLogDate(), action);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Batas hari sudah lewat. Task pada tanggal " + dailyLog.getLogDate()
                            + " tidak dapat " + action + "."
            );
        }

        if (deadlineService.isFutureDay(dailyLog.getLogDate())) {
            log.warn("Validasi gagal: tanggal {} belum tiba, task pada Daily Log ID {} tidak dapat {}",
                    dailyLog.getLogDate(), dailyLog.getId(), action);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tanggal " + dailyLog.getLogDate() + " belum tiba. Task belum dapat " + action
                            + ". Task hanya dapat diisi pada tanggal hari ini."
            );
        }
    }

    private void validateRequest(Long userId, LogTaskRequest request) {
        if (!VALID_WORK_STATUS_IDS.contains(request.workStatusId())) {
            log.warn("Validasi gagal: Work Status ID {} tidak valid", request.workStatusId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status pekerjaan harus Selesai, Berjalan, atau Tertahan"
            );
        }

        if (request.taskTypeId() == null
                || !logTaskRepository.existsActiveTaskType(request.taskTypeId())) {
            log.warn("Validasi gagal: Task Type ID {} tidak tersedia", request.taskTypeId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Jenis task tidak tersedia"
            );
        }

        if (request.durationMinutes() == null
                || request.durationMinutes() < MIN_DURATION_MINUTES
                || request.durationMinutes() > MAX_DURATION_MINUTES) {
            log.warn("Validasi gagal: Durasi {} menit di luar batas (5-1440 menit)", request.durationMinutes());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Durasi harus antara 5 dan 1440 menit"
            );
        }

        boolean projectAvailable = projectRepository.existsActiveProjectForUser(
                request.projectId(),
                userId
        );

        if (!projectAvailable) {
            log.warn("Validasi gagal: Project ID {} tidak tersedia untuk User ID {}", request.projectId(), userId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proyek tidak tersedia untuk pengguna ini"
            );
        }
    }

    private void validateConcurrentTaskLimit(
            DailyLog dailyLog,
            LocalTime startTime,
            Short durationMinutes,
            Long excludedTaskId
    ) {
        long start = startTime.toSecondOfDay();
        long end = start + durationMinutes * 60L;

        long overlappingTasks = dailyLog.getTasks().stream()
                .filter(task -> excludedTaskId == null
                        || !excludedTaskId.equals(task.getId()))
                .filter(task -> {
                    long otherStart = task.getStartTime().toSecondOfDay();
                    long otherEnd = otherStart
                            + task.getDurationMinutes() * 60L;
                    return start < otherEnd && otherStart < end;
                })
                .count();

        if (overlappingTasks >= MAX_CONCURRENT_TASKS) {
            log.warn("Validasi gagal: Maksimal task bersamaan ({}) terlampaui pada Daily Log ID {}", MAX_CONCURRENT_TASKS, dailyLog.getId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maksimal 3 task dapat berjalan pada waktu yang bersamaan. "
                            + "Ubah jam atau durasi task terlebih dahulu."
            );
        }
    }

    private String normalizeTicket(String ticket) {
        if (ticket == null) {
            return null;
        }
        String trimmed = ticket.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeObstacle(String obstacle) {
        if (obstacle == null) {
            return null;
        }
        String trimmed = obstacle.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}