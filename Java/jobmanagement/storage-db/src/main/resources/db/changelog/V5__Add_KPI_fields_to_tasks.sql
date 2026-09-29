--liquibase formatted sql

--changeset jobmanagement:V5

-- Add KPI-related fields to tasks table (Phase 3)
-- Implements Scope.md §3.3 Task Weight calculation

ALTER TABLE tasks ADD COLUMN priority VARCHAR(50) DEFAULT 'MEDIUM';
ALTER TABLE tasks ADD COLUMN difficulty INT DEFAULT 3 CHECK (difficulty BETWEEN 1 AND 5);
ALTER TABLE tasks ADD COLUMN category_code VARCHAR(100);
ALTER TABLE tasks ADD COLUMN category_data JSONB DEFAULT '{}';
ALTER TABLE tasks ADD COLUMN actual_hours NUMERIC(10,2);
ALTER TABLE tasks ADD COLUMN task_weight NUMERIC(10,2);
ALTER TABLE tasks ADD COLUMN extend_log JSONB DEFAULT '[]';
ALTER TABLE tasks ADD COLUMN difficulty_log JSONB DEFAULT '[]';
ALTER TABLE tasks ADD COLUMN schedule JSONB DEFAULT '{}';
ALTER TABLE tasks ADD COLUMN version BIGINT DEFAULT 1;

-- Update task_status enum to include CLOSED_LATE (status is a native enum column,
-- not text — new values must be added to the type itself, not a CHECK constraint)
ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'CLOSED_LATE';

-- Create indexes for KPI queries
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_difficulty ON tasks(difficulty);
CREATE INDEX idx_tasks_category_code ON tasks(category_code);
CREATE INDEX idx_tasks_deadline ON tasks(due_date);
CREATE INDEX idx_tasks_assignee_period ON tasks(created_by, created_at);

COMMENT ON COLUMN tasks.priority IS 'Task priority: LOW(0.8), MEDIUM(1.0), HIGH(1.2), URGENT(1.5)';
COMMENT ON COLUMN tasks.difficulty IS 'Task difficulty: 1=Very Easy, 5=Very Hard (Scope §3.3.1)';
COMMENT ON COLUMN tasks.task_weight IS 'Weighted KPI factor: difficulty × hours × priority × type (Scope §3.3.3)';
COMMENT ON COLUMN tasks.actual_hours IS 'Actual hours spent to complete task (for Estimate Accuracy calculation)';
