package com.logbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logbook.entity.PasswordResetToken;
import com.logbook.repository.PasswordResetTokenRepository;
import com.logbook.support.CsrfTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logbook.jadwal.aktif=false")
@AutoConfigureMockMvc
@Import(CsrfTestConfig.class)
@Transactional
class PasswordResetIntegrationTest {

    private static final String EMAIL = "rina.talent@contoh.id";
    private static final String OLD_PASSWORD = "Logbook@2026";
    private static final String NEW_PASSWORD = "Reset@2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void forgotPasswordStoresOnlyHashAndReturnsDevToken() throws Exception {
        MvcResult result = forgotPassword(EMAIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();
        String token = devToken(result);
        assertFalse(token.isBlank());

        List<PasswordResetToken> tokens = tokenRepository.findByUserIdAndUsedAtIsNull(1L);
        assertEquals(1, tokens.size());
        assertEquals(64, tokens.get(0).getTokenHash().length());
        assertFalse(tokens.get(0).getTokenHash().contains(token));
        assertFalse(tokens.get(0).getTokenHash().equals(token));
    }

    @Test
    void forgotPasswordForUnknownEmailReturnsGenericMessageWithoutToken() throws Exception {
        MvcResult result = forgotPassword("tidak.ada@contoh.id")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        assertTrue(objectMapper.readTree(result.getResponse().getContentAsString()).path("devToken").isMissingNode());
    }

    @Test
    void resetPasswordChangesPasswordAndTokenCannotBeReused() throws Exception {
        String token = devToken(forgotPassword(EMAIL).andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(token, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, NEW_PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, OLD_PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(token, NEW_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordIsRateLimitedAfterFiveRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            forgotPassword(EMAIL).andExpect(status().isOk());
        }

        forgotPassword(EMAIL).andExpect(status().isTooManyRequests());
    }

    @Test
    void resetPasswordIsRateLimitedAfterFiveInvalidAttempts() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token":"token-salah-%d","newPassword":"%s"}
                                    """.formatted(i, NEW_PASSWORD)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token-salah-6","newPassword":"%s"}
                                """.formatted(NEW_PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }

    private ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)));
    }

    private String devToken(MvcResult result) throws Exception {
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("devToken")
                .asText();
        assertNotNull(token);
        return token;
    }
}
