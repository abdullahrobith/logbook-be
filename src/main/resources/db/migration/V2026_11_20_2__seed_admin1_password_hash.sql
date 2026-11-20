-- Akun admin demo: admin1@contoh.id memakai kata sandi Logbook@2026.
-- Hash hanya diisi bila belum ada agar kata sandi yang sudah diganti tidak ditimpa.
INSERT INTO public.users (email, full_name, jenis, email_atasan, is_admin, password_hash, must_change_password, is_active)
VALUES (
    'admin1@contoh.id',
    'Admin 1',
    'KARYAWAN',
    NULL,
    TRUE,
    '$2a$10$2YCvQ1fDOiHQQa2ou4sVpO4LZW32jLX6Rt/UVljh6hLH/ayH.4H0q',
    FALSE,
    TRUE
)
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    jenis = COALESCE(users.jenis, EXCLUDED.jenis),
    is_admin = TRUE,
    is_active = TRUE,
    password_hash = CASE
        WHEN users.password_hash IS NULL OR btrim(users.password_hash) = '' THEN EXCLUDED.password_hash
        ELSE users.password_hash
    END,
    must_change_password = FALSE;
