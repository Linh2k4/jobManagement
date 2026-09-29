import { Routes } from '@angular/router';
import { NotificationListComponent } from './pages/notification-list/notification-list.component';

export const notificationsRoutes: Routes = [
  {
    path: '',
    component: NotificationListComponent
  }
];
