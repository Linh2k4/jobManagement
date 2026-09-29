import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { AuthStore } from '../../../../core/stores/auth.store';
import { TaskService } from '../../../../core/services/api/task.service';
import { TaskTypeService } from '../../../../core/services/api/task-type.service';
import { KanbanBoard, KanbanColumn, KanbanTask, TaskTypeSummary, TimeCategory } from '../../../../core/models';

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  template: `
    <div class="kanban-wrapper">
      <!-- FILTERS -->
      <div class="filter-card">
        <div class="search-box">
          <span class="icon search-icon">search</span>
          <input type="text" [(ngModel)]="search" (change)="load()" placeholder="Tìm kiếm theo tiêu đề công việc...">
        </div>
        <div class="filter-controls">
          <select [(ngModel)]="typeFilter" (change)="load()" class="type-select">
            <option [ngValue]="undefined">Tất cả phân loại</option>
            <option value="FAST">Fast Task</option>
            <option value="OFTEN">Often Task</option>
            <option value="MULTI_STEP">Multi-step Task</option>
          </select>
          <label class="toggle-chip" *ngIf="!authStore.isMember()">
            <input type="checkbox" [(ngModel)]="myTasksOnly" (change)="load()">
            <span>Chỉ của tôi</span>
          </label>
        </div>
      </div>

      <!-- BOARD COLUMNS -->
      <div class="board-grid" *ngIf="board as b">
        <!-- TODO COLUMN -->
        <div class="column-card">
          <div class="column-header todo">
            <div class="col-title">
              <span class="dot todo-dot"></span>
              <h4>Chờ thực hiện</h4>
            </div>
            <span class="col-count">{{ b.todo.length }}</span>
          </div>

          <!-- QUICK ADD -->
          <div class="quick-add-box">
            <select [(ngModel)]="quickAddType.todo" class="quick-select">
              <option [ngValue]="null" disabled selected>Loại...</option>
              <option *ngFor="let t of taskTypes" [ngValue]="t.id">{{ t.name }}</option>
            </select>
            <input
              type="text"
              placeholder="+ Thêm nhanh task..."
              [(ngModel)]="quickAddTitle.todo"
              (keyup.enter)="quickAdd('todo')"
              class="quick-input"
            >
          </div>

          <div class="cards-list" cdkDropList #todoList="cdkDropList" [cdkDropListData]="b.todo"
               [cdkDropListConnectedTo]="[doingList, doneList]" (cdkDropListDropped)="onDrop($event, 'todo')">
            <div class="kanban-task-card" *ngFor="let card of b.todo" cdkDrag [cdkDragData]="card"
                 [ngClass]="accentClass(card)">
              <ng-container *ngTemplateOutlet="cardTpl; context: { card }"></ng-container>
            </div>
            <div *ngIf="b.todo.length === 0" class="col-empty">Chưa có việc nào</div>
          </div>
        </div>

        <!-- DOING COLUMN -->
        <div class="column-card">
          <div class="column-header doing">
            <div class="col-title">
              <span class="dot doing-dot"></span>
              <h4>Đang thực hiện</h4>
            </div>
            <span class="col-count">{{ b.doing.length }}</span>
          </div>

          <div class="cards-list" cdkDropList #doingList="cdkDropList" [cdkDropListData]="b.doing"
               [cdkDropListConnectedTo]="[todoList, doneList]" (cdkDropListDropped)="onDrop($event, 'doing')">
            <div class="kanban-task-card" *ngFor="let card of b.doing" cdkDrag [cdkDragData]="card"
                 [ngClass]="[accentClass(card), card.isOverdue ? 'overdue' : '']">
              <ng-container *ngTemplateOutlet="cardTpl; context: { card }"></ng-container>
            </div>
            <div *ngIf="b.doing.length === 0" class="col-empty">Không có việc đang làm</div>
          </div>
        </div>

        <!-- DONE COLUMN -->
        <div class="column-card">
          <div class="column-header done">
            <div class="col-title">
              <span class="dot done-dot"></span>
              <h4>Hoàn thành</h4>
            </div>
            <span class="col-count">{{ b.done.length }}</span>
          </div>

          <div class="cards-list" cdkDropList #doneList="cdkDropList" [cdkDropListData]="b.done"
               [cdkDropListConnectedTo]="[todoList, doingList]" (cdkDropListDropped)="onDrop($event, 'done')">
            <div class="kanban-task-card" *ngFor="let card of b.done" cdkDrag [cdkDragData]="card"
                 [ngClass]="accentClass(card)">
              <ng-container *ngTemplateOutlet="cardTpl; context: { card }"></ng-container>
            </div>
            <div *ngIf="b.done.length === 0" class="col-empty">Chưa có việc hoàn thành</div>
          </div>
        </div>
      </div>

      <!-- LOADING -->
      <div *ngIf="!board" class="state-card">
        <div class="spinner"></div>
        <p>Đang tải bảng công việc...</p>
      </div>
    </div>

    <!-- CARD TEMPLATE -->
    <ng-template #cardTpl let-card="card">
      <div class="card-top">
        <span class="task-type-badge" [ngClass]="card.timeCategory">{{ typeLabel(card.timeCategory) }}</span>
        <div class="card-status-badges">
          <span class="priority-badge" *ngIf="card.priority" [ngClass]="'priority-' + card.priority">{{ card.priority }}</span>
          <span class="badge-alert overdue" *ngIf="card.isOverdue">QUÁ HẠN</span>
          <span class="badge-alert late" *ngIf="card.status === 'CLOSED_LATE'">TRỄ HẠN</span>
        </div>
      </div>

      <div class="card-title">{{ card.title }}</div>

      <div class="card-progress" *ngIf="card.stepsTotal > 0">
        <div class="progress-track">
          <div class="progress-fill" [style.width.%]="(card.stepsDone / card.stepsTotal) * 100"></div>
        </div>
        <span class="progress-label">{{ card.stepsDone }}/{{ card.stepsTotal }} bước</span>
      </div>

      <div class="card-footer">
        <div class="card-meta">
          <span class="meta-item" *ngIf="card.commentCount" title="Bình luận"><span class="icon">chat_bubble</span> {{ card.commentCount }}</span>
          <span class="meta-item" *ngIf="card.attachmentCount" title="Đính kèm"><span class="icon">attach_file</span> {{ card.attachmentCount }}</span>
          <span class="meta-item" *ngIf="card.dueDate" title="Hạn chót" [class.due-urgent]="card.isOverdue">
            <span class="icon">schedule</span> {{ card.dueDate | date:'dd/MM' }}
          </span>
        </div>
        <div class="card-user-action">
          <span class="assignee-avatar" *ngIf="card.assignee" [title]="card.assignee.fullName">
            {{ initials(card.assignee.fullName) }}
          </span>
          <button class="btn-card-detail" (click)="router.navigate(['/tasks', card.id])" title="Xem chi tiết">
            <span class="icon">chevron_right</span>
          </button>
        </div>
      </div>
    </ng-template>
  `,
  styles: [`
    .kanban-wrapper {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .filter-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 12px 16px;
      display: flex;
      gap: 12px;
      box-shadow: var(--shadow-sm);
      align-items: center;
      flex-wrap: wrap;
    }
    .search-box {
      flex: 1;
      min-width: 260px;
      position: relative;
      display: flex;
      align-items: center;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      color: #94a3b8;
      font-size: 20px;
    }
    .search-box input {
      padding-left: 40px !important;
    }
    .filter-controls {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .type-select {
      width: 180px;
    }
    .toggle-chip {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      font-size: 13px;
      font-weight: 600;
      color: var(--color-text);
      cursor: pointer;
      user-select: none;
    }
    .toggle-chip input {
      width: auto !important;
      cursor: pointer;
    }

    /* BOARD */
    .board-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
      align-items: start;
    }
    @media (max-width: 1000px) {
      .board-grid { grid-template-columns: 1fr; }
    }

    .column-card {
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 16px;
      display: flex;
      flex-direction: column;
      min-height: 450px;
    }

    .column-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 14px;
      margin-bottom: 12px;
      border-bottom: 1px solid var(--color-border);
    }
    .col-title {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .col-title h4 {
      margin: 0;
      font-size: 14.5px;
      font-weight: 800;
      color: var(--color-text);
    }
    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    .todo-dot { background: #94a3b8; }
    .doing-dot { background: #0284c7; }
    .done-dot { background: #059669; }

    .col-count {
      background: #ffffff;
      border: 1px solid var(--color-border);
      padding: 2px 8px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 800;
      color: var(--color-text-muted);
    }

    .quick-add-box {
      display: flex;
      gap: 6px;
      margin-bottom: 12px;
    }
    .quick-select {
      flex: 0 0 100px;
      font-size: 12px;
      padding: 6px 8px !important;
      background: #ffffff !important;
    }
    .quick-input {
      flex: 1;
      font-size: 13px;
      padding: 6px 12px !important;
      background: #ffffff !important;
    }

    .cards-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      flex: 1;
      min-height: 100px;
    }
    .col-empty {
      text-align: center;
      color: #94a3b8;
      font-size: 13px;
      padding: 30px 10px;
      border: 2px dashed #e2e8f0;
      border-radius: var(--radius-sm);
    }

    /* KANBAN TASK CARD */
    .kanban-task-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 14px;
      box-shadow: var(--shadow-sm);
      cursor: grab;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      display: flex;
      flex-direction: column;
      gap: 10px;
      border-left: 4px solid #0284c7;
    }
    .kanban-task-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary-border);
    }
    .kanban-task-card:active {
      cursor: grabbing;
    }
    .kanban-task-card.multi-step {
      border-left-color: #8b5cf6;
    }
    .kanban-task-card.overdue {
      border-color: #fecdd3;
      border-left-color: #e11d48;
      background: #fffafa;
    }

    .card-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 6px;
    }
    .task-type-badge {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 7px;
      border-radius: 4px;
      background: #f1f5f9;
      color: #475569;
    }
    .task-type-badge.FAST { background: #e0f2fe; color: #0284c7; }
    .task-type-badge.MULTI_STEP { background: #f3e8ff; color: #7e22ce; }

    .card-status-badges {
      display: flex;
      gap: 4px;
    }
    .badge-alert {
      font-size: 10px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 4px;
    }
    .badge-alert.overdue { background: #ffe4e6; color: #e11d48; border: 1px solid #fecdd3; }
    .badge-alert.late { background: #fef3c7; color: #d97706; border: 1px solid #fde68a; }

    .card-title {
      font-size: 14px;
      font-weight: 700;
      color: var(--color-text);
      line-height: 1.35;
    }

    .card-progress {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .progress-track {
      flex: 1;
      height: 6px;
      background: #e2e8f0;
      border-radius: 999px;
      overflow: hidden;
    }
    .progress-fill {
      height: 100%;
      background: #8b5cf6;
      border-radius: 999px;
    }
    .progress-label {
      font-size: 11px;
      font-weight: 700;
      color: var(--color-text-muted);
      white-space: nowrap;
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 8px;
      border-top: 1px solid #f1f5f9;
    }
    .card-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 11.5px;
      color: var(--color-text-muted);
    }
    .meta-item {
      display: flex;
      align-items: center;
      gap: 3px;
    }
    .meta-item .icon { font-size: 14px; }
    .due-urgent { color: #e11d48; font-weight: 700; }

    .card-user-action {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .assignee-avatar {
      width: 26px;
      height: 26px;
      border-radius: 8px;
      background: var(--gradient-primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      font-weight: 800;
    }
    .btn-card-detail {
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: 6px;
      width: 26px;
      height: 26px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-muted);
      transition: all 0.15s ease;
    }
    .btn-card-detail:hover {
      background: var(--color-primary);
      border-color: var(--color-primary);
      color: #ffffff;
    }
    .btn-card-detail .icon { font-size: 16px; }

    .cdk-drag-preview {
      box-shadow: var(--shadow-lg) !important;
      border-radius: var(--radius-sm);
      opacity: 0.95;
    }
    .cdk-drag-placeholder {
      opacity: 0.2;
    }

    /* SPINNER */
    .state-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 48px;
      text-align: center;
    }
    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid #e0f2fe;
      border-top-color: var(--color-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 12px auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class KanbanBoardComponent implements OnInit {
  router = inject(Router);
  authStore = inject(AuthStore);
  private taskService = inject(TaskService);
  private taskTypeService = inject(TaskTypeService);

  board: KanbanBoard | null = null;
  taskTypes: TaskTypeSummary[] = [];
  search = '';
  typeFilter?: TimeCategory;
  myTasksOnly = false;

  quickAddType: Record<KanbanColumn, number | null> = { todo: null, doing: null, done: null };
  quickAddTitle: Record<KanbanColumn, string> = { todo: '', doing: '', done: '' };

  ngOnInit() {
    this.taskTypeService.getTaskTypes().subscribe(types => this.taskTypes = types);
    this.load();
  }

  load() {
    this.taskService.getKanbanBoard({
      type: this.typeFilter,
      search: this.search || undefined,
      assigneeId: this.myTasksOnly ? this.authStore.user()?.id : undefined
    }).subscribe(board => this.board = board);
  }

  typeLabel(t: TimeCategory): string {
    return t === 'FAST' ? 'Fast Task' : t === 'MULTI_STEP' ? 'Multi-step Task' : 'Often Task';
  }

  accentClass(card: KanbanTask): string {
    return card.timeCategory === 'FAST' ? 'fast' : 'multi-step';
  }

  initials(name: string): string {
    return (name || '').split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  quickAdd(column: KanbanColumn) {
    const title = this.quickAddTitle[column].trim();
    const taskTypeId = this.quickAddType[column];
    if (!title || !taskTypeId) return;
    this.taskService.quickCreateTask(taskTypeId, title).subscribe({
      next: () => {
        this.quickAddTitle[column] = '';
        this.load();
      },
      error: (err) => alert(err.error?.message || 'Không thể tạo task')
    });
  }

  onDrop(event: CdkDragDrop<KanbanTask[]>, targetColumn: KanbanColumn) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    const card = event.previousContainer.data[event.previousIndex] as KanbanTask;

    // Multi-step tasks can't move to Done until every step is done (Scope.md §2.0.5)
    if (targetColumn === 'done' && card.timeCategory === 'MULTI_STEP' && card.stepsTotal > 0 && card.stepsDone < card.stepsTotal) {
      alert('Không thể chuyển sang Hoàn thành khi còn bước chưa xong.');
      return;
    }

    let newStatus: string;
    if (targetColumn === 'todo') newStatus = 'PENDING';
    else if (targetColumn === 'doing') newStatus = 'IN_PROGRESS';
    else newStatus = card.dueDate && new Date(card.dueDate) < new Date() ? 'CLOSED_LATE' : 'DONE';

    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);

    this.taskService.updateTaskStatus(card.id, { status: newStatus as any }).subscribe({
      error: (err) => {
        alert(err.error?.message || 'Không thể cập nhật trạng thái');
        this.load();
      }
    });
  }
}
