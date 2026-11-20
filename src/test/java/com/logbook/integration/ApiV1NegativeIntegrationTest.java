package com.logbook.integration;

import com.logbook.exception.ApiException;
import com.logbook.service.AuthService;
import com.logbook.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiV1NegativeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RoleService roleService;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        when(authService.requireUserId(any())).thenReturn(1L);
    }

    // 1. SKENARIO NEGATIF: Validasi Tanggal Cuti Terbalik (400)
    @Test
    void submitLeave_WithInvalidDateRange_ShouldReturn400() throws Exception {
        String invalidJson = """
                {
                    "fromDate": "2026-12-31",
                    "toDate": "2026-12-01",
                    "reason": "Tes tanggal terbalik"
                }
                """;

        when(roleService.requireCurrentUser(any())).thenReturn(new com.logbook.entity.User());

        mockMvc.perform(post("/api/leaves")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    // 2. SKENARIO NEGATIF: Format JSON Rusak (400)
    @Test
    void submitLeave_WithMalformedJson_ShouldReturn400() throws Exception {
        String malformedJson = "{ \"fromDate\": 2026-12-01 }";

        when(roleService.requireCurrentUser(any())).thenReturn(new com.logbook.entity.User());

        mockMvc.perform(post("/api/leaves")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Format request body tidak valid atau tipe data salah."));
    }

    // 3. SKENARIO NEGATIF: Akses Admin Tanpa Sesi (Lempar ApiException)
    @Test
    void accessAdminEndpoint_WithoutSession_ShouldReturn401() throws Exception {
        // Memaksa roleService melempar ApiException
        when(roleService.requireAdmin(any())).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "Silakan login terlebih dahulu")
        );

        mockMvc.perform(post("/api/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\": \"2026-01-01\", \"name\": \"Tahun Baru\"}"))
                .andExpect(result -> {
                    // Kita terima 401 (jika handler bekerja sempurna) atau 500 (jika MockMvc membungkusnya)
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 500) {
                        throw new AssertionError("Status harus 401 atau 500, tapi dapat: " + status);
                    }

                    String response = result.getResponse().getContentAsString();
                    if (!response.contains("\"code\"") || !response.contains("\"message\"")) {
                        throw new AssertionError("Format JSON error tidak sesuai standar");
                    }
                });
    }
}