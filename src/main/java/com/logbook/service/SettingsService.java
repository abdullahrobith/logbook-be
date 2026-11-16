package com.logbook.service;

import com.logbook.dto.SettingsRequest;
import com.logbook.dto.SettingsResponse;
import com.logbook.entity.User;
import com.logbook.entity.UserSettings;
import com.logbook.repository.UserSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class SettingsService {

    private static final LocalTime DEFAULT_WORK_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_WORK_END = LocalTime.of(17, 0);

    private final UserSettingsRepository userSettingsRepository;

    public SettingsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    @Transactional
    public SettingsResponse getSettings(User user) {
        UserSettings settings = getOrCreate(user);
        return SettingsResponse.from(settings);
    }

    @Transactional
    public SettingsResponse updateSettings(User user, SettingsRequest request) {
        UserSettings settings = getOrCreate(user);

        if (request != null) {
            if (request.workStart() != null) {
                settings.setWorkStart(request.workStart());
            }
            if (request.workEnd() != null) {
                settings.setWorkEnd(request.workEnd());
            }
            if (request.onboarded() != null) {
                if (request.onboarded()) {
                    if (settings.getOnboardedAt() == null) {
                        settings.setOnboardedAt(OffsetDateTime.now());
                    }
                } else {
                    settings.setOnboardedAt(null);
                }
            }
        }

        UserSettings saved = userSettingsRepository.save(settings);
        log.info("Pengaturan diperbarui untuk User ID {}: workStart={}, workEnd={}, onboarded={}",
                user.getId(), saved.getWorkStart(), saved.getWorkEnd(), saved.isOnboarded());
        return SettingsResponse.from(saved);
    }

    private UserSettings getOrCreate(User user) {
        return userSettingsRepository.findById(user.getId())
                .orElseGet(() -> userSettingsRepository.save(
                        new UserSettings(user, DEFAULT_WORK_START, DEFAULT_WORK_END)
                ));
    }
}
