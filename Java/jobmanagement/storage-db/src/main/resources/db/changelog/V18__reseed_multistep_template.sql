--liquibase formatted sql

--changeset jobmanagement:V18 context:dev

-- V17 dropped task_type_steps (and the rows V14 seeded into it) to rebuild
-- it under the new group/subtask/step template hierarchy. Re-seed the
-- MONTHLY_REPORT template with an equivalent tree so task creation from
-- this type still has something to clone (Scope.md §2.2).

INSERT INTO task_type_group_subtasks (task_type_id, name, group_order)
SELECT id, 'Thực hiện báo cáo', 1 FROM task_types WHERE code = 'MONTHLY_REPORT';

INSERT INTO task_type_subtasks (task_type_id, task_type_group_subtask_id, title, subtask_order, is_target, estimate_target)
SELECT tt.id, g.id, 'Soạn báo cáo tháng', 1, false, NULL
FROM task_types tt
JOIN task_type_group_subtasks g ON g.task_type_id = tt.id AND g.name = 'Thực hiện báo cáo'
WHERE tt.code = 'MONTHLY_REPORT';

INSERT INTO task_type_steps (task_type_subtask_id, step_order, name, estimate_minutes)
SELECT s.id, 1, 'Thu thập dữ liệu', 120
FROM task_type_subtasks s
JOIN task_types tt ON tt.id = s.task_type_id
WHERE tt.code = 'MONTHLY_REPORT' AND s.title = 'Soạn báo cáo tháng'
UNION ALL
SELECT s.id, 2, 'Soạn thảo báo cáo', 180
FROM task_type_subtasks s
JOIN task_types tt ON tt.id = s.task_type_id
WHERE tt.code = 'MONTHLY_REPORT' AND s.title = 'Soạn báo cáo tháng'
UNION ALL
SELECT s.id, 3, 'Lead duyệt', 60
FROM task_type_subtasks s
JOIN task_types tt ON tt.id = s.task_type_id
WHERE tt.code = 'MONTHLY_REPORT' AND s.title = 'Soạn báo cáo tháng'
UNION ALL
SELECT s.id, 4, 'Nộp báo cáo', 30
FROM task_type_subtasks s
JOIN task_types tt ON tt.id = s.task_type_id
WHERE tt.code = 'MONTHLY_REPORT' AND s.title = 'Soạn báo cáo tháng';
