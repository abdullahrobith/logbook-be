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
import org.springframework.transaction.annotation.Transactional; 

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
    // 1. API IMPORT CSV (SUDAH DISEMPURNAKAN)
    // ====================
    @Transactional 
    @PostMapping("/users/import")
    public ResponseEntity<AdminImportResponse> importUsers(
            @RequestParam("file") MultipartFile file,
            HttpSession session
    ) {
        roleService.requireAdmin(session);
        String currentUserEmail = (String) session.getAttribute("email");
        log.info("Menerima request import CSV dari Admin: {}", currentUserEmail);

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File CSV tidak boleh kosong");
        }

        List<CSVRecord> records;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Set header secara eksplisit
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("email", "nama", "jenis", "email_atasan", "admin")
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();
            CSVParser parser = new CSVParser(reader, format);
            records = parser.getRecords();
        } catch (Exception e) {
            log.warn("Gagal membaca file CSV: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format CSV tidak valid. Pastikan header: email,nama,jenis,email_atasan,admin");
        }

        // --- VALIDASI SEMUA BARIS (BATAL SEMUA KALAU ADA YANG SALAH) ---
        int lineNumber = 2;
        for (CSVRecord record : records) {
            String email = record.get("email");
            String nama = record.get("nama");
            String jenis = record.get("jenis");
            String emailAtasan = record.get("email_atasan");
            String adminStr = record.get("admin");

            // 1. Cek kolom wajib
            if (email == null || email.isBlank() || nama == null || nama.isBlank() || jenis == null || jenis.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Email, Nama, dan Jenis wajib diisi.");
            }

            // 2. Cek format email
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Format email tidak valid.");
            }

            // 3. Cek relasi atasan (Kalau diisi, pastikan email atasan ada di DB)
            if (emailAtasan != null && !emailAtasan.isBlank()) {
                Integer atasanCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, emailAtasan);
                if (atasanCount == null || atasanCount == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Email atasan '" + emailAtasan + "' tidak ditemukan.");
                }
            }

            // 4. Cek validasi admin (harus true/false)
            if (adminStr != null && !adminStr.isBlank() && !adminStr.equalsIgnoreCase("true") && !adminStr.equalsIgnoreCase("false")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Kolom admin harus 'true' atau 'false'.");
            }
            
            lineNumber++;
        }

        // --- JIKA LOLOS VALIDASI, PROSES SIMPAN ---
        int created = 0;
        int updated = 0;
        List<Map<String, String>> tempPasswords = new ArrayList<>();

        for (CSVRecord record : records) {
            String email = record.get("email");
            String nama = record.get("nama");
            String jenis = record.get("jenis");
            String emailAtasan = record.get("email_atasan");
            boolean isAdmin = Boolean.parseBoolean(record.get("admin"));

            // Proteksi: Admin tidak bisa mencabut hak admin dirinya sendiri
            if (email.equalsIgnoreCase(currentUserEmail) && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anda tidak dapat mencabut hak admin milik sendiri melalui CSV.");
            }

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);

            if (count != null && count > 0) {
                // Update user
                jdbcTemplate.update(
                    "UPDATE users SET full_name = ?, jenis = ?, email_atasan = ?, is_admin = ? WHERE email = ?",
                    nama, jenis, (emailAtasan == null || emailAtasan.isBlank()) ? null : emailAtasan, isAdmin, email
                );
                updated++;
            } else {
                // Insert user baru
                String tempPassword = UUID.randomUUID().toString().substring(0, 8);
                String passwordHash = "BCryptHashPlaceholder_" + tempPassword;

                jdbcTemplate.update(
                    "INSERT INTO users (email, full_name, jenis, email_atasan, is_admin, password_hash, must_change_password) VALUES (?, ?, ?, ?, ?, ?, true)",
                    email, nama, jenis, (emailAtasan == null || emailAtasan.isBlank()) ? null : emailAtasan, isAdmin, passwordHash
                );

                Map<String, String> passMap = new HashMap<>();
                passMap.put("email", email);
                passMap.put("password", tempPassword);
                tempPasswords.add(passMap);

                created++;
            }
        }

        // Proteksi terakhir: Pastikan minimal ada 1 admin di database
        Integer adminCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE is_admin = true", Integer.class);
        if (adminCount == null || adminCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impor dibatalkan: Sistem harus memiliki minimal 1 Admin.");
        }

        log.info("Import CSV selesai: {} user baru dibuat, {} user diupdate", created, updated);
        return ResponseEntity.ok(new AdminImportResponse(created, updated, tempPasswords));
    }


    // ==========================================
    // 2. API CRUD LIBUR NASIONAL (HOLIDAYS)
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getTalentOptions(HttpSession session) {
        roleService.requireAdmin(session);
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, full_name FROM users "
                        + "WHERE is_active = TRUE AND is_admin = FALSE "
                        + "ORDER BY full_name"
        );
        return ResponseEntity.ok(users);
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<Map<String, Object>>> getHolidays(HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request GET untuk daftar Libur Nasional");
        List<Map<String, Object>> holidays = jdbcTemplate.queryForList("SELECT * FROM holidays ORDER BY holiday_date");
        return ResponseEntity.ok(holidays);
    }

    @PostMapping("/holidays")
    public ResponseEntity<?> addHoliday(@RequestBody Map<String, String> request, HttpSession session) {
        roleService.requireAdmin(session);
        String date = request.get("date");
        String name = request.get("name");
        log.info("Menerima request POST untuk tambah Libur Nasional: {}", name);

        if (date == null || date.isBlank() || name == null || name.isBlank()) {
            log.warn("Validasi gagal: Tanggal atau Nama libur kosong");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal dan Nama wajib diisi");
        }

        jdbcTemplate.update(
            "INSERT INTO holidays (holiday_date, name) VALUES (?, ?) ON CONFLICT (holiday_date) DO UPDATE SET name = EXCLUDED.name", 
            LocalDate.parse(date), 
            name.trim()
        );
        log.info("Berhasil menambahkan Libur Nasional: {}", name);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil disimpan"));
    }

    @DeleteMapping("/holidays/{date}")
    public ResponseEntity<?> deleteHoliday(@PathVariable String date, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Libur Nasional tanggal: {}", date);
        int rows = jdbcTemplate.update("DELETE FROM holidays WHERE holiday_date = ?", LocalDate.parse(date));
        
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
        String query = "SELECT l.id, l.user_id, u.full_name, l.start_date, l.end_date, l.note "
                + "FROM leaves l JOIN users u ON l.user_id = u.id ORDER BY l.start_date";
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
        String note = request.get("note");
        if (note != null) {
            note = note.trim();
            if (note.isEmpty()) {
                note = null;
            }
        }

        if (start.isAfter(end)) {
            log.warn("Validasi gagal: Tanggal mulai cuti melebihi tanggal selesai");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal mulai tidak boleh melebihi tanggal selesai");
        }

        jdbcTemplate.update(
                "INSERT INTO leaves (user_id, start_date, end_date, note) VALUES (?, ?, ?, ?)",
                userId,
                start,
                end,
                note
        );
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