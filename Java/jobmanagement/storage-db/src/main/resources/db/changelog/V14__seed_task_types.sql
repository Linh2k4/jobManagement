--liquibase formatted sql

--changeset jobmanagement:V14 context:dev

-- Dev/local only — without this, GET /task-types (used by the task-create
-- form's dropdown) returns an empty list on a fresh database and the
-- feature is unusable end-to-end.

INSERT INTO task_types (code, name, time_category, has_estimate, default_estimate_minutes, is_multi_step, organize_by, is_active) VALUES
    ('EMAIL_REPLY', 'Trả lời email', 'FAST', true, 30, false, NULL, true),
    ('AD_HOC_REQUEST', 'Yêu cầu đột xuất', 'FAST', true, 60, false, NULL, true),
    ('WEEKLY_REPORT', 'Báo cáo tuần', 'OFTEN', true, 120, false, 'SECTION', true),
    ('MONTHLY_REPORT', 'Báo cáo tháng', 'MULTI_STEP', true, 480, true, 'MONTH', true);

INSERT INTO task_type_steps (task_type_id, step_order, name, estimate_minutes)
SELECT id, 1, 'Thu thập dữ liệu', 120 FROM task_types WHERE code = 'MONTHLY_REPORT'
UNION ALL
SELECT id, 2, 'Soạn thảo báo cáo', 180 FROM task_types WHERE code = 'MONTHLY_REPORT'
UNION ALL
SELECT id, 3, 'Lead duyệt', 60 FROM task_types WHERE code = 'MONTHLY_REPORT'
UNION ALL
SELECT id, 4, 'Nộp báo cáo', 30 FROM task_types WHERE code = 'MONTHLY_REPORT';
