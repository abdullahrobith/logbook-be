-- Migrasi waktu mulai task ke start_minute (menit sejak 00:00, rentang 0..2879).
-- Database canonical (logbook.sql) sudah memakai start_minute dan tidak memiliki
-- start_time, sehingga backfill legacy hanya boleh dijalankan bila kolom lama ada.
ALTER TABLE public.log_tasks
    ADD COLUMN IF NOT EXISTS start_minute integer;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'log_tasks'
          AND column_name = 'start_time'
    ) THEN
        EXECUTE '
            UPDATE public.log_tasks
            SET start_minute = EXTRACT(HOUR FROM start_time::time)::int * 60
                             + EXTRACT(MINUTE FROM start_time::time)::int
            WHERE start_minute IS NULL
              AND start_time IS NOT NULL';
    END IF;

    -- Baris lama yang tidak memiliki start_time tidak bisa dikonversi. Pakai
    -- jam kerja bawaan 08:00 (480 menit) agar kolom NOT NULL tetap terpenuhi.
    UPDATE public.log_tasks
    SET start_minute = 480
    WHERE start_minute IS NULL;
END
$$;

ALTER TABLE public.log_tasks
    ALTER COLUMN start_minute SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.log_tasks'::regclass
          AND conname = 'log_tasks_start_minute_check'
    ) THEN
        ALTER TABLE public.log_tasks
            ADD CONSTRAINT log_tasks_start_minute_check
            CHECK (start_minute >= 0 AND start_minute <= 2879);
    END IF;
END
$$;

ALTER TABLE public.log_tasks
    DROP COLUMN IF EXISTS start_time;
