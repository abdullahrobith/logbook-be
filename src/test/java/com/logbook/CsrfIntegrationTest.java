package com.logbook;

import com.logbook.filter.RequestIdFilter;
import com.logbook.interceptor.CsrfInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifikasi SRS LBK-AUT-04 pada lapisan API. Test ini sengaja tidak
 * meng-import CsrfTestConfig agar header memang benar-benar tidak dikirim.
 */
@SpringBootTest(properties = "logbook.jadwal.aktif=false")
@AutoConfigureMockMvc
class CsrfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String LOGIN_BODY = """
            {
                "email": "rina.talent@contoh.id",
                "password": "Logbook@2026"
            }
            """;

    @Test
    void postWithoutHeaderIsRejectedWithStableCode() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_REQUIRED"))
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(header().exists(RequestIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    void putWithWrongHeaderValueIsRejected() throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header(CsrfInterceptor.HEADER_NAME, "fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_REQUIRED"));
    }

    @Test
    void postWithHeaderIsAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(CsrfInterceptor.HEADER_NAME, CsrfInterceptor.HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rina.talent@contoh.id"));
    }

    @Test
    void headerValueIsCaseInsensitive() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(CsrfInterceptor.HEADER_NAME, "xmlhttprequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void getWithoutHeaderIsAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void optionsPreflightWithoutHeaderIsAllowed() throws Exception {
        mockMvc.perform(options("/api/auth/login"))
                .andExpect(status().is2xxSuccessful());
    }
}
