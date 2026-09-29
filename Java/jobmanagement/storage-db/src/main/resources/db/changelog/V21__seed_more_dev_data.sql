--liquibase formatted sql

--changeset jobmanagement:V21 context:dev

-- Dev/local only. V2/V14/V20 seed just enough to boot (users, groups,
-- task types) but leave categories/tasks/evaluations/kpi_snapshots/
-- leave_requests empty, so most feature screens render "0" and the
-- team-KPI-summary lazy-load bug (fixed in KpiSnapshotRepository) never
-- gets exercised end-to-end. Fill in one realistic month (2026-08) across
-- every Lead/Member so the full feature set has something to show.

-- ===== Categories (Scope §2.4) =====

INSERT INTO categories (code, name, description, icon, color, tier, owner_id, owner_name, status, sort_order)
VALUES
    ('CUSTOMER_SUPPORT', 'Chăm sóc khách hàng', 'Email và yêu cầu đột xuất từ khách hàng', 'headset', '#2563eb', 'SYSTEM', 1, 'Admin Manager', 'ACTIVE', 1),
    ('REPORTING', 'Báo cáo', 'Báo cáo định kỳ tuần/tháng', 'bar-chart', '#16a34a', 'SYSTEM', 1, 'Admin Manager', 'ACTIVE', 2),
    ('INTERNAL_OPS', 'Vận hành nội bộ', 'Công việc vận hành nội bộ phòng ban', 'settings', '#d97706', 'SYSTEM', 1, 'Admin Manager', 'ACTIVE', 3);

-- ===== Tasks + assignments =====
-- Member 4 (Lead One / primary group 3 "Group A"), 5 & 8 (Lead One / group 1),
-- 6 & 7 (Lead Two / group 2) — matches group_memberships from V20.

WITH task_rows(code, title, status, category_code, assignee_id, lead_id, group_id, due_date, completed_at) AS (
    VALUES
        ('EMAIL_REPLY', 'Trả lời email khách hàng A', 'DONE', 'CUSTOMER_SUPPORT', 4::bigint, 2::bigint, 1::bigint, DATE '2026-08-05', TIMESTAMPTZ '2026-08-05 09:00:00+07'),
        ('AD_HOC_REQUEST', 'Xử lý yêu cầu đột xuất phòng KD', 'IN_PROGRESS', 'CUSTOMER_SUPPORT', 4::bigint, 2::bigint, 1::bigint, DATE '2026-08-25', NULL),
        ('EMAIL_REPLY', 'Trả lời email nội bộ', 'DONE', 'CUSTOMER_SUPPORT', 5::bigint, 2::bigint, 1::bigint, DATE '2026-08-10', TIMESTAMPTZ '2026-08-10 14:30:00+07'),
        ('WEEKLY_REPORT', 'Báo cáo tuần 33', 'PENDING', 'REPORTING', 5::bigint, 2::bigint, 1::bigint, DATE '2026-08-22', NULL),
        ('EMAIL_REPLY', 'Trả lời email đối tác', 'CANCELLED', 'CUSTOMER_SUPPORT', 6::bigint, 3::bigint, 2::bigint, DATE '2026-08-12', NULL),
        ('AD_HOC_REQUEST', 'Yêu cầu đột xuất từ Manager', 'DONE', 'CUSTOMER_SUPPORT', 6::bigint, 3::bigint, 2::bigint, DATE '2026-08-08', TIMESTAMPTZ '2026-08-08 11:00:00+07'),
        ('WEEKLY_REPORT', 'Báo cáo tuần 34', 'IN_PROGRESS', 'REPORTING', 7::bigint, 3::bigint, 2::bigint, DATE '2026-08-24', NULL),
        ('EMAIL_REPLY', 'Trả lời email hỗ trợ', 'DONE', 'CUSTOMER_SUPPORT', 7::bigint, 3::bigint, 2::bigint, DATE '2026-08-14', TIMESTAMPTZ '2026-08-14 16:00:00+07'),
        ('AD_HOC_REQUEST', 'Xử lý yêu cầu đột xuất kỹ thuật', 'CLOSED_LATE', 'INTERNAL_OPS', 8::bigint, 2::bigint, 1::bigint, DATE '2026-08-01', TIMESTAMPTZ '2026-08-03 10:00:00+07'),
        ('WEEKLY_REPORT', 'Báo cáo tuần 35', 'PENDING', 'REPORTING', 8::bigint, 2::bigint, 1::bigint, DATE '2026-08-26', NULL)
),
inserted_tasks AS (
    INSERT INTO tasks (task_type_id, title, status, category_code, due_date, period_month, group_id, created_by, completed_at, priority, difficulty)
    SELECT tt.id, r.title, r.status, r.category_code, r.due_date, DATE '2026-08-01', r.group_id, r.lead_id, r.completed_at, 'MEDIUM', 'MEDIUM'
    FROM task_rows r
    JOIN task_types tt ON tt.code = r.code
    RETURNING id, title
)
INSERT INTO task_assignments (task_id, assignee_id, assigned_by, is_current)
SELECT it.id, r.assignee_id, r.lead_id, true
FROM inserted_tasks it
JOIN task_rows r ON r.title = it.title;

