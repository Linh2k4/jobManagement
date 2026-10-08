import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectAllTasks, selectTaskLoading, selectTaskStats } from '../../../../store/task/task.selectors';
import * as TaskActions from '../../../../store/task/task.actions';
import { TaskCreateModalComponent } from '../../components/task-create-modal/task-create-modal.component';
import { AuthStore } from '../../../../core/stores/auth.store';
import { TaskStatus } from '../../../../core/models';
import { TaskStatusLabelPipe } from '../../../../shared/pipes/task-status-label.pipe';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskCreateModalComponent, TaskStatusLabelPipe],
  template: `
    <div class="task-list-container animate-fade-in">
      <div class="page-header">
        <div class="header-left">
          <h1>Danh sách Công việc</h1>
          <p class="subtitle">Theo dõi tiến độ, phân công và hạn chót các nhiệm vụ</p>
        </div>
        <button class="btn btn-primary" *ngIf="!authStore.isMember()" (click)="createModal.open()">
          <span class="icon">add_task</span>
          <span>Tạo việc mới</span>
        </button>
      </div>

      <!-- Quick KPI Stats Bar -->
      <div class="stats-bar" *ngIf="stats$ | async as stats">
        <div class="stat-pill">
          <span class="stat-label">Tổng cộng</span>
          <span class="stat-num">{{ stats.total }}</span>
        </div>
        <div class="stat-pill success">
          <span class="stat-label">Đã xong</span>
          <span class="stat-num">{{ stats.done }}</span>
        </div>
        <div class="stat-pill danger" [class.alert]="stats.overdue > 0">
          <span class="stat-label">Quá hạn</span>
          <span class="stat-num">{{ stats.overdue }}</span>
        </div>
        <div class="stat-pill info">
          <span class="stat-label">Tỷ lệ hoàn thành</span>
          <span class="stat-num">{{ stats.completionRate | number:'1.0-0' }}%</span>
        </div>
      </div>

      <!-- Search & Filter Controls -->
      <div class="filter-card card">
        <div class="filter-grid">
          <div class="search-wrap">
            <span class="icon">search</span>
            <input
              type="text"
              [(ngModel)]="searchText"
              (keyup.enter)="onFilterChange()"
              (blur)="onFilterChange()"
              placeholder="Tìm kiếm theo tiêu đề hoặc mô tả..."
              class="search-input"
            />
          </div>

          <div class="status-wrap">
            <select [(ngModel)]="selectedStatus" (change)="onFilterChange()" class="status-select">
              <option value="">Tất cả trạng thái</option>
              <option value="PENDING">Chưa thực hiện</option>
              <option value="IN_PROGRESS">Đang thực hiện</option>
              <option value="WAITING_APPROVAL">Đang chờ duyệt</option>
              <option value="DONE">Đã hoàn thành</option>
              <option value="CLOSED_LATE">Hoàn thành trễ</option>
              <option value="CANCELLED">Đã huỷ</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Task Items List -->
      <div class="task-items-wrapper">
        <div *ngIf="isLoading$ | async" class="loading-state">
          <span class="icon spinner">sync</span>
          <p>Đang tải danh sách công việc...</p>
        </div>

        <div *ngIf="!(isLoading$ | async) && (tasks$ | async)?.length === 0" class="empty-state">
          <span class="icon empty-icon">assignment_late</span>
          <p>Không tìm thấy công việc nào phù hợp.</p>
          <button class="btn btn-primary btn-sm" *ngIf="!authStore.isMember()" (click)="createModal.open()">
            <span class="icon">add</span> Tạo công việc mới
          </button>
        </div>

        <div class="task-card card-interactive"
             *ngFor="let task of tasks$ | async"
             (click)="viewTask(task.id)">
          <div class="task-top">
            <div class="task-title-group">
              <h3>{{ task.title }}</h3>
              <span class="priority-badge" *ngIf="task.priority" [ngClass]="'priority-' + task.priority">
                {{ task.priority }}
              </span>
            </div>
            <span class="status-badge" [ngClass]="'status-' + task.status">
              {{ task.status | taskStatusLabel }}
            </span>
          </div>

          <p class="task-desc" *ngIf="task.description">{{ task.description }}</p>

          <div class="task-bottom">
            <div class="meta-tag" *ngIf="task.dueDate">
              <span class="icon">event</span>
              <span>Hạn: <strong>{{ task.dueDate | date:'dd/MM/yyyy' }}</strong></span>
            </div>
            <div class="meta-tag" *ngIf="task.currentAssignment as a">
              <span class="icon">person</span>
              <span>Giao cho: <strong>{{ a.assignee.fullName }}</strong></span>
            </div>
            <div class="meta-tag creator" *ngIf="task.createdBy">
              <span class="icon">edit_note</span>
              <span>Tạo bởi: {{ task.createdBy.fullName }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination-bar" *ngIf="totalPages > 1">
        <button class="btn btn-secondary btn-sm" [disabled]="currentPage === 0" (click)="previousPage()">
          <span class="icon">chevron_left</span> Trước
        </button>
        <span class="page-info">Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="btn btn-secondary btn-sm" [disabled]="currentPage === totalPages - 1" (click)="nextPage()">
          Sau <span class="icon">chevron_right</span>
        </button>
      </div>
    </div>

    <app-task-create-modal #createModal></app-task-create-modal>
  `,
  styles: [`
    .task-list-container {
      max-width: 1300px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      h1 {
        font-size: 26px;
        font-weight: 800;
        margin: 0 0 4px;
        background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }

      .subtitle {
        margin: 0;
        color: var(--color-text-muted);
        font-size: 13.5px;
      }
    }

    .stats-bar {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 12px;

      .stat-pill {
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm);
        padding: 12px 16px;
        display: flex;
        flex-direction: column;
        gap: 4px;
        box-shadow: var(--shadow-sm);
        transition: all 0.2s ease;

        &:hover {
          transform: translateY(-2px);
          border-color: var(--color-primary-border);
        }

        .stat-label {
          font-size: 11.5px;
          font-weight: 700;
          color: var(--color-text-muted);
          text-transform: uppercase;
        }

        .stat-num {
          font-size: 20px;
          font-weight: 800;
          color: var(--color-text);
        }

        &.success .stat-num { color: var(--color-success); }
        &.danger.alert .stat-num { color: var(--color-danger); }
        &.info .stat-num { color: var(--color-primary); }
      }
    }

    .filter-card {
      padding: 16px;
    }

    .filter-grid {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;

      .search-wrap {
        flex: 1;
        min-width: 250px;
        position: relative;
        display: flex;
        align-items: center;

        .icon {
          position: absolute;
          left: 12px;
          color: #94a3b8;
          font-size: 20px;
        }

        .search-input {
          padding-left: 38px;
          height: 42px;
          border-radius: 10px;
        }
      }

      .status-wrap {
        min-width: 180px;

        .status-select {
          height: 42px;
          border-radius: 10px;
          font-weight: 600;
        }
      }
    }

    .task-items-wrapper {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .task-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 18px 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      cursor: pointer;
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      box-shadow: var(--shadow-sm);

      &:hover {
        transform: translateY(-2px);
        border-color: var(--color-primary-border);
        box-shadow: var(--shadow-md);
      }

      .task-top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 12px;

        .task-title-group {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;

          h3 {
            margin: 0;
            font-size: 16px;
            font-weight: 700;
            color: var(--color-text);
          }
        }
      }

      .task-desc {
        margin: 0;
        font-size: 13.5px;
        color: var(--color-text-muted);
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .task-bottom {
        display: flex;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
        padding-top: 10px;
        border-top: 1px solid #f1f5f9;

        .meta-tag {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12.5px;
          color: var(--color-text-muted);

          .icon {
            font-size: 16px;
            color: var(--color-primary);
          }

          strong {
            color: var(--color-text);
          }

          &.creator {
            margin-left: auto;
            color: var(--color-text-light);
          }
        }
      }
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 40px 20px;
      color: var(--color-text-muted);
      background: white;
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;

      .empty-icon {
        font-size: 44px;
        color: #cbd5e1;
      }

      .spinner {
        animation: spin 1s linear infinite;
        font-size: 28px;
        color: var(--color-primary);
      }
    }

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }

    .pagination-bar {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 14px;
      margin-top: 10px;

      .page-info {
        font-size: 13.5px;
        font-weight: 700;
        color: var(--color-text-muted);
      }
    }
  `]
})
export class TaskListComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);
  authStore = inject(AuthStore);

  tasks$ = this.store.select(selectAllTasks);
  isLoading$ = this.store.select(selectTaskLoading);
  stats$ = this.store.select(selectTaskStats);

  selectedStatus: TaskStatus | '' = '';
  searchText = '';
  currentPage = 0;
  pageSize = 20;
  totalPages = 1;

  ngOnInit() {
    this.loadTasks();
  }

  loadTasks() {
    this.store.dispatch(TaskActions.loadTasks({
      page: this.currentPage,
      size: this.pageSize,
      filter: {
        status: this.selectedStatus || undefined,
        searchText: this.searchText || undefined
      }
    }));
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadTasks();
  }

  viewTask(id: number) {
    this.router.navigate(['/tasks', id]);
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadTasks();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadTasks();
    }
  }
}
