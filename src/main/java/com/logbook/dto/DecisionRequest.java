package com.logbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alasan pada keputusan Daily Log: alasan penolakan oleh atasan/admin atau
 * alasan pengajuan perbaikan oleh talent.
 */
public record DecisionRequest(
        @NotBlank(message = "Alasan wajib diisi")
        @Size(max = 1000, message = "Alasan maksimal 1000 karakter")
        String reason
) {
}
