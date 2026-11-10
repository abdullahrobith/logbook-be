package com.logbook.security;

/**
 * Peran pengguna yang diturunkan dari data pada setiap permintaan.
 *
 * <p>Setiap pengguna selalu berperan TALENT. Peran ATASAN muncul bila ada
 * pengguna aktif lain yang kolom email_atasan-nya berisi email pengguna ini.
 * Peran ADMIN mengikuti flag is_admin.</p>
 */
public enum Role {
    TALENT,
    ATASAN,
    ADMIN
}
