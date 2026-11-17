package com.logbook.dto;

import java.util.List;

/**
 * Hasil pemeriksaan sebelum pengiriman Daily Log (LBK-KUA-02).
 *
 * <p>Daftar {@code hints} boleh kosong: saran bersifat membantu, bukan syarat.
 * Pengiriman tetap boleh dilanjutkan tanpa memperbaiki apa pun.</p>
 */
public record SubmitCheckResponse(List<Hint> hints) {

    public static final String KIND_RINGKASAN_PENDEK = "RINGKASAN_PENDEK";
    public static final String KIND_TASK_PANJANG = "TASK_PANJANG";

    public SubmitCheckResponse {
        hints = hints == null ? List.of() : List.copyOf(hints);
    }

    public record Hint(
            Long taskId,
            String kind,
            String message
    ) {
    }
}
