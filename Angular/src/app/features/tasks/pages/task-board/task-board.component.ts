import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KanbanBoardComponent } from '../../components/kanban-board/kanban-board.component';
import { TaskListComponent } from '../task-list/task-list.component';
import { TimelineViewComponent } from '../../components/timeline-view/timeline-view.component';

type BoardTab = 'kanban' | 'timeline' | 'list';

@Component({
  selector: 'app-task-board',
  standalone: true,
  imports: [CommonModule, KanbanBoardComponent, TaskListComponent, TimelineViewComponent],
  template: `
    <div class="board-page animate-fade-in">
      <!-- PAGE HEADER -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Công việc</h1>
          <p class="page-subtitle">Quản lý, điều phối và theo dõi tiến độ các đầu việc</p>
        </div>
        <div class="header-actions">
          <div class="segmented-control">
            <button [class.active]="tab === 'kanban'" (click)="tab = 'kanban'">
              <span class="icon">view_kanban</span> Kanban
            </button>
            <button [class.active]="tab === 'timeline'" (click)="tab = 'timeline'">
              <span class="icon">view_timeline</span> Timeline
            </button>
            <button [class.active]="tab === 'list'" (click)="tab = 'list'">
              <span class="icon">view_list</span> Danh sách
            </button>
          </div>
          <button class="btn btn-primary" (click)="router.navigate(['/tasks/new'])">
            <span class="icon">add</span> Thêm công việc
          </button>
        </div>
      </div>

      <!-- MAIN TAB CONTENT -->
      <div class="tab-content">
        <app-kanban-board *ngIf="tab === 'kanban'"></app-kanban-board>
        <app-timeline-view *ngIf="tab === 'timeline'"></app-timeline-view>
        <app-task-list *ngIf="tab === 'list'"></app-task-list>
      </div>
    </div>
  `,
  styles: [`
    .board-page {
      max-width: 1440px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title {
      font-size: 26px;
      font-weight: 800;
      color: var(--color-text);
      margin: 0 0 4px 0;
      letter-spacing: -0.02em;
    }
    .page-subtitle {
      color: var(--color-text-muted);
      font-size: 14px;
      margin: 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .segmented-control {
      display: flex;
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 3px;
      box-shadow: var(--shadow-sm);
    }
    .segmented-control button {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border: none;
      background: none;
      font-size: 13px;
      font-weight: 700;
      color: var(--color-text-muted);
      border-radius: var(--radius-xs);
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .segmented-control button:hover {
      color: var(--color-primary);
    }
    .segmented-control button.active {
      background: var(--gradient-primary);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
    }
    .segmented-control button .icon { font-size: 17px; }
  `]
})
export class TaskBoardComponent {
  router = inject(Router);
  tab: BoardTab = 'kanban';
}
