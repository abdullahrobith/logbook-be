package com.logbook.controller;

import com.logbook.dto.AdminImportResponse;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @InjectMocks
    private AdminController adminController;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RoleService roleService;

    @Mock
    private HttpSession session;

    @Test
    void testNonAdminRejectedOnEndpoints() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya admin yang dapat mengakses fitur ini"))
                .when(roleService).requireAdmin(session);

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "email,nama\na@b.com,Nama".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.importUsers(file, session));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        assertThrows(ResponseStatusException.class, () -> adminController.getHolidays(session));
        assertThrows(ResponseStatusException.class, () -> adminController.getLeaves(session));
    }

    @Test
    void testImportUsersSuccessCreatesAndUpdates() {
        String csvContent = "email,nama\nnew@example.com,New User\nexisting@example.com,Existing User";
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csvContent.getBytes());

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), eq(Integer.class), eq("new@example.com")))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM users WHERE email = ?"), eq(Integer.class), eq("existing@example.com")))
                .thenReturn(1);

        ResponseEntity<AdminImportResponse> response = adminController.importUsers(file, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().created());
        assertEquals(1, response.getBody().updated());
        assertEquals(1, response.getBody().tempPasswords().size());
        assertEquals("new@example.com", response.getBody().tempPasswords().get(0).get("email"));

        verify(jdbcTemplate, times(1)).update(contains("INSERT INTO users"), eq("new@example.com"), eq("New User"), anyString());
        verify(jdbcTemplate, times(1)).update(contains("UPDATE users SET full_name"), eq("Existing User"), eq("existing@example.com"));
    }

    @Test
    void testImportUsersRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.importUsers(file, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("File CSV tidak boleh kosong", ex.getReason());
    }

    @Test
    void testImportUsersRejectsBlankEmailOrName() {
        String csvContent = "email,nama\n,John Doe";
        MockMultipartFile file = new MockMultipartFile("file", "invalid.csv", "text/csv", csvContent.getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.importUsers(file, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Email dan Nama wajib diisi"));
    }

    @Test
    void testImportUsersRejectsBrokenInputStream() throws IOException {
        MockMultipartFile brokenFile = mock(MockMultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getInputStream()).thenThrow(new IOException("Disk error"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.importUsers(brokenFile, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Format file CSV tidak valid", ex.getReason());
    }

    @Test
    void testGetHolidaysSuccess() {
        List<Map<String, Object>> mockHolidays = List.of(
                Map.of("holiday_date", "2026-01-01", "name", "Tahun Baru")
        );
        when(jdbcTemplate.queryForList(contains("SELECT * FROM holidays"))).thenReturn(mockHolidays);

        ResponseEntity<List<Map<String, Object>>> response = adminController.getHolidays(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Tahun Baru", response.getBody().get(0).get("name"));
    }

    @Test
    void testAddHolidaySuccess() {
        Map<String, String> request = Map.of("date", "2026-08-17", "name", "Hari Kemerdekaan");

        ResponseEntity<?> response = adminController.addHoliday(request, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jdbcTemplate).update(contains("INSERT INTO holidays"), eq(LocalDate.parse("2026-08-17")), eq("Hari Kemerdekaan"));
    }

    @Test
    void testAddHolidayRejectsMissingFields() {
        Map<String, String> request = new HashMap<>();
        request.put("date", "2026-08-17");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.addHoliday(request, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Tanggal dan Nama wajib diisi", ex.getReason());
    }

    @Test
    void testAddHolidayRejectsBlankName() {
        Map<String, String> request = Map.of("date", "2026-08-17", "name", "  ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> adminController.addHoliday(request, session)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testDeleteHolidaySuccess() {
        when(jdbcTemplate.update(contains("DELETE FROM holidays"), eq(LocalDate.parse("2026-08-17"))))
                .thenReturn(1);

        ResponseEntity<?> response = adminController.deleteHoliday("2026-08-17", session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteHolidayNotFound() {
        when(jdbcTemplate.update(contains("DELETE FROM holidays"), eq(LocalDate.parse("2026-08-17"))))
                .thenReturn(0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.deleteHoliday("2026-08-17", session));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Libur nasional tidak ditemukan", ex.getReason());
    }

    @Test
    void testGetLeavesSuccess() {
        List<Map<String, Object>> mockLeaves = List.of(
                Map.of("id", 1L, "user_id", 10L, "full_name", "Talent A", "note", "Cuti tahunan")
        );
        when(jdbcTemplate.queryForList(contains("SELECT l.id, l.user_id"))).thenReturn(mockLeaves);

        ResponseEntity<List<Map<String, Object>>> response = adminController.getLeaves(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Cuti tahunan", response.getBody().get(0).get("note"));
        verify(jdbcTemplate).queryForList(contains("l.note"));
    }

    @Test
    void testGetTalentOptionsReturnsActiveNonAdminUsers() {
        List<Map<String, Object>> users = List.of(
                Map.of("id", 10L, "full_name", "Talent A")
        );
        when(jdbcTemplate.queryForList(contains("is_active = TRUE AND is_admin = FALSE")))
                .thenReturn(users);

        ResponseEntity<List<Map<String, Object>>> response =
                adminController.getTalentOptions(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
        verify(roleService).requireAdmin(session);
    }

    @Test
    void testAddLeaveSuccess() {
        Map<String, String> request = Map.of(
                "userId", "10",
                "fromDate", "2026-11-20",
                "toDate", "2026-11-22"
        );

        ResponseEntity<?> response = adminController.addLeave(request, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jdbcTemplate).update(
                contains("INSERT INTO leaves"),
                eq(10L),
                eq(LocalDate.parse("2026-11-20")),
                eq(LocalDate.parse("2026-11-22")),
                isNull()
        );
    }

    @Test
    void testAddLeaveTrimsOptionalNote() {
        Map<String, String> request = Map.of(
                "userId", "10",
                "fromDate", "2026-11-20",
                "toDate", "2026-11-22",
                "note", " Cuti tahunan "
        );

        adminController.addLeave(request, session);

        verify(jdbcTemplate).update(
                contains("INSERT INTO leaves"),
                eq(10L),
                eq(LocalDate.parse("2026-11-20")),
                eq(LocalDate.parse("2026-11-22")),
                eq("Cuti tahunan")
        );
    }

    @Test
    void testAddLeaveRejectsMissingFields() {
        Map<String, String> request = Map.of("userId", "10");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.addLeave(request, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Data cuti tidak lengkap", ex.getReason());
    }

    @Test
    void testAddLeaveRejectsFromDateAfterToDate() {
        Map<String, String> request = Map.of(
                "userId", "10",
                "fromDate", "2026-11-25",
                "toDate", "2026-11-22"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.addLeave(request, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Tanggal mulai tidak boleh melebihi tanggal selesai", ex.getReason());
    }

    @Test
    void testDeleteLeaveSuccess() {
        when(jdbcTemplate.update(contains("DELETE FROM leaves"), eq(5L))).thenReturn(1);

        ResponseEntity<?> response = adminController.deleteLeave(5L, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteLeaveNotFound() {
        when(jdbcTemplate.update(contains("DELETE FROM leaves"), eq(5L))).thenReturn(0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.deleteLeave(5L, session));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Cuti tidak ditemukan", ex.getReason());
    }
}
