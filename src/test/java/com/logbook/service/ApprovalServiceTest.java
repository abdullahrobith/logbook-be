package com.logbook.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.logbook.entity.DailyLog;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {

    private static final LocalDate LOG_DATE = LocalDate.of(2026, 11, 10);
    private static final Long OWNER_ID = 7L;
    private static final Long ACTOR_ID = 2L;
    private static final String ACTOR_EMAIL = "atasan@contoh.id";

    private DailyLogRepository dailyLogRepository;
    private UserRepository userRepository;
    private RoleService roleService;
    private ApprovalService service;

    private User actor;
    private User owner;

    @BeforeEach
    void setUp() {
        dailyLogRepository = mock(DailyLogRepository.class);
        userRepository = mock(UserRepository.class);
        roleService = mock(RoleService.class);
        service = new ApprovalService(dailyLogRepository, userRepository, roleService);

        actor = user(ACTOR_ID, ACTOR_EMAIL);
        owner = user(OWNER_ID, "talent@contoh.id");
    }

    // ==========================================
    // SETUJUI
    // ==========================================

    @Test
    void approveShouldSetApprovedStatusAndRecordDecision() {
        DailyLog dailyLog = log(11L, "MENUNGGU");
        prepareDecision(dailyLog, true);

        var response = service.approve(actor, 11L);

        assertEquals("DISETUJUI", response.status());
        assertEquals(ACTOR_ID, dailyLog.getDecidedByUserId());
        assertNotNull(dailyLog.getDecidedAt());
        verify(dailyLogRepository).save(dailyLog);
    }

    @Test
    void approveShouldRejectOwnDailyLog() {
        User self = user(OWNER_ID, ACTOR_EMAIL);
        DailyLog dailyLog = log(11L, "MENUNGGU");
        when(dailyLogRepository.findById(11L)).thenReturn(Optional.of(dailyLog));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approve(self, 11L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Tidak boleh memutuskan Daily Log milik sendiri", exception.getReason());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void approveShouldRejectActorWhoIsNotSupervisorOfOwner() {
        DailyLog dailyLog = log(11L, "MENUNGGU");
        prepareDecision(dailyLog, false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approve(actor, 11L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void approveShouldRejectWhenStatusIsNotWaiting() {
        DailyLog dailyLog = log(11L, "DRAF");
        prepareDecision(dailyLog, true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approve(actor, 11L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void approveShouldFailWhenDailyLogIsNotFound() {
        when(dailyLogRepository.findById(11L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approve(actor, 11L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ==========================================
    // TOLAK
    // ==========================================

    @Test
    void rejectShouldRequireReason() {
        DailyLog dailyLog = log(11L, "MENUNGGU");
        prepareDecision(dailyLog, true);

        ResponseStatusException blank = assertThrows(
                ResponseStatusException.class,
                () -> service.reject(actor, 11L, "   ")
        );
        ResponseStatusException missing = assertThrows(
                ResponseStatusException.class,
                () -> service.reject(actor, 11L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, blank.getStatusCode());
        assertEquals("Alasan wajib diisi", blank.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, missing.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void rejectShouldStoreTrimmedReasonAndMarkRejected() {
        DailyLog dailyLog = log(11L, "MENUNGGU");
        prepareDecision(dailyLog, true);

        var response = service.reject(actor, 11L, "  Ringkasan kurang jelas  ");

        assertEquals("DITOLAK", response.status());
        assertEquals("Ringkasan kurang jelas", response.rejectionReason());
        assertEquals(ACTOR_ID, dailyLog.getDecidedByUserId());
        assertNotNull(dailyLog.getDecidedAt());
        verify(dailyLogRepository).save(dailyLog);
    }

    @Test
    void rejectShouldRejectReasonLongerThanColumn() {
        DailyLog dailyLog = log(11L, "MENUNGGU");
        prepareDecision(dailyLog, true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.reject(actor, 11L, "a".repeat(1001))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    // ==========================================
    // AJUKAN PERBAIKAN
    // ==========================================

    @Test
    void requestRevisionShouldRequireOwnership() {
        DailyLog dailyLog = log(11L, "DISETUJUI");
        when(dailyLogRepository.findById(11L)).thenReturn(Optional.of(dailyLog));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requestRevision(actor, 11L, "Ada task yang terlewat")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void requestRevisionShouldRequireApprovedStatus() {
        User talent = user(OWNER_ID, "talent@contoh.id");
        DailyLog dailyLog = log(11L, "MENUNGGU");
        when(dailyLogRepository.findById(11L)).thenReturn(Optional.of(dailyLog));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requestRevision(talent, 11L, "Ada task yang terlewat")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void requestRevisionShouldMarkRevisionRequested() {
        User talent = user(OWNER_ID, "talent@contoh.id");
        DailyLog dailyLog = log(11L, "DISETUJUI");
        when(dailyLogRepository.findById(11L)).thenReturn(Optional.of(dailyLog));
        when(dailyLogRepository.save(dailyLog)).thenReturn(dailyLog);

        var response = service.requestRevision(talent, 11L, "  Perlu menambah task rapat  ");

        assertEquals("PERBAIKAN_DIAJUKAN", response.status());
        assertEquals("Perlu menambah task rapat", response.revisionReason());
        verify(dailyLogRepository).save(dailyLog);
    }

    // ==========================================
    // SETUJUI PERBAIKAN
    // ==========================================

    @Test
    void approveRevisionShouldReopenDayAsDraft() {
        DailyLog dailyLog = log(11L, "PERBAIKAN_DIAJUKAN");
        prepareDecision(dailyLog, true);

        var response = service.approveRevision(actor, 11L);

        assertEquals("DRAF", response.status());
        assertEquals(ACTOR_ID, dailyLog.getDecidedByUserId());
        assertNotNull(dailyLog.getDecidedAt());
        verify(dailyLogRepository).save(dailyLog);
    }

    @Test
    void approveRevisionShouldRequireRevisionRequestedStatus() {
        DailyLog dailyLog = log(11L, "DISETUJUI");
        prepareDecision(dailyLog, true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approveRevision(actor, 11L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void approveRevisionShouldRejectOwnDailyLog() {
        User self = user(OWNER_ID, ACTOR_EMAIL);
        DailyLog dailyLog = log(11L, "PERBAIKAN_DIAJUKAN");
        when(dailyLogRepository.findById(11L)).thenReturn(Optional.of(dailyLog));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approveRevision(self, 11L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    // ==========================================
    // LOG INFO SETIAP KEPUTUSAN
    // ==========================================

    @Test
    void everyDecisionIsLoggedAtInfoWithActorAndLogDate() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApprovalService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            User talent = user(OWNER_ID, "talent@contoh.id");

            DailyLog waiting = log(11L, "MENUNGGU");
            prepareDecision(waiting, true);
            service.approve(actor, 11L);

            DailyLog rejected = log(12L, "MENUNGGU");
            prepareDecision(rejected, true);
            service.reject(actor, 12L, "Kurang rinci");

            DailyLog approved = log(13L, "DISETUJUI");
            when(dailyLogRepository.findById(13L)).thenReturn(Optional.of(approved));
            when(dailyLogRepository.save(approved)).thenReturn(approved);
            service.requestRevision(talent, 13L, "Perlu menambah task");

            DailyLog revision = log(14L, "PERBAIKAN_DIAJUKAN");
            prepareDecision(revision, true);
            service.approveRevision(actor, 14L);
        } finally {
            logger.detachAppender(appender);
        }

        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertEquals(4, messages.size(), "Setiap keputusan harus menulis satu log INFO");
        assertTrue(messages.get(0).contains(LOG_DATE.toString()));
        assertTrue(messages.get(0).contains(ACTOR_EMAIL));
        assertTrue(messages.get(1).contains(LOG_DATE.toString()));
        assertTrue(messages.get(1).contains(ACTOR_EMAIL));
        assertTrue(messages.get(2).contains(LOG_DATE.toString()));
        assertTrue(messages.get(2).contains("talent@contoh.id"));
        assertTrue(messages.get(3).contains(LOG_DATE.toString()));
        assertTrue(messages.get(3).contains(ACTOR_EMAIL));
        assertTrue(appender.list.stream().allMatch(
                event -> event.getLevel().toString().equals("INFO")
        ));
    }

    private void prepareDecision(DailyLog dailyLog, boolean canDecide) {
        when(dailyLogRepository.findById(dailyLog.getId()))
                .thenReturn(Optional.of(dailyLog));
        when(userRepository.findByIdAndActiveTrue(OWNER_ID))
                .thenReturn(Optional.of(owner));
        when(roleService.canDecideTalent(actor, owner)).thenReturn(canDecide);
        when(dailyLogRepository.save(dailyLog)).thenReturn(dailyLog);
    }

    private DailyLog log(Long id, String status) {
        OffsetDateTime now = OffsetDateTime.now();
        DailyLog dailyLog = new DailyLog(OWNER_ID, LOG_DATE, (short) 1, status, now, now);
        ReflectionTestUtils.setField(dailyLog, "id", id);
        return dailyLog;
    }

    private User user(Long id, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
