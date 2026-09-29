--liquibase formatted sql

--changeset jobmanagement:V12

-- Every JPA entity maps its enum fields with @Enumerated(EnumType.STRING), which
-- binds query parameters as plain VARCHAR. Postgres native ENUM columns (created
-- by V1/V3) don't accept a VARCHAR in a `=` comparison without an explicit cast
-- ("operator does not exist: task_status = character varying"), so any JPQL
-- query filtering on these columns fails at runtime. This was masked in local/dev
-- because ddl-auto:update silently rewrote these columns to VARCHAR on every
-- boot; prod (ddl-auto:validate, no such rewrite) would fail outright the first
-- time such a query ran. Convert to VARCHAR + CHECK, matching the pattern
-- already used for tasks.priority/difficulty and categories.tier/status.
--
-- task_evaluations.evaluator_type is intentionally left as a native enum: no
-- JPA entity maps that (orphaned) table, so nothing binds a mismatched
-- parameter type against it.

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50) USING (role::text);
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('MANAGER', 'LEAD', 'MEMBER'));

ALTER TABLE task_types ALTER COLUMN time_category TYPE VARCHAR(50) USING (time_category::text);
ALTER TABLE task_types ADD CONSTRAINT task_types_time_category_check CHECK (time_category IN ('FAST', 'OFTEN', 'MULTI_STEP'));

ALTER TABLE task_types ALTER COLUMN organize_by TYPE VARCHAR(50) USING (organize_by::text);
ALTER TABLE task_types ADD CONSTRAINT task_types_organize_by_check CHECK (organize_by IS NULL OR organize_by IN ('SECTION', 'MONTH'));

ALTER TABLE tasks ALTER COLUMN status DROP DEFAULT;
ALTER TABLE tasks ALTER COLUMN status TYPE VARCHAR(50) USING (status::text);
ALTER TABLE tasks ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE tasks ADD CONSTRAINT tasks_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE', 'CANCELLED', 'CLOSED_LATE'));

ALTER TABLE task_steps ALTER COLUMN status DROP DEFAULT;
ALTER TABLE task_steps ALTER COLUMN status TYPE VARCHAR(50) USING (status::text);
ALTER TABLE task_steps ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE task_steps ADD CONSTRAINT task_steps_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE'));

-- Types are now unused by any JPA-mapped column (task_evaluations.evaluator_type
-- still uses its own separate evaluator_type type, untouched above).
DROP TYPE user_role;
DROP TYPE time_category;
DROP TYPE organize_by_type;
DROP TYPE task_status;
DROP TYPE step_status;
