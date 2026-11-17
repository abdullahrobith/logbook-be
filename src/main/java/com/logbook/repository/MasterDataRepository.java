package com.logbook.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MasterDataRepository {

    private final JdbcTemplate jdbcTemplate;

    public MasterDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getActiveTaskTypes() {
        return jdbcTemplate.queryForList("SELECT id, code, name FROM task_types WHERE is_active = true ORDER BY sort_order");
    }

    public List<Map<String, Object>> getWorkStatuses() {
        return jdbcTemplate.queryForList("SELECT id, code, name FROM work_statuses ORDER BY sort_order");
    }

    public List<Map<String, Object>> getActiveDurations() {
        return jdbcTemplate.queryForList("SELECT minutes, label FROM duration_options WHERE is_active = true ORDER BY sort_order");
    }
}