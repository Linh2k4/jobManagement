--liquibase formatted sql

--changeset jobmanagement:V23

-- 1. Drop existing tasks_status_check constraint and re-add with WAITING_APPROVAL
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_check;
ALTER TABLE tasks ADD CONSTRAINT tasks_status_check 
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'WAITING_APPROVAL', 'DONE', 'CANCELLED', 'CLOSED_LATE'));

-- 2. Add review_note column to tasks table to store feedback / rejection reasons
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS review_note TEXT;
