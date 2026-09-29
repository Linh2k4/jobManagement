--liquibase formatted sql

--changeset jobmanagement:V3

-- Enum types for task management
CREATE TYPE time_category AS ENUM ('FAST', 'OFTEN', 'MULTI_STEP');
CREATE TYPE task_status AS ENUM ('PENDING', 'IN_PROGRESS', 'DONE', 'CANCELLED');
CREATE TYPE step_status AS ENUM ('PENDING', 'IN_PROGRESS', 'DONE');
CREATE TYPE organize_by_type AS ENUM ('SECTION', 'MONTH');

-- Task types (configurable by Manager)
CREATE TABLE task_types (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    time_category       time_category NOT NULL,
    has_estimate        BOOLEAN NOT NULL DEFAULT TRUE,
    default_estimate_minutes INT,
    is_multi_step       BOOLEAN NOT NULL DEFAULT FALSE,
    organize_by         organize_by_type,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_types_code ON task_types(code);
CREATE INDEX idx_task_types_time_category ON task_types(time_category);
CREATE INDEX idx_task_types_is_active ON task_types(is_active);

COMMENT ON TABLE task_types IS 'Configurable task type templates (Fast/Often/Multi-step)';
COMMENT ON COLUMN task_types.time_category IS 'FAST: same-day, OFTEN: recurring by section, MULTI_STEP: organized by month';

-- Task type steps (templates for multi-step tasks)
CREATE TABLE task_type_steps (
    id              BIGSERIAL PRIMARY KEY,
    task_type_id    BIGINT NOT NULL REFERENCES task_types(id) ON DELETE CASCADE,
    step_order      INT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    estimate_minutes INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_type_id, step_order)
);

CREATE INDEX idx_task_type_steps_task_type_id ON task_type_steps(task_type_id);

COMMENT ON TABLE task_type_steps IS 'Step templates for multi-step task types; instances copied to task_steps when a task is created';

-- Tasks
CREATE TABLE tasks (
    id              BIGSERIAL PRIMARY KEY,
    task_type_id    BIGINT NOT NULL REFERENCES task_types(id),
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status          task_status NOT NULL DEFAULT 'PENDING',
    estimate_minutes INT,
    due_date        DATE,
    section         VARCHAR(255),
    period_month    DATE,
    created_by      BIGINT NOT NULL REFERENCES users(id),
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_task_type_id ON tasks(task_type_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_created_by ON tasks(created_by);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_section ON tasks(section);
CREATE INDEX idx_tasks_period_month ON tasks(period_month);

COMMENT ON TABLE tasks IS 'Individual task instances';
COMMENT ON COLUMN tasks.estimate_minutes IS 'NULL means no estimate; drives KPI exclusion and warning triggers';
COMMENT ON COLUMN tasks.created_by IS 'Who created/originated the task (audit trail)';

-- Task instances of task_type_steps
CREATE TABLE task_steps (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    task_type_step_id BIGINT REFERENCES task_type_steps(id) ON DELETE SET NULL,
    step_order      INT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    estimate_minutes INT,
    status          step_status NOT NULL DEFAULT 'PENDING',
    assignee_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (task_id, step_order)
);

CREATE INDEX idx_task_steps_task_id ON task_steps(task_id);
CREATE INDEX idx_task_steps_assignee_id ON task_steps(assignee_id);

COMMENT ON TABLE task_steps IS 'Per-task step instances; instantiated from task_type_steps template but can diverge';

-- Task assignments (tracks who is assigned, who assigned them, supports reassignment history)
CREATE TABLE task_assignments (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    assignee_id     BIGINT NOT NULL REFERENCES users(id),
    assigned_by     BIGINT NOT NULL REFERENCES users(id),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_current      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_task_assignments_task_id ON task_assignments(task_id);
CREATE INDEX idx_task_assignments_assignee_id ON task_assignments(assignee_id);
CREATE INDEX idx_task_assignments_assigned_by ON task_assignments(assigned_by);
CREATE UNIQUE INDEX uq_task_assignments_current ON task_assignments(task_id) WHERE is_current = true;

COMMENT ON TABLE task_assignments IS 'Assignment history; only one row per task has is_current=true at any time';
COMMENT ON COLUMN task_assignments.assigned_by IS 'Who made the assignment (enables audit of who assigned to whom)';
