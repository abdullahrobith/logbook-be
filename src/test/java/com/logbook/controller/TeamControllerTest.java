package com.logbook.controller;

import com.logbook.config.TimeConfig;
import com.logbook.dto.DayDto;
import com.logbook.dto.PendingDayResponse;
import com.logbook.dto.TaskDto;
import com.logbook.dto.TeamScope;
import com.logbook.entity.User;
import com.logbook.service.DailyLogService;
import com.logbook.service.RoleService;
import com.logbook.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import(TimeConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private DailyLogService dailyLogService;

    @Test
    void getTeamShouldReturnOkForSupervisor() throws Exception {
        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setEmail("atasan@contoh.id");

        when(roleService.requireSupervisor(any())).thenReturn(supervisor);
        when(teamService.getTeam(supervisor, TeamScope.ATASAN)).thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 2L);

        mockMvc.perform(get("/api/team").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void getTeamWithAdminScopeShouldUseAdminRequirement() throws Exception {
        User admin = new User();
        admin.setId(9L);
        admin.setEmail("admin@contoh.id");

        when(roleService.requireAdmin(any())).thenReturn(admin);
        when(teamService.getTeam(admin, TeamScope.ADMIN)).thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 9L);

        mockMvc.perform(get("/api/team").param("scope", "ADMIN").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void getTeamShouldReturnForbiddenForTalent() throws Exception {
        when(roleService.requireSupervisor(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya atasan"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 1L);

        mockMvc.perform(get("/api/team").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTeamAdminScopeShouldReturnForbiddenForSupervisorOnly() throws Exception {
        when(roleService.requireAdmin(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya admin"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 2L);

        mockMvc.perform(get("/api/team").param("scope", "ADMIN").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberDayReadPathsShouldReturnDayDto() throws Exception {
        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setEmail("atasan@contoh.id");

        User member = new User();
        member.setId(3L);

        LocalDate date = LocalDate.of(2026, 10, 29);
        DayDto day = new DayDto(
                date,
                "DRAF",
                "Draf",
                true,
                OffsetDateTime.parse("2026-10-29T23:59:00+07:00"),
                null,
                new DayDto.RangeDto(480, 1020),
                180,
                List.of(new DayDto.PerProjectDto("Proyek A", "#123456", 180)),
                List.of(new TaskDto(
                        17L, "Proyek A", "#123456", "TEST-1", 600, (short) 180,
                        "Pengembangan", "Detail task", "Selesai", null
                )),
                null
        );

        when(roleService.requireSupervisorOrAdmin(any())).thenReturn(supervisor);
        when(roleService.requireCanViewTalent(supervisor, 3L)).thenReturn(member);
        when(dailyLogService.getDay(3L, date)).thenReturn(day);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 2L);

        for (String path : List.of(
                "/api/team/3/days/2026-10-29",
                "/api/team/3/daily-logs/2026-10-29"
        )) {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value("2026-10-29"))
                    .andExpect(jsonPath("$.status").value("DRAF"))
                    .andExpect(jsonPath("$.totalMinutes").value(180))
                    .andExpect(jsonPath("$.tasks.length()").value(1))
                    .andExpect(jsonPath("$.tasks[0].project").value("Proyek A"));
        }
    }

    @Test
    void dayDtoShouldExposeIdentityAndDecisionMetadata() throws Exception {
        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setEmail("atasan@contoh.id");

        User member = new User();
        member.setId(3L);

        LocalDate date = LocalDate.of(2026, 10, 29);
        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-10-29T16:30:00+07:00");
        OffsetDateTime decidedAt = OffsetDateTime.parse("2026-10-30T08:00:00+07:00");
        DayDto day = new DayDto(
                date,
                "DITOLAK",
                "Ditolak",
                false,
                OffsetDateTime.parse("2026-10-29T23:59:00+07:00"),
                null,
                new DayDto.RangeDto(480, 1020),
                180,
                List.of(new DayDto.PerProjectDto("Proyek A", "#123456", 180)),
                List.of(),
                "alasan penolakan",
                77L,
                3L,
                "Rina",
                "MAGANG",
                submittedAt,
                true,
                "alasan revisi",
                decidedAt,
                9L,
                "Admin Utama"
        );

        when(roleService.requireSupervisorOrAdmin(any())).thenReturn(supervisor);
        when(roleService.requireCanViewTalent(supervisor, 3L)).thenReturn(member);
        when(dailyLogService.getDay(3L, date)).thenReturn(day);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 2L);

        mockMvc.perform(get("/api/team/3/days/2026-10-29").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLogId").value(77))
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.fullName").value("Rina"))
                .andExpect(jsonPath("$.jenis").value("MAGANG"))
                .andExpect(jsonPath("$.submittedAt").value("2026-10-29T16:30:00+07:00"))
                .andExpect(jsonPath("$.autoSubmitted").value(true))
                .andExpect(jsonPath("$.revisionReason").value("alasan revisi"))
                .andExpect(jsonPath("$.decidedAt").value("2026-10-30T08:00:00+07:00"))
                .andExpect(jsonPath("$.decidedByUserId").value(9))
                .andExpect(jsonPath("$.decidedByName").value("Admin Utama"));
    }

    @Test
    void pendingDaysShouldReturnHariMenungguWithOrWithoutUserId() throws Exception {
        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setEmail("atasan@contoh.id");

        User member = new User();
        member.setId(3L);

        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-11-24T17:00:00+07:00");
        when(roleService.requireSupervisorOrAdmin(any())).thenReturn(supervisor);
        when(teamService.getPendingDays(eq(supervisor), any())).thenReturn(List.of(
                new PendingDayResponse(
                        3L, 77L, LocalDate.of(2026, 11, 24), 90, submittedAt, true
                )
        ));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 2L);

        mockMvc.perform(get("/api/team/pending-days").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(3))
                .andExpect(jsonPath("$[0].dailyLogId").value(77))
                .andExpect(jsonPath("$[0].date").value("2026-11-24"))
                .andExpect(jsonPath("$[0].totalDurationMinutes").value(90))
                .andExpect(jsonPath("$[0].submittedAt").value("2026-11-24T17:00:00+07:00"))
                .andExpect(jsonPath("$[0].autoSubmitted").value(true));

        when(roleService.requireCanViewTalent(supervisor, 3L)).thenReturn(member);
        when(teamService.getPendingDays(supervisor, member)).thenReturn(List.of());

        mockMvc.perform(get("/api/team/pending-days").param("userId", "3").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void pendingDaysShouldRejectTalent() throws Exception {
        when(roleService.requireSupervisorOrAdmin(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya atasan/admin"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 1L);

        mockMvc.perform(get("/api/team/pending-days").session(session))
                .andExpect(status().isForbidden());
    }
}