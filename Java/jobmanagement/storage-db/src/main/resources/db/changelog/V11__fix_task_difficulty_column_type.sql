--liquibase formatted sql

--changeset jobmanagement:V11

-- V5 created tasks.difficulty as INT (1-5), but the Task entity maps it with
-- @Enumerated(EnumType.STRING) (Difficulty.VERY_EASY..VERY_HARD) — Hibernate
-- expects VARCHAR. Masked until now by ddl-auto:update silently altering the
-- column type on every boot. Convert the numeric levels to the enum names.

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_difficulty_check;

ALTER TABLE tasks ALTER COLUMN difficulty TYPE VARCHAR(50) USING (
    CASE difficulty
        WHEN 1 THEN 'VERY_EASY'
        WHEN 2 THEN 'EASY'
        WHEN 3 THEN 'MEDIUM'
        WHEN 4 THEN 'HARD'
        WHEN 5 THEN 'VERY_HARD'
        ELSE 'MEDIUM'
    END
);

ALTER TABLE tasks ALTER COLUMN difficulty SET DEFAULT 'MEDIUM';
