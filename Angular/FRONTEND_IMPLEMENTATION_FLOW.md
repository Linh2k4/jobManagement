# JobManagement Frontend — Implementation Flow (Scope-Based)

**Status:** Ready to Implement  
**API Reference:** [API_DOCUMENTATION.md](../Java/JobManagement/API_DOCUMENTATION.md)  
**Scope Reference:** [Scope.md](../Java/JobManagement/Scope.md)

---

## Phase 1: Core Data Layers (Weeks 1-2)

### 1.1 API Client Layer

**Implementation:** Generate from OpenAPI spec via existing `generate-api-client.sh`

**Endpoints to implement:**

```typescript
// KPI Module (6 endpoints)
GET /kpi
GET /kpi/{userId}
GET /kpi/{userId}/history
GET /kpi/team/summary
GET /admin/kpi/config
PUT /admin/kpi/config

// Evaluation Module (8 endpoints)
POST /evaluations
GET /evaluations/{id}
PUT /evaluations/{id}/self-review
PUT /evaluations/{id}/lead-review
PUT /evaluations/{id}/finalize
GET /evaluations/my
GET /evaluations/team
GET /evaluations/all

// Category Module (8 endpoints)
POST /categories
GET /categories
GET /categories/{id}
PUT /categories/{id}
PATCH /categories/{id}/status
DELETE /categories/{id}
POST /categories/{id}/extra-fields
PUT /categories/{id}/extra-fields/{fieldId}

// Task Module (5 core endpoints)
POST /tasks
GET /tasks/{id}
GET /tasks/search
PUT /tasks/{id}
PATCH /tasks/{id}/status
```

**Files:** 
- `src/app/core/services/api/` (auto-generate)
- `src/app/core/models/` (types)

---

### 1.2 State Management Layer (NgRx/Akita)

**Entities to model:**

```typescript
// Store structure
/store
  /kpi
    kpi.state.ts          // KPI calculations, trends, rankings
    kpi.actions.ts        // Load, Calculate, InvalidateCache
    kpi.reducer.ts
    kpi.selectors.ts      // getCurrentUserKPI, getTeamSummary
    
  /evaluation
    evaluation.state.ts   // State machine: DRAFT→SELF→LEAD→FINALIZED
    evaluation.actions.ts // SubmitSelf, SubmitLead, Finalize
    evaluation.reducer.ts
    evaluation.selectors.ts
    
  /task
    task.state.ts         // Task list, filters, pagination
    task.actions.ts       // Load, Create, UpdateStatus, UpdateDependency
    task.reducer.ts
    task.selectors.ts
    
  /category
    category.state.ts     // System + Custom categories
    category.actions.ts   // Load, Create, AddExtraField
    category.reducer.ts
    category.selectors.ts
```

**Key features:**
- Async reducers for API calls (NgRx effects/Akita actions)
- State normalization for task lists (by status, by category)
- Selectors for derived data (KPI trend calculation, task grouping)
- Cache invalidation on task state changes (affects KPI)

---

### 1.3 Service Layer

**Core services:**

```typescript
// src/app/core/services/
kpi.service.ts
  - getCurrentUserKPI()
  - getTeamKPISummary(page, size)
  - getKPIHistory(userId)
  - updateKPIConfig(config)

evaluation.service.ts
  - createEvaluation(userId, periodMonth)
  - getEvaluation(id)
  - submitSelfReview(id, scores)
  - submitLeadReview(id, scores)
  - finalizeEvaluation(id, scores)
  - validateTransition(currentStatus, action)  // State machine guard

task.service.ts
  - createTask(taskData)
  - updateTaskStatus(taskId, newStatus)
  - validateStepDependencies(stepId)  // Multi-step unlock logic
  - calculateTaskProgress(taskId)     // (DonSteps / TotalSteps) %

category.service.ts
  - getCategories()                    // System + user's custom
  - createCategory(tier, data)
  - addExtraField(categoryId, field)
  - validateExtraFields(categoryId, values)  // Type checking

notification.service.ts
  - getNotifications()
  - markAsRead(id)
  - subscribeToTaskNotifications()     // WebSocket or polling
```

---

## Phase 2: UI Components (Weeks 2-4)

### 2.1 Dashboard Modules (Role-Based)

**Scope §5.1 → 3 role-specific dashboards**

