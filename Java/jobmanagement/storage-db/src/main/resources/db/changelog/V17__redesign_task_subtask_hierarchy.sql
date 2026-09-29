--liquibase formatted sql

--changeset jobmanagement:V17

-- Scope.md §2.1/§2.2: replace the flat Task -> TaskStep model with a real
-- hierarchy. FAST tasks get GroupSubtask -> Subtask (Subtask is the leaf,
-- ticked done manually). MULTI_STEP tasks get GroupSubtask -> Subtask ->
-- Step (Step is the leaf, has its own assignee/deadline, cascades completion
-- up through Subtask to Task). No production data exists yet for either
-- table being dropped here (task creation only shipped this session, and
-- all task_steps rows were test data already cleaned up) — a clean replace
-- avoids carrying a second, half-obsolete step model alongside the new one.

DROP TABLE IF EXISTS task_steps CASCADE;
DROP TABLE IF EXISTS task_type_steps CASCADE;

-- ===== Template layer (TaskType, MULTI_STEP only) =====

CREATE TABLE task_type_group_subtasks (
    id              BIGSERIAL PRIMARY KEY,
    task_type_id    BIGINT NOT NULL REFERENCES task_types(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    group_order     INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_type_id, group_order)
);

CREATE INDEX idx_task_type_group_subtasks_task_type_id ON task_type_group_subtasks(task_type_id);

CREATE TABLE task_type_subtasks (
    id                          BIGSERIAL PRIMARY KEY,
    task_type_id                BIGINT NOT NULL REFERENCES task_types(id) ON DELETE CASCADE,
    task_type_group_subtask_id  BIGINT REFERENCES task_type_group_subtasks(id) ON DELETE CASCADE,
    title                       VARCHAR(255) NOT NULL,
    subtask_order               INT NOT NULL,
    is_target                   BOOLEAN NOT NULL DEFAULT FALSE,
    estimate_target             INT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_type_subtasks_task_type_id ON task_type_subtasks(task_type_id);
CREATE INDEX idx_task_type_subtasks_group_id ON task_type_subtasks(task_type_group_subtask_id);

CREATE TABLE task_type_steps (
    id                      BIGSERIAL PRIMARY KEY,
    task_type_subtask_id    BIGINT NOT NULL REFERENCES task_type_subtasks(id) ON DELETE CASCADE,
    step_order              INT NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    estimate_minutes        INT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_type_subtask_id, step_order)
);

CREATE INDEX idx_task_type_steps_subtask_id ON task_type_steps(task_type_subtask_id);

COMMENT ON TABLE task_type_group_subtasks IS 'Template group headers for MULTI_STEP task types (Scope.md §2.2 Template)';
COMMENT ON TABLE task_type_subtasks IS 'Template subtasks, grouped or ungrouped (task_type_group_subtask_id nullable)';
COMMENT ON TABLE task_type_steps IS 'Template steps, cloned into steps when a MULTI_STEP task is created from this type';

-- ===== Instance layer (Task) =====

CREATE TABLE group_subtasks (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    group_order INT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_group_subtasks_task_id ON group_subtasks(task_id);

CREATE TABLE subtasks (
    id                  BIGSERIAL PRIMARY KEY,
    task_id             BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    group_subtask_id    BIGINT REFERENCES group_subtasks(id) ON DELETE CASCADE,
    subtask_order       INT NOT NULL,
    title               VARCHAR(255) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- FAST-only: Subtask is the leaf, ticked done manually
    estimate_minutes    INT,
    deadline            TIMESTAMPTZ,
    note                TEXT,
    -- MULTI_STEP-only: Subtask has its own target tracking (Scope.md §2.2 isTarget)
    is_target           BOOLEAN NOT NULL DEFAULT FALSE,
    estimate_target      INT,
    target              INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT subtasks_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX idx_subtasks_task_id ON subtasks(task_id);
CREATE INDEX idx_subtasks_group_id ON subtasks(group_subtask_id);
CREATE INDEX idx_subtasks_status ON subtasks(status);

CREATE TABLE steps (
    id                  BIGSERIAL PRIMARY KEY,
    subtask_id          BIGINT NOT NULL REFERENCES subtasks(id) ON DELETE CASCADE,
    task_type_step_id   BIGINT REFERENCES task_type_steps(id) ON DELETE SET NULL,
    step_order          INT NOT NULL,
    name                VARCHAR(255) NOT NULL,
    assignee_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    estimate_minutes    INT,
    deadline            TIMESTAMPTZ,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT steps_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX idx_steps_subtask_id ON steps(subtask_id);
CREATE INDEX idx_steps_assignee_id ON steps(assignee_id);

COMMENT ON TABLE group_subtasks IS 'Optional group headers under a Task (Scope.md §2.1/§2.2)';
COMMENT ON TABLE subtasks IS 'Leaf for FAST tasks (manual done); parent-of-steps for MULTI_STEP tasks';
COMMENT ON TABLE steps IS 'Leaf for MULTI_STEP tasks only; sequential completion within a subtask cascades up';

-- ===== Task-level target tracking (Scope.md §2.2 isTarget) =====

ALTER TABLE tasks ADD COLUMN is_target BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tasks ADD COLUMN estimate_target INT;
ALTER TABLE tasks ADD COLUMN target INT;
