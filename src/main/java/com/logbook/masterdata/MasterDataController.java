package com.logbook.masterdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tasks/master-data")
@Tag(name = "Master Data API", description = "Endpoint untuk data referensi (Jenis Task, Durasi)")
public class MasterDataController {

    private final JdbcTemplate jdbcTemplate;

    public MasterDataController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> getMasterData() {
        
        log.info("Menerima request GET untuk mengambil Master Data (Task Types, Statuses, Durations)");

        List<Map<String, Object>> types = jdbcTemplate.queryForList(
                "SELECT id, name FROM task_types WHERE is_active = true ORDER BY sort_order"
        );
        List<Map<String, Object>> statuses = jdbcTemplate.queryForList(
                "SELECT id, name FROM work_statuses ORDER BY sort_order"
        );
        List<Map<String, Object>> durations = jdbcTemplate.queryForList(
                "SELECT minutes, label FROM duration_options WHERE is_active = true ORDER BY sort_order"
        );

        log.info("Berhasil mengambil data Master Data");

        return Map.of(
            "types", types,
            "statuses", statuses,
            "durations", durations
        );
    }
}