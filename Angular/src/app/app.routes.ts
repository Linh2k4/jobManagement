import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Role } from './core/models';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.dashboardRoutes)
      },
      {
        path: 'tasks',
        loadChildren: () => import('./features/tasks/tasks.routes').then(m => m.tasksRoutes)
      },
      {
        path: 'evaluations',
        loadChildren: () => import('./features/evaluations/evaluations.routes').then(m => m.evaluationsRoutes)
      },
      {
        path: 'notifications',
        loadChildren: () => import('./features/notifications/notifications.routes').then(m => m.notificationsRoutes)
      },
      {
        path: 'category',
        loadChildren: () => import('./features/category/category.routes').then(m => m.categoryRoutes)
      },
      {
        path: 'kpi',
        canActivate: [roleGuard([Role.MANAGER, Role.LEAD])],
        loadChildren: () => import('./features/kpi/kpi.routes').then(m => m.kpiRoutes)
      },
      {
        path: 'users',
        canActivate: [roleGuard([Role.MANAGER, Role.LEAD])],
        loadChildren: () => import('./features/users/users.routes').then(m => m.usersRoutes)
      },
      {
        path: 'settings',
        loadChildren: () => import('./features/settings/settings.routes').then(m => m.settingsRoutes)
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
