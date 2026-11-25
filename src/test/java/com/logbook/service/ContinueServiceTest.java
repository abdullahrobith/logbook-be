package com.logbook.service;

import com.logbook.dto.TaskDto;
import com.logbook.entity.LogTask;
import com.logbook.entity.TaskType;
import com.logbook.entity.WorkStatus;
import com.logbook.project.ProjectDto;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.LogTaskRepository;
import com.logbook.repository.TaskTypeRepository;
import com.logbook.repository.UserRepository;
import com.logbook.repository.UserSettingsRepository;
import com.logbook.repository.WorkStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContinueServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);
    private static final List<Short> CONTINUABLE_STATUS_IDS = List.of((short) 2, (short) 3);

    private LogTaskRepository logTaskRepository;
    private WorkStatusRepository workStatusRepository;
    private ProjectRepository projectRepository;
    private TaskTypeRepository taskTypeRepository;
    private DeadlineService deadlineService;
    private ContinueService service;

    @BeforeEach
    void setUp() {
        logTaskRepository = mock(LogTaskRepository.class);
        workStatusRepository = mock(WorkStatusRepository.class);
        projectRepository = mock(ProjectRepository.class);
        taskTypeRepository = mock(TaskTypeRepository.class);
        deadlineService = mock(DeadlineService.class);

        DayDtoMapper dayDtoMapper = new DayDtoMapper(
                mock(CalendarService.class),
                deadlineService,
                mock(UserSettingsRepository.class),
                projectRepository,
                taskTypeRepository,
                workStatusRepository,
                mock(UserRepository.class)
        );
        service = new ContinueService(logTaskRepository, workStatusRepository, dayDtoMapper, deadlineService);
    }

    @Test
    void returnsInProgressTasksFromLatestDayBeforeToday() {
        givenWorkStatuses();
        givenTaskTypes();
        givenProjects();
        when(deadlineService.today()).thenReturn(TODAY);

        LogTask berjalan = task(
                14L, 1L, (short) 1, (short) 2, "ABC-153", 780, (short) 180,
                "Form transfer antarbank: validasi nominal dan rekening tujuan.", null
        );
        LogTask tertahan = task(
                21L, 2L, (short) 2, (short) 3, "INC-87", 900, (short) 60,
                "Ekspor klaim ke PDF gagal.", "Menunggu akses log server staging."
        );
        when(logTaskRepository.findFromLatestDayWithContentBefore(
                USER_ID, TODAY, CONTINUABLE_STATUS_IDS
        )).thenReturn(List.of(berjalan, tertahan));

        List<TaskDto> result = service.findContinueTasks(USER_ID);

        assertEquals(2, result.size());

        TaskDto first = result.get(0);
        assertEquals(14L, first.id());
        assertEquals("Logbook Web", first.project());
        assertEquals("blue", first.color());
        assertEquals("ABC-153", first.ticket());
        assertEquals(780, first.startMinute());
        assertEquals((short) 180, first.durationMinutes());
        assertEquals("Pengembangan", first.type());
        assertEquals("Berjalan", first.workStatus());
        assertEquals("Form transfer antarbank: validasi nominal dan rekening tujuan.", first.summary());
        assertNull(first.obstacle());

        TaskDto second = result.get(1);
        assertEquals(21L, second.id());
        assertEquals("Klaim", second.project());
        assertEquals("orange", second.color());
        assertEquals("Tertahan", second.workStatus());
        assertEquals("Menunggu akses log server staging.", second.obstacle());
    }

    @Test
    void returnsEmptyListWhenNoPreviousDayHasContent() {
        givenWorkStatuses();
        when(deadlineService.today()).thenReturn(TODAY);
        when(logTaskRepository.findFromLatestDayWithContentBefore(
                USER_ID, TODAY, CONTINUABLE_STATUS_IDS
        )).thenReturn(List.of());

        assertTrue(service.findContinueTasks(USER_ID).isEmpty());
    }

    @Test
    void returnsEmptyListWhenContinuableStatusesAreMissing() {
        List<WorkStatus> statuses = List.of(workStatus((short) 1, "SELESAI", "Selesai"));
        when(workStatusRepository.findAll()).thenReturn(statuses);

        assertTrue(service.findContinueTasks(USER_ID).isEmpty());

        verify(logTaskRepository, never())
                .findFromLatestDayWithContentBefore(any(), any(), anyCollection());
    }

    @Test
    void keepsTasksWithoutProjectInfo() {
        givenWorkStatuses();
        givenTaskTypes();
        when(deadlineService.today()).thenReturn(TODAY);
        when(projectRepository.findProjectInfoById(99L)).thenReturn(Optional.empty());

        LogTask orphan = task(
                31L, 99L, (short) 1, (short) 2, "ABC-160", 540, (short) 60,
                "Task dari proyek yang sudah tidak aktif.", null
        );
        when(logTaskRepository.findFromLatestDayWithContentBefore(
                eq(USER_ID), eq(TODAY), eq(CONTINUABLE_STATUS_IDS)
        )).thenReturn(List.of(orphan));

        List<TaskDto> result = service.findContinueTasks(USER_ID);

        assertEquals(1, result.size());
        assertEquals("ABC-160", result.get(0).ticket());
        assertNull(result.get(0).project());
        assertNull(result.get(0).color());
    }

    private void givenWorkStatuses() {
        List<WorkStatus> statuses = List.of(
                workStatus((short) 1, "SELESAI", "Selesai"),
                workStatus((short) 2, "BERJALAN", "Berjalan"),
                workStatus((short) 3, "TERTAHAN", "Tertahan")
        );
        when(workStatusRepository.findAll()).thenReturn(statuses);
    }

    private void givenTaskTypes() {
        List<TaskType> types = List.of(
                taskType((short) 1, "PENGEMBANGAN", "Pengembangan"),
                taskType((short) 2, "PERBAIKAN_BUG", "Perbaikan bug")
        );
        when(taskTypeRepository.findAll()).thenReturn(types);
    }

    private void givenProjects() {
        ProjectDto logbookWeb = new ProjectDto(1L, "Logbook Web", "Asuransi ABC", "blue");
        ProjectDto klaim = new ProjectDto(2L, "Klaim", "Asuransi ABC", "orange");
        when(projectRepository.findProjectInfoById(1L)).thenReturn(Optional.of(logbookWeb));
        when(projectRepository.findProjectInfoById(2L)).thenReturn(Optional.of(klaim));
    }

    private WorkStatus workStatus(Short id, String code, String name) {
        WorkStatus status = mock(WorkStatus.class);
        when(status.getId()).thenReturn(id);
        when(status.getCode()).thenReturn(code);
        when(status.getName()).thenReturn(name);
        return status;
    }

    private TaskType taskType(Short id, String code, String name) {
        TaskType type = mock(TaskType.class);
        when(type.getId()).thenReturn(id);
        when(type.getCode()).thenReturn(code);
        when(type.getName()).thenReturn(name);
        return type;
    }

    private LogTask task(
            Long id,
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            String ticket,
            Integer startMinute,
            Short durationMinutes,
            String summary,
            String obstacle
    ) {
        OffsetDateTime now = OffsetDateTime.parse("2026-11-16T09:00:00+07:00");
        LogTask task = new LogTask(
                null, projectId, taskTypeId, workStatusId, ticket,
                startMinute, durationMinutes, summary, obstacle, now, now
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
