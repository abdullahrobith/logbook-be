package com.logbook.masterdata;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks/master-data")
public class MasterDataController {

    private final JdbcTemplate jdbcTemplate;

    public MasterDataController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> getMasterData() {
        List<Map<String, Object>> types = jdbcTemplate.queryForList(
                "SELECT id, name FROM task_types WHERE is_active = true ORDER BY sort_order"
        );
        List<Map<String, Object>> statuses = jdbcTemplate.queryForList(
                "SELECT id, name FROM work_statuses ORDER BY sort_order"
        );
        List<Map<String, Object>> durations = jdbcTemplate.queryForList(
                "SELECT minutes, label FROM duration_options WHERE is_active = true ORDER BY sort_order"
        );


        return Map.of(
            "types", types,
            "statuses", statuses,
            "durations", durations
        );
    }
}