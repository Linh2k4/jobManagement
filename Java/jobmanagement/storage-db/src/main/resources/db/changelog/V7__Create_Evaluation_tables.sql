--liquibase formatted sql

--changeset jobmanagement:V7

-- Create evaluation workflow tables (Phase 3)
-- Implements Scope.md §3.1-3.6 Evaluation Period and Workflow

CREATE TABLE evaluation_periods (
    id BIGSERIAL PRIMARY KEY,
    period_month VARCHAR(7) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    open_date DATE NOT NULL,
    member_deadline DATE NOT NULL,
    lead_deadline DATE NOT NULL,
    manager_deadline DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1,
    CONSTRAINT eval_period_status_check CHECK (status IN ('DRAFT', 'OPEN', 'SUBMITTED', 'REVIEWED', 'FINALIZED'))
);

CREATE INDEX idx_eval_period_status ON evaluation_periods(status);
CREATE INDEX idx_eval_period_month ON evaluation_periods(period_month);

COMMENT ON TABLE evaluation_periods IS 'Evaluation period lifecycle management (Scope §3.1)';
COMMENT ON COLUMN evaluation_periods.status IS 'DRAFT→OPEN→SUBMITTED→REVIEWED→FINALIZED';

CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lead_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    manager_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_month VARCHAR(7) NOT NULL,
    -- Member self-scores (1-10)
    self_quality NUMERIC(3,1),
    self_responsibility NUMERIC(3,1),
    self_teamwork NUMERIC(3,1),
    self_initiative NUMERIC(3,1),
    self_discipline NUMERIC(3,1),
    self_notes TEXT,
    -- Lead evaluation scores (1-10)
    lead_quality NUMERIC(3,1),
    lead_responsibility NUMERIC(3,1),
    lead_teamwork NUMERIC(3,1),
    lead_discipline NUMERIC(3,1),
    lead_notes TEXT,
    -- Manager evaluation scores (1-10)
    manager_quality NUMERIC(3,1),
    manager_responsibility NUMERIC(3,1),
    manager_initiative NUMERIC(3,1),
    manager_discipline NUMERIC(3,1),
    manager_notes TEXT,
    -- Calculated scores (0-100)
    lead_score NUMERIC(10,2),
    manager_score NUMERIC(10,2),
    kpi_final NUMERIC(10,2),
    -- Workflow state
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    is_locked BOOLEAN DEFAULT false,
    self_submitted_at TIMESTAMPTZ,
    lead_submitted_at TIMESTAMPTZ,
    manager_finalized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, period_month),
    version BIGINT DEFAULT 1,
    CONSTRAINT eval_status_check CHECK (status IN ('DRAFT', 'SELF_SUBMITTED', 'LEAD_REVIEWED', 'FINALIZED'))
);

CREATE INDEX idx_eval_status ON evaluations(status);
CREATE INDEX idx_eval_period ON evaluations(period_month);
CREATE INDEX idx_eval_user_period ON evaluations(user_id, period_month);
CREATE INDEX idx_eval_locked ON evaluations(is_locked);

COMMENT ON TABLE evaluations IS 'Member → Lead → Manager evaluation workflow (Scope §3.1-3.6)';
COMMENT ON COLUMN evaluations.status IS 'DRAFT→SELF_SUBMITTED→LEAD_REVIEWED→FINALIZED (state machine)';
COMMENT ON COLUMN evaluations.is_locked IS 'Locked=true after manager finalizes, prevents edits';
COMMENT ON COLUMN evaluations.lead_score IS 'Average of lead criteria (0-100)';
COMMENT ON COLUMN evaluations.manager_score IS 'Average of manager criteria (0-100)';
COMMENT ON COLUMN evaluations.kpi_final IS 'Final KPI: (Auto×40% + Lead×35% + Mgr×25%)';
