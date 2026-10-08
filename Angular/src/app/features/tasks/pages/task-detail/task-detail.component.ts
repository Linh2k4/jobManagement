import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectSelectedTask, selectTaskDetailLoading } from '../../../../store/task/task.selectors';
import * as TaskActions from '../../../../store/task/task.actions';
import { AuthStore } from '../../../../core/stores/auth.store';
import { TaskService } from '../../../../core/services/api/task.service';
import { UserService, UserInfo } from '../../../../core/services/api/user.service';
import { Task, TaskComment, TaskAttachment, Subtask, TaskStatus, Difficulty, DeadlineExtension, TaskAssignmentInfo } from '../../../../core/models';
import { TaskStatusLabelPipe } from '../../../../shared/pipes/task-status-label.pipe';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskStatusLabelPipe],
  template: `
    <div class="task-detail-container animate-fade-in" *ngIf="task$ | async as task; else loadingOrEmpty">
      <!-- TOP NAVIGATION & ACTIONS -->
      <div class="top-nav-bar">
        <button class="btn-back" (click)="router.navigate(['/tasks'])">
          <span class="icon">arrow_back</span>
          <span>Quay lại danh sách</span>
        </button>

        <div class="top-actions">
          <button class="btn btn-secondary btn-sm" *ngIf="canManageTask(task)" (click)="openEditTaskModal(task)">
            <span class="icon">edit</span>
            <span>Chỉnh sửa công việc</span>
          </button>
          <span class="status-badge-lg" [ngClass]="'status-' + task.status">
            {{ task.status | taskStatusLabel }}
          </span>
        </div>
      </div>

      <!-- REJECTION NOTE / REVIEW COMMENT -->
      <div class="review-alert-card" *ngIf="task.reviewNote">
        <div class="alert-icon-wrap danger">
          <span class="icon">feedback</span>
        </div>
        <div class="alert-body">
          <h4 class="alert-title">Nhận xét & Lý do từ Leader / Admin:</h4>
          <div class="alert-content-box">{{ task.reviewNote }}</div>
        </div>
      </div>

      <!-- WAITING APPROVAL BANNER -->
      <div class="approval-alert-card" *ngIf="task.status === 'WAITING_APPROVAL'">
        <div class="alert-icon-wrap warning">
          <span class="icon pulse">hourglass_top</span>
        </div>
        <div class="alert-body">
          <h4 class="alert-title">Đang chờ duyệt hoàn thành</h4>
          <p class="alert-desc">Công việc này đã hoàn tất và đang chờ Leader tạo việc hoặc Admin phê duyệt.</p>
        </div>
      </div>

      <!-- TASK TITLE HERO -->
      <div class="task-hero-card">
        <div class="hero-header-row">
          <div class="hero-main">
            <div class="hero-badges">
              <span class="priority-badge" *ngIf="task.priority" [ngClass]="'priority-' + task.priority">
                Ưu tiên: {{ task.priority }}
              </span>
              <span class="difficulty-badge" *ngIf="task.difficulty">
                Độ khó: {{ task.difficulty }}/5
              </span>
              <span class="section-tag" *ngIf="task.section">
                # {{ task.section }}
              </span>
            </div>
            <h1 class="task-main-title">{{ task.title }}</h1>
          </div>
          <div class="hero-actions" *ngIf="canManageTask(task)">
            <button class="btn btn-primary" (click)="openEditTaskModal(task)">
              <span class="icon">edit</span>
              <span>Chỉnh sửa công việc</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 3-COLUMN METRICS GRID -->
      <div class="details-grid">
        <!-- CARD 1: OVERVIEW -->
        <div class="card detail-card">
          <div class="card-header-clean">
            <div class="header-title-flex">
              <span class="icon card-icon">info</span>
              <h3>Tổng quan</h3>
            </div>
            <button class="btn-text-action" *ngIf="canManageTask(task)" (click)="openEditTaskModal(task)">
              <span class="icon">edit</span> Sửa thông tin
            </button>
          </div>

          <div class="field-group">
            <span class="field-label">Mô tả công việc</span>
            <div class="desc-box" *ngIf="task.description; else noDesc">
              {{ task.description }}
            </div>
            <ng-template #noDesc>
              <p class="text-muted-italic">Không có mô tả chi tiết</p>
            </ng-template>
          </div>

          <div class="field-group">
            <span class="field-label">Độ khó (1 - 5)</span>
            <div class="difficulty-row">
              <span class="diff-value" *ngIf="task.difficulty as d; else noDiff">{{ d }} / 5</span>
              <ng-template #noDiff><span class="text-muted-italic">Chưa thiết lập</span></ng-template>

              <div class="inline-control" *ngIf="canSetDifficulty(task)">
                <select [(ngModel)]="difficultyInput" class="custom-select-sm">
                  <option [ngValue]="1">1 - Rất dễ</option>
                  <option [ngValue]="2">2 - Dễ</option>
                  <option [ngValue]="3">3 - Trung bình</option>
                  <option [ngValue]="4">4 - Khó</option>
                  <option [ngValue]="5">5 - Rất khó</option>
                </select>
                <button class="btn btn-secondary btn-sm" (click)="saveDifficulty(task.id)">Lưu</button>
              </div>
            </div>
          </div>
        </div>

        <!-- CARD 2: TIMELINE & ESTIMATES -->
        <div class="card detail-card">
          <div class="card-header-clean">
            <div class="header-title-flex">
              <span class="icon card-icon">schedule</span>
              <h3>Tiến độ & Hạn chót</h3>
            </div>
            <button class="btn-text-action" *ngIf="canManageTask(task)" (click)="openEditTaskModal(task)">
              <span class="icon">edit</span> Sửa hạn
            </button>
          </div>

          <div class="field-group" *ngIf="task.estimateMinutes">
            <span class="field-label">Thời gian ước lượng</span>
            <div class="field-value-highlight">
              <span class="icon">timer</span>
              <span>{{ task.estimateMinutes }} phút</span>
            </div>
          </div>

          <div class="field-group">
            <span class="field-label">Hạn hoàn thành (Deadline)</span>
            <div class="field-value-highlight">
              <span class="icon">event</span>
              <span>{{ task.dueDate ? (task.dueDate | date:'dd/MM/yyyy') : 'Chưa đặt hạn' }}</span>
            </div>

            <!-- Direct Extend for Manager and Leader who created task -->
            <div class="extension-form-box" *ngIf="canManageTask(task)">
              <span class="form-sub-label">Gia hạn trực tiếp:</span>
              <div class="form-inputs-row">
                <input type="number" min="1" [(ngModel)]="extendDaysInput" placeholder="Số ngày" class="custom-input-sm" style="width: 80px;">
                <input type="text" [(ngModel)]="extendReasonInput" placeholder="Lý do gia hạn" class="custom-input-sm flex-1">
                <button class="btn btn-primary btn-sm" (click)="saveExtendDeadline(task.id)" [disabled]="!extendDaysInput">Gia hạn</button>
              </div>
            </div>

            <!-- Request Extension for Member (Assignee) -->
            <ng-container *ngIf="!canManageTask(task)">
              <div class="pending-ext-banner" *ngIf="pendingExtension">
                <span class="icon">pending_actions</span>
                <div>
                  <strong>Đang chờ duyệt gia hạn</strong>
                  <p>Xin dời sang {{ pendingExtension.requestedDeadline | date:'dd/MM/yyyy' }}</p>
                </div>
              </div>

              <div class="extension-form-box" *ngIf="!pendingExtension && task.status !== 'DONE' && task.status !== 'CLOSED_LATE' && task.status !== 'CANCELLED'">
                <span class="form-sub-label">Yêu cầu xin gia hạn:</span>
                <div class="form-inputs-row">
                  <input type="date" [(ngModel)]="extDeadlineInput" class="custom-input-sm">
                  <input type="text" [(ngModel)]="extReasonInput" placeholder="Lý do xin gia hạn (tối thiểu 10 ký tự)" class="custom-input-sm flex-1">
                  <button class="btn btn-secondary btn-sm" (click)="requestExtension(task.id)">Xin gia hạn</button>
                </div>
              </div>
            </ng-container>
          </div>

          <div class="field-group" *ngIf="task.completedAt">
            <span class="field-label">Thời điểm hoàn thành</span>
            <div class="field-value-success">
              <span class="icon">check_circle</span>
              <span>{{ task.completedAt | date:'HH:mm - dd/MM/yyyy' }}</span>
            </div>
          </div>
        </div>

        <!-- CARD 3: ASSIGNMENT & ACTIONS -->
        <div class="card detail-card highlight-card">
          <div class="card-header-clean">
            <div class="header-title-flex">
              <span class="icon card-icon">group</span>
              <h3>Phân công & Người tham gia ({{ getActiveAssignments(task).length }})</h3>
            </div>
          </div>

          <div class="field-group">
            <span class="field-label">Người tạo công việc</span>
            <div class="user-chip-detail">
              <div class="user-avatar-sm creator">
                {{ initials(task.createdBy.fullName) }}
              </div>
              <div class="user-info-text">
                <strong>{{ task.createdBy.fullName }}</strong>
                <span>{{ task.createdBy.email }}</span>
              </div>
            </div>
          </div>

          <div class="field-group">
            <div class="field-label-row">
              <span class="field-label">Người thực hiện</span>
              <button class="btn-text-danger-sm" *ngIf="canManageTask(task) && getActiveAssignments(task).length > 1" (click)="unassignAllUsers(task.id)">
                Hủy tất cả
              </button>
            </div>

            <!-- List of all active assignees -->
            <div class="assignees-list" *ngIf="getActiveAssignments(task).length > 0; else unassignedTpl">
              <div class="assignee-card-box" *ngFor="let a of getActiveAssignments(task)">
                <div class="user-chip-detail">
                  <div class="user-avatar-sm assignee">
                    {{ initials(a.assignee.fullName) }}
                  </div>
                  <div class="user-info-text">
                    <strong>{{ a.assignee.fullName }}</strong>
                    <span>{{ a.assignee.email }}</span>
                  </div>
                </div>

                <button class="btn-icon-danger" *ngIf="canManageTask(task)" (click)="unassignSpecificUser(task.id, a.assignee.id)" title="Xóa người này khỏi công việc">
                  <span class="icon">close</span>
                </button>
              </div>
            </div>

            <ng-template #unassignedTpl>
              <div class="unassigned-notice">
                <span class="icon">person_off</span>
                <span>Chưa phân công nhân sự</span>
              </div>
            </ng-template>

            <!-- Assign Dropdown for Leader (Members only) & Manager (Anyone) -->
            <div class="reassign-controls" *ngIf="canManageTask(task)">
              <select [(ngModel)]="selectedAssigneeId" class="custom-select">
                <option [ngValue]="null">
                  {{ authStore.isLead() ? '-- Chọn nhân viên (Member) để giao việc --' : '-- Chọn nhân sự để giao việc --' }}
                </option>
                <option *ngFor="let u of getAvailableUsers(task)" [ngValue]="u.id">
                  {{ u.fullName }} ({{ u.role }}) - {{ u.email }}
                </option>
              </select>
              <button class="btn btn-secondary btn-sm" (click)="assignUser(task.id)" [disabled]="!selectedAssigneeId">
                <span class="icon">person_add</span>
                <span>+ Thêm người</span>
              </button>
            </div>
          </div>

          <!-- WORKFLOW ACTION BUTTONS -->
          <div class="workflow-actions-wrapper">
            <!-- Member / Assignee actions -->
            <div class="action-btn-group" *ngIf="isCurrentAssignee(task)">
              <button class="btn btn-secondary btn-full" *ngIf="task.status === 'PENDING'" (click)="updateStatus(task.id, 'IN_PROGRESS')">
                <span class="icon">play_arrow</span> Bắt đầu thực hiện
              </button>
              <button class="btn btn-primary btn-full" *ngIf="task.status === 'IN_PROGRESS' || task.status === 'PENDING'" (click)="submitCompletion(task.id)">
                <span class="icon">send</span> Gửi duyệt hoàn thành
              </button>
            </div>

            <!-- Leader / Admin actions -->
            <div class="action-btn-group" *ngIf="canManageTask(task)">
              <ng-container *ngIf="task.status === 'WAITING_APPROVAL'">
                <button class="btn btn-primary btn-full" (click)="approveCompletion(task.id)">
                  <span class="icon">check_circle</span> Phê duyệt hoàn thành
                </button>
                <button class="btn btn-danger btn-full" (click)="openRejectModal(task.id)">
                  <span class="icon">cancel</span> Từ chối hoàn thành
                </button>
              </ng-container>

              <button class="btn btn-danger-outline btn-full" (click)="cancelTask(task.id)" [disabled]="task.status === 'CANCELLED'">
                <span class="icon">block</span> Huỷ công việc
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- EDIT TASK MODAL (Leader & Admin) -->
      <div class="modal-backdrop" *ngIf="showEditModal">
        <div class="modal-box animate-fade-in">
          <div class="modal-header-clean">
            <span class="icon">edit_note</span>
            <h3>Chỉnh sửa công việc</h3>
          </div>
          <p class="modal-sub">Cập nhật thông tin chi tiết của công việc:</p>

          <div class="modal-form-grid">
            <div class="modal-form-group">
              <label>Tiêu đề công việc <span class="required">*</span></label>
              <input type="text" [(ngModel)]="editTitle" placeholder="Tiêu đề..." class="custom-input">
            </div>

            <div class="modal-form-group">
              <label>Mô tả công việc</label>
              <textarea [(ngModel)]="editDescription" placeholder="Mô tả chi tiết..." rows="3" class="custom-textarea"></textarea>
            </div>

            <div class="modal-row-2">
              <div class="modal-form-group">
                <label>Hạn hoàn thành (Deadline)</label>
                <input type="date" [(ngModel)]="editDueDate" class="custom-input">
              </div>
              <div class="modal-form-group">
                <label>Thời gian ước lượng (phút)</label>
                <input type="number" min="1" [(ngModel)]="editEstimateMinutes" placeholder="Phút" class="custom-input">
              </div>
            </div>

            <div class="modal-form-group">
              <label>Phòng ban / Nhóm (Section)</label>
              <input type="text" [(ngModel)]="editSection" placeholder="Ví dụ: Kỹ thuật, Marketing..." class="custom-input">
            </div>
          </div>

          <div class="modal-btns">
            <button class="btn btn-secondary" (click)="closeEditTaskModal()">Hủy</button>
            <button class="btn btn-primary" (click)="saveEditTask(task.id)" [disabled]="!editTitle.trim()">
              <span class="icon">save</span> Lưu thay đổi
            </button>
          </div>
        </div>
      </div>

      <!-- REJECT MODAL -->
      <div class="modal-backdrop" *ngIf="showRejectModal">
        <div class="modal-box animate-fade-in">
          <div class="modal-header-danger">
            <span class="icon">warning</span>
            <h3>Từ chối duyệt hoàn thành</h3>
          </div>
          <p class="modal-sub">Vui lòng nhập nhận xét và lý do từ chối để nhân sự cập nhật lại công việc:</p>
          <textarea [(ngModel)]="rejectReason" placeholder="Nhập lý do từ chối cụ thể (bắt buộc)..." rows="4" class="custom-textarea"></textarea>
          <div class="modal-btns">
            <button class="btn btn-secondary" (click)="closeRejectModal()">Hủy</button>
            <button class="btn btn-danger" (click)="confirmReject()" [disabled]="!rejectReason.trim()">
              <span class="icon">check</span> Xác nhận từ chối
            </button>
          </div>
        </div>
      </div>

      <!-- SUBTASKS & STEPS -->
      <div class="card section-card">
        <div class="section-card-header">
          <div class="sec-title-wrap">
            <span class="icon">checklist</span>
            <h3>Các bước & Việc con ({{ task.subtasks?.length || 0 }})</h3>
          </div>
        </div>

        <div class="group-block" *ngFor="let g of task.groupSubtasks">
          <div class="group-header">
            <span class="icon">folder</span>
            <strong>{{ g.name }}</strong>
            <button class="btn-text-danger" *ngIf="canManageTask(task)" (click)="deleteGroupRow(task.id, g.id)">Xoá nhóm</button>
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
              <span class="status-badge" [ngClass]="'status-' + s.status">{{ s.status }}</span>
              <span class="subtask-title">{{ s.title }}</span>
              <span class="meta-pill" *ngIf="s.estimateMinutes"><span class="icon">timer</span> {{ s.estimateMinutes }} phút</span>
              <span class="meta-pill" *ngIf="s.deadline"><span class="icon">event</span> {{ s.deadline | date:'dd/MM/yyyy' }}</span>
              <div class="subtask-actions">
                <ng-container *ngIf="s.steps.length === 0">
                  <button class="btn btn-secondary btn-xs" *ngIf="s.status !== 'IN_PROGRESS' && s.status !== 'DONE'"
                          (click)="setSubtaskStatus(task.id, s.id, 'IN_PROGRESS')">Bắt đầu</button>
                  <button class="btn btn-primary btn-xs" *ngIf="s.status !== 'DONE'"
                          (click)="setSubtaskStatus(task.id, s.id, 'DONE')">Xong</button>
                </ng-container>
                <button class="btn-icon-danger" *ngIf="canManageTask(task)" (click)="deleteSubtaskRow(task.id, s.id)" title="Xóa">
                  <span class="icon">delete</span>
                </button>
              </div>
            </div>
            <p class="note" *ngIf="s.note">{{ s.note }}</p>
            <div class="target-bar" *ngIf="s.isTarget">Target: {{ s.target ?? 0 }}/{{ s.estimateTarget ?? 0 }}</div>

            <div class="step-item" *ngFor="let step of s.steps">
              <span class="step-order">{{ step.stepOrder }}</span>
              <span class="step-name">{{ step.name }}</span>
              <span class="step-assignee" *ngIf="step.assignee"><span class="icon">person</span> {{ step.assignee.fullName }}</span>
              <span class="meta-pill" *ngIf="step.estimateMinutes">{{ step.estimateMinutes }} phút</span>
              <span class="status-badge" [ngClass]="'status-' + step.status">{{ step.status }}</span>
              <div class="step-actions">
                <button class="btn btn-secondary btn-xs" *ngIf="step.status !== 'IN_PROGRESS' && step.status !== 'DONE'"
                        (click)="setStepStatus(task.id, s.id, step.id, 'IN_PROGRESS')">Bắt đầu</button>
                <button class="btn btn-primary btn-xs" *ngIf="step.status !== 'DONE'"
                        (click)="setStepStatus(task.id, s.id, step.id, 'DONE')">Xong</button>
                <button class="btn-icon-danger" *ngIf="canManageTask(task)" (click)="deleteStepRow(task.id, s.id, step.id)" title="Xóa">
                  <span class="icon">delete</span>
                </button>
              </div>
            </div>
            <div class="form-inputs-row sub-row" *ngIf="canManageTask(task)">
              <input type="text" [(ngModel)]="newStepName[s.id]" placeholder="Tên bước mới..." class="custom-input-sm flex-1">
              <button class="btn btn-secondary btn-sm" (click)="addStepRow(task.id, s.id)" [disabled]="!newStepName[s.id]">+ Thêm bước</button>
            </div>
          </div>
        </ng-template>

        <div class="create-subtask-bar" *ngIf="canManageTask(task)">
          <div class="form-inputs-row">
            <input type="text" [(ngModel)]="newGroupName" placeholder="Tên nhóm mới..." class="custom-input flex-1">
            <button class="btn btn-secondary" (click)="addGroupRow(task.id)" [disabled]="!newGroupName">+ Thêm nhóm</button>
          </div>
          <div class="form-inputs-row">
            <select [(ngModel)]="newSubtaskGroupId" class="custom-select">
              <option [ngValue]="null">(Không thuộc nhóm)</option>
              <option *ngFor="let g of task.groupSubtasks" [ngValue]="g.id">{{ g.name }}</option>
            </select>
            <input type="text" [(ngModel)]="newSubtaskTitle" placeholder="Tên việc con mới..." class="custom-input flex-1">
            <input type="number" min="1" [(ngModel)]="newSubtaskEstimate" placeholder="Phút (ước lượng)" class="custom-input" style="width: 140px;">
            <button class="btn btn-primary" (click)="addSubtaskRow(task.id)" [disabled]="!newSubtaskTitle">+ Thêm việc con</button>
          </div>
        </div>
      </div>

      <!-- ATTACHMENTS & COMMENTS GRID -->
      <div class="split-grid">
        <!-- ATTACHMENTS -->
        <div class="card section-card">
          <div class="section-card-header">
            <div class="sec-title-wrap">
              <span class="icon">attach_file</span>
              <h3>Tệp đính kèm ({{ attachments.length }})</h3>
            </div>
          </div>

          <div class="attachment-list">
            <div class="attachment-row" *ngFor="let a of attachments">
              <span class="file-name-link" (click)="downloadAttachment(task.id, a)">
                <span class="icon">description</span> {{ a.fileName }}
              </span>
              <span class="file-meta">{{ formatSize(a.fileSize) }} · {{ a.uploadedByName }} · {{ a.createdAt | date:'dd/MM' }}</span>
              <button class="btn-icon-danger" (click)="deleteAttachment(task.id, a.id)">
                <span class="icon">delete</span>
              </button>
            </div>
            <div *ngIf="attachments.length === 0" class="empty-hint">Chưa có tệp đính kèm nào.</div>
          </div>

          <div class="upload-box">
            <input type="file" id="fileUploadInput" (change)="onFileSelected($event, task.id)" class="file-input-hidden">
            <label for="fileUploadInput" class="btn btn-secondary btn-full">
              <span class="icon">upload_file</span> Chọn tệp để tải lên
            </label>
          </div>
        </div>

        <!-- COMMENTS -->
        <div class="card section-card">
          <div class="section-card-header">
            <div class="sec-title-wrap">
              <span class="icon">chat</span>
              <h3>Bình luận & Trao đổi ({{ comments.length }})</h3>
            </div>
          </div>

          <div class="comments-list">
            <div class="comment-bubble" *ngFor="let c of comments">
              <div class="comment-head">
                <strong>{{ c.userFullName }}</strong>
                <span class="comment-time">{{ c.createdAt | date:'HH:mm - dd/MM' }}</span>
                <button class="btn-text-danger" *ngIf="canDeleteComment(c)" (click)="deleteComment(task.id, c.id)">Xoá</button>
              </div>
              <p class="comment-body">{{ c.content }}</p>
            </div>
            <div *ngIf="comments.length === 0" class="empty-hint">Chưa có bình luận nào.</div>
          </div>

          <div class="comment-input-area">
            <textarea [(ngModel)]="newComment" placeholder="Viết bình luận trao đổi..." rows="2" class="custom-textarea"></textarea>
            <button class="btn btn-primary" (click)="addComment(task.id)" [disabled]="!newComment.trim()">
              <span class="icon">send</span> Gửi
            </button>
          </div>
        </div>
      </div>

      <!-- PRIVATE NOTES -->
      <div class="card section-card">
        <div class="section-card-header">
          <div class="sec-title-wrap">
            <span class="icon">lock</span>
            <h3>Ghi chú cá nhân (chỉ bạn nhìn thấy)</h3>
          </div>
        </div>
        <textarea [(ngModel)]="privateNotes" placeholder="Ghi chú riêng tư về công việc này..." rows="3" class="custom-textarea"></textarea>
        <div class="notes-footer">
          <button class="btn btn-secondary btn-sm" (click)="savePrivateNotes(task.id)">
            <span class="icon">save</span> Lưu ghi chú
          </button>
        </div>
      </div>

      <!-- DEADLINE EXTENSION HISTORY -->
      <div class="card section-card" *ngIf="extensionHistory.length > 0">
        <div class="section-card-header">
          <div class="sec-title-wrap">
            <span class="icon">history</span>
            <h3>Lịch sử xin gia hạn hạn chót</h3>
          </div>
        </div>
        <div class="ext-list">
          <div class="ext-row" *ngFor="let e of extensionHistory">
            <span class="status-badge" [ngClass]="'ext-' + e.status">{{ extStatusLabel(e.status) }}</span>
            <span class="ext-dates">{{ e.currentDeadline | date:'dd/MM/yyyy' }} <span class="icon">arrow_forward</span> {{ e.requestedDeadline | date:'dd/MM/yyyy' }}</span>
            <span class="ext-meta">bởi <strong>{{ e.requestedBy.fullName }}</strong></span>
            <span class="ext-meta" *ngIf="e.reviewedBy">· duyệt bởi <strong>{{ e.reviewedBy.fullName }}</strong></span>
          </div>
        </div>
      </div>

      <!-- EDIT TASK MODAL (Lead & Admin) -->
      <div class="modal-backdrop" *ngIf="showEditModal" (click)="closeEditTaskModal()">
        <div class="modal-box" (click)="$event.stopPropagation()">
          <div class="modal-header-clean">
            <span class="icon">edit_note</span>
            <h3>Chỉnh sửa công việc</h3>
          </div>
          <p class="modal-sub">Cập nhật tiêu đề, mô tả, hạn chót và thông tin công việc.</p>

          <div class="modal-form-grid">
            <div class="modal-form-group">
              <label>Tiêu đề công việc <span class="required">*</span></label>
              <input type="text" [(ngModel)]="editTitle" class="custom-input" placeholder="Nhập tiêu đề...">
            </div>

            <div class="modal-form-group">
              <label>Mô tả chi tiết</label>
              <textarea [(ngModel)]="editDescription" rows="4" class="custom-textarea" placeholder="Nhập mô tả chi tiết..."></textarea>
            </div>

            <div class="modal-row-2">
              <div class="modal-form-group">
                <label>Hạn hoàn thành (Deadline)</label>
                <input type="date" [(ngModel)]="editDueDate" class="custom-input">
              </div>
              <div class="modal-form-group">
                <label>Thời gian ước lượng (phút)</label>
                <input type="number" min="1" [(ngModel)]="editEstimateMinutes" class="custom-input" placeholder="Phút...">
              </div>
            </div>

            <div class="modal-form-group">
              <label>Khu vực / Phân loại (Section)</label>
              <input type="text" [(ngModel)]="editSection" class="custom-input" placeholder="Ví dụ: Backend, Frontend, Design...">
            </div>
          </div>

          <div class="modal-btns">
            <button class="btn btn-secondary" (click)="closeEditTaskModal()">Hủy</button>
            <button class="btn btn-primary" (click)="saveEditTask(task.id)" [disabled]="!editTitle.trim()">
              <span class="icon">save</span> Lưu thay đổi
            </button>
          </div>
        </div>
      </div>

      <!-- REJECT COMPLETION MODAL -->
      <div class="modal-backdrop" *ngIf="showRejectModal" (click)="closeRejectModal()">
        <div class="modal-box" (click)="$event.stopPropagation()">
          <div class="modal-header-danger">
            <span class="icon">cancel</span>
            <h3>Từ chối phê duyệt hoàn thành</h3>
          </div>
          <p class="modal-sub">Vui lòng nhập nhận xét / lý do để người thực hiện chỉnh sửa lại:</p>

          <div class="modal-form-grid">
            <div class="modal-form-group">
              <label>Nhận xét & Lý do từ chối <span class="required">*</span></label>
              <textarea [(ngModel)]="rejectReason" rows="4" class="custom-textarea" placeholder="Nêu rõ lý do từ chối hoặc cần làm thêm gì..."></textarea>
            </div>
          </div>

          <div class="modal-btns">
            <button class="btn btn-secondary" (click)="closeRejectModal()">Hủy</button>
            <button class="btn btn-danger" (click)="confirmRejectCompletion()" [disabled]="!rejectReason.trim()">
              <span class="icon">send</span> Xác nhận từ chối
            </button>
          </div>
        </div>
      </div>
    </div>

    <ng-template #loadingOrEmpty>
      <div class="no-task-card" *ngIf="isLoading$ | async; else notFound">
        <span class="icon spinner">sync</span>
        <p>Đang tải thông tin công việc...</p>
      </div>
      <ng-template #notFound>
        <div class="no-task-card">
          <span class="icon">search_off</span>
          <p>Không tìm thấy công việc này hoặc bạn không có quyền truy cập.</p>
          <button class="btn btn-primary" (click)="router.navigate(['/tasks'])">Về danh sách công việc</button>
        </div>
      </ng-template>
    </ng-template>
  `,
  styles: [`
    .task-detail-container {
      max-width: 1300px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding-bottom: 40px;
    }

    /* TOP BAR */
    .top-nav-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
    }
    .btn-back {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: none;
      border: none;
      color: var(--color-primary);
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      padding: 6px 10px;
      border-radius: var(--radius-xs);
      transition: all 0.15s ease;
    }
    .btn-back:hover {
      background: var(--color-primary-light);
      transform: translateX(-2px);
    }
    .top-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .status-badge-lg {
      display: inline-flex;
      align-items: center;
      padding: 6px 16px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 0.02em;
    }

    /* ALERTS */
    .review-alert-card {
      background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 100%);
      border: 1.5px solid #fecdd3;
      border-radius: var(--radius-md);
      padding: 16px 20px;
      display: flex;
      gap: 14px;
      align-items: flex-start;
      box-shadow: var(--shadow-sm);
    }
    .approval-alert-card {
      background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
      border: 1.5px solid #fde68a;
      border-radius: var(--radius-md);
      padding: 16px 20px;
      display: flex;
      gap: 14px;
      align-items: flex-start;
      box-shadow: var(--shadow-sm);
    }
    .alert-icon-wrap {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .alert-icon-wrap.danger { background: #fee2e2; color: #e11d48; }
    .alert-icon-wrap.warning { background: #fef3c7; color: #d97706; }
    .alert-body { flex: 1; }
    .alert-title { margin: 0 0 6px 0; font-size: 15px; font-weight: 800; color: #881337; }
    .approval-alert-card .alert-title { color: #92400e; }
    .alert-desc { margin: 0; font-size: 13.5px; color: #b45309; }
    .alert-content-box {
      background: #ffffff;
      border: 1px solid #fecdd3;
      border-radius: 8px;
      padding: 10px 14px;
      font-size: 14px;
      color: #9f1239;
      line-height: 1.5;
      white-space: pre-line;
    }

    /* HERO CARD */
    .task-hero-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 24px 28px;
      box-shadow: var(--shadow-sm);
    }
    .hero-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
    }
    .hero-main { flex: 1; min-width: 250px; }
    .hero-badges { display: flex; gap: 8px; margin-bottom: 10px; align-items: center; flex-wrap: wrap; }
    .difficulty-badge {
      font-size: 11px;
      font-weight: 800;
      padding: 3px 8px;
      border-radius: var(--radius-full);
      background: #f1f5f9;
      color: #475569;
      border: 1px solid #e2e8f0;
    }
    .section-tag {
      font-size: 11px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: var(--radius-full);
      background: #f0fdf4;
      color: #166534;
      border: 1px solid #bbf7d0;
    }
    .hero-actions { display: flex; align-items: center; gap: 10px; }
    .task-main-title {
      font-size: 24px;
      font-weight: 800;
      color: var(--color-text);
      margin: 0;
      line-height: 1.35;
    }

    /* 3-COLUMN GRID */
    .details-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
    }
    @media (max-width: 992px) {
      .details-grid { grid-template-columns: 1fr; }
    }

    .detail-card {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 22px;
    }
    .highlight-card {
      border-top: 4px solid var(--color-primary);
    }

    .card-header-clean {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding-bottom: 12px;
      border-bottom: 1.5px solid var(--color-border);
    }
    .header-title-flex {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .card-header-clean h3 { margin: 0; font-size: 16px; font-weight: 800; }
    .card-icon { color: var(--color-primary); font-size: 20px; }
    .btn-text-action {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: none;
      border: none;
      color: var(--color-primary);
      font-weight: 700;
      font-size: 13px;
      cursor: pointer;
      padding: 2px 6px;
      border-radius: var(--radius-xs);
      transition: all 0.15s ease;
    }
    .btn-text-action:hover {
      background: var(--color-primary-light);
      text-decoration: underline;
    }
    .btn-text-action .icon {
      font-size: 16px;
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .field-label {
      font-size: 11px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--color-text-muted);
    }
    .desc-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: var(--radius-sm);
      padding: 12px 14px;
      font-size: 13.5px;
      color: var(--color-text);
      line-height: 1.6;
      white-space: pre-line;
      max-height: 180px;
      overflow-y: auto;
    }
    .text-muted-italic { color: var(--color-text-light); font-style: italic; font-size: 13px; margin: 0; }

    .difficulty-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      flex-wrap: wrap;
    }
    .diff-value { font-size: 15px; font-weight: 800; color: var(--color-text); }

    .field-value-highlight {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 700;
      color: var(--color-text);
    }
    .field-value-highlight .icon { color: var(--color-primary); font-size: 18px; }
    .field-value-success {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 700;
      color: var(--color-success);
    }

    .extension-form-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: var(--radius-sm);
      padding: 10px 12px;
      margin-top: 8px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .form-sub-label { font-size: 11.5px; font-weight: 700; color: var(--color-text-muted); }
    .form-inputs-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .flex-1 { flex: 1; }

    .pending-ext-banner {
      background: #fffbeb;
      border: 1px solid #fde68a;
      border-radius: var(--radius-sm);
      padding: 10px;
      display: flex;
      gap: 10px;
      align-items: center;
      margin-top: 8px;
      color: #92400e;
    }
    .pending-ext-banner strong { display: block; font-size: 12.5px; }
    .pending-ext-banner p { margin: 0; font-size: 11.5px; color: #b45309; }

    /* USER CHIPS */
    .user-chip-detail {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .user-avatar-sm {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 800;
      flex-shrink: 0;
    }
    .user-avatar-sm.creator { background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%); }
    .user-avatar-sm.assignee { background: linear-gradient(135deg, #38bdf8 0%, #6366f1 100%); }
    .user-info-text strong { display: block; font-size: 14px; color: var(--color-text); }
    .user-info-text span { font-size: 12px; color: var(--color-text-muted); }

    .field-label-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .btn-text-danger-sm {
      background: none;
      border: none;
      color: #e11d48;
      font-size: 11.5px;
      font-weight: 700;
      cursor: pointer;
      text-decoration: underline;
      padding: 0;
    }
    .btn-text-danger-sm:hover { color: #be123c; }

    .assignees-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .assignee-card-box {
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 8px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: background 0.15s ease;
    }
    .assignee-card-box:hover {
      background: #f1f5f9;
    }
    .unassigned-notice {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--color-text-light);
      font-size: 13.5px;
      padding: 8px 0;
    }
    .reassign-controls {
      display: flex;
      gap: 8px;
      margin-top: 8px;
    }
    .btn-text-danger {
      background: none;
      border: none;
      color: #e11d48;
      cursor: pointer;
      font-size: 12px;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 0;
    }
    .btn-text-danger:hover { text-decoration: underline; }

    /* WORKFLOW ACTION BUTTONS */
    .workflow-actions-wrapper {
      margin-top: auto;
      padding-top: 14px;
      border-top: 1.5px solid var(--color-border);
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .action-btn-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .btn-full { width: 100%; justify-content: center; }
    .btn-danger-outline {
      background: #ffffff;
      color: #e11d48;
      border: 1.5px solid #fecdd3;
    }
    .btn-danger-outline:hover {
      background: #fff1f2;
      border-color: #e11d48;
    }

    /* SECTION CARD & SUBTASKS */
    .section-card { padding: 22px; }
    .section-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1.5px solid var(--color-border);
    }
    .sec-title-wrap {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .sec-title-wrap h3 { margin: 0; font-size: 17px; font-weight: 800; }
    .sec-title-wrap .icon { color: var(--color-primary); font-size: 22px; }

    .group-block {
      margin-bottom: 16px;
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 12px;
    }
    .group-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: var(--color-text);
      margin-bottom: 10px;
    }
    .group-header strong { flex: 1; }
    .group-header .icon { color: #f59e0b; }

    .subtask-item {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 12px 14px;
      margin-bottom: 8px;
    }
    .subtask-main {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
    .subtask-title {
      flex: 1;
      min-width: 140px;
      font-weight: 700;
      font-size: 14px;
      color: var(--color-text);
    }
    .meta-pill {
      font-size: 11.5px;
      color: var(--color-text-muted);
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
    .subtask-actions { display: flex; align-items: center; gap: 6px; }
    .btn-xs { padding: 4px 10px; font-size: 11.5px; border-radius: 6px; }
    .btn-icon-danger {
      background: none;
      border: none;
      color: #94a3b8;
      cursor: pointer;
      padding: 4px;
      border-radius: 6px;
      display: flex;
      align-items: center;
    }
    .btn-icon-danger:hover { color: #e11d48; background: #fff1f2; }

    .step-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 0 8px 24px;
      border-top: 1px solid #f1f5f9;
      font-size: 13px;
      flex-wrap: wrap;
    }
    .step-order {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: #e2e8f0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 700;
    }
    .step-name { flex: 1; min-width: 120px; font-weight: 500; }
    .step-assignee { color: var(--color-text-muted); font-size: 12px; display: inline-flex; align-items: center; gap: 3px; }
    .step-actions { display: flex; gap: 4px; }
    .sub-row { margin-top: 10px; padding-left: 24px; }

    .create-subtask-bar {
      margin-top: 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: #f8fafc;
      border: 1.5px dashed var(--color-border);
      border-radius: var(--radius-sm);
      padding: 14px;
    }

    /* SPLIT GRID */
    .split-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }
    @media (max-width: 900px) {
      .split-grid { grid-template-columns: 1fr; }
    }

    .attachment-list, .comments-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 16px;
      max-height: 280px;
      overflow-y: auto;
    }
    .attachment-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding: 8px 12px;
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
    }
    .file-name-link {
      cursor: pointer;
      color: var(--color-primary);
      font-weight: 600;
      font-size: 13.5px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      flex: 1;
    }
    .file-name-link:hover { text-decoration: underline; }
    .file-meta { font-size: 11.5px; color: var(--color-text-muted); }
    .file-input-hidden { display: none; }
    .upload-box label { cursor: pointer; }

    .comment-bubble {
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 10px 14px;
    }
    .comment-head {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
    }
    .comment-head strong { font-size: 13px; color: var(--color-text); }
    .comment-time { font-size: 11px; color: var(--color-text-muted); margin-right: auto; }
    .comment-body { margin: 0; font-size: 13.5px; color: var(--color-text); line-height: 1.5; }
    .comment-input-area { display: flex; flex-direction: column; gap: 8px; }

    .empty-hint {
      text-align: center;
      padding: 24px 10px;
      color: var(--color-text-light);
      font-size: 13px;
      font-style: italic;
    }

    .notes-footer {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
    }

    .ext-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .ext-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      font-size: 13px;
      flex-wrap: wrap;
    }
    .ext-dates { font-weight: 700; color: var(--color-text); display: inline-flex; align-items: center; gap: 6px; }
    .ext-meta { color: var(--color-text-muted); }
    .ext-PENDING { background: #fffbeb; color: #d97706; border: 1px solid #fde68a; }
    .ext-APPROVED { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; }
    .ext-REJECTED { background: #fff1f2; color: #e11d48; border: 1px solid #fecdd3; }
    .ext-EXPIRED { background: #f1f5f9; color: #64748b; border: 1px solid #cbd5e1; }

    /* MODAL */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1100;
      padding: 20px;
    }
    .modal-box {
      background: #ffffff;
      border-radius: var(--radius-lg);
      padding: 28px;
      width: 100%;
      max-width: 540px;
      box-shadow: var(--shadow-lg);
      border: 1px solid var(--color-border);
    }
    .modal-header-clean {
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--color-primary);
      margin-bottom: 6px;
    }
    .modal-header-clean h3 { margin: 0; font-size: 20px; font-weight: 800; color: var(--color-text); }
    .modal-header-clean .icon { font-size: 26px; }

    .modal-header-danger {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #e11d48;
      margin-bottom: 8px;
    }
    .modal-header-danger h3 { margin: 0; font-size: 20px; font-weight: 800; color: #0f172a; }
    .modal-header-danger .icon { font-size: 28px; color: #e11d48; }

    .modal-sub { color: var(--color-text-muted); font-size: 13.5px; margin: 0 0 16px 0; }
    .modal-form-grid { display: flex; flex-direction: column; gap: 14px; }
    .modal-form-group { display: flex; flex-direction: column; gap: 6px; }
    .modal-form-group label { font-size: 12px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; }
    .modal-form-group .required { color: #e11d48; }
    .modal-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .modal-btns { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }

    /* CUSTOM INPUTS */
    .custom-input, .custom-select, .custom-textarea {
      width: 100%;
      font-size: 14px;
      padding: 10px 14px;
      border: 1.5px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: #ffffff;
      color: var(--color-text);
      outline: none;
      transition: all 0.2s ease;
    }
    .custom-input:focus, .custom-select:focus, .custom-textarea:focus {
      border-color: var(--color-primary);
      box-shadow: 0 0 0 3px var(--color-primary-glow);
    }
    .custom-input-sm, .custom-select-sm {
      font-size: 13px;
      padding: 6px 10px;
      border: 1.5px solid var(--color-border);
      border-radius: var(--radius-xs);
      background: #ffffff;
      color: var(--color-text);
      outline: none;
    }

    /* PULSE & SPINNER */
    .pulse { animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .5; } }

    .no-task-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 60px 20px;
      text-align: center;
      color: var(--color-text-muted);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }
    .spinner {
      font-size: 32px;
      color: var(--color-primary);
      animation: spin 1s linear infinite;
    }
    @keyframes spin { 100% { transform: rotate(360deg); } }
  `]
})
export class TaskDetailComponent implements OnInit {
  router = inject(Router);
  private route = inject(ActivatedRoute);
  private store = inject(Store);
  authStore = inject(AuthStore);
  private taskService = inject(TaskService);
  private userService = inject(UserService);

