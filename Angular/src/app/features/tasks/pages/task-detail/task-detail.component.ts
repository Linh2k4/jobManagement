import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectSelectedTask, selectTaskDetailLoading } from '../../../../store/task/task.selectors';
import * as TaskActions from '../../../../store/task/task.actions';
import { AuthStore } from '../../../../core/stores/auth.store';
import { TaskService } from '../../../../core/services/api/task.service';
import { Task, TaskComment, TaskAttachment, Subtask, TaskStatus, Difficulty, DeadlineExtension } from '../../../../core/models';
import { TaskStatusLabelPipe } from '../../../../shared/pipes/task-status-label.pipe';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskStatusLabelPipe],
  template: `
    <div class="task-detail-container" *ngIf="task$ | async as task; else loadingOrEmpty">
      <button class="back-link" (click)="router.navigate(['/tasks'])"><span class="icon">arrow_back</span> Quay lại danh sách</button>

      <div class="header">
        <h1>{{ task.title }}</h1>
        <span class="status" [ngClass]="'status-' + task.status">{{ task.status | taskStatusLabel }}</span>
      </div>

      <div class="detail-grid">
        <div class="section">
          <h3>Tổng quan</h3>
          <div class="field" *ngIf="task.description">
            <label>Mô tả</label>
            <p>{{ task.description }}</p>
          </div>
          <div class="field" *ngIf="task.priority">
            <label>Độ ưu tiên</label>
            <span class="priority-badge" [ngClass]="'priority-' + task.priority">{{ task.priority }}</span>
          </div>
          <div class="field">
            <label>Độ khó</label>
            <p *ngIf="task.difficulty as d; else noDifficulty">{{ d }}/5</p>
            <ng-template #noDifficulty><p class="muted">Chưa đặt</p></ng-template>
            <div class="inline-form" *ngIf="canSetDifficulty(task)">
              <select [(ngModel)]="difficultyInput">
                <option [ngValue]="1">1 - Rất dễ</option>
                <option [ngValue]="2">2 - Dễ</option>
                <option [ngValue]="3">3 - Trung bình</option>
                <option [ngValue]="4">4 - Khó</option>
                <option [ngValue]="5">5 - Rất khó</option>
              </select>
              <button (click)="saveDifficulty(task.id)">Lưu</button>
            </div>
          </div>
        </div>

        <div class="section">
          <h3>Chi tiết</h3>
          <div class="field" *ngIf="task.estimateMinutes">
            <label>Ước lượng</label>
            <p>{{ task.estimateMinutes }} phút</p>
          </div>
          <div class="field">
            <label>Hạn chót</label>
            <p>{{ task.dueDate ? (task.dueDate | date:'mediumDate') : 'Chưa đặt' }}</p>

            <!-- Manager: extend directly (self-approves anyway, Scope §12.7) -->
            <div class="inline-form" *ngIf="authStore.isManager() && canExtendDeadline(task)">
              <input type="number" min="1" [(ngModel)]="extendDaysInput" placeholder="Số ngày">
              <input type="text" [(ngModel)]="extendReasonInput" placeholder="Lý do gia hạn">
              <button (click)="saveExtendDeadline(task.id)">Gia hạn</button>
            </div>

            <!-- Member/Lead: request-and-approval flow -->
            <ng-container *ngIf="!authStore.isManager()">
              <div class="pending-ext" *ngIf="pendingExtension">
                Đang chờ duyệt: dời sang {{ pendingExtension.requestedDeadline | date:'mediumDate' }}
                (gửi lúc {{ pendingExtension.createdAt | date:'short' }})
              </div>
              <div class="inline-form" *ngIf="!pendingExtension && task.status !== 'DONE' && task.status !== 'CANCELLED'">
                <input type="date" [(ngModel)]="extDeadlineInput">
                <input type="text" [(ngModel)]="extReasonInput" placeholder="Lý do (tối thiểu 10 ký tự)">
                <button (click)="requestExtension(task.id)">Xin gia hạn</button>
              </div>
            </ng-container>
          </div>
          <div class="field" *ngIf="task.completedAt">
            <label>Hoàn thành lúc</label>
            <p>{{ task.completedAt | date:'medium' }}</p>
          </div>
        </div>

        <div class="section">
          <h3>Phân công</h3>
          <div class="field">
            <label>Người tạo</label>
            <p>{{ task.createdBy.fullName }}</p>
          </div>
          <div class="field">
            <label>Người thực hiện</label>
            <p *ngIf="task.currentAssignment as a; else unassigned">{{ a.assignee.fullName }}</p>
            <ng-template #unassigned><p class="muted">Chưa phân công</p></ng-template>
          </div>
          <div class="actions" *ngIf="isCurrentAssignee(task)">
            <button (click)="updateStatus(task.id, 'IN_PROGRESS')" [disabled]="task.status === 'IN_PROGRESS'">
              Bắt đầu
            </button>
            <button (click)="updateStatus(task.id, 'DONE')" [disabled]="task.status === 'DONE'">
              Hoàn thành
            </button>
          </div>
          <div class="actions" *ngIf="canManageTask(task)">
            <button class="btn-danger" (click)="cancelTask(task.id)" [disabled]="task.status === 'CANCELLED'">
              Huỷ việc
            </button>
          </div>
        </div>
      </div>

      <!-- SUBTASKS: Task -> GroupSubtask (optional) -> Subtask -> Step (Scope.md §2.1/§2.2) -->
      <div class="section wide">
        <h3>Các bước ({{ task.subtasks?.length || 0 }})</h3>

        <div class="group-block" *ngFor="let g of task.groupSubtasks">
          <div class="group-header">
            <strong>{{ g.name }}</strong>
            <button class="btn-link" *ngIf="canManageTask(task)" (click)="deleteGroupRow(task.id, g.id)">Xoá nhóm</button>
          </div>
          <ng-container *ngFor="let s of subtasksInGroup(task, g.id)">
            <ng-container *ngTemplateOutlet="subtaskRow; context: { s: s, task: task }"></ng-container>
          </ng-container>
        </div>

        <ng-container *ngFor="let s of ungroupedSubtasks(task)">
          <ng-container *ngTemplateOutlet="subtaskRow; context: { s: s, task: task }"></ng-container>
        </ng-container>

        <ng-template #subtaskRow let-s="s" let-task="task">
          <div class="subtask-item">
            <div class="subtask-main">
              <span class="status small" [ngClass]="'status-' + s.status">{{ s.status }}</span>
              <span class="subtask-title">{{ s.title }}</span>
              <span class="muted" *ngIf="s.estimateMinutes">{{ s.estimateMinutes }} phút</span>
              <span class="muted" *ngIf="s.deadline">{{ s.deadline | date:'short' }}</span>
              <div class="subtask-actions">
                <ng-container *ngIf="s.steps.length === 0">
                  <button *ngIf="s.status !== 'IN_PROGRESS' && s.status !== 'DONE'"
                          (click)="setSubtaskStatus(task.id, s.id, 'IN_PROGRESS')">Bắt đầu</button>
                  <button *ngIf="s.status !== 'DONE'"
                          (click)="setSubtaskStatus(task.id, s.id, 'DONE')">Xong</button>
                </ng-container>
                <button class="btn-danger" *ngIf="canManageTask(task)" (click)="deleteSubtaskRow(task.id, s.id)">Xoá</button>
              </div>
            </div>
            <p class="muted note" *ngIf="s.note">{{ s.note }}</p>
            <div class="target-bar" *ngIf="s.isTarget">Target: {{ s.target ?? 0 }}/{{ s.estimateTarget ?? 0 }}</div>

            <div class="step-item" *ngFor="let step of s.steps">
              <span class="step-order">{{ step.stepOrder }}</span>
              <span class="step-name">{{ step.name }}</span>
              <span class="muted" *ngIf="step.assignee">{{ step.assignee.fullName }}</span>
              <span class="muted" *ngIf="step.estimateMinutes">{{ step.estimateMinutes }} phút</span>
              <span class="status small" [ngClass]="'status-' + step.status">{{ step.status }}</span>
              <div class="step-actions">
                <button *ngIf="step.status !== 'IN_PROGRESS' && step.status !== 'DONE'"
                        (click)="setStepStatus(task.id, s.id, step.id, 'IN_PROGRESS')">Bắt đầu</button>
                <button *ngIf="step.status !== 'DONE'"
                        (click)="setStepStatus(task.id, s.id, step.id, 'DONE')">Xong</button>
                <button class="btn-danger" *ngIf="canManageTask(task)" (click)="deleteStepRow(task.id, s.id, step.id)">Xoá</button>
              </div>
            </div>
            <div class="inline-form" *ngIf="canManageTask(task)">
              <input type="text" [(ngModel)]="newStepName[s.id]" placeholder="Tên step mới">
              <button (click)="addStepRow(task.id, s.id)" [disabled]="!newStepName[s.id]">+ Thêm step</button>
            </div>
          </div>
        </ng-template>

        <div class="inline-form" *ngIf="canManageTask(task)">
          <input type="text" [(ngModel)]="newGroupName" placeholder="Tên nhóm mới">
          <button (click)="addGroupRow(task.id)" [disabled]="!newGroupName">+ Thêm nhóm</button>
        </div>
        <div class="inline-form" *ngIf="canManageTask(task)">
          <select [(ngModel)]="newSubtaskGroupId">
            <option [ngValue]="null">(Không thuộc nhóm)</option>
            <option *ngFor="let g of task.groupSubtasks" [ngValue]="g.id">{{ g.name }}</option>
          </select>
          <input type="text" [(ngModel)]="newSubtaskTitle" placeholder="Tên việc con mới">
          <input type="number" min="1" [(ngModel)]="newSubtaskEstimate" placeholder="Phút (tuỳ chọn)">
          <button (click)="addSubtaskRow(task.id)" [disabled]="!newSubtaskTitle">+ Thêm việc con</button>
        </div>
      </div>

      <!-- ATTACHMENTS -->
      <div class="section wide">
        <h3>Tệp đính kèm ({{ attachments.length }})</h3>
        <div class="attachment-item" *ngFor="let a of attachments">
          <span class="file-name" (click)="downloadAttachment(task.id, a)"><span class="icon">attach_file</span> {{ a.fileName }}</span>
          <span class="muted">{{ formatSize(a.fileSize) }} · {{ a.uploadedByName }} · {{ a.createdAt | date:'short' }}</span>
          <button class="btn-danger" (click)="deleteAttachment(task.id, a.id)">Xoá</button>
        </div>
        <input type="file" (change)="onFileSelected($event, task.id)">
      </div>

      <!-- COMMENTS -->
      <div class="section wide">
        <h3>Bình luận ({{ comments.length }})</h3>
        <div class="comment-item" *ngFor="let c of comments">
          <div class="comment-header">
            <strong>{{ c.userFullName }}</strong>
            <span class="muted">{{ c.createdAt | date:'short' }}</span>
            <button class="btn-link" *ngIf="canDeleteComment(c)" (click)="deleteComment(task.id, c.id)">Xoá</button>
          </div>
          <p>{{ c.content }}</p>
        </div>
        <div class="inline-form">
          <textarea [(ngModel)]="newComment" placeholder="Viết bình luận..."></textarea>
          <button (click)="addComment(task.id)" [disabled]="!newComment">Gửi</button>
        </div>
      </div>

      <!-- PRIVATE NOTES -->
      <div class="section wide">
        <h3>Ghi chú riêng (chỉ mình bạn thấy)</h3>
        <textarea [(ngModel)]="privateNotes" placeholder="Ghi chú cá nhân về công việc này..."></textarea>
        <button (click)="savePrivateNotes(task.id)">Lưu ghi chú</button>
      </div>

      <!-- DEADLINE EXTENSION HISTORY -->
      <div class="section wide" *ngIf="extensionHistory.length > 0">
        <h3>Lịch sử gia hạn deadline</h3>
        <div class="ext-line" *ngFor="let e of extensionHistory">
          <span class="status-badge" [ngClass]="'ext-' + e.status">{{ extStatusLabel(e.status) }}</span>
          <span>{{ e.currentDeadline | date:'shortDate' }} <span class="icon">arrow_forward</span> {{ e.requestedDeadline | date:'shortDate' }}</span>
          <span class="muted">bởi {{ e.requestedBy.fullName }}</span>
          <span class="muted" *ngIf="e.reviewedBy">· duyệt bởi {{ e.reviewedBy.fullName }}</span>
        </div>
      </div>
    </div>

    <ng-template #loadingOrEmpty>
      <div class="no-task" *ngIf="isLoading$ | async; else notFound">Đang tải...</div>
      <ng-template #notFound><div class="no-task">Không tìm thấy công việc.</div></ng-template>
    </ng-template>
  `,
  styles: [`
    .task-detail-container { padding: 20px; max-width: 900px; margin: 0 auto; }
    .back-link { background: none; border: none; color: #1d4ed8; cursor: pointer; padding: 0 0 15px; font-size: 14px; }
    .header { margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
    .header h1 { margin: 0; }
    .status { padding: 6px 12px; border-radius: 10px; font-weight: bold; white-space: nowrap; }
    .status.small { padding: 2px 8px; font-size: 11px; }
    .priority-badge { padding: 4px 8px; border-radius: 10px; font-size: 12px; font-weight: bold; }
    .detail-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; margin-bottom: 20px; }
    .section { background: white; border: 1px solid #ddd; border-radius: 16px; padding: 15px; }
    .section.wide { margin-bottom: 20px; }
    .section h3 { margin-top: 0; }
    .field { margin-bottom: 12px; }
    .field label { font-weight: bold; display: block; margin-bottom: 4px; font-size: 13px; color: #555; }
    .field p { margin: 0; }
    .muted { color: #999; }
    .actions { display: flex; gap: 10px; margin-top: 15px; flex-wrap: wrap; }
    .actions button, .inline-form button { padding: 8px 16px; border: 1px solid #ddd; border-radius: 10px; cursor: pointer; background: white; }
    .actions button:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-danger { color: #c62828; border-color: #ef9a9a !important; }
    .btn-link { background: none; border: none; color: #c62828; cursor: pointer; padding: 0; font-size: 12px; }
    .inline-form { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; align-items: center; }
    .inline-form input, .inline-form select, .inline-form textarea { padding: 6px; border: 1px solid #ddd; border-radius: 10px; }
    .inline-form textarea { width: 100%; min-height: 60px; box-sizing: border-box; }
    .step-item, .attachment-item, .comment-item {
      display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; flex-wrap: wrap;
    }
    .step-order { background: #eee; border-radius: 50%; width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; font-size: 12px; }
    .step-name, .file-name { flex: 1; min-width: 120px; }
    .file-name { cursor: pointer; color: #1d4ed8; }
    .step-actions { display: flex; gap: 6px; }
    .step-actions button { padding: 4px 10px; font-size: 12px; border: 1px solid #ddd; border-radius: 10px; background: white; cursor: pointer; }
    .group-block { margin-bottom: 16px; }
    .group-header { display: flex; align-items: center; gap: 10px; padding: 6px 0; border-bottom: 2px solid #ddd; margin-bottom: 6px; }
    .subtask-item { padding: 10px 0 10px 14px; border-bottom: 1px solid #f0f0f0; border-left: 3px solid #e3f2fd; }
    .subtask-main { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .subtask-title { flex: 1; min-width: 120px; font-weight: 500; }
    .subtask-actions { display: flex; gap: 6px; }
    .subtask-actions button { padding: 4px 10px; font-size: 12px; border: 1px solid #ddd; border-radius: 10px; background: white; cursor: pointer; }
    .subtask-item .step-item { padding-left: 14px; }
    .note { margin: 4px 0 0; font-size: 13px; font-style: italic; }
    .target-bar { font-size: 12px; color: #1d4ed8; margin-top: 4px; }
    .comment-item { flex-direction: column; align-items: stretch; }
    .comment-header { display: flex; gap: 10px; align-items: center; }
    .comment-header strong { flex: 0; }
    .no-task { text-align: center; color: #999; padding: 40px; }
    .pending-ext { font-size: 13px; color: #ef6c00; background: #fff3e0; padding: 8px; border-radius: 10px; margin-top: 8px; }
    .ext-line { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; flex-wrap: wrap; }
    .ext-PENDING { background: #fff3e0; color: #ef6c00; }
    .ext-APPROVED { background: #c8e6c9; color: #2e7d32; }
    .ext-REJECTED { background: #ffcdd2; color: #c62828; }
    .ext-EXPIRED { background: #eeeeee; color: #9e9e9e; }
  `]
})
export class TaskDetailComponent implements OnInit {
  router = inject(Router);
  private route = inject(ActivatedRoute);
  private store = inject(Store);
  authStore = inject(AuthStore);
  private taskService = inject(TaskService);

