import { Routes } from '@angular/router';
import { SettingsComponent } from './pages/settings/settings.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { KpiConfigComponent } from './pages/kpi-config/kpi-config.component';

export const settingsRoutes: Routes = [
  { path: '', component: SettingsComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'kpi-config', component: KpiConfigComponent }
];
