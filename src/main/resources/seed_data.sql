-- ==========================================
-- SEED DATA LENGKAP UNTUK SIT LOG BOOK
-- ==========================================

-- 1. BIKIN USERS 
INSERT INTO users (id, email, full_name, jenis, email_atasan, is_admin, password_hash, must_change_password, is_active) VALUES 
-- Admin (Atasan-nya null, Admin=true)
(100, 'admin@padepokan.com', 'Bambang Hendrayanto', 'KARYAWAN', null, true, '$2a$10$7EqJtq98hPqEX7fNZaFWoO5S6mI.2T.w76d1G.4h5n6e7r8t9y0u1', false, true),
-- Atasan (Atasan-nya null, Admin=false)
(101, 'atasan@padepokan.com', 'Herlina Rastiana', 'KARYAWAN', null, false, '$2a$10$7EqJtq98hPqEX7fNZaFWoO5S6mI.2T.w76d1G.4h5n6e7r8t9y0u1', false, true),
-- Talent Rina (Atasannya Pak Atasan)
(1, 'rina@padepokan.com', 'Rina Talent', 'MAGANG', 'atasan@padepokan.com', false, '$2a$10$7EqJtq98hPqEX7fNZaFWoO5S6mI.2T.w76d1G.4h5n6e7r8t9y0u1', false, true),
-- Talent Budi (Atasannya Pak Atasan)
(2, 'budi@padepokan.com', 'Budi Talent', 'PKL', 'atasan@padepokan.com', false, '$2a$10$7EqJtq98hPqEX7fNZaFWoO5S6mI.2T.w76d1G.4h5n6e7r8t9y0u1', false, true)
ON CONFLICT (id) DO UPDATE SET 
    full_name = EXCLUDED.full_name,
    jenis = EXCLUDED.jenis;

-- 2. Bikin 2 Client (Perusahaan)
INSERT INTO clients (id, name, color, is_internal, is_active) VALUES 
(101, 'Bank XYZ', '#0A6FD6', false, true),
(102, 'Asuransi ABC', '#F5A623', false, true)
ON CONFLICT (id) DO NOTHING;

-- 3. Bikin 2 Project
INSERT INTO projects (id, client_id, name, is_active) VALUES 
(101, 101, 'Mobile App Bank XYZ', true),
(102, 102, 'Web Portal Asuransi ABC', true)
ON CONFLICT (id) DO NOTHING;

-- 4. Daftarkan Rina (ID 1) & Budi (ID 2) ke Project
INSERT INTO project_members (project_id, user_id, joined_at, last_used_at) VALUES 
(101, 1, CURRENT_DATE, NOW()),
(102, 1, CURRENT_DATE, NOW()),
(101, 2, CURRENT_DATE, NOW())
ON CONFLICT DO NOTHING;

-- 5. Bikin Daily Log
INSERT INTO daily_logs (id, user_id, log_date, work_schedule_id, status, created_at, updated_at) VALUES 
(101, 1, CURRENT_DATE, 1, 'DRAF', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 6. Bikin Task (Ujian Kalkulasi)
INSERT INTO log_tasks (id, daily_log_id, project_id, task_type_id, work_status_id, ticket, start_time, duration_minutes, summary, obstacle) VALUES 
(101, 101, 101, 1, 1, 'TKT-1', '09:00:00', 60, 'Bikin fitur login', null),
(102, 101, 102, 3, 1, 'TKT-2', '09:30:00', 60, 'Meeting sinkronisasi API', null),
(103, 101, 101, 7, 1, null, '12:00:00', 60, 'Istirahat siang', null)
ON CONFLICT (id) DO NOTHING;