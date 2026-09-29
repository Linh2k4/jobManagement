--liquibase formatted sql

--changeset jobmanagement:V13

-- Backs task.service.ts's comments/private-notes/attachments features, which
-- previously called endpoints with no backend support at all.

CREATE TABLE task_comments (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);

CREATE TABLE task_private_notes (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notes       TEXT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_id, user_id)
);

CREATE TABLE task_attachments (
    id            BIGSERIAL PRIMARY KEY,
    task_id       BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    uploaded_by   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name     VARCHAR(255) NOT NULL,
    object_key    VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100),
    file_size     BIGINT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_attachments_task_id ON task_attachments(task_id);

COMMENT ON TABLE task_comments IS 'Free-text discussion thread per task';
COMMENT ON TABLE task_private_notes IS 'Per-user private scratch notes on a task; one row per (task, user)';
COMMENT ON TABLE task_attachments IS 'Uploaded file metadata; binary content lives in MinIO under object_key';
