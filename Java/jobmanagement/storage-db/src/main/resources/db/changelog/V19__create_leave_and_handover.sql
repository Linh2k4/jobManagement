--liquibase formatted sql

--changeset jobmanagement:V19

-- Scope.md §14: a Member's approved leave scans their in-window tasks and
-- proposes handovers (Lead confirms each). "Group" isn't a real entity in
-- this app's schema yet (see V17's comment on the same gap for §2.2's
-- template tree) — the suggestion ranking and pending-request scoping use
-- the existing Member -> Lead relationship instead of a group.

CREATE TABLE leave_requests (
    id                  BIGSERIAL PRIMARY KEY,
    member_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    leave_type          VARCHAR(50) NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    reason              TEXT NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewed_by         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at         TIMESTAMPTZ,
    review_note         TEXT,
    handover_status     VARCHAR(50) NOT NULL DEFAULT 'NONE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT leave_requests_type_check CHECK (leave_type IN
        ('SICK', 'ANNUAL', 'UNPAID', 'MATERNITY', 'PATERNITY', 'BEREAVEMENT', 'OTHER')),
    CONSTRAINT leave_requests_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT leave_requests_handover_status_check CHECK (handover_status IN ('NONE', 'PARTIAL', 'COMPLETE')),
    CONSTRAINT leave_requests_dates_check CHECK (end_date >= start_date)
);

CREATE INDEX idx_leave_requests_member_id ON leave_requests(member_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);

CREATE TABLE handover_suggestions (
    id                      BIGSERIAL PRIMARY KEY,
    leave_request_id        BIGINT NOT NULL REFERENCES leave_requests(id) ON DELETE CASCADE,
    task_id                 BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    step_id                 BIGINT REFERENCES steps(id) ON DELETE CASCADE,
    entity_type             VARCHAR(50) NOT NULL,
    current_deadline        TIMESTAMPTZ,
    suggested_assignee_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action                  VARCHAR(50) NOT NULL DEFAULT 'REASSIGN',
    confirmed                BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_assignee_id    BIGINT REFERENCES users(id) ON DELETE SET NULL,
    confirmed_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT handover_suggestions_entity_type_check CHECK (entity_type IN ('FAST_TASK', 'STEP')),
    CONSTRAINT handover_suggestions_action_check CHECK (action IN ('REASSIGN', 'EXTEND_DEADLINE', 'SKIP'))
);

CREATE INDEX idx_handover_suggestions_leave_request_id ON handover_suggestions(leave_request_id);

-- Same VARCHAR+CHECK bug class as V12/V15 — get the new values in the
-- constraint from the start instead of discovering the gap on first insert.
ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('REMINDER', 'TASK_ASSIGNED', 'TASK_DUE', 'SYSTEM', 'LEAVE_REQUEST', 'HANDOVER'));
