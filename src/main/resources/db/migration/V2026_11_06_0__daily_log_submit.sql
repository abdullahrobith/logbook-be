DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.daily_logs
        GROUP BY user_id, log_date
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Migration dibatalkan: ditemukan duplikat (user_id, log_date) pada daily_logs';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.log_tasks
        WHERE duration_minutes < 5 OR duration_minutes > 1440
    ) THEN
        RAISE EXCEPTION 'Migration dibatalkan: ditemukan duration_minutes di luar rentang 5-1440';
    END IF;
END
$$;

DROP INDEX IF EXISTS public.idx_daily_logs_user_date;

CREATE UNIQUE INDEX idx_daily_logs_user_date
    ON public.daily_logs
    USING btree (user_id, log_date);

ALTER TABLE public.log_tasks
    DROP CONSTRAINT IF EXISTS log_tasks_duration_minutes_fkey;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'log_tasks_duration_minutes_check'
          AND conrelid = 'public.log_tasks'::regclass
    ) THEN
        EXECUTE 'ALTER TABLE public.log_tasks ADD CONSTRAINT log_tasks_duration_minutes_check CHECK (duration_minutes BETWEEN 5 AND 1440)';
    END IF;
END
$$;
