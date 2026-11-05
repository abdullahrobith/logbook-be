
package com.logbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogbookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession session;

    private static final String EMAIL = "rina.talent@contoh.id";
    private static final String DAILY_LOG_ID = "3";
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
        .andExpect(status().isBadRequest());
    }

    @Test
    void getDailyLogsWithoutDateShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                get("/api/daily-logs")
                        .session(session)
        )
        .andExpect(status().isBadRequest());
    }
}
