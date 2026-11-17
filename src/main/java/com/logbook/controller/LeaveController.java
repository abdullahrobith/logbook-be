package com.logbook.controller;

import com.logbook.dto.LeaveRequest;
import com.logbook.entity.User;
import com.logbook.service.LeaveService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final RoleService roleService;

    public LeaveController(LeaveService leaveService, RoleService roleService) {
        this.leaveService = leaveService;
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<String> submit(@Valid @RequestBody LeaveRequest request, HttpSession session) {
        // PERBAIKAN: Ambil user yang login pakai requireCurrentUser
        User actor = roleService.requireCurrentUser(session); 
        
        // Panggil service dengan ID user tersebut
        leaveService.submitLeave(actor.getId(), request);
        
        return ResponseEntity.ok("Pengajuan cuti berhasil");
    }
}