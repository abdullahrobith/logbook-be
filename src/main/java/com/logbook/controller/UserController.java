package com.logbook.controller;

import com.logbook.dto.ImportResultResponse;
import com.logbook.service.UserImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserImportService userImportService;

    public UserController(UserImportService userImportService) {
        this.userImportService = userImportService;
    }

    @PostMapping("/import-csv")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File CSV tidak boleh kosong");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Proses file CSV
            List<ImportResultResponse> results = userImportService.processCsvFile(reader);
            
            // Kembalikan response AMAN (Hanya berisi email, status, dan pesan)
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Gagal memproses file: " + e.getMessage());
        }
    }
}