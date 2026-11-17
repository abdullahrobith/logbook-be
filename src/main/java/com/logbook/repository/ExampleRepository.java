package com.logbook.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ExampleRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getCards() {
        return jdbcTemplate.queryForList("SELECT * FROM example_cards ORDER BY sort_order");
    }

    public List<Map<String, Object>> getEntries() {
        return jdbcTemplate.queryForList("SELECT * FROM example_entries ORDER BY sort_order");
    }

    public List<Map<String, Object>> getTips() {
        return jdbcTemplate.queryForList("SELECT title, body FROM writing_tips WHERE is_active = true ORDER BY sort_order");
    }
}