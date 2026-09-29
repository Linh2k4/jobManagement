--liquibase formatted sql

--changeset jobmanagement:V2 context:dev

-- Seed data for development environment
-- Passwords are BCrypt hashed ("admin123" -> specific hashes below)
-- This migration only runs in dev profile; will be skipped in prod

-- Insert Manager
INSERT INTO users (email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES ('admin@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Admin Manager', 'MANAGER', true, now(), now())
ON CONFLICT (email) DO NOTHING;

-- Get the Manager ID for reference
WITH manager AS (
  SELECT id FROM users WHERE email = 'admin@company.com' LIMIT 1
)

-- Insert Team Leads (under the Manager)
INSERT INTO users (email, password_hash, full_name, role, manager_id, is_active, created_at, updated_at)
SELECT 'lead1@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Lead One', 'LEAD'::user_role, m.id, true, now(), now() FROM manager m
UNION ALL
SELECT 'lead2@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Lead Two', 'LEAD'::user_role, m.id, true, now(), now() FROM manager m
ON CONFLICT (email) DO NOTHING;

-- Get Lead IDs for reference
WITH leads AS (
  SELECT id, email FROM users WHERE role = 'LEAD' AND email IN ('lead1@company.com', 'lead2@company.com')
)

-- Insert Members (under the Leads)
INSERT INTO users (email, password_hash, full_name, role, lead_id, is_active, created_at, updated_at)
SELECT 'member1@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Member One', 'MEMBER'::user_role, l.id, true, now(), now() FROM leads l WHERE l.email = 'lead1@company.com'
UNION ALL
SELECT 'member2@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Member Two', 'MEMBER'::user_role, l.id, true, now(), now() FROM leads l WHERE l.email = 'lead1@company.com'
UNION ALL
SELECT 'member3@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Member Three', 'MEMBER'::user_role, l.id, true, now(), now() FROM leads l WHERE l.email = 'lead2@company.com'
UNION ALL
SELECT 'member4@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Member Four', 'MEMBER'::user_role, l.id, true, now(), now() FROM leads l WHERE l.email = 'lead2@company.com'
UNION ALL
SELECT 'member5@company.com', '$2b$10$nQ3pMwj52nR9NvrtHzbaTuaqkWwQOZ60qxF8PB3HP0D/WmPQRpGeG', 'Member Five', 'MEMBER'::user_role, l.id, true, now(), now() FROM leads l WHERE l.email = 'lead1@company.com'
ON CONFLICT (email) DO NOTHING;
