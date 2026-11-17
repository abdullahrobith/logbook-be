package com.logbook.service;

import com.logbook.dto.DayInfoResponse;
import com.logbook.dto.SummaryResponse;
import com.logbook.dto.SummaryResponse.ObstacleSummary;
import com.logbook.dto.SummaryResponse.ProjectSummary;
import com.logbook.dto.SummaryResponse.ShortSummaryDay;
import com.logbook.dto.SummaryResponse.TaskSummary;
import com.logbook.dto.SummaryResponse.TypeSummary;
import com.logbook.entity.LogTask;
import com.logbook.entity.TaskType;
import com.logbook.entity.WorkStatus;
import com.logbook.project.ProjectDto;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.LogTaskRepository;
import com.logbook.repository.TaskTypeRepository;
import com.logbook.repository.WorkStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Menyusun Ringkasanku, yaitu rekap mingguan/bulanan milik talent sendiri
 * (SRS LBK-KUA-04). Tidak ada skor dan tidak ada penilaian kualitas di sini;
 * hari dengan ringkasan pendek hanya disebut secara netral.
 *
 * <p>Aturan perhitungan:</p>
 * <ul>
 *   <li>WEEK = Senin-Minggu, MONTH = tanggal 1 sampai akhir bulan.</li>
 *   <li>{@code totalMinutes} = jumlah union durasi task per hari.</li>
 *   <li>{@code perType} dan {@code perProject} = jumlah durasi penuh task.</li>
 *   <li>Hari libur, cuti, dan akhir pekan tidak menambah {@code workdays} atau
 *       {@code daysRecorded}, tetapi task yang tetap dicatat pada hari itu
 *       masih masuk rekap durasi dan daftar pekerjaan.</li>
 * </ul>
 */
@Slf4j
@Service
public class SummaryService {

    private final LogTaskRepository logTaskRepository;
    private final CalendarService calendarService;
    private final DeadlineService deadlineService;
    private final ProjectRepository projectRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final WorkStatusRepository workStatusRepository;
    private final int ringkasanMin;

