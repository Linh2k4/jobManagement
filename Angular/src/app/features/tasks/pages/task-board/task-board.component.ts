import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KanbanBoardComponent } from '../../components/kanban-board/kanban-board.component';
import { TaskListComponent } from '../task-list/task-list.component';
import { TimelineViewComponent } from '../../components/timeline-view/timeline-view.component';

type BoardTab = 'kanban' | 'list' | 'timeline';

@Component({
  selector: 'app-task-board',
  standalone: true,
  imports: [CommonModule, KanbanBoardComponent, TaskListComponent, TimelineViewComponent],
  template: `
    <div class="board-page">
      <div class="tabs">
        <button [class.active]="tab === 'kanban'" (click)="tab = 'kanban'">Kanban</button>
        <button [class.active]="tab === 'timeline'" (click)="tab = 'timeline'">Timeline</button>
        <button [class.active]="tab === 'list'" (click)="tab = 'list'">Danh sách</button>
      </div>

      <app-kanban-board *ngIf="tab === 'kanban'"></app-kanban-board>
      <app-task-list *ngIf="tab === 'list'"></app-task-list>
      <app-timeline-view *ngIf="tab === 'timeline'"></app-timeline-view>
    </div>
  `,
  styles: [`
    .board-page { padding: 20px; max-width: 1400px; margin: 0 auto; }
    .tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid #ddd; }
    .tabs button {
      padding: 10px 18px; border: none; background: none; cursor: pointer;
      font-weight: 600; color: #777; border-bottom: 2px solid transparent;
    }
    .tabs button.active { color: #2563eb; border-bottom-color: #2563eb; }
    .coming-soon { text-align: center; color: #999; padding: 60px; }
  `]
})
export class TaskBoardComponent {
  tab: BoardTab = 'kanban';
}
