package com.logbook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        String email,

        @NotBlank(message = "Kata sandi wajib diisi")
        @Size(max = 72, message = "Kata sandi maksimal 72 karakter")
        String password
) {
}
