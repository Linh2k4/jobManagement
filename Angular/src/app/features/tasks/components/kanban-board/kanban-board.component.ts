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
    <div class="board-filters">
      <input type="text" [(ngModel)]="search" (change)="load()" placeholder="Tìm theo tên task...">
      <select [(ngModel)]="typeFilter" (change)="load()">
        <option [ngValue]="undefined">Tất cả loại</option>
        <option value="FAST">Fast Task</option>
        <option value="OFTEN">Often Task</option>
        <option value="MULTI_STEP">Multi-step Task</option>
      </select>
      <label class="toggle" *ngIf="!authStore.isMember()">
        <input type="checkbox" [(ngModel)]="myTasksOnly" (change)="load()">
        Chỉ của tôi
      </label>
    </div>

    <div class="board" *ngIf="board as b">
      <div class="column" cdkDropList #todoList="cdkDropList" [cdkDropListData]="b.todo"
           [cdkDropListConnectedTo]="[doingList, doneList]" (cdkDropListDropped)="onDrop($event, 'todo')">
        <div class="column-header">Chờ thực hiện ({{ b.todo.length }})</div>
        <div class="quick-add">
          <select [(ngModel)]="quickAddType.todo">
            <option [ngValue]="null" disabled>Loại...</option>
            <option *ngFor="let t of taskTypes" [ngValue]="t.id">{{ t.name }}</option>
          </select>
          <input type="text" placeholder="+ Thêm task..." [(ngModel)]="quickAddTitle.todo"
                 (keyup.enter)="quickAdd('todo')">
        </div>
        <div class="card" *ngFor="let card of b.todo" cdkDrag [cdkDragData]="card"
             [ngClass]="accentClass(card)">
          <ng-container *ngTemplateOutlet="cardTpl; context: { card }"></ng-container>
        </div>
      </div>

      <div class="column" cdkDropList #doingList="cdkDropList" [cdkDropListData]="b.doing"
           [cdkDropListConnectedTo]="[todoList, doneList]" (cdkDropListDropped)="onDrop($event, 'doing')">
        <div class="column-header">Đang thực hiện ({{ b.doing.length }})</div>
        <div class="card" *ngFor="let card of b.doing" cdkDrag [cdkDragData]="card"
             [ngClass]="[accentClass(card), card.isOverdue ? 'overdue' : '']">
          <ng-container *ngTemplateOutlet="cardTpl; context: { card }"></ng-container>
        </div>
      </div>

      <div class="column" cdkDropList #doneList="cdkDropList" [cdkDropListData]="b.done"
           [cdkDropListConnectedTo]="[todoList, doingList]" (cdkDropListDropped)="onDrop($event, 'done')">
        <div class="column-header">Hoàn thành ({{ b.done.length }})</div>
        <div class="card" *ngFor="let card of b.done" cdkDrag [cdkDragData]="card"
             [ngClass]="accentClass(card)">
          <ng-container *ngTemplateOutlet="cardTpl; context: { card }"></ng-container>
        </div>
      </div>
    </div>

    <div *ngIf="!board" class="loading">Đang tải...</div>

    <ng-template #cardTpl let-card="card">
      <div class="card-title">{{ card.title }}</div>
      <div class="card-badges">
        <span class="badge type">{{ typeLabel(card.timeCategory) }}</span>
        <span class="badge priority" *ngIf="card.priority" [ngClass]="'priority-' + card.priority">{{ card.priority }}</span>
        <span class="badge overdue-badge" *ngIf="card.isOverdue">QUÁ HẠN</span>
        <span class="badge late-badge" *ngIf="card.status === 'CLOSED_LATE'">Trễ</span>
      </div>
      <div class="card-progress" *ngIf="card.stepsTotal > 0">
        <div class="bar"><div class="fill" [style.width.%]="(card.stepsDone / card.stepsTotal) * 100"></div></div>
        <span>{{ card.stepsDone }}/{{ card.stepsTotal }} bước</span>
      </div>
      <div class="card-footer">
        <span class="meta" *ngIf="card.commentCount"><span class="icon">chat</span> {{ card.commentCount }}</span>
        <span class="meta" *ngIf="card.attachmentCount"><span class="icon">attach_file</span> {{ card.attachmentCount }}</span>
        <span class="meta" *ngIf="card.dueDate"><span class="icon">calendar_month</span> {{ card.dueDate | date:'shortDate' }}</span>
        <span class="assignee" *ngIf="card.assignee" [title]="card.assignee.fullName">
          {{ initials(card.assignee.fullName) }}
        </span>
        <button class="detail-btn" (click)="router.navigate(['/tasks', card.id])">Xem chi tiết</button>
      </div>
    </ng-template>
  `,
  styles: [`
    .board-filters { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
    .board-filters input[type="text"], .board-filters select { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .toggle { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #555; cursor: pointer; }
    .loading { text-align: center; color: #999; padding: 40px; }
    .board { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; align-items: start; }
    .column {
      background: #f5f6f8; border-radius: 16px; padding: 12px; min-height: 200px;
    }
    .column-header { font-weight: bold; margin-bottom: 12px; color: #333; }
    .quick-add { display: flex; gap: 6px; margin-bottom: 10px; }
    .quick-add select { flex: 0 0 90px; font-size: 12px; padding: 4px; }
    .quick-add input { flex: 1; padding: 6px; border: 1px solid #ddd; border-radius: 10px; font-size: 13px; }
    .card {
      background: white; border-radius: 10px; padding: 12px; margin-bottom: 10px;
      border-left: 4px solid #F59E0B; box-shadow: 0 1px 3px rgba(0,0,0,0.08); cursor: grab;
    }
    .card.multi-step { border-left-color: #8B5CF6; }
    .card.overdue { border: 2px solid #EF4444; border-left: 4px solid #EF4444; }
    .card.cdk-drag-preview { box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
    .card.cdk-drag-placeholder { opacity: 0.3; }
    .card-title { font-weight: bold; margin-bottom: 8px; line-height: 1.3; }
    .card-badges { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
    .badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
    .badge.type { background: #eee; color: #555; }
    .overdue-badge { background: #EF4444; color: white; }
    .late-badge { background: #F59E0B; color: white; }
    .card-progress { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 11px; color: #666; }
    .bar { flex: 1; height: 6px; background: #eee; border-radius: 3px; overflow: hidden; }
    .fill { height: 100%; background: #8B5CF6; }
    .card-footer { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #777; flex-wrap: wrap; }
    .assignee {
      margin-left: auto; width: 24px; height: 24px; border-radius: 50%; background: #2563eb; color: white;
      display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: bold;
    }
    .detail-btn { padding: 4px 10px; font-size: 11px; border: 1px solid #ddd; border-radius: 10px; background: white; cursor: pointer; }
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
    return name.split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
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
