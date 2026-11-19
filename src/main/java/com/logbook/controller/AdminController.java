package com.logbook.controller;

import com.logbook.dto.AdminImportResponse;
import com.logbook.dto.AdminLeaveResponse;
import com.logbook.entity.Holiday;
import com.logbook.entity.User;
import com.logbook.exception.ApiException;
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
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin API", description = "Endpoint khusus HR/Admin (Impor CSV, Libur, Cuti)")
public class AdminController {

    private final AdminService adminService;
    private final RoleService roleService;

    public AdminController(AdminService adminService, RoleService roleService) {
        this.adminService = adminService;
        this.roleService = roleService;
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

    // ==================== 2. API CRUD LIBUR NASIONAL (HOLIDAYS) ====================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getTalentOptions(HttpSession session) {
        roleService.requireAdmin(session);
        return ResponseEntity.ok(adminService.getTalentOptions());
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<Holiday>> getHolidays(HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request GET untuk daftar Libur Nasional");
        return ResponseEntity.ok(adminService.getHolidays());
    }

    @PostMapping("/holidays")
    public ResponseEntity<?> addHoliday(@RequestBody Map<String, String> request, HttpSession session) {
        roleService.requireAdmin(session);
        String date = request.get("date");
        String name = request.get("name");
        log.info("Menerima request POST untuk tambah Libur Nasional: {}", name);

        if (date == null || date.isBlank() || name == null || name.isBlank()) {
            log.warn("Validasi gagal: Tanggal atau Nama libur kosong");
            // UBAH KE ApiException
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tanggal dan Nama wajib diisi");
        }

        adminService.addHoliday(LocalDate.parse(date), name);
        log.info("Berhasil menambahkan Libur Nasional: {}", name);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil disimpan"));
    }

    @DeleteMapping("/holidays/{date}")
    public ResponseEntity<?> deleteHoliday(@PathVariable String date, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Libur Nasional tanggal: {}", date);
        adminService.deleteHoliday(LocalDate.parse(date));
        log.info("Berhasil menghapus Libur Nasional tanggal: {}", date);
        return ResponseEntity.ok(Map.of("message", "Libur nasional berhasil dihapus"));
    }

    // ==================== 3. API CRUD CUTI (LEAVES) ====================
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
        String fromDate = request.get("fromDate");
        String toDate = request.get("toDate");
        log.info("Menerima request POST untuk tambah Cuti User ID: {}", userIdStr);

        if (userIdStr == null || fromDate == null || toDate == null) {
            log.warn("Validasi gagal: Data cuti tidak lengkap");
            // UBAH KE ApiException
            throw new ApiException(HttpStatus.BAD_REQUEST, "Data cuti tidak lengkap");
        }

        adminService.addLeave(Long.parseLong(userIdStr), LocalDate.parse(fromDate), LocalDate.parse(toDate), request.get("note"));
        log.info("Berhasil menambahkan Cuti untuk User ID: {}", userIdStr);
        return ResponseEntity.ok(Map.of("message", "Cuti berhasil disimpan"));
    }

    @DeleteMapping("/leaves/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id, HttpSession session) {
        roleService.requireAdmin(session);
        log.info("Menerima request DELETE untuk Cuti ID: {}", id);
        adminService.deleteLeave(id);
        log.info("Berhasil menghapus Cuti ID: {}", id);
        return ResponseEntity.ok(Map.of("message", "Cuti berhasil dihapus"));
    }
}