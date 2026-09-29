import { Routes } from '@angular/router';
import { EvaluationListComponent } from './pages/evaluation-list/evaluation-list.component';

export const evaluationsRoutes: Routes = [
  {
    path: '',
    component: EvaluationListComponent
  }
];