  task$ = this.store.select(selectSelectedTask);
  isLoading$ = this.store.select(selectTaskDetailLoading);

  comments: TaskComment[] = [];
  attachments: TaskAttachment[] = [];
  privateNotes = '';

  newComment = '';
  newGroupName = '';
  newSubtaskTitle = '';
  newSubtaskEstimate: number | null = null;
  newSubtaskGroupId: number | null = null;
  newStepName: Record<number, string> = {};
  difficultyInput: Difficulty = 3;
  extendDaysInput: number | null = null;
  extendReasonInput = '';
  extDeadlineInput = '';
  extReasonInput = '';
  pendingExtension: DeadlineExtension | null = null;
  extensionHistory: DeadlineExtension[] = [];

  private readonly extStatusLabels: Record<string, string> = {
    PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt',
    REJECTED: 'Từ chối',
    EXPIRED: 'Hết hạn'
  };

  ngOnInit() {
    this.route.params.subscribe(params => {
      const id = Number(params['id']);
      if (id) {
        this.store.dispatch(TaskActions.loadTask({ id }));
        this.loadComments(id);
        this.loadAttachments(id);
        this.loadPrivateNotes(id);
        this.loadExtensionHistory(id);
      }
    });
  }

  extStatusLabel(status: string): string {
    return this.extStatusLabels[status] ?? status;
  }

