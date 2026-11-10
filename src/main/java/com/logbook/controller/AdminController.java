package com.logbook.controller;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logbook.dto.AdminImportResponse;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin API", description = "Endpoint khusus HR/Admin (Impor CSV, Libur, Cuti)")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;
    private final RoleService roleService;

    public AdminController(JdbcTemplate jdbcTemplate, RoleService roleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.roleService = roleService;
    }

    // ====================
    // 1. API IMPORT CSV 
    // ====================
    @PostMapping("/users/import")
    public ResponseEntity<AdminImportResponse> importUsers(
            @RequestParam("file") MultipartFile file,
            HttpSession session
    ) {
        roleService.requireAdmin(session);

        log.info("Menerima request import CSV dari Admin");

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File CSV tidak boleh kosong");
        }

        List<CSVRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader() 
                    .setSkipHeaderRecord(true)
                    .build();
            CSVParser parser = new CSVParser(reader, format);
            records = parser.getRecords();
        } catch (Exception e) {
            log.warn("Gagal membaca file CSV: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format file CSV tidak valid");
        }

        int lineNumber = 2;
        for (CSVRecord record : records) {
            String email = record.get("email");
            String nama = record.get("nama");

            if (email == null || email.isBlank() || nama == null || nama.isBlank()) {
                log.warn("Validasi gagal di baris {}: Email atau Nama kosong", lineNumber);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Email dan Nama wajib diisi.");
            }
            lineNumber++;
        }

        int created = 0;
        int updated = 0;
        List<Map<String, String>> tempPasswords = new ArrayList<>();

        for (CSVRecord record : records) {
            String email = record.get("email");
            String nama = record.get("nama");

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);

            if (count != null && count > 0) {
                jdbcTemplate.update("UPDATE users SET full_name = ? WHERE email = ?", nama, email);
                updated++;
            } else {
                String tempPassword = UUID.randomUUID().toString().substring(0, 8);
                String passwordHash = "BCryptHashPlaceholder_" + tempPassword;

                jdbcTemplate.update(
                    "INSERT INTO users (email, full_name, password_hash, must_change_password) VALUES (?, ?, ?, true)",
                    email, nama, passwordHash
                );

                Map<String, String> passMap = new HashMap<>();
                passMap.put("email", email);
                passMap.put("password", tempPassword);
                tempPasswords.add(passMap);

                created++;
            }
        }

        log.info("Import CSV selesai: {} user baru dibuat, {} user diupdate", created, updated);
        return ResponseEntity.ok(new AdminImportResponse(created, updated, tempPasswords));
    }


    // ==========================================
    // 2. API CRUD LIBUR NASIONAL (HOLIDAYS)
    // ==========================================
    @GetMapping("/holidays")
    public ResponseEntity<List<Map<String, Object>>> getHolidays(HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request GET untuk daftar Libur Nasional");
        List<Map<String, Object>> holidays = jdbcTemplate.queryForList("SELECT * FROM holidays ORDER BY date");
        return ResponseEntity.ok(holidays);
    }

    @PostMapping("/holidays")
    public ResponseEntity<?> addHoliday(@RequestBody Map<String, String> request, HttpSession session) {
        roleService.requireAdmin(session);
        String date = request.get("date");
        String name = request.get("name");
        log.info("Menerima request POST untuk tambah Libur Nasional: {}", name);

        if (date == null || name == null) {
            log.warn("Validasi gagal: Tanggal atau Nama libur kosong");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal dan Nama wajib diisi");
        }

        jdbcTemplate.update("INSERT INTO holidays (date, name) VALUES (?, ?) ON CONFLICT (date) DO UPDATE SET name = EXCLUDED.name", LocalDate.parse(date), name);
        log.info("Berhasil menambahkan Libur Nasional: {}", name);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil disimpan"));
    }

    @DeleteMapping("/holidays/{date}")
    public ResponseEntity<?> deleteHoliday(@PathVariable String date, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Libur Nasional tanggal: {}", date);
        int rows = jdbcTemplate.update("DELETE FROM holidays WHERE date = ?", LocalDate.parse(date));
        
        if (rows == 0) {
            log.warn("Gagal hapus: Libur Nasional tanggal {} tidak ditemukan", date);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Libur nasional tidak ditemukan");
        }
        log.info("Berhasil menghapus Libur Nasional tanggal: {}", date);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil dihapus"));
    }


    // ==========================================
    // 3. API CRUD CUTI (LEAVES)
    // ==========================================
    @GetMapping("/leaves")
    public ResponseEntity<List<Map<String, Object>>> getLeaves(HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request GET untuk daftar Cuti Talent");
        String query = "SELECT l.id, l.user_id, u.full_name, l.from_date, l.to_date FROM leaves l JOIN users u ON l.user_id = u.id ORDER BY l.from_date";
        List<Map<String, Object>> leaves = jdbcTemplate.queryForList(query);
        return ResponseEntity.ok(leaves);
    }

    @PostMapping("/leaves")
    public ResponseEntity<?> addLeave(@RequestBody Map<String, String> request, HttpSession session) {
        roleService.requireAdmin(session);
        String userIdStr = request.get("userId");
        String fromDate = request.get("fromDate");
        String toDate = request.get("toDate");
        log.info("Menerima request POST untuk tambah Cuti User ID: {}", userIdStr);

        if (userIdStr == null || fromDate == null || toDate == null) {
            log.warn("Validasi gagal: Data cuti tidak lengkap");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data cuti tidak lengkap");
        }

        Long userId = Long.parseLong(userIdStr);
        LocalDate start = LocalDate.parse(fromDate);
        LocalDate end = LocalDate.parse(toDate);

        if (start.isAfter(end)) {
            log.warn("Validasi gagal: Tanggal mulai cuti melebihi tanggal selesai");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal mulai tidak boleh melebihi tanggal selesai");
        }

        jdbcTemplate.update("INSERT INTO leaves (user_id, from_date, to_date) VALUES (?, ?, ?)", userId, start, end);
        log.info("Berhasil menambahkan Cuti untuk User ID: {}", userId);
        return ResponseEntity.ok(Map.of("message", "Cuti berhasil disimpan"));
    }

    @DeleteMapping("/leaves/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Cuti ID: {}", id);
        int rows = jdbcTemplate.update("DELETE FROM leaves WHERE id = ?", id);
        
        if (rows == 0) {
            log.warn("Gagal hapus: Cuti ID {} tidak ditemukan", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuti tidak ditemukan");
        }
        log.info("Berhasil menghapus Cuti ID: {}", id);
        return ResponseEntity.ok(Map.of("message", "Cuti berhasil dihapus"));
    }
}