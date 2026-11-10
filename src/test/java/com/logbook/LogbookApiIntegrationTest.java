
package com.logbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogbookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession session;

    private static final String EMAIL = "rina.talent@contoh.id";
    private static final String DAILY_LOG_ID = "3";
    private static final String SUBMITTED_DAILY_LOG_ID = "1";
    private static final String DAILY_LOG_DATE = "2026-10-29";

    private String validTaskJson() {
        return """
                {
                    "projectId": 1,
                    "taskTypeId": 1,
                    "workStatusId": 1,
                    "ticket": "TEST-101",
                    "startTime": "17:00",
                    "durationMinutes": 30,
                    "summary": "Integration test task",
                    "obstacle": "Tidak ada"
                }
                """;
    }

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

    // ==========================================
    // AUTHENTICATION TESTING
    // ==========================================

    @Test
    void loginShouldReturnUserData() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "rina.talent@contoh.id"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void getCurrentUserShouldReturnUserData() throws Exception {
        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void logoutShouldReturnNoContent() throws Exception {
        mockMvc.perform(
                post("/api/auth/logout")
                        .session(session)
        )
        .andExpect(status().isNoContent());

        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "emailtidakterdaftar@example.com"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankEmailShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": ""
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    // ==========================================
    // DAILY LOG TESTING
    // ==========================================

    @Test
    void getDailyLogsShouldReturnData() throws Exception {
        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", DAILY_LOG_DATE)
                        .session(session)
        )
        .andExpect(status().isOk());
    }

    @Test
    void submitDailyLogShouldReturnSubmittedStatus() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/submit", DAILY_LOG_ID)
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(Integer.parseInt(DAILY_LOG_ID)))
        .andExpect(jsonPath("$.status").value("MENUNGGU"))
        .andExpect(jsonPath("$.submittedAt").isNotEmpty());
    }

    @Test
    void withdrawDailyLogShouldReturnDraftStatus() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/withdraw", SUBMITTED_DAILY_LOG_ID)
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(Integer.parseInt(SUBMITTED_DAILY_LOG_ID)))
        .andExpect(jsonPath("$.status").value("DRAF"))
        .andExpect(jsonPath("$.submittedAt").isEmpty());
    }

    // ==========================================
    // LOG TASK TESTING
    // ==========================================

    @Test
    void createTaskShouldReturnCreated() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", DAILY_LOG_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson())
        )
        .andExpect(status().isCreated());
    }

    @Test
    void taskCrudShouldWorkSuccessfully() throws Exception {

        // CREATE
        MvcResult createResult = mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", DAILY_LOG_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson())
        )
        .andExpect(status().isCreated())
        .andReturn();

        // GET TASK ID
        String responseBody = createResult.getResponse().getContentAsString();

        Matcher matcher = Pattern
                .compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(responseBody);

        if (!matcher.find()) {
            throw new AssertionError(
                    "ID task tidak ditemukan dalam response: " + responseBody
            );
        }

        long taskId = Long.parseLong(matcher.group(1));

        // UPDATE
        mockMvc.perform(
                put("/api/tasks/{id}", taskId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "projectId": 1,
                                    "taskTypeId": 1,
                                    "workStatusId": 2,
                                    "ticket": "TEST-101",
                                    "startTime": "17:30",
                                    "durationMinutes": 45,
                                    "summary": "Integration test update task",
                                    "obstacle": "Ada kendala kecil"
                                }
                                """)
        )
        .andExpect(status().isOk());

        // DELETE
        mockMvc.perform(
                delete("/api/tasks/{id}", taskId)
                        .session(session)
        )
        .andExpect(status().isNoContent());
    }

    // ==========================================
    // PROJECT TESTING
    // ==========================================

    @Test
    void getProjectsShouldReturnData() throws Exception {
        mockMvc.perform(
                get("/api/projects")
                        .session(session)
        )
        .andExpect(status().isOk());
    }

    // ==========================================
    // MASTER DATA TESTING
    // ==========================================

    @Test
    void getMasterDataShouldReturnData() throws Exception {
        mockMvc.perform(
                get("/api/tasks/master-data")
        )
        .andExpect(status().isOk());
    }

    // ==========================================
    // EXAMPLE TESTING
    // ==========================================

    @Test
    void getExamplesShouldReturnData() throws Exception {
        mockMvc.perform(
                get("/api/examples")
        )
        .andExpect(status().isOk());
    }

    // ==========================================
    // NEGATIVE TESTING: UNAUTHORIZED ACCESS
    // ==========================================

    @Test
    void getProjectsWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/projects")
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/auth/me")
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void getDailyLogsWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/daily-logs")
                        .param("date", DAILY_LOG_DATE)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void createTaskWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", DAILY_LOG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson())
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTaskWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                put("/api/tasks/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson())
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteTaskWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(
                delete("/api/tasks/999999")
        )
        .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // NEGATIVE TESTING: DATA NOT FOUND
    // ==========================================

    @Test
    void createTaskWithUnknownDailyLogShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/999999/tasks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void updateUnknownTaskShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                put("/api/tasks/999999")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTaskJson())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void deleteUnknownTaskShouldReturnNotFound() throws Exception {
        mockMvc.perform(
                delete("/api/tasks/999999")
                        .session(session)
        )
        .andExpect(status().isNotFound());
    }

    // ==========================================
    // NEGATIVE TESTING: INVALID REQUEST
    // ==========================================

    @Test
    void createTaskWithInvalidWorkStatusShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", DAILY_LOG_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "projectId": 1,
                                    "taskTypeId": 1,
                                    "workStatusId": 999,
                                    "ticket": "TEST-101",
                                    "startTime": "17:00",
                                    "durationMinutes": 30,
                                    "summary": "Invalid work status",
                                    "obstacle": "Tidak ada"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void createTaskWithInvalidTicketShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", DAILY_LOG_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "projectId": 1,
                                    "taskTypeId": 1,
                                    "workStatusId": 1,
                                    "ticket": "INVALID TICKET!",
                                    "startTime": "17:00",
                                    "durationMinutes": 30,
                                    "summary": "Invalid ticket format",
                                    "obstacle": "Tidak ada"
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Format tiket tidak valid. Contoh: ABC-154"))
        .andExpect(jsonPath("$.fieldErrors.ticket").value("Format tiket tidak valid. Contoh: ABC-154"));
    }

    @Test
    void createFourthOverlappingTaskShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                post("/api/daily-logs/{dailyLogId}/tasks", DAILY_LOG_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "projectId": 1,
                                    "taskTypeId": 1,
                                    "workStatusId": 1,
                                    "ticket": "TEST-104",
                                    "startTime": "09:30",
                                    "durationMinutes": 30,
                                    "summary": "Task keempat pada jam yang sama",
                                    "obstacle": "Tidak ada"
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
                "Maksimal 3 task dapat berjalan pada waktu yang bersamaan. "
                        + "Ubah jam atau durasi task terlebih dahulu."
        ));
    }

    @Test
    void getDailyLogsWithoutDateShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                get("/api/daily-logs")
                        .session(session)
        )
        .andExpect(status().isBadRequest());
    }

    // ==========================================
    // ROLE AND AUTHORIZATION TESTING
    // ==========================================

    @Test
    void getRolesShouldReturnTalentForRegularUser() throws Exception {
        mockMvc.perform(
                get("/api/auth/roles")
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.roles", hasItem("TALENT")))
        .andExpect(jsonPath("$.talent").value(true))
        .andExpect(jsonPath("$.atasan").value(false))
        .andExpect(jsonPath("$.admin").value(false))
        .andExpect(jsonPath("$.teamSize").value(0));
    }

    @Test
    void meShouldIncludeRoles() throws Exception {
        mockMvc.perform(
                get("/api/auth/me")
                        .session(session)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", hasItem("TALENT")));
    }

    @Test
    void getRolesWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRolesShouldReturnAtasanWhenEmailAtasanPointsToUser() throws Exception {
        insertUser("atasan.role@contoh.id", "Atasan Role", false);
        jdbcTemplate.update(
                "UPDATE users SET email_atasan = ? WHERE id = 2",
                "atasan.role@contoh.id"
        );

        MockHttpSession supervisorSession = loginAs("atasan.role@contoh.id");

        mockMvc.perform(
                get("/api/auth/roles")
                        .session(supervisorSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", hasItem("TALENT")))
        .andExpect(jsonPath("$.roles", hasItem("ATASAN")))
        .andExpect(jsonPath("$.atasan").value(true))
        .andExpect(jsonPath("$.admin").value(false))
        .andExpect(jsonPath("$.teamSize").value(1));
    }

    @Test
    void getRolesShouldReturnAdminWhenFlagIsSet() throws Exception {
        insertUser("admin.role@contoh.id", "Admin Role", true);

        MockHttpSession adminSession = loginAs("admin.role@contoh.id");

        mockMvc.perform(
                get("/api/auth/roles")
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", hasItem("TALENT")))
        .andExpect(jsonPath("$.roles", hasItem("ADMIN")))
        .andExpect(jsonPath("$.admin").value(true))
        .andExpect(jsonPath("$.atasan").value(false));
    }

    @Test
    void talentCannotReadAnotherTalentDailyLog() throws Exception {
        mockMvc.perform(
                get("/api/daily-logs/users/2")
                        .param("date", DAILY_LOG_DATE)
                        .session(session)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void supervisorCanReadTeamDailyLog() throws Exception {
        insertUser("atasan.tim@contoh.id", "Atasan Tim", false);
        jdbcTemplate.update(
                "UPDATE users SET email_atasan = ? WHERE id = 2",
                "atasan.tim@contoh.id"
        );

        MockHttpSession supervisorSession = loginAs("atasan.tim@contoh.id");

        mockMvc.perform(
                get("/api/daily-logs/users/2")
                        .param("date", DAILY_LOG_DATE)
                        .session(supervisorSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4))
        .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void supervisorCannotReadDailyLogOutsideTeam() throws Exception {
        insertUser("atasan.lain@contoh.id", "Atasan Lain", false);

        MockHttpSession supervisorSession = loginAs("atasan.lain@contoh.id");

        mockMvc.perform(
                get("/api/daily-logs/users/2")
                        .param("date", DAILY_LOG_DATE)
                        .session(supervisorSession)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyTalentDailyLog() throws Exception {
        insertUser("admin.baca@contoh.id", "Admin Baca", true);

        MockHttpSession adminSession = loginAs("admin.baca@contoh.id");

        mockMvc.perform(
                get("/api/daily-logs/users/2")
                        .param("date", DAILY_LOG_DATE)
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void nonAdminCannotImportUsers() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "email,nama\nbaru.import@contoh.id,Baru Import\n"
                        .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                multipart("/api/admin/users/import")
                        .file(file)
                        .session(session)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void adminCanImportUsers() throws Exception {
        insertUser("admin.import@contoh.id", "Admin Import", true);

        MockHttpSession adminSession = loginAs("admin.import@contoh.id");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "email,nama\nbaru.import@contoh.id,Baru Import\n"
                        .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                multipart("/api/admin/users/import")
                        .file(file)
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1));
    }

    // ==========================================
    // TEAM (ATASAN/ADMIN) TESTING
    // ==========================================

    @Test
    void talentCannotAccessTeamList() throws Exception {
        mockMvc.perform(get("/api/team").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorSeesOnlyOwnTeam() throws Exception {
        insertUser("atasan.tim.api@contoh.id", "Atasan Tim API", false);
        jdbcTemplate.update(
                "UPDATE users SET email_atasan = ? WHERE id = 2",
                "atasan.tim.api@contoh.id"
        );

        MockHttpSession supervisorSession = loginAs("atasan.tim.api@contoh.id");

        mockMvc.perform(get("/api/team").session(supervisorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].status").value("BELUM_MULAI"))
                .andExpect(jsonPath("$[0].totalDurationMinutes").value(0))
                .andExpect(jsonPath("$[0].pendingDays").value(0));
    }

    @Test
    void teamListShowsTodayStatusWithUnionTotalAndPendingDays() throws Exception {
        insertUser("atasan.union@contoh.id", "Atasan Union", false);
        jdbcTemplate.update(
                "UPDATE users SET email_atasan = ? WHERE id = 2",
                "atasan.union@contoh.id"
        );

        long dailyLogId = insertTodayDailyLog(2L, "MENUNGGU");
        insertTask(dailyLogId, LocalTime.of(9, 0), 60, "Task pagi");
        insertTask(dailyLogId, LocalTime.of(9, 30), 60, "Task tumpang tindih");

        MockHttpSession supervisorSession = loginAs("atasan.union@contoh.id");

        mockMvc.perform(get("/api/team").session(supervisorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dailyLogId").value(dailyLogId))
                .andExpect(jsonPath("$[0].status").value("MENUNGGU"))
                .andExpect(jsonPath("$[0].totalDurationMinutes").value(90))
                .andExpect(jsonPath("$[0].pendingDays").value(1));
    }

    @Test
    void adminSeesEveryoneInTeamList() throws Exception {
        insertUser("admin.tim.api@contoh.id", "Admin Tim API", true);

        MockHttpSession adminSession = loginAs("admin.tim.api@contoh.id");

        mockMvc.perform(get("/api/team").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", hasItems(1, 2, 3)));
    }

    @Test
    void supervisorCanReadTeamDailyLogThroughTeamEndpoint() throws Exception {
        insertUser("atasan.baca.tim@contoh.id", "Atasan Baca Tim", false);
        jdbcTemplate.update(
                "UPDATE users SET email_atasan = ? WHERE id = 2",
                "atasan.baca.tim@contoh.id"
        );

        MockHttpSession supervisorSession = loginAs("atasan.baca.tim@contoh.id");

        mockMvc.perform(
                get("/api/team/2/daily-logs/{date}", DAILY_LOG_DATE)
                        .session(supervisorSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4))
        .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    void supervisorCannotReadDailyLogOutsideTeamThroughTeamEndpoint() throws Exception {
        insertUser("atasan.baca.lain@contoh.id", "Atasan Baca Lain", false);

        MockHttpSession supervisorSession = loginAs("atasan.baca.lain@contoh.id");

        mockMvc.perform(
                get("/api/team/2/daily-logs/{date}", DAILY_LOG_DATE)
                        .session(supervisorSession)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void talentCannotUseTeamDailyLogEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/team/2/daily-logs/{date}", DAILY_LOG_DATE)
                        .session(session)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyDailyLogThroughTeamEndpoint() throws Exception {
        insertUser("admin.baca.tim@contoh.id", "Admin Baca Tim", true);

        MockHttpSession adminSession = loginAs("admin.baca.tim@contoh.id");

        mockMvc.perform(
                get("/api/team/2/daily-logs/{date}", DAILY_LOG_DATE)
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
    }

    // ==========================================
    // ADMIN DAILY LOG LIST TESTING
    // ==========================================

    @Test
    void talentCannotListAllDailyLogs() throws Exception {
        mockMvc.perform(get("/api/admin/daily-logs").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListsAllTalentDailyLogs() throws Exception {
        long talentId = insertTalent("talent.daftar@contoh.id", "Talent Daftar");
        long dailyLogId = insertDailyLog(talentId, LocalDate.of(2026, 12, 1), "DRAF");

        MockHttpSession adminSession = loginAsAdmin(
                "admin.daftar@contoh.id",
                "Admin Daftar"
        );

        mockMvc.perform(get("/api/admin/daily-logs").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) dailyLogId)))
                .andExpect(jsonPath("$[*].userId", hasItem((int) talentId)));
    }

    @Test
    void adminCanFilterDailyLogsByDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 2);
        long firstTalent = insertTalent("talent.tanggal.a@contoh.id", "Talent Tanggal A");
        long secondTalent = insertTalent("talent.tanggal.b@contoh.id", "Talent Tanggal B");

        long firstLog = insertDailyLog(firstTalent, date, "DRAF");
        long secondLog = insertDailyLog(secondTalent, date, "MENUNGGU");
        insertDailyLog(firstTalent, date.plusDays(1), "DRAF");

        MockHttpSession adminSession = loginAsAdmin(
                "admin.filter.tanggal@contoh.id",
                "Admin Filter Tanggal"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("date", date.toString())
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].id", hasItems((int) firstLog, (int) secondLog)));
    }

    @Test
    void adminCanFilterDailyLogsByUser() throws Exception {
        long talentId = insertTalent("talent.pengguna@contoh.id", "Talent Pengguna");
        long firstLog = insertDailyLog(talentId, LocalDate.of(2026, 12, 4), "DRAF");
        insertDailyLog(talentId, LocalDate.of(2026, 12, 5), "MENUNGGU");
        insertTask(firstLog, LocalTime.of(9, 0), 60, "Task pagi");
        insertTask(firstLog, LocalTime.of(9, 30), 60, "Task tumpang tindih");

        MockHttpSession adminSession = loginAsAdmin(
                "admin.filter.pengguna@contoh.id",
                "Admin Filter Pengguna"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("userId", String.valueOf(talentId))
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].userId", everyItem(is((int) talentId))))
        .andExpect(jsonPath("$[*].fullName", everyItem(is("Talent Pengguna"))))
        .andExpect(jsonPath("$[*].email", everyItem(is("talent.pengguna@contoh.id"))))
        .andExpect(jsonPath(
                "$[?(@.id == %d)].taskCount".formatted(firstLog),
                hasItem(2)
        ))
        .andExpect(jsonPath(
                "$[?(@.id == %d)].totalDurationMinutes".formatted(firstLog),
                hasItem(90)
        ));
    }

    @Test
    void adminCanFilterDailyLogsByStatus() throws Exception {
        long talentId = insertTalent("talent.status@contoh.id", "Talent Status");
        long waitingLog = insertDailyLog(talentId, LocalDate.of(2026, 12, 6), "MENUNGGU");
        long draftLog = insertDailyLog(talentId, LocalDate.of(2026, 12, 7), "DRAF");

        MockHttpSession adminSession = loginAsAdmin(
                "admin.filter.status@contoh.id",
                "Admin Filter Status"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("status", "MENUNGGU")
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].status", everyItem(is("MENUNGGU"))))
        .andExpect(jsonPath("$[*].id", hasItem((int) waitingLog)))
        .andExpect(jsonPath("$[*].id", not(hasItem((int) draftLog))));
    }

    @Test
    void adminStatusFilterIsCaseInsensitive() throws Exception {
        long talentId = insertTalent("talent.lowercase@contoh.id", "Talent Lowercase");
        long dailyLogId = insertDailyLog(
                talentId,
                LocalDate.of(2026, 12, 8),
                "PERBAIKAN_DIAJUKAN"
        );

        MockHttpSession adminSession = loginAsAdmin(
                "admin.filter.lowercase@contoh.id",
                "Admin Filter Lowercase"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("userId", String.valueOf(talentId))
                        .param("status", "perbaikan_diajukan")
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value((int) dailyLogId))
        .andExpect(jsonPath("$[0].status").value("PERBAIKAN_DIAJUKAN"));
    }

    @Test
    void adminCanCombineDateUserAndStatusFilters() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 9);
        long talentId = insertTalent("talent.gabung@contoh.id", "Talent Gabung");
        long dailyLogId = insertDailyLog(talentId, date, "DRAF");
        insertTask(dailyLogId, LocalTime.of(9, 0), 60, "Task pagi");
        insertTask(dailyLogId, LocalTime.of(9, 30), 60, "Task tumpang tindih");
        insertDailyLog(talentId, date.plusDays(1), "MENUNGGU");

        MockHttpSession adminSession = loginAsAdmin(
                "admin.filter.gabung@contoh.id",
                "Admin Filter Gabung"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("date", date.toString())
                        .param("userId", String.valueOf(talentId))
                        .param("status", "draf")
                        .session(adminSession)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value((int) dailyLogId))
        .andExpect(jsonPath("$[0].userId").value((int) talentId))
        .andExpect(jsonPath("$[0].status").value("DRAF"))
        .andExpect(jsonPath("$[0].taskCount").value(2))
        .andExpect(jsonPath("$[0].totalDurationMinutes").value(90));
    }

    @Test
    void adminInvalidStatusFilterReturnsBadRequest() throws Exception {
        MockHttpSession adminSession = loginAsAdmin(
                "admin.status.salah@contoh.id",
                "Admin Status Salah"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("status", "SELESAI")
                        .session(adminSession)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void adminInvalidDateFilterReturnsBadRequest() throws Exception {
        MockHttpSession adminSession = loginAsAdmin(
                "admin.tanggal.salah@contoh.id",
                "Admin Tanggal Salah"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("date", "2026-13-40")
                        .session(adminSession)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void adminInvalidUserFilterReturnsBadRequest() throws Exception {
        MockHttpSession adminSession = loginAsAdmin(
                "admin.pengguna.salah@contoh.id",
                "Admin Pengguna Salah"
        );

        mockMvc.perform(
                get("/api/admin/daily-logs")
                        .param("userId", "bukan-angka")
                        .session(adminSession)
        )
        .andExpect(status().isBadRequest());
    }

    private MockHttpSession loginAsAdmin(String email, String fullName) throws Exception {
        insertUser(email, fullName, true);
        return loginAs(email);
    }

    private long insertTalent(String email, String fullName) {
        insertUser(email, fullName, false);

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );

        if (userId == null) {
            throw new AssertionError("Pengguna gagal dibuat.");
        }

        return userId;
    }

    private long insertDailyLog(long userId, LocalDate date, String status) {
        jdbcTemplate.update(
                "INSERT INTO daily_logs (user_id, log_date, work_schedule_id, status, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                userId,
                date,
                status
        );

        Long dailyLogId = jdbcTemplate.queryForObject(
                "SELECT id FROM daily_logs WHERE user_id = ? AND log_date = ?",
                Long.class,
                userId,
                date
        );

        if (dailyLogId == null) {
            throw new AssertionError("Daily log gagal dibuat.");
        }

        return dailyLogId;
    }

    private void insertUser(String email, String fullName, boolean admin) {
        jdbcTemplate.update(
                "INSERT INTO users (email, full_name, is_admin, is_active) "
                        + "VALUES (?, ?, ?, true)",
                email,
                fullName,
                admin
        );
    }

    private long insertTodayDailyLog(Long userId, String status) {
        LocalDate today = LocalDate.now();

        jdbcTemplate.update(
                "INSERT INTO daily_logs (user_id, log_date, work_schedule_id, status, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                userId,
                today,
                status
        );

        Long dailyLogId = jdbcTemplate.queryForObject(
                "SELECT id FROM daily_logs WHERE user_id = ? AND log_date = ?",
                Long.class,
                userId,
                today
        );

        if (dailyLogId == null) {
            throw new AssertionError("Daily log hari ini gagal dibuat.");
        }

        return dailyLogId;
    }

    private void insertTask(
            long dailyLogId,
            LocalTime startTime,
            int durationMinutes,
            String summary
    ) {
        jdbcTemplate.update(
                "INSERT INTO log_tasks (daily_log_id, project_id, task_type_id, work_status_id, "
                        + "start_time, duration_minutes, summary, created_at, updated_at) "
                        + "VALUES (?, 1, 1, 1, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                dailyLogId,
                startTime,
                durationMinutes,
                summary
        );
    }

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s"
                                }
                                """.formatted(email))
        )
        .andExpect(status().isOk())
        .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
