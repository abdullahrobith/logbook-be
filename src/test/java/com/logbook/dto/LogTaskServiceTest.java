package com.logbook.service;

import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.LogTaskRequest;
import com.logbook.entity.DailyLog;
import com.logbook.entity.LogTask;
import com.logbook.project.ProjectRepository;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.LogTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogTaskServiceTest {

    private DailyLogRepository dailyLogRepository;
    private LogTaskRepository logTaskRepository;
    private ProjectRepository projectRepository;

    private LogTaskService service;

    private final Long userId = 1L;
    private final Long dailyLogId = 10L;

    private DailyLog dailyLog;

    @BeforeEach
    void setUp() {
        dailyLogRepository = mock(DailyLogRepository.class);
        logTaskRepository = mock(LogTaskRepository.class);
        projectRepository = mock(ProjectRepository.class);

        service = new LogTaskService(
                dailyLogRepository,
                logTaskRepository,
                projectRepository
        );

        dailyLog = mock(DailyLog.class);

        when(dailyLog.getId()).thenReturn(dailyLogId);
        when(dailyLog.getUserId()).thenReturn(userId);
        when(dailyLog.getStatus()).thenReturn("DRAF");
        when(dailyLog.getTasks()).thenReturn(List.of());

        when(dailyLogRepository.findById(dailyLogId))
                .thenReturn(Optional.of(dailyLog));
        when(logTaskRepository.existsActiveTaskType(any()))
                .thenReturn(true);
    }

    private LogTaskRequest validRequest() {
        return new LogTaskRequest(
                1L,
                (short) 1,
                (short) 2,
                " ABC-154 ",
                LocalTime.of(9, 0),
                (short) 120,
                "  Mengembangkan fitur login  ",
                "  Kendala koneksi  "
        );
    }

    @Test
    void createTaskShouldSucceed() {
        when(projectRepository.existsActiveProjectForUser(1L, userId))
                .thenReturn(true);

        when(logTaskRepository.save(any(LogTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyLogResponse.TaskResponse response =
                service.createTask(userId, dailyLogId, validRequest());

        assertNotNull(response);
        assertEquals(1L, response.projectId());
        assertEquals("ABC-154", response.ticket());
        assertEquals("Mengembangkan fitur login", response.summary());
        assertEquals("Kendala koneksi", response.obstacle());

        verify(logTaskRepository).save(any(LogTask.class));
    }

    @Test
    void createTaskShouldFailWhenDailyLogNotFound() {
        when(dailyLogRepository.findById(dailyLogId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, validRequest())
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenDailyLogBelongsToAnotherUser() {
        when(dailyLog.getUserId()).thenReturn(99L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, validRequest())
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(projectRepository, never())
                .existsActiveProjectForUser(any(), any());

        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenDailyLogIsNotDraft() {
        when(dailyLog.getStatus()).thenReturn("TERKIRIM");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, validRequest())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenWorkStatusIsInvalid() {
        LogTaskRequest request = new LogTaskRequest(
                1L,
                (short) 1,
                (short) 99,
                "ABC-154",
                LocalTime.of(9, 0),
                (short) 120,
                "Mengerjakan fitur",
                null
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, request)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(projectRepository, never())
                .existsActiveProjectForUser(any(), any());

        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenTaskTypeIsInvalid() {
        when(logTaskRepository.existsActiveTaskType((short) 99))
                .thenReturn(false);

        LogTaskRequest request = new LogTaskRequest(
                1L,
                (short) 99,
                (short) 1,
                "ABC-154",
                LocalTime.of(9, 0),
                (short) 120,
                "Mengerjakan fitur",
                null
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(projectRepository, never())
                .existsActiveProjectForUser(any(), any());
        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenDurationIsOutsideAllowedRange() {
        LogTaskRequest request = new LogTaskRequest(
                1L,
                (short) 1,
                (short) 1,
                "ABC-154",
                LocalTime.of(9, 0),
                (short) 4,
                "Mengerjakan fitur",
                null
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(projectRepository, never())
                .existsActiveProjectForUser(any(), any());
        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenProjectDoesNotBelongToUser() {
        when(projectRepository.existsActiveProjectForUser(1L, userId))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, validRequest())
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldFailWhenThreeTasksAlreadyOverlap() {
        LogTask firstTask = overlappingTask(
                1L,
                LocalTime.of(8, 0),
                (short) 120
        );
        LogTask secondTask = overlappingTask(
                2L,
                LocalTime.of(9, 0),
                (short) 60
        );
        LogTask thirdTask = overlappingTask(
                3L,
                LocalTime.of(9, 30),
                (short) 60
        );
        when(projectRepository.existsActiveProjectForUser(1L, userId))
                .thenReturn(true);
        when(dailyLog.getTasks()).thenReturn(List.of(
                firstTask,
                secondTask,
                thirdTask
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(userId, dailyLogId, validRequest())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Maksimal 3 task dapat berjalan pada waktu yang bersamaan. "
                        + "Ubah jam atau durasi task terlebih dahulu.",
                exception.getReason()
        );
        verify(logTaskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldSucceedWhenOnlyTwoTasksOverlap() {
        LogTask firstTask = overlappingTask(
                1L,
                LocalTime.of(8, 0),
                (short) 120
        );
        LogTask secondTask = overlappingTask(
                2L,
                LocalTime.of(9, 0),
                (short) 60
        );
        when(projectRepository.existsActiveProjectForUser(1L, userId))
                .thenReturn(true);
        when(dailyLog.getTasks()).thenReturn(List.of(
                firstTask,
                secondTask
        ));
        when(logTaskRepository.save(any(LogTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.createTask(
                userId,
                dailyLogId,
                validRequest()
        ));

        verify(logTaskRepository).save(any(LogTask.class));
    }

    @Test
    void updateTaskShouldExcludeItselfFromConcurrentLimit() {
        LogTask task = overlappingTask(5L, LocalTime.of(9, 0), (short) 120);
        LogTask firstTask = overlappingTask(
                1L,
                LocalTime.of(8, 0),
                (short) 120
        );
        LogTask secondTask = overlappingTask(
                2L,
                LocalTime.of(9, 0),
                (short) 60
        );
        when(task.getDailyLog()).thenReturn(dailyLog);
        when(logTaskRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(task));
        when(dailyLog.getTasks()).thenReturn(List.of(
                task,
                firstTask,
                secondTask
        ));
        when(projectRepository.existsActiveProjectForUser(1L, userId))
                .thenReturn(true);
        when(logTaskRepository.save(any(LogTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.updateTask(
                userId,
                5L,
                validRequest()
        ));

        verify(logTaskRepository).save(task);
    }

    @Test
    void deleteTaskShouldFailWhenDailyLogIsNotDraft() {
        LogTask task = mock(LogTask.class);
        DailyLog submittedLog = mock(DailyLog.class);

        when(logTaskRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(task));

        when(task.getDailyLog()).thenReturn(submittedLog);
        when(submittedLog.getStatus()).thenReturn("TERKIRIM");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteTask(userId, 5L)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(logTaskRepository, never()).delete(any());
    }

    @Test
    void deleteTaskShouldSucceedWhenDailyLogIsDraft() {
        LogTask task = mock(LogTask.class);
        DailyLog draftLog = mock(DailyLog.class);

        when(logTaskRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(task));

        when(task.getDailyLog()).thenReturn(draftLog);
        when(draftLog.getStatus()).thenReturn("DRAF");

        assertDoesNotThrow(() -> service.deleteTask(userId, 5L));

        verify(logTaskRepository).delete(task);
    }

    private LogTask overlappingTask(
            Long id,
            LocalTime startTime,
            Short durationMinutes
    ) {
        LogTask task = mock(LogTask.class);
        when(task.getId()).thenReturn(id);
        when(task.getStartTime()).thenReturn(startTime);
        when(task.getDurationMinutes()).thenReturn(durationMinutes);
        return task;
    }
}