  isCurrentAssignee(task: Task): boolean {
    const userId = this.authStore.user()?.id;
    return task.currentAssignment?.assignee.id === userId;
  }

  canManageTask(task: Task): boolean {
    const userId = this.authStore.user()?.id;
    return this.authStore.isManager() || task.createdBy.id === userId;
  }

  canSetDifficulty(task: Task): boolean {
    return this.authStore.isManager() || this.authStore.isLead();
  }

  canExtendDeadline(task: Task): boolean {
    return this.canManageTask(task);
  }

  canDeleteComment(comment: TaskComment): boolean {
    const userId = this.authStore.user()?.id;
    return this.authStore.isManager() || comment.userId === userId;
  }

  // ---- Task actions ----

  updateStatus(id: number, status: TaskStatus) {
    this.store.dispatch(TaskActions.updateTaskStatus({ id, request: { status } }));
  }

  cancelTask(id: number) {
    if (confirm('Bạn có chắc muốn huỷ công việc này?')) {
      this.store.dispatch(TaskActions.updateTaskStatus({ id, request: { status: 'CANCELLED' } }));
    }
  }

  saveDifficulty(id: number) {
    this.taskService.updateDifficulty(id, this.difficultyInput).subscribe(() => {
      this.store.dispatch(TaskActions.loadTask({ id }));
    });
  }

