--liquibase formatted sql

--changeset jobmanagement:V6

-- Create KPI calculation tables (Phase 3)
-- Implements Scope.md §3.4 KPI calculation and caching

CREATE TABLE kpi_configurations (
    id BIGSERIAL PRIMARY KEY,
    period_month VARCHAR(7) NOT NULL UNIQUE,
    wcr_weight NUMERIC(5,2) NOT NULL DEFAULT 60.0 CHECK (wcr_weight >= 0),
    vi_weight NUMERIC(5,2) NOT NULL DEFAULT 25.0 CHECK (vi_weight >= 0),
    ea_weight NUMERIC(5,2) NOT NULL DEFAULT 15.0 CHECK (ea_weight >= 0),
    auto_score_weight NUMERIC(5,2) NOT NULL DEFAULT 40.0 CHECK (auto_score_weight >= 0),
    lead_score_weight NUMERIC(5,2) NOT NULL DEFAULT 35.0 CHECK (lead_score_weight >= 0),
    manager_score_weight NUMERIC(5,2) NOT NULL DEFAULT 25.0 CHECK (manager_score_weight >= 0),
    working_hours_per_day INTEGER NOT NULL DEFAULT 8,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1
);

COMMENT ON TABLE kpi_configurations IS 'KPI formula weights per evaluation period (Scope §3.4)';
COMMENT ON COLUMN kpi_configurations.wcr_weight IS 'Weighted Completion Rate weight (default 60%)';
COMMENT ON COLUMN kpi_configurations.vi_weight IS 'Volume Index weight (default 25%)';
COMMENT ON COLUMN kpi_configurations.ea_weight IS 'Estimate Accuracy weight (default 15%)';

CREATE TABLE kpi_components (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_month VARCHAR(7) NOT NULL,
    wcr NUMERIC(10,2),
    vi NUMERIC(10,2),
    ea NUMERIC(10,2),
    auto_score NUMERIC(10,2),
    lead_score NUMERIC(10,2),
    manager_score NUMERIC(10,2),
    kpi_final NUMERIC(10,2),
    ranking VARCHAR(50),
    previous_month_kpi NUMERIC(10,2),
    trend VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, period_month),
    version BIGINT DEFAULT 1
);

CREATE INDEX idx_kpi_comp_user_period ON kpi_components(user_id, period_month);
CREATE INDEX idx_kpi_comp_period ON kpi_components(period_month);
CREATE INDEX idx_kpi_comp_final ON kpi_components(kpi_final DESC);

COMMENT ON TABLE kpi_components IS 'Real-time KPI calculation results (5-min TTL cache)';
COMMENT ON COLUMN kpi_components.wcr IS 'Weighted Completion Rate (0-100)';
COMMENT ON COLUMN kpi_components.vi IS 'Volume Index (0-120, capped)';
COMMENT ON COLUMN kpi_components.ea IS 'Estimate Accuracy (0-100)';

CREATE TABLE kpi_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lead_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    period_month VARCHAR(7) NOT NULL,
    wcr NUMERIC(10,2),
    vi NUMERIC(10,2),
    ea NUMERIC(10,2),
    auto_score NUMERIC(10,2),
    lead_score NUMERIC(10,2),
    manager_score NUMERIC(10,2),
    kpi_final NUMERIC(10,2) NOT NULL,
    ranking VARCHAR(50),
    is_locked BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, period_month),
    version BIGINT DEFAULT 1
);

CREATE INDEX idx_kpi_snap_period ON kpi_snapshots(period_month);
CREATE INDEX idx_kpi_snap_locked ON kpi_snapshots(is_locked, period_month);

COMMENT ON TABLE kpi_snapshots IS 'Immutable monthly KPI records locked after month-end (Scope §3.4)';
COMMENT ON COLUMN kpi_snapshots.is_locked IS 'Locked=true prevents any modifications after finalization';
