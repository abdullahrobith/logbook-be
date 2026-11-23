package com.logbook;

import com.logbook.support.CsrfTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test untuk Ringkasanku (SRS LBK-KUA-04). Data uji dibuat dengan
 * JDBC pada tanggal jauh di masa depan yang sengaja dipilih agar tidak tabrakan
 * dengan fixture lokal, lalu digulung kembali oleh @Transactional.
 */
@SpringBootTest(properties = {
        "logbook.jadwal.aktif=false"
})
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
@Transactional
class SummaryIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);
    private static final LocalDate TUESDAY = LocalDate.of(2031, 3, 4);
    private static final LocalDate SATURDAY = LocalDate.of(2031, 3, 8);
    private static final LocalDate LATER_IN_MONTH = LocalDate.of(2031, 3, 20);

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
                                    "email": "rina.talent@contoh.id",
                                    "password": "Logbook@2026"
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
    void weeklySummaryReturnsAggregatesAndNeutralShortSummaryDays() throws Exception {
        insertDay(MONDAY);
        insertDay(TUESDAY);
        insertDay(SATURDAY);
        insertTask(MONDAY, 1L, (short) 1, (short) 1, 480, 120, longSummary(), null);
        insertTask(MONDAY, 2L, (short) 2, (short) 2, 540, 60, longSummary(), null);
        insertTask(TUESDAY, 1L, (short) 1, (short) 3, 600, 90,
                "Singkat", "Menunggu akses log server staging.");
        insertTask(SATURDAY, 3L, (short) 3, (short) 1, 480, 60, longSummary(), null);

        mockMvc.perform(
                get("/api/summary")
                        .param("period", "WEEK")
                        .param("date", MONDAY.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMinutes").value(270))
        .andExpect(jsonPath("$.daysRecorded").value(2))
        .andExpect(jsonPath("$.workdays").value(5))
        .andExpect(jsonPath("$.perType[0].name").value("Pengembangan"))
        .andExpect(jsonPath("$.perType[0].minutes").value(210))
        .andExpect(jsonPath("$.perProject[0].name").value("Mobile Banking"))
        .andExpect(jsonPath("$.perProject[0].minutes").value(210))
        .andExpect(jsonPath("$.done.length()").value(2))
        .andExpect(jsonPath("$.ongoing.length()").value(2))
        .andExpect(jsonPath("$.obstacles.length()").value(1))
        .andExpect(jsonPath("$.shortSummaryDays[0].date").value(TUESDAY.toString()))
        .andExpect(jsonPath("$.shortSummaryDays[0].count").value(1))
        .andExpect(jsonPath("$.shortSummaryDays[0]", not(hasKey("score"))))
        .andExpect(jsonPath("$.shortSummaryDays[0]", not(hasKey("skor"))));
    }

    @Test
    void monthlySummaryIncludesTasksOutsideTheSelectedWeek() throws Exception {
        insertDay(MONDAY);
        insertDay(LATER_IN_MONTH);
        insertTask(MONDAY, 1L, (short) 1, (short) 1, 480, 60, longSummary(), null);
        insertTask(LATER_IN_MONTH, 1L, (short) 1, (short) 1, 480, 90, longSummary(), null);

        mockMvc.perform(
                get("/api/summary")
                        .param("period", "WEEK")
                        .param("date", MONDAY.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMinutes").value(60));

        mockMvc.perform(
                get("/api/summary")
                        .param("period", "MONTH")
                        .param("date", MONDAY.toString())
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMinutes").value(150))
        .andExpect(jsonPath("$.daysRecorded").value(2))
        .andExpect(jsonPath("$.workdays").value(21));
    }

    @Test
    void invalidPeriodReturnsBadRequest() throws Exception {
        mockMvc.perform(
                get("/api/summary")
                        .param("period", "YEAR")
                        .param("date", MONDAY.toString())
                        .session(session)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Period harus WEEK atau MONTH"));
    }

    @Test
    void summaryRequiresLogin() throws Exception {
        mockMvc.perform(
                get("/api/summary")
                        .param("period", "WEEK")
                        .param("date", MONDAY.toString())
        )
        .andExpect(status().isUnauthorized());
    }

    private void insertDay(LocalDate date) {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Jakarta"));
        jdbcTemplate.update("""
                        INSERT INTO daily_logs (
                            user_id, log_date, work_schedule_id, status, created_at, updated_at
                        )
                        VALUES (?, ?, 1, 'DRAF', ?, ?)
                        ON CONFLICT (user_id, log_date) DO NOTHING
                        """,
                USER_ID, java.sql.Date.valueOf(date), now, now);
    }

    private void insertTask(
            LocalDate date,
            Long projectId,
            Short taskTypeId,
            Short workStatusId,
            int startMinute,
            int durationMinutes,
            String summary,
            String obstacle
    ) {
        Long dailyLogId = jdbcTemplate.queryForObject(
                "SELECT id FROM daily_logs WHERE user_id = ? AND log_date = ?",
                Long.class,
                USER_ID,
                java.sql.Date.valueOf(date)
        );
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Jakarta"));
        jdbcTemplate.update("""
                        INSERT INTO log_tasks (
                            daily_log_id, project_id, task_type_id, work_status_id,
                            ticket, start_minute, duration_minutes, summary, obstacle,
                            created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                dailyLogId,
                projectId,
                taskTypeId,
                workStatusId,
                "SUM-" + dailyLogId + "-" + startMinute,
                startMinute,
                durationMinutes,
                summary,
                obstacle,
                now,
                now
        );
    }

    private String longSummary() {
        return "Menyelesaikan alur transfer antarbank dengan validasi rekening.";
    }
}
