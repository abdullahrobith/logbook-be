package com.logbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class HolidayRequest {

    @NotNull(message = "Tanggal libur tidak boleh kosong")
    private LocalDate holidayDate;

    @NotBlank(message = "Nama libur tidak boleh kosong")
    private String name;

    // Getters and Setters
    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}