package com.logbook.exception;

import org.springframework.http.HttpStatus;

/**
 * Dipakai ketika pengguna masih memiliki flag wajib ganti kata sandi.
 * Kode stabil dipakai frontend untuk mengalihkan ke halaman ganti kata sandi.
 */
public class PasswordChangeRequiredException extends ApiException {

    public static final String CODE = "PASSWORD_CHANGE_REQUIRED";
    private static final String MESSAGE = "Kata sandi harus diganti sebelum melanjutkan";

    public PasswordChangeRequiredException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }

    public String code() {
        return CODE;
    }
}