  task$ = this.store.select(selectSelectedTask);
  isLoading$ = this.store.select(selectTaskDetailLoading);

  users: UserInfo[] = [];
  selectedAssigneeId: number | null = null;

  // Edit Task Modal
  showEditModal = false;
  editTitle = '';
  editDescription = '';
  editDueDate = '';
  editEstimateMinutes: number | null = null;
  editSection = '';

  // Reject Modal
  showRejectModal = false;
  rejectReason = '';
  rejectingTaskId: number | null = null;

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

    if (!this.authStore.isMember()) {
      this.userService.listUsers().subscribe({
        next: (res) => this.users = res.data ?? [],
        error: () => {}
      });
    }
  }

  extStatusLabel(status: string): string {
    return this.extStatusLabels[status] ?? status;
  }

  initials(name?: string): string {
    return (name || '').split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  getActiveAssignments(task: Task): TaskAssignmentInfo[] {
    if (task.currentAssignments && task.currentAssignments.length > 0) {
      return task.currentAssignments;
    }
    return task.currentAssignment ? [task.currentAssignment] : [];
  }

  isCurrentAssignee(task: Task): boolean {
    const userId = this.authStore.user()?.id;
    if (!userId) return false;
    const active = this.getActiveAssignments(task);
    return active.some(a => a.assignee?.id === userId);
  }

  getAvailableUsers(task: Task): UserInfo[] {
    const activeIds = new Set(this.getActiveAssignments(task).map(a => a.assignee?.id));
    return this.users.filter(u => {
      if (activeIds.has(u.id)) return false;
      if (this.authStore.isLead()) {
        return u.role === 'MEMBER';
      }
      return true;
    });
  }

  canManageTask(task: Task): boolean {
    const user = this.authStore.user();
    if (!user) return false;
    if (this.authStore.isManager() || this.authStore.isLead()) return true;
    return String(task.createdBy?.id) === String(user.id) || task.createdBy?.email === user.email;
  }

  canSetDifficulty(task: Task): boolean {
    return this.canManageTask(task);
  }

  canExtendDeadline(task: Task): boolean {
    return this.canManageTask(task);
  }

  canDeleteComment(comment: TaskComment): boolean {
    const userId = this.authStore.user()?.id;
    return this.authStore.isManager() || comment.userId === userId;
  }

  // ---- Edit Task Modal Actions (Lead & Admin) ----

  openEditTaskModal(task: Task) {
    this.editTitle = task.title;
    this.editDescription = task.description || '';
    this.editDueDate = task.dueDate || '';
    this.editEstimateMinutes = task.estimateMinutes || null;
    this.editSection = task.section || '';
    this.showEditModal = true;
  }

  closeEditTaskModal() {
    this.showEditModal = false;
  }

  saveEditTask(taskId: number) {
    if (!this.editTitle.trim()) return;
    this.taskService.updateTask(taskId, {
      title: this.editTitle.trim(),
      description: this.editDescription.trim() || undefined,
      dueDate: this.editDueDate || undefined,
      estimateMinutes: this.editEstimateMinutes ?? undefined,
      section: this.editSection.trim() || undefined
    }).subscribe({
      next: () => {
        this.closeEditTaskModal();
        this.reloadTask(taskId);
      },
      error: (err) => alert(err.error?.message || 'Không thể cập nhật thông tin công việc')
    });
  }

  // ---- Task assignment & approval actions ----

  assignUser(taskId: number) {
    if (!this.selectedAssigneeId) return;
    this.taskService.assignTask(taskId, this.selectedAssigneeId).subscribe({
      next: () => {
        this.selectedAssigneeId = null;
        this.reloadTask(taskId);
      },
      error: (err) => alert(err.error?.message || 'Không thể phân công công việc')
    });
  }

  unassignSpecificUser(taskId: number, assigneeId: number) {
    if (confirm('Bạn có chắc muốn xóa người này khỏi danh sách thực hiện?')) {
      this.taskService.unassignTask(taskId, assigneeId).subscribe({
        next: () => this.reloadTask(taskId),
        error: (err) => alert(err.error?.message || 'Không thể xóa người tham gia')
      });
    }
  }

  unassignAllUsers(taskId: number) {
    if (confirm('Bạn có chắc muốn hủy tất cả phân công của công việc này?')) {
      this.taskService.unassignTask(taskId).subscribe({
        next: () => this.reloadTask(taskId),
        error: (err) => alert(err.error?.message || 'Không thể hủy phân công')
      });
    }
  }

  unassignUser(taskId: number) {
    this.unassignAllUsers(taskId);
  }

  submitCompletion(taskId: number) {
    if (confirm('Bạn có chắc muốn gửi hoàn thành công việc này để Leader / Admin duyệt?')) {
      this.taskService.submitCompletion(taskId).subscribe({
        next: () => this.reloadTask(taskId),
        error: (err) => alert(err.error?.message || 'Không thể gửi hoàn thành công việc')
      });
    }
  }

  approveCompletion(taskId: number) {
    if (confirm('Xác nhận duyệt hoàn thành công việc này?')) {
      this.taskService.approveCompletion(taskId).subscribe({
        next: () => this.reloadTask(taskId),
        error: (err) => alert(err.error?.message || 'Không thể duyệt hoàn thành')
      });
    }
  }

  openRejectModal(taskId: number) {
    this.rejectingTaskId = taskId;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal() {
    this.showRejectModal = false;
    this.rejectingTaskId = null;
    this.rejectReason = '';
  }

  confirmReject() {
    if (!this.rejectingTaskId || !this.rejectReason.trim()) return;
    this.taskService.rejectCompletion(this.rejectingTaskId, this.rejectReason.trim()).subscribe({
      next: () => {
        const tid = this.rejectingTaskId;
        this.closeRejectModal();
        if (tid) this.reloadTask(tid);
      },
      error: (err) => alert(err.error?.message || 'Không thể từ chối hoàn thành')
    });
  }

  confirmRejectCompletion() {
    this.confirmReject();
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
    this.taskService.extendDeadline(id, this.extendDaysInput, this.extendReasonInput).subscribe({
      next: () => {
        this.store.dispatch(TaskActions.loadTask({ id }));
        this.extendDaysInput = null;
        this.extendReasonInput = '';
      },
      error: (err) => alert(err.error?.message || 'Không thể gia hạn deadline')
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
