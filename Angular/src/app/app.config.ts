import { ApplicationConfig, provideZoneChangeDetection, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';

import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

import { taskReducer } from './store/task/task.reducer';
import { kpiReducer } from './store/kpi/kpi.reducer';
import { evaluationReducer } from './store/evaluation/evaluation.reducer';
import { categoryReducer } from './store/category/category.reducer';
import { TaskEffects } from './store/task/task.effects';
import { KpiEffects } from './store/kpi/kpi.effects';
import { EvaluationEffects } from './store/evaluation/evaluation.effects';
import { CategoryEffects } from './store/category/category.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    ),
    provideStore({
      task: taskReducer,
      kpi: kpiReducer,
      evaluation: evaluationReducer,
      category: categoryReducer,
    }),
    provideEffects([TaskEffects, KpiEffects, EvaluationEffects, CategoryEffects]),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() })
  ]
};
