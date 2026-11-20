-- Akun demo dari dump logbook.sql memakai kata sandi: Logbook@2026
-- Hash BCrypt ini hanya dipakai bila fixture belum memiliki password_hash.
UPDATE public.users
SET password_hash = '$2a$10$2YCvQ1fDOiHQQa2ou4sVpO4LZW32jLX6Rt/UVljh6hLH/ayH.4H0q',
    must_change_password = false
WHERE password_hash IS NULL
  AND (
      (id = 1 AND email = 'rina.talent@contoh.id')
      OR (id = 2 AND email = 'budi.santoso@contoh.id')
      OR (id = 3 AND email = 'sari.wulan@contoh.id')
  );
