package com.logbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface MasterDataRepository extends JpaRepository<Object, Long> {

    @Query(value = "SELECT id, code, name FROM task_types WHERE is_active = true ORDER BY sort_order", nativeQuery = true)
    List<Map<String, Object>> getActiveTaskTypes();

    @Query(value = "SELECT id, code, name FROM work_statuses ORDER BY sort_order", nativeQuery = true)
    List<Map<String, Object>> getWorkStatuses();

    @Query(value = "SELECT minutes, label FROM duration_options WHERE is_active = true ORDER BY sort_order", nativeQuery = true)
    List<Map<String, Object>> getActiveDurations();
}