  saveExtendDeadline(id: number) {
    if (!this.extendDaysInput) return;
    this.taskService.extendDeadline(id, this.extendDaysInput, this.extendReasonInput).subscribe(() => {
      this.store.dispatch(TaskActions.loadTask({ id }));
      this.extendDaysInput = null;
      this.extendReasonInput = '';
    });
  }

  private loadExtensionHistory(taskId: number) {
    this.taskService.getDeadlineExtensionHistory(taskId).subscribe(history => {
      this.extensionHistory = history;
      this.pendingExtension = history.find(h => h.status === 'PENDING') ?? null;
    });
  }

  requestExtension(taskId: number) {
    if (!this.extDeadlineInput || this.extReasonInput.trim().length < 10) {
      alert('Vui lòng chọn ngày và nhập lý do (tối thiểu 10 ký tự).');
      return;
    }
    this.taskService.requestDeadlineExtension(taskId, this.extDeadlineInput, this.extReasonInput).subscribe({
      next: () => {
        this.extDeadlineInput = '';
        this.extReasonInput = '';
        this.loadExtensionHistory(taskId);
        this.reloadTask(taskId);
      },
      error: (err) => alert(err.error?.message || 'Không thể gửi yêu cầu gia hạn')
    });
  }

  // ---- Group subtasks / subtasks / steps ----
  // Task -> GroupSubtask (optional) -> Subtask -> Step (Scope.md §2.1/§2.2).
  // The tree is embedded in TaskResponse, so a mutation just re-dispatches
  // loadTask instead of maintaining a separate local list (same pattern
  // already used by saveDifficulty/saveExtendDeadline above).

