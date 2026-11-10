package com.logbook.repository;

/**
 * Proyeksi jumlah Daily Log berstatus MENUNGGU per pengguna.
 */
public interface PendingDayCountProjection {

    Long getUserId();

    long getTotal();
}
