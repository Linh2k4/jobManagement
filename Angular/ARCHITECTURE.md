# Angular Architecture - Job Management System

Đây là cấu trúc **Angular 18 Standalone + Signal-Based** cho dự án Job Management.

## 📁 Cấu trúc thư mục

```
src/app/
├── core/                              # Singleton services, guards, stores
│   ├── guards/
│   │   ├── auth.guard.ts              # Check isAuthenticated
│   │   └── role.guard.ts              # Check role-based access
│   │
│   ├── interceptors/
│   │   ├── auth.interceptor.ts        # Inject JWT token
│   │   └── error.interceptor.ts       # Handle HTTP errors
│   │
│   ├── services/
│   │   ├── api.service.ts             # Base HTTP wrapper
│   │   ├── auth.service.ts            # Login/logout/refresh
│   │   └── index.ts                   # Export all services
│   │
│   ├── stores/
│   │   ├── auth.store.ts              # Signal: user, token, isAuthenticated
│   │   ├── notification.store.ts      # Signal: unreadCount
│   │   └── index.ts
│   │
│   └── models/
│       ├── user.model.ts
│       ├── role.enum.ts
│       └── index.ts
│
├── shared/                            # Reusable components, models, utils
│   ├── components/
│   │   ├── button/
│   │   ├── card/
│   │   ├── modal/
│   │   └── form/
│   │
│   ├── directives/
│   │   ├── has-role.directive.ts      # [appHasRole]="MANAGER"
│   │   └── debounce-click.directive.ts
│   │
│   ├── pipes/
│   │   ├── safe.pipe.ts               # SafeHtml
│   │   └── truncate.pipe.ts
│   │
│   ├── models/
│   │   ├── api-response.model.ts      # ApiResponse<T>, PagedResponse<T>
│   │   ├── pagination.model.ts
│   │   └── index.ts
│   │
│   ├── utils/
│   │   ├── validators.ts              # Form validators
│   │   ├── helpers.ts                 # Utility functions
│   │   └── constants.ts               # App constants
│   │
│   ├── layout/
│   │   ├── header/
│   │   │   ├── header.component.ts    # Navigation, logout
│   │   │   └── header.component.html
│   │   │
│   │   └── sidebar/
│   │       ├── sidebar.component.ts   # Menu by role
│   │       └── sidebar.component.html
│   │
│   └── index.ts                       # Star exports
│
├── features/                          # Feature modules (lazy loaded)
│   ├── auth/
│   │   ├── pages/
│   │   │   ├── login/
│   │   │   │   ├── login.component.ts
│   │   │   │   ├── login.component.html
│   │   │   │   └── login.component.scss
│   │   │   │
│   │   │   └── register/
│   │   │       ├── register.component.ts
│   │   │       ├── register.component.html
│   │   │       └── register.component.scss
│   │   │
│   │   ├── services/
│   │   │   └── auth.service.ts        # (or use core/services/auth.service)
│   │   │
│   │   └── auth.routes.ts             # Lazy-loaded routes
│   │
│   ├── tasks/
│   │   ├── pages/
│   │   │   ├── task-list/
│   │   │   └── task-detail/
│   │   │
│   │   ├── components/
│   │   │   ├── task-form/
│   │   │   ├── task-card/
│   │   │   └── task-filter/
│   │   │
│   │   ├── services/
│   │   │   └── task.service.ts        # Task API calls
│   │   │
│   │   ├── store/ (optional)
│   │   │   ├── task.store.ts          # Signal: tasks, filters, loading
│   │   │   └── task.actions.ts
│   │   │
│   │   └── tasks.routes.ts
│   │
│   ├── evaluations/
│   │   ├── pages/
│   │   ├── components/
│   │   ├── services/
│   │   └── evaluations.routes.ts
│   │
│   ├── dashboard/
│   │   ├── pages/
│   │   │   ├── dashboard-home/
│   │   │   ├── kpi/
│   │   │   └── team-kpi/
│   │   │
│   │   ├── components/
│   │   │   ├── kpi-card/
│   │   │   ├── chart/
│   │   │   └── stats/
│   │   │
│   │   ├── services/
│   │   │   └── dashboard.service.ts
│   │   │
│   │   └── dashboard.routes.ts
│   │
│   └── notifications/
│       ├── pages/
│       ├── components/
│       ├── services/
│       └── notifications.routes.ts
│
├── layouts/
│   └── main-layout/
│       ├── main-layout.component.ts   # AppComponent wrapper
│       ├── main-layout.component.html
│       └── main-layout.routes.ts      # Child routes (with header/sidebar)
│
├── app.routes.ts                      # Root routes (lazy load features)
├── app.config.ts                      # Providers (interceptors, guards)
├── app.component.ts                   # Root component
├── app.component.html
└── app.component.scss
```

