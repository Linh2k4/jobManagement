--liquibase formatted sql

--changeset jobmanagement:V16

-- Deadline extension request & approval flow (Scope.md §12). status is
-- VARCHAR + CHECK, not a native Postgres enum — see V12's note on why
-- (Hibernate's @Enumerated(STRING) can't compare against a native enum).
CREATE TABLE deadline_extension_requests (
    id                  BIGSERIAL PRIMARY KEY,
    task_id             BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    requested_by        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_deadline    DATE NOT NULL,
    requested_deadline  DATE NOT NULL,
    reason              TEXT NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewed_by         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at         TIMESTAMPTZ,
    review_note         TEXT,
    extension_number    INT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT dext_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_dext_task_id ON deadline_extension_requests(task_id);
CREATE INDEX idx_dext_status ON deadline_extension_requests(status);

-- Only one PENDING request per task at a time (Scope.md §12.4 "Không overlap").
CREATE UNIQUE INDEX uq_dext_task_pending ON deadline_extension_requests(task_id) WHERE status = 'PENDING';

COMMENT ON TABLE deadline_extension_requests IS 'Deadline extension request/approval workflow (Scope §12)';