  private reloadTask(taskId: number) {
    this.store.dispatch(TaskActions.loadTask({ id: taskId }));
  }

  subtasksInGroup(task: Task, groupId: number): Subtask[] {
    return (task.subtasks || []).filter(s => s.groupSubtaskId === groupId);
  }

  ungroupedSubtasks(task: Task): Subtask[] {
    return (task.subtasks || []).filter(s => !s.groupSubtaskId);
  }

  addGroupRow(taskId: number) {
    if (!this.newGroupName) return;
    this.taskService.createGroupSubtask(taskId, this.newGroupName).subscribe(() => {
      this.newGroupName = '';
      this.reloadTask(taskId);
    });
  }

  deleteGroupRow(taskId: number, groupId: number) {
    if (confirm('Xoá nhóm này? (sẽ xoá cả việc con và bước bên trong)')) {
      this.taskService.deleteGroupSubtask(taskId, groupId).subscribe(() => this.reloadTask(taskId));
    }
  }

  addSubtaskRow(taskId: number) {
    if (!this.newSubtaskTitle) return;
    this.taskService.createSubtask(taskId, {
      groupSubtaskId: this.newSubtaskGroupId ?? undefined,
      title: this.newSubtaskTitle,
      estimateMinutes: this.newSubtaskEstimate ?? undefined
    }).subscribe(() => {
      this.newSubtaskTitle = '';
      this.newSubtaskEstimate = null;
      this.newSubtaskGroupId = null;
      this.reloadTask(taskId);
    });
  }

