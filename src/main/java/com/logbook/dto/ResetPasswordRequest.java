package com.logbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token wajib diisi")
        String token,

        @NotBlank(message = "Kata sandi baru wajib diisi")
        @Size(min = 6, message = "Kata sandi baru minimal 6 karakter")
        String newPassword
) {
}