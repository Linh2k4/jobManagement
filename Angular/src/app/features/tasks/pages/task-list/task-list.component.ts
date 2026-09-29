import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectAllTasks, selectTaskLoading, selectTaskStats } from '../../../../store/task/task.selectors';
import * as TaskActions from '../../../../store/task/task.actions';
import { TaskCreateModalComponent } from '../../components/task-create-modal/task-create-modal.component';
import { TaskStatus } from '../../../../core/models';
import { TaskStatusLabelPipe } from '../../../../shared/pipes/task-status-label.pipe';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TaskCreateModalComponent, TaskStatusLabelPipe],
  template: `
    <div class="task-list-container">
      <div class="header">
        <h1>Công việc</h1>
        <button (click)="createModal.open()">+ Việc mới</button>
      </div>

      <div class="filters">
        <select [(ngModel)]="selectedStatus" (change)="onFilterChange()">
          <option value="">Tất cả trạng thái</option>
          <option value="PENDING">Chờ xử lý</option>
          <option value="IN_PROGRESS">Đang thực hiện</option>
          <option value="DONE">Hoàn thành</option>
          <option value="CLOSED_LATE">Hoàn thành trễ</option>
          <option value="CANCELLED">Đã huỷ</option>
        </select>

        <input type="text" [(ngModel)]="searchText" (change)="onFilterChange()" placeholder="Tìm theo tiêu đề...">
      </div>

      <div class="stats" *ngIf="stats$ | async as stats">
        <span>Tổng: {{ stats.total }}</span>
        <span>Hoàn thành: {{ stats.done }}</span>
        <span>Quá hạn: {{ stats.overdue }}</span>
        <span>Tiến độ: {{ stats.completionRate | number:'1.0-0' }}%</span>
      </div>

      <div class="task-list">
        <div *ngIf="isLoading$ | async" class="loading">Đang tải...</div>
        <div *ngIf="!(isLoading$ | async) && (tasks$ | async)?.length === 0" class="empty">
          Không có công việc nào.
        </div>

        <div class="task-item" *ngFor="let task of tasks$ | async"
             [ngClass]="'status-' + task.status"
             (click)="viewTask(task.id)">
          <div class="task-header">
            <h3>{{ task.title }}</h3>
            <span class="priority-badge" *ngIf="task.priority" [ngClass]="'priority-' + task.priority">
              {{ task.priority }}
            </span>
          </div>
          <p class="description" *ngIf="task.description">{{ task.description }}</p>
          <div class="task-meta">
            <span class="deadline" *ngIf="task.dueDate"><span class="icon">calendar_month</span> {{ task.dueDate | date:'shortDate' }}</span>
            <span class="assignee" *ngIf="task.currentAssignment as a"><span class="icon">person</span> {{ a.assignee.fullName }}</span>
            <span class="creator"><span class="icon">edit_note</span> {{ task.createdBy.fullName }}</span>
          </div>
          <div class="task-footer">
            <span class="status" [ngClass]="'status-' + task.status">{{ task.status | taskStatusLabel }}</span>
          </div>
        </div>
      </div>

      <div class="pagination" *ngIf="totalPages > 1">
        <button [disabled]="currentPage === 0" (click)="previousPage()"><span class="icon">arrow_back</span> Trước</button>
        <span>Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
        <button [disabled]="currentPage === totalPages - 1" (click)="nextPage()">Sau <span class="icon">arrow_forward</span></button>
      </div>
    </div>

    <app-task-create-modal #createModal></app-task-create-modal>
  `,
  styles: [`
    .task-list-container { padding: 20px; max-width: 1000px; margin: 0 auto; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .header button { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 10px; cursor: pointer; }
    .filters { display: flex; gap: 10px; margin-bottom: 20px; }
    .filters select, .filters input { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .stats { display: flex; gap: 20px; margin-bottom: 20px; padding: 10px; background: #f5f5f5; border-radius: 10px; }
    .loading, .empty { text-align: center; color: #999; padding: 40px; }
    .task-item {
      background: white; border: 1px solid #ddd; border-radius: 16px; padding: 15px;
      margin-bottom: 10px; cursor: pointer; transition: 0.3s;
    }
    .task-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
    .task-header { display: flex; justify-content: space-between; align-items: start; margin-bottom: 10px; gap: 10px; }
    .task-header h3 { margin: 0; }
    .priority-badge {
      padding: 4px 8px; border-radius: 10px; font-size: 12px; font-weight: bold; white-space: nowrap;
    }
    .description { color: #555; margin: 0 0 10px; }
    .task-meta { display: flex; gap: 15px; font-size: 13px; color: #666; margin: 10px 0; flex-wrap: wrap; }
    .task-footer { display: flex; gap: 10px; }
    .status { padding: 4px 8px; border-radius: 10px; font-size: 12px; font-weight: bold; }
    .pagination { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 20px; }
  `]
})
export class TaskListComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);

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
