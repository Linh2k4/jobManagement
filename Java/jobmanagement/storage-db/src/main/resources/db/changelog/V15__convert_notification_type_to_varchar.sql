--liquibase formatted sql

--changeset jobmanagement:V15

-- Same class of bug as V12 (native Postgres enum vs Hibernate's
-- @Enumerated(STRING) sending plain VARCHAR) — missed on notifications.type
-- during that pass because nothing had ever actually inserted a
-- notification until task auto-assignment started triggering one.

ALTER TABLE notifications ALTER COLUMN type TYPE VARCHAR(50) USING (type::text);
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('REMINDER', 'TASK_ASSIGNED', 'TASK_DUE', 'SYSTEM'));

DROP TYPE notification_type;
