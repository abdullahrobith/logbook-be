package com.logbook.service;

import com.logbook.dto.DayDto;
import com.logbook.dto.DayInfoResponse;
import com.logbook.dto.TaskDto;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.entity.TaskType;
import com.logbook.entity.User;
import com.logbook.entity.UserSettings;
import com.logbook.entity.WorkStatus;
import com.logbook.project.ProjectDto;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.TaskTypeRepository;
import com.logbook.repository.UserRepository;
import com.logbook.repository.UserSettingsRepository;
import com.logbook.repository.WorkStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DayDtoMapper {

    private final CalendarService calendarService;
    private final DeadlineService deadlineService;
    private final UserSettingsRepository userSettingsRepository;
    private final ProjectRepository projectRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final WorkStatusRepository workStatusRepository;
    private final UserRepository userRepository;

    public DayDtoMapper(
            CalendarService calendarService,
            DeadlineService deadlineService,
            UserSettingsRepository userSettingsRepository,
            ProjectRepository projectRepository,
            TaskTypeRepository taskTypeRepository,
            WorkStatusRepository workStatusRepository,
            UserRepository userRepository
    ) {
        this.calendarService = calendarService;
        this.deadlineService = deadlineService;
        this.userSettingsRepository = userSettingsRepository;
        this.projectRepository = projectRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.workStatusRepository = workStatusRepository;
        this.userRepository = userRepository;
    }

    public DayDto toDayDto(DailyLog dailyLog) {
        List<LogTask> tasks = dailyLog.getTasks() == null ? List.of() : dailyLog.getTasks();

        Map<Long, ProjectDto> projectInfo = projectInfoOf(tasks);
        List<TaskDto> taskDtos = toTaskDtos(tasks, projectInfo);

        // perProject: jumlah durasi penuh per proyek (SRS: perProject dan rekap per jenis = jumlah durasi penuh)
        Map<Long, Integer> minutesPerProject = new LinkedHashMap<>();
        Map<Long, ProjectDto> perProjectInfo = new LinkedHashMap<>();
        for (LogTask t : tasks) {
            minutesPerProject.merge(t.getProjectId(), TaskDurationUtil.minutesOf(t), Integer::sum);
            perProjectInfo.putIfAbsent(t.getProjectId(), projectInfo.get(t.getProjectId()));
        }
        List<DayDto.PerProjectDto> perProject = minutesPerProject.entrySet().stream().map(e -> {
            ProjectDto p = perProjectInfo.get(e.getKey());
            String name = p != null ? p.project_name() : null;
            String color = p != null ? p.color() : null;
            return new DayDto.PerProjectDto(name, color, e.getValue());
        }).toList();

        int totalMinutes = TaskDurationUtil.unionMinutes(tasks);
        DayDto.RangeDto range = computeRange(dailyLog.getUserId(), tasks);

        DayInfoResponse dayInfo = calendarService.describe(dailyLog.getUserId(), dailyLog.getLogDate());
        String dayLabel = null;
        if (dayInfo != null) {
            if (dayInfo.holidayName() != null) dayLabel = "Libur";
            else if (dayInfo.onLeave()) dayLabel = "Cuti";
        }

        boolean editable = deadlineService.isEditable(
                dailyLog.getUserId(),
                dailyLog.getStatus(),
                dailyLog.getLogDate()
        );

        User owner = userRepository.findById(dailyLog.getUserId()).orElse(null);
        String decidedByName = null;
        if (dailyLog.getDecidedByUserId() != null) {
            decidedByName = userRepository.findById(dailyLog.getDecidedByUserId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return new DayDto(
                dailyLog.getLogDate(),
                dailyLog.getStatus(),
                statusLabel(dailyLog.getStatus()),
                editable,
                deadlineService.deadlineFor(dailyLog.getUserId(), dailyLog.getLogDate()),
                dayLabel,
                range,
                totalMinutes,
                perProject,
                taskDtos,
                dailyLog.getRejectionReason(),
                dailyLog.getId(),
                dailyLog.getUserId(),
                owner == null ? null : owner.getFullName(),
                owner == null ? null : owner.getJenis(),
                dailyLog.getSubmittedAt(),
                dailyLog.isAutoSubmitted(),
                dailyLog.getRevisionReason(),
                dailyLog.getDecidedAt(),
                dailyLog.getDecidedByUserId(),
                decidedByName
        );
    }

    /**
     * Memetakan task menjadi {@link TaskDto} lengkap dengan nama proyek, warna,
     * jenis, dan status pekerjaan. Dipakai juga oleh saran "Lanjutkan" pada
     * form task (LBK-TSK-07).
     */
    public List<TaskDto> toTaskDtos(List<LogTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return toTaskDtos(tasks, projectInfoOf(tasks));
    }

    private List<TaskDto> toTaskDtos(List<LogTask> tasks, Map<Long, ProjectDto> projectInfo) {
        Map<Short, String> taskTypeNames = taskTypeNames();
        Map<Short, String> workStatusNames = workStatusNames();

        return tasks.stream().map(t -> {
            ProjectDto p = projectInfo.get(t.getProjectId());
            String projectName = p != null ? p.project_name() : null;
            String color = p != null ? p.color() : null;
            String typeName = taskTypeNames.getOrDefault(t.getTaskTypeId(), null);
            String wsName = workStatusNames.getOrDefault(t.getWorkStatusId(), null);
            return new TaskDto(
                    t.getId(),
                    projectName,
                    color,
                    t.getTicket(),
                    t.getStartMinute(),
                    t.getDurationMinutes(),
                    typeName,
                    t.getSummary(),
                    wsName,
                    t.getObstacle()
            );
        }).toList();
    }

    private Map<Short, String> taskTypeNames() {
        return taskTypeRepository.findAll().stream()
                .collect(Collectors.toMap(TaskType::getId, TaskType::getName, (a, b) -> a));
    }

    private Map<Short, String> workStatusNames() {
        return workStatusRepository.findAll().stream()
                .collect(Collectors.toMap(WorkStatus::getId, WorkStatus::getName, (a, b) -> a));
    }

    private Map<Long, ProjectDto> projectInfoOf(List<LogTask> tasks) {
        Map<Long, ProjectDto> projectInfo = new HashMap<>();
        for (LogTask t : tasks) {
            projectInfo.computeIfAbsent(t.getProjectId(), id ->
                    projectRepository.findProjectInfoById(id).orElse(null));
        }
        return projectInfo;
    }

    private DayDto.RangeDto computeRange(Long userId, List<LogTask> tasks) {
        UserSettings settings = userSettingsRepository.findById(userId).orElse(null);
        int start = toMinutes(settings != null ? settings.getWorkStart() : LocalTime.of(8,0));
        int end = toMinutes(settings != null ? settings.getWorkEnd() : LocalTime.of(17,0));
        for (LogTask t : tasks) {
            int s = t.getStartMinute() != null ? t.getStartMinute() : 0;
            int e = s + (t.getDurationMinutes() != null ? t.getDurationMinutes() : 0);
            start = Math.min(start, s);
            end = Math.max(end, e);
        }
        if (end < start) end = start;
        return new DayDto.RangeDto(start, end);
    }

    private int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private String statusLabel(String status) {
        if (status == null) return null;
        return switch (status.toUpperCase()) {
            case "DRAF" -> "Draf";
            case "MENUNGGU" -> "Menunggu";
            case "DISETUJUI" -> "Disetujui";
            case "DITOLAK" -> "Ditolak";
            case "TERLEWAT" -> "Terlewat";
            case "PERBAIKAN_DIAJUKAN" -> "Perbaikan diajukan";
            default -> status;
        };
    }
}
