-- Drop the single-assignment-per-task index
DROP INDEX IF EXISTS uq_task_assignments_current;

-- Create unique index on (task_id, assignee_id) WHERE is_current = true
-- This allows multiple distinct users to be concurrently assigned to the same task.
CREATE UNIQUE INDEX IF NOT EXISTS uq_task_assignments_task_assignee_current
ON task_assignments (task_id, assignee_id)
WHERE is_current = true;
