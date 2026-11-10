package com.logbook.repository;

import com.logbook.entity.DailyLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    Optional<DailyLog> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "tasks")
    List<DailyLog> findByUserIdInAndLogDate(Collection<Long> userIds, LocalDate logDate);

    @EntityGraph(attributePaths = "tasks")
    @Query("""
            select distinct dl
            from DailyLog dl
            where dl.logDate = coalesce(:logDate, dl.logDate)
              and dl.userId = coalesce(:userId, dl.userId)
              and upper(dl.status) = coalesce(
                    upper(cast(:status as string)),
                    upper(dl.status)
                  )
            order by dl.logDate desc, dl.id desc
            """)
    List<DailyLog> findForAdmin(
            @Param("logDate") LocalDate logDate,
            @Param("userId") Long userId,
            @Param("status") String status
    );

    @Query("""
            select dl.userId as userId, count(dl) as total
            from DailyLog dl
            where dl.userId in :userIds
              and dl.status = :status
            group by dl.userId
            """)
    List<PendingDayCountProjection> countByUserIdInAndStatus(
            @Param("userIds") Collection<Long> userIds,
            @Param("status") String status
    );

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
