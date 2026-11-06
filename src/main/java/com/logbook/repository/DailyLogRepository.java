package com.logbook.repository;

import com.logbook.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    Optional<DailyLog> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO daily_logs (
                user_id,
                log_date,
                work_schedule_id,
                status,
                created_at,
                updated_at
            )
            VALUES (
                :userId,
                :logDate,
                :workScheduleId,
                :status,
                :now,
                :now
            )
            ON CONFLICT (user_id, log_date) DO NOTHING
            """, nativeQuery = true)
    int insertDraftIfAbsent(
            @Param("userId") Long userId,
            @Param("logDate") LocalDate logDate,
            @Param("workScheduleId") Short workScheduleId,
            @Param("status") String status,
            @Param("now") OffsetDateTime now
    );
}