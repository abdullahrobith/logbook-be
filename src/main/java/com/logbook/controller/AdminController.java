package com.logbook.controller;

import com.logbook.dto.AdminImportResponse;
import com.logbook.dto.AdminLeaveResponse;
import com.logbook.dto.UserResponse;
import com.logbook.entity.Holiday;
import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.UserRepository;
import com.logbook.security.Role;
import com.logbook.service.AdminService;
import com.logbook.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin API", description = "Endpoint khusus HR/Admin (Impor CSV, Libur, Cuti)")
public class AdminController {

    private final AdminService adminService;
    private final RoleService roleService;
    private final UserRepository userRepository;

    public AdminController(
            AdminService adminService,
            RoleService roleService,
            UserRepository userRepository
    ) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.userRepository = userRepository;
    }

    private LocalDate parseIsoDate(String rawDate, String fieldName) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " wajib diisi");
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException ex) {
            log.warn("Format tanggal tidak valid untuk {}: '{}'", fieldName, rawDate);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Format " + fieldName + " tidak valid. Gunakan format YYYY-MM-DD"
            );
        }
    }

    private Long parsePositiveId(String rawId, String fieldName) {
        if (rawId == null || rawId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " wajib diisi");
        }
        try {
            long parsed = Long.parseLong(rawId.trim());
            if (parsed <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " harus berupa angka positif");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            log.warn("Format ID tidak valid untuk {}: '{}'", fieldName, rawId);
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " tidak valid");
        }
    }

    // ==================== 1. API IMPORT CSV ====================
    @PostMapping("/users/import")
    public ResponseEntity<AdminImportResponse> importUsers(
            @RequestParam("file") MultipartFile file,
            HttpSession session
    ) {
        roleService.requireAdmin(session);
        String currentUserEmail = (String) session.getAttribute("email");
        log.info("Menerima request import CSV dari Admin: {}", currentUserEmail);

        AdminImportResponse response = adminService.importUsers(file, currentUserEmail);

        log.info("Import CSV selesai: {} user baru dibuat, {} user diupdate", response.created(), response.updated());
        return ResponseEntity.ok(response);
    }

    // ==================== 2. API USERS (DTO TANPA PASSWORD HASH) ====================
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(HttpSession session) {
        roleService.requireAdmin(session);

        List<User> users = userRepository.findAll();

        List<UserResponse> userResponses = users.stream()
                .map(user -> {
                    Collection<Role> userRoles = roleService.rolesOf(user);
                    return UserResponse.from(user, userRoles);
                })
                .toList();

        return ResponseEntity.ok(userResponses);
    }

    // ==================== 3. API CRUD LIBUR NASIONAL (HOLIDAYS) ====================
    @GetMapping("/holidays")
    public ResponseEntity<List<Holiday>> getHolidays(HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request GET untuk daftar Libur Nasional");
        return ResponseEntity.ok(adminService.getHolidays());
    }

    @PostMapping("/holidays")
    public ResponseEntity<?> addHoliday(@RequestBody Map<String, String> request, HttpSession session) {
        roleService.requireAdmin(session);
        String dateStr = request.get("date");
        String name = request.get("name");
        log.info("Menerima request POST untuk tambah Libur Nasional: {}", name);

        if (dateStr == null || dateStr.isBlank() || name == null || name.isBlank()) {
            log.warn("Validasi gagal: Tanggal atau Nama libur kosong");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tanggal dan Nama wajib diisi");
        }

        LocalDate date = parseIsoDate(dateStr, "tanggal");
        adminService.addHoliday(date, name.trim());
        log.info("Berhasil menambahkan Libur Nasional: {}", name);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil disimpan"));
    }

    @DeleteMapping("/holidays/{date}")
    public ResponseEntity<?> deleteHoliday(@PathVariable String date, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Libur Nasional tanggal: {}", date);
        LocalDate parsedDate = parseIsoDate(date, "tanggal");
        adminService.deleteHoliday(parsedDate);
        log.info("Berhasil menghapus Libur Nasional tanggal: {}", date);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil dihapus"));
    }

    // ==================== 4. API CRUD CUTI (LEAVES) ====================
    @GetMapping("/leaves")
    public ResponseEntity<List<AdminLeaveResponse>> getLeaves(HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request GET untuk daftar Cuti Talent");
        return ResponseEntity.ok(adminService.getLeaves());
    }

    @PostMapping("/leaves")
    public ResponseEntity<?> addLeave(@RequestBody Map<String, String> request, HttpSession session) {
        roleService.requireAdmin(session);
        String userIdStr = request.get("userId");
        String fromDateStr = request.get("fromDate");
        String toDateStr = request.get("toDate");
        log.info("Menerima request POST untuk tambah Cuti User ID: {}", userIdStr);

        if (userIdStr == null || fromDateStr == null || toDateStr == null) {
            log.warn("Validasi gagal: Data cuti tidak lengkap");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Data cuti tidak lengkap");
        }

        Long userId = parsePositiveId(userIdStr, "userId");
        LocalDate fromDate = parseIsoDate(fromDateStr, "fromDate");
        LocalDate toDate = parseIsoDate(toDateStr, "toDate");

        if (toDate.isBefore(fromDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "toDate tidak boleh lebih awal dari fromDate");
        }

        adminService.addLeave(userId, fromDate, toDate, request.get("note"));
        log.info("Berhasil menambahkan Cuti untuk User ID: {}", userId);
        return ResponseEntity.ok(Map.of("message", "Cuti berhasil disimpan"));
    }

    @DeleteMapping("/leaves/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Cuti ID: {}", id);
        if (id == null || id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID cuti harus berupa angka positif");
        }
        adminService.deleteLeave(id);
        log.info("Berhasil menghapus Cuti ID: {}", id);
        return ResponseEntity.ok(Map.of("message", "Cuti berhasil dihapus"));
    }
}