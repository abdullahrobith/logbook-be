package com.logbook.controller;

import com.logbook.dto.DayDto;
import com.logbook.dto.TaskDto;
import com.logbook.entity.User;
import com.logbook.service.DailyLogService;
import com.logbook.service.RoleService;
import com.logbook.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
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

        when(roleService.requireSupervisorOrAdmin(any())).thenReturn(supervisor);
        when(teamService.getTeam(supervisor)).thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 2L);

        mockMvc.perform(get("/api/team").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void getTeamShouldReturnForbiddenForTalent() throws Exception {
        when(roleService.requireSupervisorOrAdmin(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya atasan atau admin"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 1L);

        mockMvc.perform(get("/api/team").session(session))
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
}