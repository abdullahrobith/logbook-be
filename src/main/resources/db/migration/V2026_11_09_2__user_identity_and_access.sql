ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS jenis character varying(32),
    ADD COLUMN IF NOT EXISTS email_atasan character varying(255),
    ADD COLUMN IF NOT EXISTS is_admin boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS password_hash character varying(255),
    ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT true;

-- Peran diturunkan dari data: atasan adalah pengguna yang email-nya dipakai
-- pada email_atasan pengguna lain, sedangkan admin memakai flag is_admin.
-- Tidak ada kolom atau tabel role terpisah.
