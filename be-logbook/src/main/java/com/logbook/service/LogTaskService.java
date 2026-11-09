package com.logbook.service;

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

import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class LogTaskService {

    private static final String STATUS_DRAF = "DRAF";

    private static final Set<Short> VALID_WORK_STATUS_IDS =
            Set.of((short) 1, (short) 2, (short) 3);

    private final DailyLogRepository dailyLogRepository;
    private final LogTaskRepository logTaskRepository;
    private final ProjectRepository projectRepository;

    public LogTaskService(
            DailyLogRepository dailyLogRepository,
            LogTaskRepository logTaskRepository,
            ProjectRepository projectRepository
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.logTaskRepository = logTaskRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public DailyLogResponse.TaskResponse createTask(
            Long userId,
            Long dailyLogId,
            LogTaskRequest request
    ) {
        DailyLog dailyLog = dailyLogRepository.findById(dailyLogId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily log tidak ditemukan"
                ));

        if (!dailyLog.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Daily log bukan milik pengguna ini"
            );
        }

        requireDraft(dailyLog);
        validateRequest(userId, request);

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

        return DailyLogResponse.TaskResponse.from(logTaskRepository.save(logTask));
    }

    @Transactional
    public DailyLogResponse.TaskResponse updateTask(
            Long userId,
            Long taskId,
            LogTaskRequest request
    ) {
        LogTask logTask = findOwnedTask(userId, taskId);

        requireDraft(logTask.getDailyLog());
        validateRequest(userId, request);

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

        return DailyLogResponse.TaskResponse.from(logTaskRepository.save(logTask));
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        LogTask logTask = findOwnedTask(userId, taskId);

        requireDraft(logTask.getDailyLog());
        logTaskRepository.delete(logTask);
    }

    private LogTask findOwnedTask(Long userId, Long taskId) {
        return logTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task tidak ditemukan"
                ));
    }

    private void requireDraft(DailyLog dailyLog) {
        if (!STATUS_DRAF.equalsIgnoreCase(dailyLog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Task hanya dapat diubah pada Daily Log berstatus DRAF"
            );
        }
    }

    private void validateRequest(Long userId, LogTaskRequest request) {
        if (!VALID_WORK_STATUS_IDS.contains(request.workStatusId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status pekerjaan harus Selesai, Berjalan, atau Tertahan"
            );
        }

        boolean projectAvailable = projectRepository.existsActiveProjectForUser(
                request.projectId(),
                userId
        );

        if (!projectAvailable) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proyek tidak tersedia untuk pengguna ini"
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
