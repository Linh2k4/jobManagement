import { Routes } from '@angular/router';
import { KpiDashboardComponent } from './pages/kpi-dashboard/kpi-dashboard.component';
import { MembersKpiComponent } from './pages/members-kpi/members-kpi.component';

export const kpiRoutes: Routes = [
  { path: '', component: KpiDashboardComponent },
  { path: 'members', component: MembersKpiComponent }
];
