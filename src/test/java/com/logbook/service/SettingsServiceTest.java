package com.logbook.service;

import com.logbook.dto.SettingsRequest;
import com.logbook.dto.SettingsResponse;
import com.logbook.entity.User;
import com.logbook.entity.UserSettings;
import com.logbook.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private DeadlineService deadlineService;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-11-11T03:00:00Z"),
            ZoneId.of("Asia/Jakarta")
    );

    @InjectMocks
    private SettingsService settingsService;

    private static final LocalDate ACTIVE_DATE = LocalDate.of(2026, 11, 11);

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(deadlineService.activeDay(1L)).thenReturn(ACTIVE_DATE);
    }

    @Test
    void getSettingsReturnsExistingSettings() {
        UserSettings existing = new UserSettings(user, LocalTime.of(9, 0), LocalTime.of(18, 0));
        existing.setOnboardedAt(OffsetDateTime.now(clock));
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));

        SettingsResponse response = settingsService.getSettings(user);

        assertEquals(LocalTime.of(9, 0), response.workStart());
        assertEquals(LocalTime.of(18, 0), response.workEnd());
        assertTrue(response.onboarded());
        assertEquals(ACTIVE_DATE, response.activeDate());
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void getSettingsCreatesDefaultWhenMissing() {
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsResponse response = settingsService.getSettings(user);

        assertEquals(LocalTime.of(8, 0), response.workStart());
        assertEquals(LocalTime.of(17, 0), response.workEnd());
        assertFalse(response.onboarded());

        ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(LocalTime.of(8, 0), captor.getValue().getWorkStart());
        assertEquals(LocalTime.of(17, 0), captor.getValue().getWorkEnd());
    }

    @Test
    void updateSettingsUpdatesWorkHours() {
        UserSettings existing = new UserSettings(user, LocalTime.of(8, 0), LocalTime.of(17, 0));
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsRequest request = new SettingsRequest(LocalTime.of(7, 30), LocalTime.of(16, 30), null);
        SettingsResponse response = settingsService.updateSettings(user, request);

        assertEquals(LocalTime.of(7, 30), response.workStart());
        assertEquals(LocalTime.of(16, 30), response.workEnd());
        assertFalse(response.onboarded());
        assertEquals(ACTIVE_DATE, response.activeDate());
    }

    @Test
    void updateSettingsSetsOnboardedTrue() {
        UserSettings existing = new UserSettings(user, LocalTime.of(8, 0), LocalTime.of(17, 0));
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsRequest request = new SettingsRequest(null, null, true);
        SettingsResponse response = settingsService.updateSettings(user, request);

        assertTrue(response.onboarded());
        assertNotNull(existing.getOnboardedAt());
    }

    @Test
    void updateSettingsKeepsExistingOnboardedTimestamp() {
        UserSettings existing = new UserSettings(user, LocalTime.of(8, 0), LocalTime.of(17, 0));
        OffsetDateTime original = OffsetDateTime.now(clock).minusDays(1);
        existing.setOnboardedAt(original);
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsRequest request = new SettingsRequest(null, null, true);
        SettingsResponse response = settingsService.updateSettings(user, request);

        assertTrue(response.onboarded());
        assertEquals(original, existing.getOnboardedAt());
    }

    @Test
    void updateSettingsClearsOnboardedWhenFalse() {
        UserSettings existing = new UserSettings(user, LocalTime.of(8, 0), LocalTime.of(17, 0));
        existing.setOnboardedAt(OffsetDateTime.now(clock));
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsRequest request = new SettingsRequest(null, null, false);
        SettingsResponse response = settingsService.updateSettings(user, request);

        assertFalse(response.onboarded());
        assertNull(existing.getOnboardedAt());
    }

    @Test
    void updateSettingsIgnoresNullRequest() {
        UserSettings existing = new UserSettings(user, LocalTime.of(8, 0), LocalTime.of(17, 0));
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsResponse response = settingsService.updateSettings(user, null);

        assertEquals(LocalTime.of(8, 0), response.workStart());
        assertEquals(LocalTime.of(17, 0), response.workEnd());
        assertFalse(response.onboarded());
    }
}
