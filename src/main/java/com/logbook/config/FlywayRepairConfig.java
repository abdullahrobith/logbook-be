package com.logbook.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Menyelaraskan checksum Flyway otomatis sebelum migrasi di environment
 * development. Diperlukan karena migrasi yang sudah terpasang pernah diperbaiki
 * agar kompatibel dengan skema canonical.
 *
 * <p>Produksi memakai {@code logbook.flyway.repair-checksums=false} agar
 * perubahan migrasi yang tidak disengaja tetap terdeteksi oleh Flyway.</p>
 */
@Slf4j
@Configuration
public class FlywayRepairConfig {

    @Bean
    @ConditionalOnProperty(name = "logbook.flyway.repair-checksums", havingValue = "true")
    FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            log.warn("Flyway repair dijalankan: checksum migrasi diselaraskan dengan file aktif");
            flyway.repair();
            flyway.migrate();
        };
    }
}
