package com.logbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mengaktifkan tugas berkala, termasuk kirim otomatis dan penandaan TERLEWAT
 * pada {@link com.logbook.service.DailyLogAutomationService}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