```typescript
// src/app/features/dashboard/
member-dashboard/
  - my-tasks-today.component         // Fast + Often today
  - task-progress.component          // In-progress / overdue highlight
  - kpi-card.component              // Real-time KPI of current month
  - reminders-widget.component      // Upcoming reminders
  - weekly-stats.component          // Done/overdue/pending bars

lead-dashboard/
  - team-overview.component         // Status distribution (pie chart)
  - team-members-kpi.component      // KPI rankings with bars
  - overdue-tasks.component         // Red-flagged task list per member
  - multi-step-timeline.component   // Gantt view of ongoing multi-steps
  - workload-alerts.component       // Members > 1.5× avg warning

manager-dashboard/
  - system-overview.component       // All teams, all leads
  - kpi-group-summary.component     // KPI by group (table + charts)
  - progress-trend.component        // Monthly/quarterly trend lines
  - contract-expiry-alerts.component // 30-day warnings
  - system-alerts.component         // Config issues, missing estimates
```

**Key visualizations:**
- Donut chart (task completion %)
- Bar chart (team member performance)
- Gantt/timeline (multi-step progress)
- Trend lines (KPI month-over-month)

---

### 2.2 Task Management Components

**Scope §2 → Fast / Often / Multi-step task workflows**

```typescript
// src/app/features/tasks/
task-create-modal/
  - form-builder.component          // Render form based on task type
  - category-selector.component     // Load extra fields for selected category
  - estimate-input.component        // Validate estimate > 0 (KPI)
  - deadline-calculator.component   // Auto-calc based on priority offset
  - step-builder.component          // Multi-step: define steps, deps, persons

task-list/
  - task-filters.component          // By status, category, priority, assignee
  - task-card.component             // Show: title, priority, deadline, category icon
  - task-progress-bar.component     // Multi-step: visual bar (N steps done)

task-detail/
  - task-header.component           // Title, status badge, priority
  - task-body.component             // Description, category data, extra fields
  - step-timeline.component         // Multi-step: sequential + parallel states
  - dependency-graph.component      // Show: "Step 2 waits for Step 1"
  - comment-thread.component        // Collaborative notes

task-status-update/
  - status-transition.component     // Guard: show only valid next states
  - actual-hours-input.component    // For KPI Estimate Accuracy (EA)
  - step-unlock-trigger.component   // Auto-unlock next sequential step
```

**Key behaviors:**
- Create modal title changes based on task type (Fast vs Often template)
- Extra fields dynamically render per category selection
- Status transitions enforce state machine (Scope §2: Draft→Pending→Progress→Done)
- Multi-step shows "awaiting X" when dependent step not done
- Actual hours input only for completed tasks (EA calculation)

---

### 2.3 Evaluation Workflow Components

**Scope §3 → Member → Lead → Manager state machine**

```typescript
// src/app/features/evaluations/
evaluation-period-banner.component  // "Period open until X, deadline is Y"

evaluation-form/
  - self-evaluation.component       // Member scores self (10 criteria)
  - self-submit-modal.component     // Confirm submit → transition DRAFT→SELF_SUBMITTED
  
  - lead-evaluation.component       // Lead scores member (5-7 criteria)
  - lead-submit-modal.component     // Confirm → LEAD_REVIEWED
  
  - manager-evaluation.component    // Manager scores (5-7 criteria) + KPI preview
  - manager-finalize-modal.component // Chốt → FINALIZED (locked)
  - kpi-calculation-preview.component // Show formula: (Auto×40% + Lead×35% + Mgr×25%)

evaluation-list/
  - my-evaluations.component        // Member: filters by period status
  - team-evaluations.component      // Lead: list of member evaluations
  - all-evaluations.component       // Manager: all user evaluations

evaluation-detail/
  - status-timeline.component       // Show: Draft→Self→Lead→Finalized steps
  - score-comparison.component      // Self vs Lead vs Manager scores
  - kpi-result.component            // Final KPI + ranking badge
```

**State machine guards (Scope §3.6):**
- Self-evaluation must be SUBMITTED before Lead can see/rate
- Lead must SUBMIT before Manager can access finalize button
- After Manager finalizes: evaluation LOCKED (no edits)
- Only Manager can unlock with reason (audit log)

---

### 2.4 Category Management Components

**Scope §6 → System (Manager) + Custom (Lead) categories**