  setSubtaskStatus(taskId: number, subtaskId: number, status: string) {
    this.taskService.updateSubtaskStatus(taskId, subtaskId, status).subscribe({
      next: () => this.reloadTask(taskId),
      error: (err) => alert(err.error?.message || 'Không thể cập nhật trạng thái')
    });
  }

  deleteSubtaskRow(taskId: number, subtaskId: number) {
    if (confirm('Xoá việc con này?')) {
      this.taskService.deleteSubtask(taskId, subtaskId).subscribe(() => this.reloadTask(taskId));
    }
  }

  addStepRow(taskId: number, subtaskId: number) {
    const name = this.newStepName[subtaskId];
    if (!name) return;
    this.taskService.createStep(subtaskId, { name }).subscribe(() => {
      this.newStepName[subtaskId] = '';
      this.reloadTask(taskId);
    });
  }

  setStepStatus(taskId: number, subtaskId: number, stepId: number, status: string) {
    this.taskService.updateStepStatus(subtaskId, stepId, status).subscribe({
      next: () => this.reloadTask(taskId),
      error: (err) => alert(err.error?.message || 'Không thể cập nhật trạng thái bước')
    });
  }

  deleteStepRow(taskId: number, subtaskId: number, stepId: number) {
    if (confirm('Xoá bước này?')) {
      this.taskService.deleteStep(subtaskId, stepId).subscribe(() => this.reloadTask(taskId));
    }
  }

  // ---- Comments ----

  private loadComments(taskId: number) {
    this.taskService.getTaskComments(taskId).subscribe(res => this.comments = res.data);
  }

  addComment(taskId: number) {
    if (!this.newComment) return;
    this.taskService.addComment(taskId, this.newComment).subscribe(() => {
      this.newComment = '';
      this.loadComments(taskId);
    });
  }

  deleteComment(taskId: number, commentId: number) {
    if (confirm('Xoá bình luận này?')) {
      this.taskService.deleteComment(taskId, commentId).subscribe(() => this.loadComments(taskId));
    }
  }

  // ---- Attachments ----

  private loadAttachments(taskId: number) {
    this.taskService.getAttachments(taskId).subscribe(res => this.attachments = res.data);
  }

  onFileSelected(event: Event, taskId: number) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.taskService.uploadAttachment(taskId, file).subscribe(() => {
      this.loadAttachments(taskId);
      (event.target as HTMLInputElement).value = '';
    });
  }

  downloadAttachment(taskId: number, attachment: TaskAttachment) {
    this.taskService.downloadAttachment(taskId, attachment.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = attachment.fileName;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  deleteAttachment(taskId: number, attachmentId: number) {
    if (confirm('Xoá tệp này?')) {
      this.taskService.deleteAttachment(taskId, attachmentId).subscribe(() => this.loadAttachments(taskId));
    }
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // ---- Private notes ----

  private loadPrivateNotes(taskId: number) {
    this.taskService.getPrivateNotes(taskId).subscribe(res => this.privateNotes = res.data);
  }

  savePrivateNotes(taskId: number) {
    this.taskService.updatePrivateNotes(taskId, this.privateNotes).subscribe();
  }
}
