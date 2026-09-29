import { Routes } from '@angular/router';
import { TaskBoardComponent } from './pages/task-board/task-board.component';
import { TaskDetailComponent } from './pages/task-detail/task-detail.component';
import { DeadlineExtensionsComponent } from './pages/deadline-extensions/deadline-extensions.component';

export const tasksRoutes: Routes = [
  {
    path: '',
    component: TaskBoardComponent
  },
  {
    path: 'deadline-extensions',
    component: DeadlineExtensionsComponent
  },
  {
    path: ':id',
    component: TaskDetailComponent
  }
];
