package com.logbook.controller;

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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}