package com.logbook.service;

import com.logbook.dto.DayInfoResponse;
import com.logbook.dto.SummaryResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.entity.TaskType;
import com.logbook.entity.WorkStatus;
import com.logbook.project.ProjectDto;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.LogTaskRepository;
import com.logbook.repository.TaskTypeRepository;
import com.logbook.repository.WorkStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummaryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);
    private static final LocalDate TUESDAY = LocalDate.of(2031, 3, 4);
    private static final LocalDate SATURDAY = LocalDate.of(2031, 3, 8);
    private static final LocalDate LATER_IN_MONTH = LocalDate.of(2031, 3, 20);
    private static final int RINGKASAN_MIN = 20;

    private LogTaskRepository logTaskRepository;
    private CalendarService calendarService;
    private DeadlineService deadlineService;
    private ProjectRepository projectRepository;
    private TaskTypeRepository taskTypeRepository;
    private WorkStatusRepository workStatusRepository;
    private SummaryService service;

    @BeforeEach
    void setUp() {
        logTaskRepository = mock(LogTaskRepository.class);
        calendarService = mock(CalendarService.class);
        deadlineService = mock(DeadlineService.class);
        projectRepository = mock(ProjectRepository.class);
        taskTypeRepository = mock(TaskTypeRepository.class);
        workStatusRepository = mock(WorkStatusRepository.class);

        List<TaskType> taskTypes = List.of(
                taskType((short) 1, "PENGEMBANGAN", "Pengembangan"),
                taskType((short) 2, "PERBAIKAN_BUG", "Perbaikan bug"),
                taskType((short) 3, "RAPAT", "Rapat")
        );
        List<WorkStatus> workStatuses = List.of(
                workStatus((short) 1, "SELESAI", "Selesai"),
                workStatus((short) 2, "BERJALAN", "Berjalan"),
                workStatus((short) 3, "TERTAHAN", "Tertahan")
        );
        when(taskTypeRepository.findAll()).thenReturn(taskTypes);
        when(workStatusRepository.findAll()).thenReturn(workStatuses);
        when(projectRepository.findProjectInfoById(1L)).thenReturn(
                Optional.of(new ProjectDto(1L, "Mobile Banking", "Bank XYZ", "blue")));
        when(projectRepository.findProjectInfoById(2L)).thenReturn(
                Optional.of(new ProjectDto(2L, "Klaim", "Asuransi ABC", "orange")));
        when(projectRepository.findProjectInfoById(3L)).thenReturn(
                Optional.of(new ProjectDto(3L, "Internal Padepokan", "Padepokan", "green")));

        when(calendarService.describeRange(eq(USER_ID), any(), any()))
                .thenAnswer(invocation -> days(
                        invocation.getArgument(1),
                        invocation.getArgument(2)
                ));
        when(deadlineService.activeDay(USER_ID)).thenReturn(MONDAY);

        service = new SummaryService(
                logTaskRepository,
                calendarService,
                deadlineService,
                projectRepository,
                taskTypeRepository,
                workStatusRepository,
                RINGKASAN_MIN
        );
    }

    @Test
    void weeklySummaryUsesUnionForTotalAndFullDurationsForBreakdowns() {
        when(logTaskRepository.findForUserBetween(
                eq(USER_ID), eq(MONDAY), eq(MONDAY.plusDays(6))
        )).thenReturn(List.of(
                task(1L, MONDAY, 1L, (short) 1, (short) 1, 480, 120, longSummary(), null),
                task(2L, MONDAY, 2L, (short) 2, (short) 2, 540, 60, longSummary(), null),
                task(3L, TUESDAY, 1L, (short) 1, (short) 3, 600, 90,
                        "Singkat", "Menunggu akses log server staging."),
                task(4L, SATURDAY, 3L, (short) 3, (short) 1, 480, 60, longSummary(), null)
        ));

        SummaryResponse response = service.getSummary(USER_ID, "week", MONDAY.toString());

        // Union: Senin 120 + Selasa 90 + Sabtu 60. Sabtu bukan hari kerja.
        assertEquals(270, response.totalMinutes());
        assertEquals(2, response.daysRecorded());
        assertEquals(5, response.workdays());

        assertEquals(3, response.perType().size());
        assertEquals("Pengembangan", response.perType().get(0).name());
        assertEquals(210, response.perType().get(0).minutes());
        assertEquals("Perbaikan bug", response.perType().get(1).name());
        assertEquals("Rapat", response.perType().get(2).name());

        assertEquals(3, response.perProject().size());
        assertEquals("Mobile Banking", response.perProject().get(0).name());
        assertEquals(210, response.perProject().get(0).minutes());
        assertEquals("blue", response.perProject().get(0).color());

        assertEquals(2, response.done().size());
        assertEquals(2, response.ongoing().size());
        assertEquals(1, response.obstacles().size());
        assertEquals(TUESDAY, response.obstacles().get(0).date());
        assertEquals("Menunggu akses log server staging.", response.obstacles().get(0).obstacle());

        assertEquals(1, response.shortSummaryDays().size());
        assertEquals(TUESDAY, response.shortSummaryDays().get(0).date());
        assertEquals(1, response.shortSummaryDays().get(0).count());

        verify(logTaskRepository).findForUserBetween(USER_ID, MONDAY, MONDAY.plusDays(6));
    }

    @Test
    void blankDateUsesActiveDayAndMonthRange() {
        when(logTaskRepository.findForUserBetween(
                USER_ID,
                LocalDate.of(2031, 3, 1),
                LocalDate.of(2031, 3, 31)
        )).thenReturn(List.of(
                task(5L, LATER_IN_MONTH, 1L, (short) 1, (short) 1,
                        480, 90, longSummary(), null)
        ));

        SummaryResponse response = service.getSummary(USER_ID, "MONTH", "  ");

        assertEquals(90, response.totalMinutes());
        assertEquals(1, response.daysRecorded());
        assertEquals(21, response.workdays());
        assertTrue(response.done().stream()
                .anyMatch(item -> item.date().equals(LATER_IN_MONTH)));
        verify(deadlineService).activeDay(USER_ID);
    }

    @Test
    void tasksOnHolidaysAreRecordedButNotCountedAsWorkdays() {
        when(calendarService.describeRange(
                eq(USER_ID), eq(MONDAY), eq(MONDAY.plusDays(6))
        )).thenReturn(List.of(new DayInfoResponse(MONDAY, "Libur nasional", false, false, false)));
        when(logTaskRepository.findForUserBetween(
                USER_ID, MONDAY, MONDAY.plusDays(6)
        )).thenReturn(List.of(
                task(6L, MONDAY, 1L, (short) 1, (short) 1, 480, 60, longSummary(), null)
        ));

        SummaryResponse response = service.getSummary(USER_ID, "WEEK", MONDAY.toString());

        assertEquals(60, response.totalMinutes());
        assertEquals(0, response.daysRecorded());
        assertEquals(0, response.workdays());
        assertEquals(1, response.perType().size());
        assertEquals(1, response.done().size());
    }

    @Test
    void zeroThresholdOmitsShortSummaryDays() {
        SummaryService disabled = new SummaryService(
                logTaskRepository,
                calendarService,
                deadlineService,
                projectRepository,
                taskTypeRepository,
                workStatusRepository,
                0
        );
        when(logTaskRepository.findForUserBetween(
                USER_ID, MONDAY, MONDAY.plusDays(6)
        )).thenReturn(List.of(
                task(7L, MONDAY, 1L, (short) 1, (short) 1, 480, 60, "Singkat", null)
        ));

        SummaryResponse response = disabled.getSummary(USER_ID, "WEEK", MONDAY.toString());

        assertTrue(response.shortSummaryDays().isEmpty());
    }

    @Test
    void invalidPeriodAndDateReturnBadRequest() {
        ResponseStatusException periodError = assertThrows(
                ResponseStatusException.class,
                () -> service.getSummary(USER_ID, "YEAR", MONDAY.toString())
        );
        assertEquals(HttpStatus.BAD_REQUEST, periodError.getStatusCode());
        assertEquals("Period harus WEEK atau MONTH", periodError.getReason());

        ResponseStatusException dateError = assertThrows(
                ResponseStatusException.class,
                () -> service.getSummary(USER_ID, "WEEK", "03-03-2031")
        );
        assertEquals(HttpStatus.BAD_REQUEST, dateError.getStatusCode());
        assertEquals("Tanggal tidak valid", dateError.getReason());

        verify(logTaskRepository, never()).findForUserBetween(any(), any(), any());
    }

    private List<DayInfoResponse> days(LocalDate fromDate, LocalDate toDate) {
        List<DayInfoResponse> days = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            boolean weekend = CalendarService.isWeekend(date);
            days.add(new DayInfoResponse(date, null, false, weekend, !weekend));
        }
        return days;
    }

    private LogTask task(
            Long id,
            LocalDate date,
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            int startMinute,
            int durationMinutes,
            String summary,
            String obstacle
    ) {
        OffsetDateTime now = OffsetDateTime.parse("2031-03-03T09:00:00+07:00");
        DailyLog dailyLog = new DailyLog(USER_ID, date, (short) 1, "DRAF", now, now);
        LogTask task = new LogTask(
                dailyLog,
                projectId,
                taskTypeId,
                workStatusId,
                "ABC-" + id,
                startMinute,
                (short) durationMinutes,
                summary,
                obstacle,
                now,
                now
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private TaskType taskType(Short id, String code, String name) {
        TaskType taskType = mock(TaskType.class);
        when(taskType.getId()).thenReturn(id);
        when(taskType.getCode()).thenReturn(code);
        when(taskType.getName()).thenReturn(name);
        return taskType;
    }

    private WorkStatus workStatus(Short id, String code, String name) {
        WorkStatus status = mock(WorkStatus.class);
        when(status.getId()).thenReturn(id);
        when(status.getCode()).thenReturn(code);
        when(status.getName()).thenReturn(name);
        return status;
    }

    private String longSummary() {
        return "Menyelesaikan alur transfer antarbank dengan validasi rekening.";
    }
}
