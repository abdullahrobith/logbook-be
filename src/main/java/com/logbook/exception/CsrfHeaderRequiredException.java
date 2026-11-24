package com.logbook.exception;

import org.springframework.http.HttpStatus;

/**
 * Dipakai ketika permintaan yang mengubah data tidak menyertakan header
 * pelindung CSRF yang diwajibkan SRS LBK-AUT-04.
 *
 * <p>Kode stabil dipakai frontend untuk membedakan penolakan CSRF dari
 * penolakan otorisasi biasa.</p>
 */
public class CsrfHeaderRequiredException extends ApiException {

    public static final String CODE = "CSRF_REQUIRED";
    private static final String MESSAGE = "Header X-Requested-With wajib disertakan pada permintaan ini";

    public CsrfHeaderRequiredException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }

    public String code() {
        return CODE;
    }
}