## 🎯 Patterns & Best Practices

### 1. **Signal-Based State Management (không NgRx)**
```typescript
// core/stores/task.store.ts
import { Injectable } from '@angular/core';
import { signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TaskStore {
  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(false);
  readonly filter = signal<TaskFilter>({ status: 'ALL' });

  readonly filteredTasks = computed(() =>
    this.tasks().filter(t => this.matchesFilter(t, this.filter()))
  );

  readonly totalCount = computed(() => this.tasks().length);

  // Methods to update state
  loadTasks(filter: TaskFilter) {
    this.loading.set(true);
    // API call here
  }
}
```

### 2. **Feature-Based Services**
```typescript
// features/tasks/services/task.service.ts
import { Injectable } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class TaskService {
  constructor(private api: ApiService) {}

  getTasks(filter?: any) {
    return this.api.get<Task[]>('tasks', filter);
  }

  createTask(task: CreateTaskRequest) {
    return this.api.post<Task>('tasks', task);
  }

  updateTask(id: number, task: UpdateTaskRequest) {
    return this.api.put<Task>(`tasks/${id}`, task);
  }

  deleteTask(id: number) {
    return this.api.delete<void>(`tasks/${id}`);
  }
}
```

### 3. **Lazy-Loaded Routes**
```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
      },
      {
        path: 'tasks',
        loadChildren: () =>
          import('./features/tasks/tasks.routes').then(m => m.TASKS_ROUTES)
      },
      {
        path: 'evaluations',
        canActivate: [roleGuard([Role.MANAGER, Role.LEAD])],
        loadChildren: () =>
          import('./features/evaluations/evaluations.routes').then(m => m.EVALUATIONS_ROUTES)
      }
    ]
  },
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  }
];
```

### 4. **Standalone Components**
```typescript
// features/tasks/pages/task-list/task-list.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskStore } from '../../store/task.store';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, TaskCardComponent, TaskFilterComponent],
  templateUrl: './task-list.component.html'
})
export class TaskListComponent implements OnInit {
  readonly tasks = this.taskStore.tasks;
  readonly loading = this.taskStore.loading;

  constructor(
    private taskStore: TaskStore,
    private taskService: TaskService
  ) {}

  ngOnInit() {
    this.taskService.getTasks().subscribe(response => {
      this.taskStore.tasks.set(response.data);
    });
  }
}
```

### 5. **Interceptors for JWT**
```typescript
// core/interceptors/auth.interceptor.ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authStore = inject(AuthStore);
  const token = authStore.token();

  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req);
};
```

## 🔒 Access Control

### Role-Based Routes
```typescript
{
  path: 'admin',
  canActivate: [roleGuard([Role.MANAGER])],
  loadChildren: () => import('./features/admin/admin.routes')
}
```

### Role-Based Visibility (Directive)
```typescript
// shared/directives/has-role.directive.ts
@Directive({
  selector: '[appHasRole]',
  standalone: true
})
export class HasRoleDirective {
  @Input() set appHasRole(roles: Role[]) {
    const userRole = this.authStore.user()?.role;
    const hasAccess = userRole && roles.includes(userRole);
    this.viewContainer.clear();
    if (hasAccess) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authStore: AuthStore
  ) {}
}
```

Usage:
```html
<button *appHasRole="[Role.MANAGER, Role.LEAD]">Edit Users</button>
```

## 🔄 Data Flow

```
Component
    ↓ (injects)
Feature Service (TaskService)
    ↓ (calls)
Api Service (HttpClient + interceptors)
    ↓ (decorated with)
AuthInterceptor (adds JWT token)
ErrorInterceptor (shows snackbar on error)
    ↓ (sends to)
Backend API
    ↓ (returns)
ApiResponse<T>
    ↓ (update)
Signal Store (TaskStore)
    ↓ (component reads)
Component signals (this.taskStore.tasks())
    ↓ (renders)
Template with *ngIf, *ngFor, signal()
```

## 📦 Environment Setup

**Development** (proxy):
```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: '/api/v1'  // Proxy tới http://localhost:8080/api/v1
};
```

**Production** (CORS):
```typescript
// environment.production.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.jobmanagement.com/api/v1'
};
```

## ✨ Key Takeaways

- **Signals**: Use `signal()`, `computed()` thay vì RxJS observers khi có thể
- **Standalone**: Mỗi component là `standalone: true`
- **Lazy Load**: Features load on-demand via `loadChildren`
- **Feature Isolation**: Mỗi feature có service, store, và routes riêng
- **Shared**: Shared folder chỉ chứa reusable components, directives, models
- **Core**: Core folder chỉ import 1 lần trong AppModule (AppConfig cho standalone)

Cấu trúc này scale tốt từ 10 screens đến 100+ screens trong 1 ứng dụng.
