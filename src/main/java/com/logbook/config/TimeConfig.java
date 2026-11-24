package com.logbook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Sumber tunggal waktu aplikasi. Seluruh perhitungan tanggal memakai
 * {@link Clock} dan {@link ZoneId} dari sini, sehingga zona waktu konsisten
 * (default: Asia/Jakarta) dan dapat dipatok dalam pengujian.
 */
@Configuration
public class TimeConfig {

    @Bean
    public ZoneId applicationZoneId(
            @Value("${logbook.zona-waktu:Asia/Jakarta}") String zoneId
    ) {
        return ZoneId.of(zoneId);
    }

    @Bean
    public Clock applicationClock(ZoneId applicationZoneId) {
        return Clock.system(applicationZoneId);
    }
}