```typescript
// src/app/features/categories/
category-list/
  - system-categories.component     // Tier: SYSTEM (Manager only)
  - custom-categories.component     // Tier: CUSTOM (Lead's own)
  - category-filter.component       // By tier, by owner, by status

category-form/
  - category-editor.component       // Name, description, icon, color
  - allowed-task-types.component    // Checkboxes: FAST, OFTEN, MULTI_STEP
  - extra-fields-builder.component  // Drag-drop form fields (8 types)
  
category-extra-fields/
  - field-type-picker.component     // TEXT, TEXTAREA, NUMBER, DATE, etc.
  - select-options-editor.component // For SELECT/MULTISELECT types
  - field-order-manager.component   // Drag-drop sortOrder

category-preview/
  - preview-task-form.component     // Show: "When user creates task, form looks like..."
```

**Key logic:**
- Lead can only CRUD Custom categories (Custom = owner_id matches)
- System categories: lead sees but cannot edit
- Extra fields cannot be deleted if category has tasks (only new fields allowed)
- Field type cannot change (must create new field)
- visibleInList toggle → column appears in task list view

---

## Phase 3: Advanced Features (Weeks 4-5)

### 3.1 Notification & Reminder System

**Scope §4 → Automated notifications + personal reminders**

```typescript
// src/app/features/notifications/
notification-bell.component
  - unread-count badge
  - notification dropdown (recent 10)
  - mark-all-read button

notification-types:
  - TaskAssigned: "You've been assigned: Task X"
  - TaskDeadlineApproaching: "Task X due in 24h"
  - TaskOverdue: "Task X is overdue (2 days)"
  - StepReadyToStart: "Step Y of task X is ready"
  - EvaluationDeadline: "Self-evaluation due in 24h"
  - ContractExpiring: "Contract ABC expires in 30 days"

reminder-modal/
  - create-reminder-form.component
    - title, message (text)
    - date + time picker
    - recurrence: None / Daily / Weekly
    - link to task (optional)
  - reminder-list.component
    - filters: active, completed, dismissed
    - dismiss or snooze buttons

notification-preferences.component
  - Toggle notifications per event type
  - Email / In-app channel selection
  - Quiet hours configuration
```

**Architecture:**
- In-app notifications: polling API `GET /notifications` every 60s or WebSocket
- Personal reminders: local storage + scheduled checks
- Notification state: stored in store, cleared on app exit

---

### 3.2 KPI Dashboard & Analytics

**Scope §3.4 → KPI formula visualization**

```typescript
// src/app/features/kpi/
kpi-dashboard/
  - kpi-overview-card.component
    - Display: KPI_final, ranking (Xuất sắc/Tốt/Đạt/...)
    - Trend arrow: ↑ vs prev month
    - % change badge: +2.5%
    
  - kpi-components-breakdown.component
    - WCR: (completedWeight / totalWeight) %
    - VI: (effectiveHours / working hours) % [capped 120]
    - EA: (1 - avg error) % [Estimate Accuracy]
    - autoScore = WCR×60% + VI×25% + EA×15%
    
  - kpi-inputs-breakdown.component
    - Lead score: avg of Lead's 5 criteria
    - Manager score: avg of Manager's 5 criteria
    - Display formula: auto×40% + lead×35% + mgr×25%
    
  - kpi-history-chart.component
    - Line chart: KPI_final over 12 months
    - Ranking color coding per threshold
    - Hover: show WCR/VI/EA breakdown for that month

kpi-team-ranking/
  - member-ranking-table.component
    - Columns: Rank, Name, KPI_final, WCR, VI, Trend
    - Sort by KPI_final (desc)
    - Filter by date range

kpi-config-admin/
  - weight-sliders.component       // w_wcr, w_vi, w_ea
  - scoring-weights.component     // W1, W2, W3 (auto, lead, mgr)
  - ranking-thresholds.component  // Xuất sắc ≥90, Tốt ≥75, etc.
  - difficulty-multiplier.component // difficulty 1-5 → weight
```

**Formula implementation:**
```typescript
// src/app/shared/utils/kpi-calculator.ts
export function calculateWCR(tasks: Task[]): number {
  // (Σ completionScore) / (Σ taskWeight) × 100
}

export function calculateVI(tasks: Task[], workingHours: number): number {
  // (Σ effectiveHours of DONE) / workingHours × 100, capped 120
}

export function calculateEA(tasks: Task[]): number {
  // (1 - avg(|actual - estimate| / estimate)) × 100
}

export function calculateAutoScore(wcr, vi, ea, weights): number {
  // wcr × w_wcr + vi × w_vi + ea × w_ea
}

export function calculateKPIFinal(autoScore, leadScore, managerScore, weights): number {
  // autoScore × W1 + leadScore × W2 + managerScore × W3
}
```

