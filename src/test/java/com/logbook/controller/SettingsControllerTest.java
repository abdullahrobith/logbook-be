package com.logbook.controller;

import com.logbook.dto.SettingsRequest;
import com.logbook.dto.SettingsResponse;
import com.logbook.entity.User;
import com.logbook.service.RoleService;
import com.logbook.service.SettingsService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @InjectMocks
    private SettingsController settingsController;

    @Mock
    private SettingsService settingsService;

    @Mock
    private RoleService roleService;

    @Mock
    private HttpSession session;

    @Test
    void getSettingsReturnsSettingsForCurrentUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(roleService.requireCurrentUser(session)).thenReturn(user);

        SettingsResponse expected = new SettingsResponse(LocalTime.of(8, 0), LocalTime.of(17, 0), false);
        when(settingsService.getSettings(user)).thenReturn(expected);

        SettingsResponse response = settingsController.getSettings(session);

        assertEquals(expected, response);
        verify(roleService).requireCurrentUser(session);
        verify(settingsService).getSettings(user);
    }

    @Test
    void getSettingsRejectsUnauthenticatedUser() {
        when(roleService.requireCurrentUser(session))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesi berakhir atau belum masuk"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> settingsController.getSettings(session)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(settingsService);
    }

    @Test
    void updateSettingsDelegatesToService() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(roleService.requireCurrentUser(session)).thenReturn(user);

        SettingsRequest request = new SettingsRequest(LocalTime.of(9, 0), LocalTime.of(18, 0), true);
        SettingsResponse expected = new SettingsResponse(LocalTime.of(9, 0), LocalTime.of(18, 0), true);
        when(settingsService.updateSettings(user, request)).thenReturn(expected);

        SettingsResponse response = settingsController.updateSettings(request, session);

        assertEquals(expected, response);
        verify(settingsService).updateSettings(user, request);
    }

    @Test
    void updateSettingsRejectsUnauthenticatedUser() {
        when(roleService.requireCurrentUser(session))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesi berakhir atau belum masuk"));

        SettingsRequest request = new SettingsRequest(null, null, true);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> settingsController.updateSettings(request, session)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(settingsService);
    }
}
