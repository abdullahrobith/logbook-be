package com.logbook.dto;

import java.time.LocalDate;

/**
 * Keterangan satu tanggal dari sudut pandang seorang pengguna: apakah tanggal
 * itu libur nasional, cuti, akhir pekan, atau hari kerja yang wajib diisi.
 *
 * <p>Hari libur, cuti, dan akhir pekan tidak pernah menjadi TERLEWAT, tetapi
 * tetap boleh diisi bila talent bekerja.</p>
 */
public record DayInfoResponse(
        LocalDate date,
        String holidayName,
        boolean onLeave,
        boolean weekend,
        boolean workingDay
) {

    /**
     * Nilai netral untuk pemanggil yang tidak memiliki data kalender.
     */
    public static DayInfoResponse none() {
        return new DayInfoResponse(null, null, false, false, true);
    }
}
