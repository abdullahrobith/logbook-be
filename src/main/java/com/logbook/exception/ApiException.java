package com.logbook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Error bisnis dengan status HTTP tertentu.
 *
 * <p>Kelas ini tetap merupakan {@link ResponseStatusException} agar penanganan
 * galat lama dan test yang menunggu tipe tersebut terus berjalan, sambil
 * menyediakan akses {@code getStatus()} untuk penanganan galat terstandarisasi.</p>
 */
public class ApiException extends ResponseStatusException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(status, message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return getReason();
    }
}
