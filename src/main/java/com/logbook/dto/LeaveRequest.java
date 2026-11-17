package com.logbook.dto; 

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class LeaveRequest {

    @NotNull(message = "Tanggal mulai tidak boleh kosong")
    private LocalDate fromDate;

    @NotNull(message = "Tanggal selesai tidak boleh kosong")
    private LocalDate toDate;

    private String reason;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @AssertTrue(message = "Tanggal mulai tidak boleh lebih besar dari tanggal selesai")
    public boolean isDateRangeValid() {
        if (fromDate == null || toDate == null) {
            return true; 
        }
        return !fromDate.isAfter(toDate);
    }
}