---

### 3.3 Multi-Step Task Gantt & Timeline

**Scope §2.3 → Visualize sequential + parallel steps**

```typescript
// src/app/features/tasks/multi-step-timeline/
multi-step-gantt.component
  - Y-axis: Step names (Step 1, Step 2, ...)
  - X-axis: Timeline (start date to deadline)
  - Bars colored by status:
    - Gray: PENDING (awaiting previous)
    - Blue: IN_PROGRESS
    - Green: DONE
    - Red: OVERDUE
  - Dependencies: arrow from Step 1 → Step 2 if Sequential
  - Auto-scroll to today
  - Hover: show step details (assignee, actual duration)

step-detail-panel.component
  - Show current step status
  - Assignee + reassignment button (if Manager)
  - Estimate vs actual hours
  - Dependencies: "Waits for Step 1" or "Parallel with Step 2"
  - "Mark Done" button (if step not already done)
  - Comment thread for step
```

**Key behaviors:**
- Sequential step appears grayed until previous is DONE
- Clicking "Mark Done" on step → automatically unlock next Sequential step
- Parallel steps can be marked Done in any order
- Task progress bar updates: visually show (3 of 5 steps done)

---

## Phase 4: Admin & Configuration (Week 5)

### 4.1 Admin Settings Panel

**Scope §7 → System-wide configuration**

```typescript
// src/app/features/admin/
admin-dashboard/
  ├── task-type-config/
  │   ├── name, icon, color per type
  │   ├── deadline-offset per type (FAST: cuối ngày, OFTEN: +3 ngày)
  │   └── typeMultiplier in KPI formula
  │
  ├── priority-config/
  │   ├── name, color, priorityMultiplier
  │   ├── deadline-offset per priority
  │   └── enable/disable priority levels
  │
  ├── evaluation-criteria-config/
  │   ├── add/edit/remove criteria
  │   ├── assign evaluators (Lead/Manager/Both)
  │   ├── weights (must sum to 100%)
  │   └── preview impact on scoring
  │
  ├── kpi-formula-config/
  │   ├── weight sliders: W1 (auto), W2 (lead), W3 (mgr)
  │   ├── sub-weights: w_wcr, w_vi, w_ea
  │   ├── difficulty multiplier table (1-5 → value)
  │   ├── ranking thresholds (Xuất sắc, Tốt, Đạt, etc.)
  │   └── preview: show how current month's KPI changes
  │
  ├── master-data-management/
  │   ├── CRUD dropdown entries (Công văn type, Priority, etc.)
  │   ├── System vs Custom filter
  │   ├── sortOrder drag-drop
  │   └── mark inactive (not delete if in use)
  │
  └── system-rules/
      ├── max-custom-categories-per-lead (default: 20)
      ├── max-extra-fields-per-category (default: 10)
      ├── max-steps-per-multistep (default: 20)
      ├── max-extensions-per-task (default: 3)
      ├── deadline-offset limits
      └── audit-log viewer (all config changes)
```

---

### 4.2 Audit Logging

**Track all mutations:**

```typescript
// src/app/core/services/audit.service.ts
interface AuditLog {
  id: string
  actor: User
  action: 'CREATE' | 'UPDATE' | 'DELETE'
  entity: 'Task' | 'Evaluation' | 'Category' | 'Config'
  entityId: string
  oldValue: any
  newValue: any
  timestamp: Date
  reason?: string  // Why was difficulty changed, why was task extended
}

// Auto-log on:
// - Task difficulty change (even by same user, show justification)
// - Task deadline extension (log reason)
// - Evaluation state transitions
// - Category field changes
// - Config changes (KPI weights, thresholds, etc.)
```

---

## Phase 5: Testing & Polish (Week 6)

### 5.1 Unit Tests

**Coverage targets (per RULE.md: 70% minimum)**

