package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.logbook.entity.Leave;
import com.logbook.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Baris daftar cuti talent untuk halaman admin.
 *
 * <p>Entitas {@link Leave} hanya menyimpan user_id, sehingga nama talent
 * digabungkan dari {@link User} sebelum dikirim ke frontend.</p>
 */
public record AdminLeaveResponse(
        Long id,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        String note,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
    public static AdminLeaveResponse from(Leave leave, User user) {
        return new AdminLeaveResponse(
                leave.getId(),
                leave.getUserId(),
                user == null ? null : user.getFullName(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getReason(),
                leave.getCreatedAt()
        );
    }
}
