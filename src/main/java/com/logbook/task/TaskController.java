package com.logbook.task;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final JdbcTemplate jdbcTemplate;

    public TaskController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // API PUT: Ubah Task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskRequestDto request) {
        String sql = "UPDATE log_tasks SET project_id = ?, task_type_id = ?, work_status_id = ?, ticket = ?, start_time = CAST(? AS time), duration_minutes = ?, summary = ?, obstacle = ? WHERE id = ?";
        
        int rowsAffected = jdbcTemplate.update(sql, 
            request.project_id(), request.task_type_id(), request.work_status_id(),
            request.ticket(), request.start_time(), request.duration_minutes(),
            request.summary(), request.obstacle(), id);

        if (rowsAffected > 0) {
            // Di kontrak aslinya balikin DayDto, tapi buat simplifikasi kita balikin message sukses
            return ResponseEntity.ok(Map.of("message", "Task berhasil diubah"));
        } else {
            return ResponseEntity.status(404).body(Map.of("message", "Task tidak ditemukan"));
        }
    }

    // API DELETE: Hapus Task
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        // Cek dulu apakah task ini ada
        String checkSql = "SELECT COUNT(*) FROM log_tasks WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

        if (count == null || count == 0) {
            return ResponseEntity.status(404).body(Map.of("message", "Task tidak ditemukan"));
        }

        // Hapus task
        String deleteSql = "DELETE FROM log_tasks WHERE id = ?";
        jdbcTemplate.update(deleteSql, id);
        return ResponseEntity.ok(Map.of("message", "Task berhasil dihapus"));
    }
}