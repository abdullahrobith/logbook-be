ALTER TABLE public.log_tasks
    ADD COLUMN IF NOT EXISTS start_minute integer;

UPDATE public.log_tasks
SET start_minute = (EXTRACT(HOUR FROM start_time)::int * 60 + EXTRACT(MINUTE FROM start_time)::int)
WHERE start_minute IS NULL;

ALTER TABLE public.log_tasks
    ALTER COLUMN start_minute SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'log_tasks_start_minute_check'
    ) THEN
        ALTER TABLE public.log_tasks
            ADD CONSTRAINT log_tasks_start_minute_check CHECK (start_minute >= 0 AND start_minute <= 2879);
    END IF;
END $$;

ALTER TABLE public.log_tasks
    DROP COLUMN IF EXISTS start_time;
