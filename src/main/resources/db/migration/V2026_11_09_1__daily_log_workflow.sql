ALTER TABLE public.daily_logs
    ALTER COLUMN status TYPE character varying(32);

-- Status lama sebelum workflow persetujuan memakai TERKIRIM.
UPDATE public.daily_logs
SET status = 'MENUNGGU'
WHERE status = 'TERKIRIM';

ALTER TABLE public.daily_logs
    ADD COLUMN IF NOT EXISTS rejection_reason character varying(1000),
    ADD COLUMN IF NOT EXISTS decided_by_user_id bigint,
    ADD COLUMN IF NOT EXISTS decided_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS auto_submitted boolean NOT NULL DEFAULT false;

ALTER TABLE public.daily_logs
    DROP CONSTRAINT IF EXISTS daily_logs_status_check;

ALTER TABLE public.daily_logs
    ADD CONSTRAINT daily_logs_status_check
    CHECK (status IN (
        'DRAF',
        'MENUNGGU',
        'DISETUJUI',
        'DITOLAK',
        'TERLEWAT',
        'PERBAIKAN_DIAJUKAN',
        'DRAF_DIBUKA_KEMBALI'
    ));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'daily_logs_decided_by_user_id_fkey'
          AND conrelid = 'public.daily_logs'::regclass
    ) THEN
        ALTER TABLE public.daily_logs
            ADD CONSTRAINT daily_logs_decided_by_user_id_fkey
            FOREIGN KEY (decided_by_user_id) REFERENCES public.users(id);
    END IF;
END
$$;
