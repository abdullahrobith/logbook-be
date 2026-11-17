package com.logbook.controller;

import com.logbook.dto.HolidayRequest;
import com.logbook.entity.Holiday;
import com.logbook.service.HolidayService;
import com.logbook.service.RoleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;
    private final RoleService roleService;

    public HolidayController(HolidayService holidayService, RoleService roleService) {
        this.holidayService = holidayService;
        this.roleService = roleService;
    }

    @GetMapping
    public List<Holiday> getAll(HttpSession session) {
        roleService.requireCurrentUser(session); 
        return holidayService.getAllHolidays();
    }

    @PostMapping
    public Holiday create(@Valid @RequestBody HolidayRequest request, HttpSession session) {
        roleService.requireAdmin(session); 
        return holidayService.createHoliday(request);
    }
}