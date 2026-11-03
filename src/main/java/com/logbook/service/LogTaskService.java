
package com.logbook.service;

import com.logbook.dto.CreateLogTaskRequest;
import com.logbook.dto.DailyLogResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
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

    private final DailyLogRepository dailyLogRepository;
    private final LogTaskRepository logTaskRepository;

    private static final Set<Short> VALID_WORK_STATUS_IDS =
            Set.of((short) 1, (short) 2, (short) 3);

    public LogTaskService(
            DailyLogRepository dailyLogRepository,
            LogTaskRepository logTaskRepository
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.logTaskRepository = logTaskRepository;
    }

    @Transactional
    public DailyLogResponse.TaskResponse createTask(
            Long dailyLogId,
            CreateLogTaskRequest request
    ) {

        // Memastikan Daily Log tersedia.
        DailyLog dailyLog = dailyLogRepository.findById(dailyLogId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Daily log tidak ditemukan"
                ));

        // Task hanya boleh ditambahkan pada Daily Log berstatus DRAF.
        if (!"DRAF".equalsIgnoreCase(dailyLog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Task hanya dapat ditambahkan pada Daily Log berstatus DRAF"
            );
        }

        // Memastikan status pekerjaan valid.
        if (!VALID_WORK_STATUS_IDS.contains(request.workStatusId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status pekerjaan harus Selesai, Berjalan, atau Tertahan"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        String ticket = request.ticket();

        if (ticket != null) {
            ticket = ticket.trim();

            if (ticket.isEmpty()) {
                ticket = null;
            }
        }

        // Membuat task baru.
        LogTask logTask = new LogTask(
                dailyLog,
                request.projectId(),
                request.taskTypeId(),
                request.workStatusId(),
                ticket,
                request.startTime(),
                request.durationMinutes(),
                request.summary().trim(),
                now,
                now
        );

        // Menyimpan task.
        LogTask savedTask = logTaskRepository.save(logTask);

        return DailyLogResponse.TaskResponse.from(savedTask);
    }
}
