import { TaskState } from './task/task.state';
import { KpiState } from './kpi/kpi.state';
import { EvaluationState } from './evaluation/evaluation.state';
import { CategoryState } from './category/category.state';

/**
 * Root application state
 * Contains all feature states for task, KPI, evaluation, and category management
 */
export interface AppState {
  task: TaskState;
  kpi: KpiState;
  evaluation: EvaluationState;
  category: CategoryState;
}

/**
 * Root store configuration
 * Initialize NgRx in app.config.ts:
 *
 * import { provideStore } from '@ngrx/store';
 * import { provideEffects } from '@ngrx/effects';
 * import * as fromTask from '@app/store/task/task.reducer';
 * import * as fromKpi from '@app/store/kpi/kpi.reducer';
 * import * as fromEvaluation from '@app/store/evaluation/evaluation.reducer';
 * import * as fromCategory from '@app/store/category/category.reducer';
 * import { TaskEffects } from '@app/store/task/task.effects';
 * import { KpiEffects } from '@app/store/kpi/kpi.effects';
 * import { EvaluationEffects } from '@app/store/evaluation/evaluation.effects';
 * import { CategoryEffects } from '@app/store/category/category.effects';
 *
 * export const appConfig: ApplicationConfig = {
 *   providers: [
 *     provideStore({
 *       task: fromTask.taskReducer,
 *       kpi: fromKpi.kpiReducer,
 *       evaluation: fromEvaluation.evaluationReducer,
 *       category: fromCategory.categoryReducer,
 *     }),
 *     provideEffects([
 *       TaskEffects,
 *       KpiEffects,
 *       EvaluationEffects,
 *       CategoryEffects
 *     ]),
 *     ...
 *   ]
 * };
 */
