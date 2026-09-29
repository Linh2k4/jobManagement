--liquibase formatted sql

--changeset jobmanagement:V20

-- Scope.md §13: a Member can belong to more than one Group, each with an
-- independent Lead and independent KPI. No Group concept exists in the
-- schema today — only the single User.lead FK. This migration adds Group/
-- GroupMembership as an additive layer (User.lead/manager are untouched,
-- so every already-built feature that reads them keeps working exactly as
-- before) and threads group_id through Task/Evaluation/KpiComponent/
-- KpiSnapshot so KPI can actually be split per group instead of merged.

CREATE TABLE groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    lead_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_groups_lead_id ON groups(lead_id);

CREATE TABLE group_memberships (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    member_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (group_id, member_id)
);

CREATE INDEX idx_group_memberships_group_id ON group_memberships(group_id);
CREATE INDEX idx_group_memberships_member_id ON group_memberships(member_id);

COMMENT ON TABLE groups IS 'A Lead-managed team a Member can belong to (Scope.md §13) — additive to User.lead, not a replacement';
COMMENT ON TABLE group_memberships IS 'N-N Member<->Group. is_primary marks the group that stands in for the old single-Lead relationship wherever a feature is not yet group-aware';

-- One Group per existing Lead, one primary membership per existing
-- Member->Lead pairing — makes Group a real, populated concept from the
-- start instead of an empty table nothing points at.
INSERT INTO groups (name, lead_id)
SELECT 'Nhóm của ' || full_name, id FROM users WHERE role = 'LEAD';

INSERT INTO group_memberships (group_id, member_id, is_primary)
SELECT g.id, u.id, true
FROM users u
JOIN groups g ON g.lead_id = u.lead_id
WHERE u.role = 'MEMBER' AND u.lead_id IS NOT NULL;

-- ===== Task.group_id =====

ALTER TABLE tasks ADD COLUMN group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_group_id ON tasks(group_id);

-- Best-effort backfill from the task's current assignee's primary group.
UPDATE tasks t
SET group_id = gm.group_id
FROM task_assignments ta
JOIN group_memberships gm ON gm.member_id = ta.assignee_id AND gm.is_primary = true
WHERE ta.task_id = t.id AND ta.is_current = true;

-- ===== evaluations: split KPI by group, not just by user =====

ALTER TABLE evaluations ADD COLUMN group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL;

UPDATE evaluations e
SET group_id = gm.group_id
FROM group_memberships gm
WHERE gm.member_id = e.user_id AND gm.is_primary = true;

ALTER TABLE evaluations DROP CONSTRAINT IF EXISTS evaluations_user_id_period_month_key;
DROP INDEX IF EXISTS idx_eval_user_period;
CREATE UNIQUE INDEX idx_eval_user_group_period ON evaluations(user_id, group_id, period_month);

-- ===== kpi_components: same "one row per user per period" shape as evaluations =====

ALTER TABLE kpi_components ADD COLUMN group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL;

UPDATE kpi_components kc
SET group_id = gm.group_id
FROM group_memberships gm
WHERE gm.member_id = kc.user_id AND gm.is_primary = true;

ALTER TABLE kpi_components DROP CONSTRAINT IF EXISTS kpi_components_user_id_period_month_key;
DROP INDEX IF EXISTS idx_kpi_comp_user_period;
CREATE UNIQUE INDEX idx_kpi_comp_user_group_period ON kpi_components(user_id, group_id, period_month);

-- ===== kpi_snapshots: same shape again =====

ALTER TABLE kpi_snapshots ADD COLUMN group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL;

UPDATE kpi_snapshots ks
SET group_id = gm.group_id
FROM group_memberships gm
WHERE gm.member_id = ks.user_id AND gm.is_primary = true;

ALTER TABLE kpi_snapshots DROP CONSTRAINT IF EXISTS kpi_snapshots_user_id_period_month_key;
CREATE UNIQUE INDEX idx_kpi_snap_user_group_period ON kpi_snapshots(user_id, group_id, period_month);
