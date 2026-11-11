-- Alasan pengajuan perbaikan Daily Log yang sudah disetujui (LBK-KRM-05).
-- Dipakai bersama rejection_reason: rejection_reason diisi atasan saat menolak,
-- revision_reason diisi talent saat mengajukan perbaikan.
ALTER TABLE public.daily_logs
    ADD COLUMN IF NOT EXISTS revision_reason character varying(1000);
