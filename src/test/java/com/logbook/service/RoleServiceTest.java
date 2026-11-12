package com.logbook.service;

import com.logbook.dto.RoleResponse;
import com.logbook.entity.User;
import com.logbook.repository.UserRepository;
import com.logbook.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceTest {

    private UserRepository userRepository;
    private RoleService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new RoleService(userRepository);
    }

    @Test
    void everyUserIsTalent() {
        User user = user(1L, "rina@contoh.id", false);

        Set<Role> roles = service.rolesOf(user);

        assertEquals(Set.of(Role.TALENT), roles);
    }

    @Test
    void supervisorRoleComesFromSubordinateEmailAtasan() {
        User user = user(2L, "atasan@contoh.id", false);
        when(userRepository.existsByEmailAtasanIgnoreCaseAndActiveTrue("atasan@contoh.id"))
                .thenReturn(true);

        Set<Role> roles = service.rolesOf(user);

        assertTrue(roles.contains(Role.TALENT));
        assertTrue(roles.contains(Role.ATASAN));
        assertFalse(roles.contains(Role.ADMIN));
    }

    @Test
    void adminRoleComesFromFlag() {
        User user = user(3L, "admin@contoh.id", true);

        Set<Role> roles = service.rolesOf(user);

        assertTrue(roles.contains(Role.TALENT));
        assertTrue(roles.contains(Role.ADMIN));
        assertFalse(roles.contains(Role.ATASAN));
    }

    @Test
    void currentRolesIncludesTeamSizeForSupervisor() {
        User user = user(2L, "atasan@contoh.id", false);
        when(userRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAtasanIgnoreCaseAndActiveTrue("atasan@contoh.id"))
                .thenReturn(true);
        when(userRepository.countByEmailAtasanIgnoreCaseAndActiveTrue("atasan@contoh.id"))
                .thenReturn(3L);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, 2L);

        RoleResponse response = service.currentRoles(session);

        assertEquals(3L, response.teamSize());
        assertTrue(response.atasan());
        assertFalse(response.admin());
        assertEquals(2, response.roles().size());
    }

    @Test
    void talentCanViewOwnDataButNotOthers() {
        User talent = user(1L, "rina@contoh.id", false);
        User other = user(2L, "budi@contoh.id", false);

        assertTrue(service.canViewTalent(talent, talent));
        assertFalse(service.canViewTalent(talent, other));
    }

    @Test
    void supervisorCanViewAndDecideOwnTeamOnly() {
        User supervisor = user(2L, "atasan@contoh.id", false);
        User teamMember = talentOf(3L, "rina@contoh.id", "atasan@contoh.id");
        User outsider = talentOf(4L, "budi@contoh.id", "lain@contoh.id");

        assertTrue(service.canViewTalent(supervisor, teamMember));
        assertTrue(service.canDecideTalent(supervisor, teamMember));
        assertFalse(service.canViewTalent(supervisor, outsider));
        assertFalse(service.canDecideTalent(supervisor, outsider));
    }

    @Test
    void adminCanViewAndDecideEveryoneExceptOwnDailyLog() {
        User admin = user(1L, "admin@contoh.id", true);
        User talent = talentOf(2L, "rina@contoh.id", "lain@contoh.id");

        assertTrue(service.canViewTalent(admin, talent));
        assertTrue(service.canDecideTalent(admin, talent));
        assertFalse(service.canDecideTalent(admin, admin));
    }

    @Test
    void requireAdminRejectsNonAdmin() {
        User talent = user(1L, "rina@contoh.id", false);
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(talent));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, 1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireAdmin(session)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireCanViewTalentRejectsOtherTalent() {
        User actor = user(1L, "rina@contoh.id", false);
        User other = talentOf(2L, "budi@contoh.id", "lain@contoh.id");
        when(userRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(other));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireCanViewTalent(actor, 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireCanViewTalentReturnsNotFoundForUnknownUser() {
        User actor = user(1L, "rina@contoh.id", false);
        when(userRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireCanViewTalent(actor, 99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void requireSupervisorOrAdminRejectsRegularTalent() {
        User talent = user(1L, "rina@contoh.id", false);
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(talent));
        when(userRepository.existsByEmailAtasanIgnoreCaseAndActiveTrue("rina@contoh.id"))
                .thenReturn(false);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, 1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireSupervisorOrAdmin(session)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireCanDecideTalentRejectsOwnDailyLog() {
        User admin = user(1L, "admin@contoh.id", true);
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireCanDecideTalent(admin, 1L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireCanDecideTalentAllowsSupervisorForSubordinate() {
        User supervisor = user(2L, "atasan@contoh.id", false);
        User subordinate = talentOf(3L, "rina@contoh.id", "atasan@contoh.id");
        when(userRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(subordinate));

        User target = service.requireCanDecideTalent(supervisor, 3L);

        assertEquals(subordinate, target);
    }

    @Test
    void requireCanDecideTalentAllowsAdminForOtherTalent() {
        User admin = user(1L, "admin@contoh.id", true);
        User talent = talentOf(3L, "rina@contoh.id", "atasan@contoh.id");
        when(userRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(talent));

        User target = service.requireCanDecideTalent(admin, 3L);

        assertEquals(talent, target);
    }

    @Test
    void requireCanDecideTalentRejectsSupervisorForNonSubordinate() {
        User supervisor = user(2L, "atasan@contoh.id", false);
        User outsider = talentOf(4L, "budi@contoh.id", "atasan.lain@contoh.id");
        when(userRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(outsider));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireCanDecideTalent(supervisor, 4L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireCanDecideTalentReturnsNotFoundWhenTalentDoesNotExist() {
        User supervisor = user(2L, "atasan@contoh.id", false);
        when(userRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireCanDecideTalent(supervisor, 99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void requireAdminAndSupervisorOrAdminAllowAuthorizedUsers() {
        User admin = user(1L, "admin@contoh.id", true);
        User supervisor = user(2L, "atasan@contoh.id", false);
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(supervisor));
        when(userRepository.existsByEmailAtasanIgnoreCaseAndActiveTrue("atasan@contoh.id"))
                .thenReturn(true);

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute(AuthService.SESSION_USER_ID, 1L);

        MockHttpSession supervisorSession = new MockHttpSession();
        supervisorSession.setAttribute(AuthService.SESSION_USER_ID, 2L);

        assertEquals(admin, service.requireAdmin(adminSession));
        assertEquals(admin, service.requireSupervisorOrAdmin(adminSession));
        assertEquals(supervisor, service.requireSupervisorOrAdmin(supervisorSession));
    }

    private User user(Long id, String email, boolean admin) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.isAdmin()).thenReturn(admin);
        return user;
    }

    private User talentOf(Long id, String email, String emailAtasan) {
        User user = user(id, email, false);
        when(user.getEmailAtasan()).thenReturn(emailAtasan);
        return user;
    }
}
