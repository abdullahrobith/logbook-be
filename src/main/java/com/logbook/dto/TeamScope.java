package com.logbook.dto;

/**
 * Cakupan daftar Tim yang diminta.
 *
 * <p>ATASAN hanya mengembalikan bawahan langsung berdasarkan email_atasan,
 * sedangkan ADMIN mengembalikan seluruh pengguna aktif. Cakupan dibuat
 * eksplisit agar pengguna yang sekaligus admin dan atasan tidak melihat
 * daftar yang salah pada halaman Tim atasan.</p>
 */
public enum TeamScope {
    ATASAN,
    ADMIN
}
