-- ==========================================
-- SEED DATA UNTUK SIT LOG BOOK
-- ==========================================

-- 1. Bikin 2 Client (Perusahaan)
INSERT INTO clients (id, name, color, is_internal, is_active) VALUES 
(101, 'Bank XYZ', '#0A6FD6', false, true),
(102, 'Asuransi ABC', '#F5A623', false, true)
ON CONFLICT (id) DO NOTHING;

-- 2. Bikin 2 Project
INSERT INTO projects (id, client_id, name, is_active) VALUES 
(101, 101, 'Mobile App Bank XYZ', true),
(102, 102, 'Web Portal Asuransi ABC', true)
ON CONFLICT (id) DO NOTHING;

-- 3. Daftarkan User 
INSERT INTO project_members (project_id, user_id, joined_at, last_used_at) VALUES 
(101, 1, CURRENT_DATE, NOW()),
(102, 1, CURRENT_DATE, NOW())
ON CONFLICT DO NOTHING;

-- 4. Bikin Daily Log 
INSERT INTO daily_logs (id, user_id, log_date, work_schedule_id, status, created_at, updated_at) VALUES 
(101, 1, CURRENT_DATE, 1, 'DRAF', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 5. Bikin Task 
INSERT INTO log_tasks (id, daily_log_id, project_id, task_type_id, work_status_id, ticket, start_time, duration_minutes, summary, obstacle) VALUES 
(101, 101, 101, 1, 1, 'TKT-1', '09:00:00', 60, 'Bikin fitur login', null),
(102, 101, 102, 3, 1, 'TKT-2', '09:30:00', 60, 'Meeting sinkronisasi API', null),
(103, 101, 101, 7, 1, null, '12:00:00', 60, 'Istirahat siang', null)
ON CONFLICT (id) DO NOTHING;