package com.logbook.service;

import com.logbook.dto.DayDto;
import com.logbook.dto.DayTaskRequest;
import com.logbook.dto.LogTaskRequest;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.entity.TaskType;
import com.logbook.entity.WorkStatus;
import com.logbook.exception.ApiException;
import com.logbook.project.ProjectDto;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.LogTaskRepository;
import com.logbook.repository.TaskTypeRepository;
import com.logbook.repository.WorkStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class DayTaskService {

    private final DailyLogService dailyLogService;
    private final LogTaskService logTaskService;
    private final LogTaskRepository logTaskRepository;
    private final ProjectRepository projectRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final WorkStatusRepository workStatusRepository;
    private final DayDtoMapper dayDtoMapper;

    public DayTaskService(
            DailyLogService dailyLogService,
            LogTaskService logTaskService,
            LogTaskRepository logTaskRepository,
            ProjectRepository projectRepository,
            TaskTypeRepository taskTypeRepository,
            WorkStatusRepository workStatusRepository,
            DayDtoMapper dayDtoMapper
    ) {
        this.dailyLogService = dailyLogService;
        this.logTaskService = logTaskService;
        this.logTaskRepository = logTaskRepository;
        this.projectRepository = projectRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.workStatusRepository = workStatusRepository;
        this.dayDtoMapper = dayDtoMapper;
    }

    @Transactional
    public DayDto createTask(Long userId, LocalDate date, DayTaskRequest request) {
        DailyLog dailyLog = dailyLogService.getOrCreateEntity(userId, date);
        LogTaskRequest internal = toInternalRequest(userId, request);
        logTaskService.createTask(userId, dailyLog.getId(), internal);
        // reload
        DailyLog refreshed = dailyLogService.getOrThrow(userId, date);
        return dayDtoMapper.toDayDto(refreshed);
    }

    @Transactional
    public DayDto updateTask(Long userId, Long taskId, DayTaskRequest request) {
        LogTask task = logTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task tidak ditemukan"));
        DailyLog dailyLog = task.getDailyLog();
        LogTaskRequest internal = toInternalRequest(userId, request);
        logTaskService.updateTask(userId, taskId, internal);
        DailyLog refreshed = dailyLogService.getOrThrow(dailyLog.getUserId(), dailyLog.getLogDate());
        return dayDtoMapper.toDayDto(refreshed);
    }

    @Transactional
    public DayDto deleteTask(Long userId, Long taskId) {
        LogTask task = logTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task tidak ditemukan"));
        Long ownerId = task.getDailyLog().getUserId();
        LocalDate date = task.getDailyLog().getLogDate();
        logTaskService.deleteTask(userId, taskId);
        DailyLog refreshed = dailyLogService.getOrThrow(ownerId, date);
        return dayDtoMapper.toDayDto(refreshed);
    }

    private LogTaskRequest toInternalRequest(Long userId, DayTaskRequest request) {
        Long projectId = resolveProjectId(userId, request.project());
        Short taskTypeId = resolveTaskTypeId(request.type());
        Short workStatusId = resolveWorkStatusId(request.workStatus());
        return new LogTaskRequest(
                projectId,
                taskTypeId,
                workStatusId,
                request.ticket(),
                request.startMinute(),
                request.durationMinutes(),
                request.summary(),
                request.obstacle()
        );
    }

    private Long resolveProjectId(Long userId, String projectName) {
        if (projectName == null || projectName.isBlank()) {
            // fallback: try first active project? or error
            throw new ApiException(HttpStatus.BAD_REQUEST, "Proyek wajib diisi");
        }
        return projectRepository.findProjectsByUserId(userId).stream()
                .filter(p -> projectName.equalsIgnoreCase(p.project_name()))
                .map(ProjectDto::project_id)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Proyek tidak tersedia untuk pengguna ini"));
    }

    private Short resolveTaskTypeId(String type) {
        if (type == null || type.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Jenis task tidak tersedia");
        }
        return taskTypeRepository.findByNameIgnoreCase(type)
                .or(() -> taskTypeRepository.findByCodeIgnoreCase(type))
                .filter(TaskType::isActive)
                .map(TaskType::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Jenis task tidak tersedia"));
    }

    private Short resolveWorkStatusId(String workStatus) {
        if (workStatus == null || workStatus.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status pekerjaan harus Selesai, Berjalan, atau Tertahan");
        }
        return workStatusRepository.findByNameIgnoreCase(workStatus)
                .or(() -> workStatusRepository.findByCodeIgnoreCase(workStatus))
                .map(WorkStatus::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Status pekerjaan harus Selesai, Berjalan, atau Tertahan"));
    }
}