-- ===== Evaluations (Scope §3.1-3.6) =====
-- Member 4 & 5: fully FINALIZED so the evaluation history/detail screens
-- have a locked record to render. Member 6/7/8: mid-workflow states so the
-- Lead/Manager review queues aren't empty either.

INSERT INTO evaluations (
    user_id, lead_id, manager_id, group_id, period_month,
    self_quality, self_responsibility, self_teamwork, self_initiative, self_discipline,
    lead_quality, lead_responsibility, lead_teamwork, lead_discipline,
    manager_quality, manager_responsibility, manager_initiative, manager_discipline,
    lead_score, manager_score, kpi_final,
    status, is_locked, self_submitted_at, lead_submitted_at, manager_finalized_at
) VALUES
    (4, 2, 1, 1, '2026-08',
     8.0, 8.5, 9.0, 7.5, 8.0,
     8.0, 8.0, 8.5, 8.0,
     8.5, 8.0, 8.0, 8.5,
     82.0, 83.0, 78.5,
     'FINALIZED', true, '2026-08-28 09:00:00+07', '2026-08-29 10:00:00+07', '2026-08-30 15:00:00+07'),
    (5, 2, 1, 1, '2026-08',
     7.0, 7.5, 8.0, 7.0, 7.5,
     7.5, 7.0, 7.5, 7.5,
     7.5, 7.5, 7.0, 8.0,
     74.0, 75.0, 70.0,
     'FINALIZED', true, '2026-08-28 09:10:00+07', '2026-08-29 10:10:00+07', '2026-08-30 15:10:00+07'),
    (6, 3, 1, 2, '2026-08',
     6.5, 7.0, 6.5, 6.0, 7.0,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL,
     'SELF_SUBMITTED', false, '2026-08-28 09:20:00+07', NULL, NULL),
    (7, 3, 1, 2, '2026-08',
     7.5, 7.0, 8.0, 8.0, 7.5,
     7.5, 7.5, 8.0, 7.5,
     NULL, NULL, NULL, NULL,
     76.0, NULL, NULL,
     'LEAD_REVIEWED', false, '2026-08-28 09:30:00+07', '2026-08-29 11:00:00+07', NULL),
    (8, 2, 1, 1, '2026-08',
     NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL, NULL,
     NULL, NULL, NULL,
     'DRAFT', false, NULL, NULL, NULL);

-- ===== KPI snapshots (Scope §3.4) =====
-- Locked snapshots for the period so /api/v1/kpi/team/summary (Manager view)
-- and the Lead's team view have real rows to fetch — this is the exact
-- query path fixed in KpiSnapshotRepository.findLockedSnapshotsByPeriod /
-- findTeamSnapshotsByLead (see storage-db KpiSnapshotRepository).

INSERT INTO kpi_snapshots (user_id, lead_id, group_id, period_month, wcr, vi, ea, auto_score, lead_score, manager_score, kpi_final, ranking, is_locked)
VALUES
    (4, 2, 1, '2026-08', 85.0, 90.0, 80.0, 84.5, 82.0, 83.0, 78.5, 'Tốt', true),
    (5, 2, 1, '2026-08', 70.0, 75.0, 68.0, 71.0, 74.0, 75.0, 70.0, 'Đạt', true),
    (6, 3, 2, '2026-08', 55.0, 60.0, 50.0, 55.0, NULL, NULL, 55.0, 'Cần cải thiện', true),
    (7, 3, 2, '2026-08', 78.0, 80.0, 75.0, 77.5, 76.0, NULL, 77.5, 'Tốt', true),
    (8, 2, 1, '2026-08', 40.0, 45.0, 38.0, 41.0, NULL, NULL, 41.0, 'Không đạt', true);

-- ===== Leave requests (Scope §14) =====

INSERT INTO leave_requests (member_id, leave_type, start_date, end_date, reason, status, reviewed_by, reviewed_at, review_note, handover_status)
VALUES
    (4, 'SICK', DATE '2026-08-25', DATE '2026-08-26', 'Sốt virus, cần nghỉ 2 ngày', 'PENDING', NULL, NULL, NULL, 'NONE'),
    (6, 'ANNUAL', DATE '2026-08-15', DATE '2026-08-16', 'Nghỉ phép năm về quê', 'APPROVED', 3, '2026-08-10 08:00:00+07', 'Đã bàn giao công việc đầy đủ', 'COMPLETE');
