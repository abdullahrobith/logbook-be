CREATE TABLE IF NOT EXISTS public.user_settings (
    user_id bigint PRIMARY KEY REFERENCES public.users(id),
    work_start time without time zone NOT NULL DEFAULT TIME '08:00',
    work_end time without time zone NOT NULL DEFAULT TIME '17:00',
    onboarded_at timestamp with time zone NULL
);

-- Pengaturan awal untuk pengguna yang sudah ada mengikuti jam kerja bawaan.
INSERT INTO public.user_settings (user_id, work_start, work_end, onboarded_at)
SELECT u.id, TIME '08:00', TIME '17:00', NULL
FROM public.users u
ON CONFLICT (user_id) DO NOTHING;
