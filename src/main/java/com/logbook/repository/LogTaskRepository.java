
package com.logbook.repository;

import com.logbook.entity.LogTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LogTaskRepository extends JpaRepository<LogTask, Long> {

    @Query("SELECT t FROM LogTask t WHERE t.id = :id AND t.dailyLog.userId = :userId")
    Optional<LogTask> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM task_types
                WHERE id = :taskTypeId
                AND is_active = true
            )
            """, nativeQuery = true)
    boolean existsActiveTaskType(@Param("taskTypeId") Short taskTypeId);
}
