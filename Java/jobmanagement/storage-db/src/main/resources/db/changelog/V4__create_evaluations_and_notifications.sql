--liquibase formatted sql

--changeset jobmanagement:V4

-- Enum types for task_evaluations and notifications
CREATE TYPE evaluator_type AS ENUM ('SELF', 'LEAD', 'MANAGER');
CREATE TYPE notification_type AS ENUM ('REMINDER', 'TASK_ASSIGNED', 'TASK_DUE', 'SYSTEM');

-- Evaluations table
CREATE TABLE task_evaluations (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    evaluatee_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    evaluator_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    evaluator_type  evaluator_type NOT NULL,
    score           INT NOT NULL CHECK (score >= 1 AND score <= 5),
    comment         TEXT,
    period_month    DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_evaluations_evaluatee_id ON task_evaluations(evaluatee_id);
CREATE INDEX idx_task_evaluations_task_id ON task_evaluations(task_id);
CREATE INDEX idx_task_evaluations_period_month ON task_evaluations(period_month);

COMMENT ON TABLE task_evaluations IS 'Member/Lead/Manager task_evaluations for tasks or period-based reviews';
COMMENT ON COLUMN task_evaluations.score IS 'Rating 1-5';
COMMENT ON COLUMN task_evaluations.period_month IS 'For period-based task_evaluations (vs task-based); NULL means task-based';

-- Notifications table
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            notification_type NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    related_task_id BIGINT,
    remind_at       TIMESTAMPTZ,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_remind_at ON notifications(remind_at);

COMMENT ON TABLE notifications IS 'User notifications for reminders, task assignments, and system events';
COMMENT ON COLUMN notifications.related_task_id IS 'NULL for system notifications';
COMMENT ON COLUMN notifications.remind_at IS 'Reminder time; NULL for non-reminder notifications';
