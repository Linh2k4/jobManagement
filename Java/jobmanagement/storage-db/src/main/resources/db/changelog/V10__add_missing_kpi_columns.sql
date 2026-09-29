--liquibase formatted sql

--changeset jobmanagement:V10

-- Columns mapped by the JPA entities but never added to the schema — they only
-- ever existed because ddl-auto:update silently patched them in on every boot.
-- Surfaced by switching local/dev to ddl-auto:validate (matching prod).

ALTER TABLE kpi_configurations ADD COLUMN notes TEXT;

ALTER TABLE kpi_snapshots ADD COLUMN notes TEXT;
ALTER TABLE kpi_snapshots ADD COLUMN tasks_completed BIGINT;
ALTER TABLE kpi_snapshots ADD COLUMN tasks_overdue BIGINT;
ALTER TABLE kpi_snapshots ADD COLUMN total_estimate_hours NUMERIC(10,2);
ALTER TABLE kpi_snapshots ADD COLUMN total_actual_hours NUMERIC(10,2);
