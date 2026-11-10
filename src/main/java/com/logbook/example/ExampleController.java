package com.logbook.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/examples")
public class ExampleController {

    private final JdbcTemplate jdbcTemplate;

    public ExampleController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> getExamples() {
        
        log.info("Menerima request GET untuk data Contoh Log Book");

        String queryCards = "SELECT * FROM example_cards ORDER BY sort_order";
        String queryEntries = "SELECT * FROM example_entries ORDER BY sort_order";
        String queryTips = "SELECT title, body FROM writing_tips WHERE is_active = true ORDER BY sort_order";

        List<Map<String, Object>> cards = jdbcTemplate.queryForList(queryCards);
        List<Map<String, Object>> entries = jdbcTemplate.queryForList(queryEntries);
        List<Map<String, Object>> tips = jdbcTemplate.queryForList(queryTips);

        log.info("Berhasil mengambil data Contoh Log Book");

        return Map.of(
            "cards", cards,
            "entries", entries,
            "tips", tips
        );
    }
}