```
src/app/core/services/
  - kpi.service.spec.ts          (calculateWCR, calculateVI, calculateKPIFinal)
  - evaluation.service.spec.ts   (state transitions, guards)
  - task.service.spec.ts         (dependency validation, auto-unlock)
  - category.service.spec.ts     (extra field validation)

src/app/shared/utils/
  - kpi-calculator.spec.ts       (formula accuracy)
  - task-status-machine.spec.ts  (valid transitions)
  - date-calculator.spec.ts      (deadline offsets per priority)
```

### 5.2 Integration Tests

```
- Task creation → API call → store update → list refresh
- Evaluation workflow → submit self → Lead sees → Lead submits → locked
- Multi-step unlock → Step 1 DONE → Step 2 unlocked
- Category creation → extra fields render in task form
```

### 5.3 E2E Tests (Cypress)

```
- Member dashboard loads KPI real-time
- Task can be created with category + extra fields
- Multi-step task Gantt renders correctly
- Evaluation state machine: blocks Lead from seeing until Self submitted
```

---

## Critical Implementation Order

1. **API client generation** (depends on backend: DONE ✅)
2. **State management** (KPI, Task, Evaluation, Category stores)
3. **Dashboard by role** (Member → Lead → Manager)
4. **Task CRUD with category extra fields**
5. **Evaluation workflow with state machine guards**
6. **Multi-step task Gantt view**
7. **KPI formula visualization**
8. **Notifications & reminders**
9. **Admin configuration panels**
10. **Tests & performance tuning**

---

## Component File Structure

```
src/app/
├── features/
│   ├── dashboard/
│   │   ├── member-dashboard/
│   │   ├── lead-dashboard/
│   │   └── manager-dashboard/
│   ├── tasks/
│   │   ├── task-create-modal/
│   │   ├── task-list/
│   │   ├── task-detail/
│   │   └── multi-step-timeline/
│   ├── evaluations/
│   │   ├── evaluation-form/
│   │   ├── evaluation-list/
│   │   └── evaluation-detail/
│   ├── categories/
│   │   ├── category-list/
│   │   ├── category-form/
│   │   └── extra-fields-builder/
│   ├── notifications/
│   │   ├── notification-bell/
│   │   ├── reminder-modal/
│   │   └── notification-preferences/
│   ├── kpi/
│   │   ├── kpi-dashboard/
│   │   ├── kpi-team-ranking/
│   │   └── kpi-config-admin/
│   └── admin/
│       ├── task-type-config/
│       ├── priority-config/
│       ├── evaluation-criteria-config/
│       ├── kpi-formula-config/
│       ├── master-data-management/
│       └── audit-log-viewer/
├── core/
│   ├── services/
│   │   ├── api/
│   │   ├── kpi.service.ts
│   │   ├── evaluation.service.ts
│   │   ├── task.service.ts
│   │   ├── category.service.ts
│   │   ├── notification.service.ts
│   │   └── audit.service.ts
│   └── guards/
│       ├── evaluation-state.guard.ts
│       └── role-based.guard.ts
├── store/
│   ├── kpi/
│   ├── evaluation/
│   ├── task/
│   └── category/
└── shared/
    ├── utils/
    │   ├── kpi-calculator.ts
    │   ├── task-status-machine.ts
    │   └── date-calculator.ts
    └── models/
        ├── kpi.model.ts
        ├── evaluation.model.ts
        ├── task.model.ts
        └── category.model.ts
```

---

## Known Constraints & Decision Points

1. **State Management Choice**: NgRx vs Akita vs simple service-based
   - Recommendation: **NgRx Effects** for async API handling (evaluation state machine, KPI calculations)

2. **KPI Caching**: Backend caches 5 minutes, frontend should mirror
   - Recommendation: HttpClient with caching interceptor

3. **Real-time Updates**: Notifications could use polling or WebSocket
   - Recommendation: Start with 60s polling, upgrade to WebSocket if load permits

4. **Form Builder for Extra Fields**: Dynamic form generation per category
   - Recommendation: **Angular Reactive Forms** with FormGroup.addControl() / removeControl()

5. **Charts Library**: For KPI trend lines, team rankings, Gantt
   - Recommendation: **ngx-charts** (simple) or **Apache ECharts** (powerful)

6. **Multi-tenant UI**: If leading multiple teams (multiple Leads)
   - Recommendation: Team context switcher in header, store selected team in store

---

**Next Step:** Await code-reviewer feedback, then prioritize Phase 1 implementation.
