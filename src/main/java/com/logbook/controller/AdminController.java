package com.logbook.controller;

import com.logbook.dto.AdminImportResponse;
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
@RequestMapping("/api/admin/users")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;

    public AdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/import")
    public ResponseEntity<AdminImportResponse> importUsers(@RequestParam("file") MultipartFile file) {
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
                String passwordHash = "BCryptHashPlaceholder_" + tempPassword; // Sementara, nanti sesuaikan dengan BCrypt beneran

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
}