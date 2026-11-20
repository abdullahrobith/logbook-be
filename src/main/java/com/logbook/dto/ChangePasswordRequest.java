package com.logbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Kata sandi saat ini wajib diisi")
        String currentPassword,

        @NotBlank(message = "Kata sandi baru wajib diisi")
        @Size(min = 8, max = 72, message = "Kata sandi baru minimal 8 dan maksimal 72 karakter")
        String newPassword
) {
}