    public SummaryService(
            LogTaskRepository logTaskRepository,
            CalendarService calendarService,
            DeadlineService deadlineService,
            ProjectRepository projectRepository,
            TaskTypeRepository taskTypeRepository,
            WorkStatusRepository workStatusRepository,
            @Value("${logbook.saran.ringkasan-min:20}") int ringkasanMin
    ) {
        this.logTaskRepository = logTaskRepository;
        this.calendarService = calendarService;
        this.deadlineService = deadlineService;
        this.projectRepository = projectRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.workStatusRepository = workStatusRepository;
        this.ringkasanMin = Math.max(ringkasanMin, 0);
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long userId, String period, String date) {
        SummaryPeriod summaryPeriod = parsePeriod(period);
        LocalDate anchor = parseDate(userId, date);
        LocalDate fromDate = startDateOf(anchor, summaryPeriod);
        LocalDate toDate = endDateOf(anchor, summaryPeriod);

        log.info("Menyusun rekap {} untuk User ID {}, rentang {} sampai {}",
                summaryPeriod, userId, fromDate, toDate);

        List<LogTask> tasks = logTaskRepository.findForUserBetween(userId, fromDate, toDate);
        if (tasks == null) {
            tasks = List.of();
        }

        Map<LocalDate, List<LogTask>> tasksByDay = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getDailyLog().getLogDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<DayInfoResponse> dayInfos = calendarService.describeRange(userId, fromDate, toDate);
        if (dayInfos == null) {
            dayInfos = List.of();
        }
        Map<LocalDate, DayInfoResponse> dayInfoByDate = dayInfos.stream()
                .collect(Collectors.toMap(
                        DayInfoResponse::date,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        int workdays = (int) dayInfos.stream()
                .filter(DayInfoResponse::workingDay)
                .count();

        Map<Short, String> typeNames = typeNamesOf();
        Map<Short, StatusInfo> statusInfo = statusInfoOf();
        Map<Long, ProjectDto> projectInfo = projectInfoOf(tasks);

        Map<Short, Integer> minutesByType = new LinkedHashMap<>();
        Map<Long, Integer> minutesByProject = new LinkedHashMap<>();
        List<TaskSummary> done = new ArrayList<>();
        List<TaskSummary> ongoing = new ArrayList<>();
        List<ObstacleSummary> obstacles = new ArrayList<>();
        List<ShortSummaryDay> shortSummaryDays = new ArrayList<>();
        Set<LocalDate> recordedWorkingDays = new HashSet<>();
        int totalMinutes = 0;

        for (Map.Entry<LocalDate, List<LogTask>> entry : tasksByDay.entrySet()) {
            LocalDate day = entry.getKey();
            List<LogTask> dayTasks = entry.getValue();

            totalMinutes += TaskDurationUtil.unionMinutes(dayTasks);

            DayInfoResponse dayInfo = dayInfoByDate.get(day);
            if (dayInfo != null && dayInfo.workingDay()) {
                recordedWorkingDays.add(day);
            }

            int shortSummaryCount = 0;
            for (LogTask task : dayTasks) {
                int duration = TaskDurationUtil.minutesOf(task);
                minutesByType.merge(task.getTaskTypeId(), duration, Integer::sum);
                minutesByProject.merge(task.getProjectId(), duration, Integer::sum);

                StatusInfo status = statusInfo.get(task.getWorkStatusId());
                TaskSummary taskSummary = toTaskSummary(day, task, typeNames, projectInfo, status);
                if (status != null && status.isDone()) {
                    done.add(taskSummary);
                } else if (status != null && status.isOngoing()) {
                    ongoing.add(taskSummary);
                }

                String obstacle = trimmed(task.getObstacle());
                if (obstacle != null) {
                    obstacles.add(new ObstacleSummary(
                            day,
                            task.getId(),
                            projectNameOf(projectInfo.get(task.getProjectId())),
                            trimmed(task.getTicket()),
                            obstacle
                    ));
                }

                if (ringkasanMin > 0 && taskSummary.summary().length() < ringkasanMin) {
                    shortSummaryCount++;
                }
            }

            if (shortSummaryCount > 0) {
                shortSummaryDays.add(new ShortSummaryDay(day, shortSummaryCount));
            }
        }

        List<TypeSummary> perType = minutesByType.entrySet().stream()
                .map(entry -> new TypeSummary(
                        typeNames.getOrDefault(entry.getKey(), "Jenis tidak diketahui"),
                        entry.getValue()
                ))
                .sorted(typeOrder())
                .toList();

        List<ProjectSummary> perProject = minutesByProject.entrySet().stream()
                .map(entry -> {
                    ProjectDto project = projectInfo.get(entry.getKey());
                    return new ProjectSummary(
                            projectNameOf(project),
                            project == null ? null : project.color(),
                            entry.getValue()
                    );
                })
                .sorted(projectOrder())
                .toList();

        SummaryResponse response = new SummaryResponse(
                totalMinutes,
                recordedWorkingDays.size(),
                workdays,
                perType,
                perProject,
                done,
                ongoing,
                obstacles,
                shortSummaryDays
        );

        log.info("Rekap {} untuk User ID {} selesai: {} menit, {} dari {} hari kerja",
                summaryPeriod, userId, totalMinutes, response.daysRecorded(), workdays);
        return response;
    }

    private SummaryPeriod parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            throw badRequest("Period wajib diisi");
        }
        try {
            return SummaryPeriod.valueOf(period.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            log.warn("Period rekap tidak dikenal: {}", period);
            throw badRequest("Period harus WEEK atau MONTH");
        }
    }

    private LocalDate parseDate(Long userId, String date) {
        LocalDate parsed;
        if (date == null || date.isBlank()) {
            parsed = deadlineService.activeDay(userId);
        } else {
            try {
                parsed = LocalDate.parse(date.trim());
            } catch (DateTimeParseException exception) {
                log.warn("Tanggal rekap tidak valid: {}", date);
                throw badRequest("Tanggal tidak valid");
            }
        }
        if (parsed == null) {
            throw badRequest("Tanggal tidak valid");
        }
        return parsed;
    }

    private LocalDate startDateOf(LocalDate anchor, SummaryPeriod period) {
        if (period == SummaryPeriod.WEEK) {
            return anchor.with(DayOfWeek.MONDAY);
        }
        return anchor.withDayOfMonth(1);
    }

    private LocalDate endDateOf(LocalDate anchor, SummaryPeriod period) {
        if (period == SummaryPeriod.WEEK) {
            return anchor.with(DayOfWeek.MONDAY).plusDays(6);
        }
        return anchor.with(TemporalAdjusters.lastDayOfMonth());
    }

    private Map<Short, String> typeNamesOf() {
        return taskTypeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        TaskType::getId,
                        TaskType::getName,
                        (first, second) -> first
                ));
    }

    private Map<Short, StatusInfo> statusInfoOf() {
        return workStatusRepository.findAll().stream()
                .collect(Collectors.toMap(
                        WorkStatus::getId,
                        status -> new StatusInfo(status.getCode(), status.getName()),
                        (first, second) -> first
                ));
    }

    private Map<Long, ProjectDto> projectInfoOf(List<LogTask> tasks) {
        Map<Long, ProjectDto> projectInfo = new HashMap<>();
        tasks.stream()
                .map(LogTask::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(projectId -> projectInfo.put(
                        projectId,
                        projectRepository.findProjectInfoById(projectId).orElse(null)
                ));
        return projectInfo;
    }

    private TaskSummary toTaskSummary(
            LocalDate day,
            LogTask task,
            Map<Short, String> typeNames,
            Map<Long, ProjectDto> projectInfo,
            StatusInfo status
    ) {
        ProjectDto project = projectInfo.get(task.getProjectId());
        return new TaskSummary(
                day,
                task.getId(),
                projectNameOf(project),
                project == null ? null : project.color(),
                trimmed(task.getTicket()),
                typeNames.get(task.getTaskTypeId()),
                summaryOf(task),
                status == null ? null : status.name(),
                task.getStartMinute(),
                task.getDurationMinutes() == null ? null : (int) task.getDurationMinutes()
        );
    }

    private String projectNameOf(ProjectDto project) {
        return project == null ? null : project.project_name();
    }

    private String summaryOf(LogTask task) {
        String summary = trimmed(task.getSummary());
        return summary == null ? "" : summary;
    }

    private String trimmed(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Comparator<TypeSummary> typeOrder() {
        return Comparator.comparingInt(TypeSummary::minutes)
                .reversed()
                .thenComparing(TypeSummary::name,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<ProjectSummary> projectOrder() {
        return Comparator.comparingInt(ProjectSummary::minutes)
                .reversed()
                .thenComparing(ProjectSummary::name,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private enum SummaryPeriod {
        WEEK,
        MONTH
    }

    private record StatusInfo(String code, String name) {

        boolean isDone() {
            return "SELESAI".equals(normalizedCode());
        }

        boolean isOngoing() {
            String normalized = normalizedCode();
            return "BERJALAN".equals(normalized) || "TERTAHAN".equals(normalized);
        }

        private String normalizedCode() {
            return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        }
    }
}
