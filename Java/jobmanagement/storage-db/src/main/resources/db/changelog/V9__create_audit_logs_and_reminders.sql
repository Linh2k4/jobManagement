--liquibase formatted sql

--changeset jobmanagement:V9

-- Audit trail: who did what to which entity, kept independent of business tables
-- (nullable FK: log rows must survive the acting user being deleted).
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT NOT NULL,
    details         TEXT,
    status          VARCHAR(50),
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address      VARCHAR(255),
    user_agent      VARCHAR(255)
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);

-- Personal / task reminders: one-time or recurring, time-triggered.
CREATE TABLE reminders (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id                 BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    title                   VARCHAR(255) NOT NULL,
    message                 TEXT,
    remind_at               TIMESTAMPTZ NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    is_recurring            BOOLEAN DEFAULT FALSE,
    recurrence_pattern      VARCHAR(50),
    last_reminder_sent_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_task_id ON reminders(task_id);
CREATE INDEX idx_reminders_remind_at ON reminders(remind_at);
CREATE INDEX idx_reminders_is_active ON reminders(is_active);
