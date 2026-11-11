package com.logbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Memastikan aturan batas hari benar-benar ditegakkan saat
 * {@code logbook.batas-hari.aktif=true}: hari yang sudah lewat dan hari yang
 * belum tiba terkunci, hanya tanggal hari ini yang dapat diisi.
 */
@SpringBootTest(properties = {
        "logbook.batas-hari.aktif=true",
        "logbook.zona-waktu=Asia/Jakarta"
})
@AutoConfigureMockMvc
@Transactional
class DailyLogDeadlineIntegrationTest {

    private static final String PAST_DAILY_LOG_ID = "3";
    private static final String PAST_DAILY_LOG_DATE = "2026-10-29";
    private static final String PAST_TASK_ID = "12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "rina.talent@contoh.id"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andReturn();

        session = (MockHttpSession) result.getRequest().getSession(false);

        if (session == null) {
            throw new AssertionError("Session login tidak berhasil dibuat.");
        }
    }

    @Test
    void pastDailyLogResponseShouldNotBeEditableAndExposeDeadline() throws Exception {
        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", PAST_DAILY_LOG_DATE)
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editable").value(false))
        .andExpect(jsonPath("$.deadline", startsWith("2026-10-30T00:00")));
    }

    @Test
    void pastDayShouldRejectNewTask() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", PAST_DAILY_LOG_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson("17:00"))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    @Test
    void pastDayShouldRejectSubmit() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/submit", PAST_DAILY_LOG_ID)
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    @Test
    void pastDayShouldRejectWithdraw() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/withdraw", "1")
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    @Test
    void pastTaskShouldRejectUpdate() throws Exception {
        mockMvc.perform(
                put("/api/tasks/{id}", PAST_TASK_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson("17:00"))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    @Test
    void pastTaskShouldRejectDelete() throws Exception {
        mockMvc.perform(
                delete("/api/tasks/{id}", PAST_TASK_ID)
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    @Test
    void pastDayShouldRejectDailyLogCreationWhenMissing() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs")
                        .param("date", "2000-01-01")
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    @Test
    void currentDayShouldRemainEditableAndAllowNewTask() throws Exception {
        LocalDate today = LocalDate.now();
        Long dailyLogId = ensureDraftLog(today);

        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", today.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editable").value(true))
        .andExpect(jsonPath("$.deadline").isNotEmpty());

        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", dailyLogId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson("17:00"))
        )
        .andExpect(status().isCreated());
    }

    @Test
    void futureDayShouldBeLockedAndRejectNewTask() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        Long dailyLogId = ensureDraftLog(futureDate);

        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", futureDate.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editable").value(false))
        .andExpect(jsonPath("$.deadline").isNotEmpty());

        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", dailyLogId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson("17:00"))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("belum tiba")));

        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/submit", dailyLogId)
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("belum tiba")));
    }

    @Test
    void futureDayShouldRejectDailyLogCreationWhenMissing() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs")
                        .param("date", "2099-01-01")
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("belum tiba")));
    }

    @Test
    void rejectedTodayShouldBeEditableAndAllowResubmit() throws Exception {
        LocalDate today = LocalDate.now();
        Long dailyLogId = ensureLog(today, "DITOLAK");

        jdbcTemplate.update(
                "INSERT INTO log_tasks (daily_log_id, project_id, task_type_id, "
                        + "work_status_id, start_time, duration_minutes, summary, "
                        + "created_at, updated_at) "
                        + "VALUES (?, 1, 1, 1, '09:00', 60, 'Task yang perlu diperbaiki', "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                dailyLogId
        );
        jdbcTemplate.update(
                "UPDATE daily_logs SET rejection_reason = ?, decided_by_user_id = 2, "
                        + "decided_at = CURRENT_TIMESTAMP WHERE id = ?",
                "Ringkasan kurang rinci",
                dailyLogId
        );

        Long taskId = jdbcTemplate.queryForObject(
                "SELECT id FROM log_tasks WHERE daily_log_id = ?",
                Long.class,
                dailyLogId
        );

        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", today.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editable").value(true))
        .andExpect(jsonPath("$.rejectionReason").value("Ringkasan kurang rinci"));

        mockMvc.perform(
                put("/api/tasks/{id}", taskId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson("10:00"))
        )
        .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/submit", dailyLogId)
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MENUNGGU"))
        .andExpect(jsonPath("$.rejectionReason").value(nullValue()));
    }

    @Test
    void rejectedPastDayShouldStayLocked() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        Long dailyLogId = ensureLog(pastDate, "DITOLAK");

        jdbcTemplate.update(
                "UPDATE daily_logs SET rejection_reason = ? WHERE id = ?",
                "Ringkasan kurang rinci",
                dailyLogId
        );

        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", pastDate.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editable").value(false))
        .andExpect(jsonPath("$.rejectionReason").value("Ringkasan kurang rinci"));

        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/submit", dailyLogId)
                        .session(session)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Batas hari")));
    }

    /**
     * Database lokal bisa saja sudah berisi Daily Log pada tanggal tersebut.
     * Bersihkan lebih dahulu agar test membuat data miliknya sendiri; seluruh
     * perubahan akan di-rollback oleh {@code @Transactional}.
     */
    private Long ensureDraftLog(LocalDate date) {
        return ensureLog(date, "DRAF");
    }

    private Long ensureLog(LocalDate date, String status) {
        jdbcTemplate.update(
                "DELETE FROM log_tasks WHERE daily_log_id IN "
                        + "(SELECT id FROM daily_logs WHERE user_id = 1 AND log_date = ?)",
                date
        );
        jdbcTemplate.update(
                "DELETE FROM daily_logs WHERE user_id = 1 AND log_date = ?",
                date
        );
        jdbcTemplate.update(
                "INSERT INTO daily_logs (user_id, log_date, work_schedule_id, status, "
                        + "created_at, updated_at) "
                        + "VALUES (1, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                date,
                status
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM daily_logs WHERE user_id = 1 AND log_date = ?",
                Long.class,
                date
        );
    }

    private String validTaskJson(String startTime) {
        return """
                {
                    "projectId": 1,
                    "taskTypeId": 1,
                    "workStatusId": 1,
                    "ticket": "TEST-101",
                    "startTime": "%s",
                    "durationMinutes": 30,
                    "summary": "Uji batas hari",
                    "obstacle": "Tidak ada"
                }
                """.formatted(startTime);
    }
}
