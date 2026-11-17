
package com.logbook.repository;

import com.logbook.entity.LogTask;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LogTaskRepository extends JpaRepository<LogTask, Long> {

    @Query("SELECT t FROM LogTask t WHERE t.id = :id AND t.dailyLog.userId = :userId")
    Optional<LogTask> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Task berstatus tertentu dari hari terakhir yang berisi catatan milik
     * seorang pengguna sebelum {@code before}. Dipakai GET /api/continue
     * (LBK-TSK-07) untuk menawarkan pekerjaan Berjalan/Tertahan yang dapat
     * dilanjutkan.
     */
    @Query("""
            SELECT t
            FROM LogTask t
            WHERE t.dailyLog.userId = :userId
              AND t.workStatusId IN :workStatusIds
              AND t.dailyLog.logDate = (
                    SELECT MAX(filled.logDate)
                    FROM DailyLog filled
                    WHERE filled.userId = :userId
                      AND filled.logDate < :before
                      AND EXISTS (
                            SELECT 1
                            FROM LogTask anyTask
                            WHERE anyTask.dailyLog = filled
                      )
              )
            ORDER BY t.startMinute ASC, t.id ASC
            """)
    List<LogTask> findFromLatestDayWithContentBefore(
            @Param("userId") Long userId,
            @Param("before") LocalDate before,
            @Param("workStatusIds") Collection<Short> workStatusIds
    );

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM task_types
                WHERE id = :taskTypeId
                AND is_active = true
            )
            """, nativeQuery = true)
    boolean existsActiveTaskType(@Param("taskTypeId") Short taskTypeId);

    /**
     * Seluruh task milik satu pengguna dalam rentang tanggal, beserta Daily Log
     * induknya. Dipakai GET /api/summary untuk mengagregasi rekap tanpa
     * mengambil data pengguna lain.
     */
    @EntityGraph(attributePaths = "dailyLog")
    @Query("""
            SELECT t
            FROM LogTask t
            WHERE t.dailyLog.userId = :userId
              AND t.dailyLog.logDate BETWEEN :fromDate AND :toDate
            ORDER BY t.dailyLog.logDate ASC, t.startMinute ASC, t.id ASC
            """)
    List<LogTask> findForUserBetween(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
