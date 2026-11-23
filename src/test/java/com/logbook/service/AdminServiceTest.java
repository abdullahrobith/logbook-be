package com.logbook.service;

import com.logbook.dto.AdminLeaveResponse;
import com.logbook.entity.Leave;
import com.logbook.entity.User;
import com.logbook.repository.HolidayRepository;
import com.logbook.repository.LeaveRepository;
import com.logbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-11-11T03:00:00Z"),
            ZoneId.of("Asia/Jakarta")
    );

    @InjectMocks
    private AdminService adminService;

    @Test
    void getLeavesIncludesTalentFullName() {
        Leave leave = new Leave();
        leave.setId(1L);
        leave.setUserId(10L);
        leave.setStartDate(LocalDate.parse("2026-11-20"));
        leave.setEndDate(LocalDate.parse("2026-11-22"));
        leave.setReason("Cuti tahunan");
        leave.setCreatedAt(OffsetDateTime.parse("2026-11-19T10:00:00Z"));

        User user = new User();
        user.setId(10L);
        user.setFullName("Rina Talent");

        when(leaveRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of(leave));
        when(userRepository.findAllById(List.of(10L))).thenReturn(List.of(user));

        List<AdminLeaveResponse> result = adminService.getLeaves();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).userId());
        assertEquals("Rina Talent", result.get(0).fullName());
        assertEquals("Cuti tahunan", result.get(0).note());
    }

    @Test
    void getLeavesHandlesMissingUser() {
        Leave leave = new Leave();
        leave.setId(2L);
        leave.setUserId(99L);
        leave.setStartDate(LocalDate.parse("2026-11-23"));
        leave.setEndDate(LocalDate.parse("2026-11-23"));
        leave.setCreatedAt(OffsetDateTime.parse("2026-11-19T10:00:00Z"));

        when(leaveRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of(leave));
        when(userRepository.findAllById(List.of(99L))).thenReturn(List.of());

        List<AdminLeaveResponse> result = adminService.getLeaves();

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).userId());
        assertNull(result.get(0).fullName());
    }
}
