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

        when(dailyLogRepository.findById(dailyLogId))
                .thenReturn(Optional.of(dailyLog));
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
}