package com.logbook.dto;

/**
 * Status Daily Log yang bisa diminta pada daftar hari halaman Tim.
 *
 * <p>Default endpoint adalah {@code MENUNGGU}. {@code PERBAIKAN_DIAJUKAN}
 * ditambahkan agar atasan dan admin dapat menemukan hari yang menunggu
 * persetujuan perbaikan dari endpoint yang sama.</p>
 */
public enum PendingDayStatus {
    MENUNGGU,
    PERBAIKAN_DIAJUKAN
}
