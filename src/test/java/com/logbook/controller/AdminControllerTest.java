package com.logbook.controller;

import com.logbook.dto.AdminImportResponse;
import com.logbook.exception.ApiException;
import com.logbook.entity.Holiday;
import com.logbook.entity.Leave;
import com.logbook.entity.User;
import com.logbook.service.AdminService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    private AdminService adminService;

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
    void testImportUsersSuccess() {
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "email,nama\nnew@example.com,New User".getBytes());
        AdminImportResponse mockResponse = new AdminImportResponse(1, 0, List.of(Map.of("email", "new@example.com", "password", "12345678")));

        when(session.getAttribute("email")).thenReturn("admin@contoh.id");
        when(adminService.importUsers(any(), anyString())).thenReturn(mockResponse);

        ResponseEntity<AdminImportResponse> response = adminController.importUsers(file, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().created());
        assertEquals(0, response.getBody().updated());
        assertEquals(1, response.getBody().tempPasswords().size());
        assertEquals("new@example.com", response.getBody().tempPasswords().get(0).get("email"));
    }

    @Test
    void testImportUsersRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        
        when(adminService.importUsers(any(), any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "File CSV tidak boleh kosong"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminController.importUsers(file, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("File CSV tidak boleh kosong", ex.getReason());
    }

    @Test
    void testGetHolidaysSuccess() {
        List<Holiday> mockHolidays = List.of(new Holiday());
        when(adminService.getHolidays()).thenReturn(mockHolidays);

        ResponseEntity<List<Holiday>> response = adminController.getHolidays(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testAddHolidaySuccess() {
        Map<String, String> request = Map.of("date", "2026-08-17", "name", "Hari Kemerdekaan");

        ResponseEntity<?> response = adminController.addHoliday(request, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).addHoliday(eq(LocalDate.parse("2026-08-17")), eq("Hari Kemerdekaan"));
    }

    @Test
    void testAddHolidayRejectsMissingFields() {
        Map<String, String> request = new HashMap<>();
        request.put("date", "2026-08-17");

        ApiException ex = assertThrows(ApiException.class,
                () -> adminController.addHoliday(request, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Tanggal dan Nama wajib diisi", ex.getMessage());
    }

    @Test
    void testDeleteHolidaySuccess() {
        ResponseEntity<?> response = adminController.deleteHoliday("2026-08-17", session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).deleteHoliday(eq(LocalDate.parse("2026-08-17")));
    }

    @Test
    void testGetLeavesSuccess() {
        List<Leave> mockLeaves = List.of(new Leave());
        when(adminService.getLeaves()).thenReturn(mockLeaves);

        ResponseEntity<List<Leave>> response = adminController.getLeaves(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetTalentOptionsReturnsActiveNonAdminUsers() {
        List<User> users = List.of(new User());
        when(adminService.getTalentOptions()).thenReturn(users);

        ResponseEntity<List<User>> response = adminController.getTalentOptions(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
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
        verify(adminService).addLeave(eq(10L), eq(LocalDate.parse("2026-11-20")), eq(LocalDate.parse("2026-11-22")), isNull());
    }

    @Test
    void testAddLeaveRejectsMissingFields() {
        Map<String, String> request = Map.of("userId", "10");

        ApiException ex = assertThrows(ApiException.class,
                () -> adminController.addLeave(request, session));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Data cuti tidak lengkap", ex.getMessage());
    }

    @Test
    void testDeleteLeaveSuccess() {
        ResponseEntity<?> response = adminController.deleteLeave(5L, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).deleteLeave(5L);
    }
}