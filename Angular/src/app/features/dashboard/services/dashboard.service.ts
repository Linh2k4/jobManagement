import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { combineLatest, map } from 'rxjs';
import {
  selectAllTasks,
  selectTaskStats,
  selectHighPriorityTasks,
  selectUpcomingDeadlines,
  selectOverdueTasks
} from '../../../store/task/task.selectors';
import { Task } from '../../../core/models';
import {
  selectCurrentUserKpi,
  selectTeamKpi,
  selectTopPerformers,
  selectTeamKpiTrends
} from '../../../store/kpi/kpi.selectors';
import {
  selectAllEvaluations,
  selectCurrentPeriod
} from '../../../store/evaluation/evaluation.selectors';
import * as TaskActions from '../../../store/task/task.actions';
import * as KpiActions from '../../../store/kpi/kpi.actions';
import * as EvaluationActions from '../../../store/evaluation/evaluation.actions';

/**
 * Dashboard Service - Orchestrates data from multiple stores
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  constructor(private store: Store) {}

  /** Status counts for the progress donut, shared by all three dashboard views. */
  private statusBreakdown(tasks: Task[]) {
    return {
      pending: tasks.filter(t => t.status === 'PENDING').length,
      inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
      done: tasks.filter(t => t.status === 'DONE').length,
      closedLate: tasks.filter(t => t.status === 'CLOSED_LATE').length,
      cancelled: tasks.filter(t => t.status === 'CANCELLED').length
    };
  }

  /** Overdue tasks sorted soonest-overdue-first, for the "trễ hạn" timeline. */
  private overdueList(tasks: Task[]) {
    return [...tasks]
      .sort((a, b) => new Date(a.dueDate || 0).getTime() - new Date(b.dueDate || 0).getTime());
  }

  getMemberDashboardData() {
    return combineLatest({
      tasks: this.store.select(selectAllTasks),
      taskStats: this.store.select(selectTaskStats),
      currentKpi: this.store.select(selectCurrentUserKpi),
      upcomingDeadlines: this.store.select(selectUpcomingDeadlines),
      highPriorityTasks: this.store.select(selectHighPriorityTasks),
      overdueTasks: this.store.select(selectOverdueTasks),
      evaluations: this.store.select(selectAllEvaluations),
      evaluationPeriod: this.store.select(selectCurrentPeriod)
    }).pipe(
      map(data => ({
        taskCount: data.taskStats.total,
        completionRate: data.taskStats.completionRate,
        statusBreakdown: this.statusBreakdown(data.tasks),
        kpi: data.currentKpi,
        upcomingDeadlines: data.upcomingDeadlines,
        highPriorityCount: data.highPriorityTasks.length,
        overdueTasksList: this.overdueList(data.overdueTasks),
        hasActiveEvaluation: data.evaluations.some(e =>
          e.status !== 'FINALIZED' && e.periodMonth === data.evaluationPeriod?.periodMonth
        )
      }))
    );
  }

  getLeadDashboardData() {
    return combineLatest({
      tasks: this.store.select(selectAllTasks),
      taskStats: this.store.select(selectTaskStats),
      teamKpi: this.store.select(selectTeamKpi),
      overdueTasks: this.store.select(selectOverdueTasks),
      upcomingDeadlines: this.store.select(selectUpcomingDeadlines)
    }).pipe(
      map(data => ({
        teamSize: data.teamKpi.length,
        avgKpi: data.teamKpi.length > 0
          ? data.teamKpi.reduce((sum, k) => sum + (k.kpiFinal || 0), 0) / data.teamKpi.length
          : 0,
        completionRate: data.taskStats.completionRate,
        statusBreakdown: this.statusBreakdown(data.tasks),
        overdueTasks: data.overdueTasks.length,
        overdueTasksList: this.overdueList(data.overdueTasks),
        upcomingDeadlines: data.upcomingDeadlines,
        teamMembers: data.teamKpi
      }))
    );
  }

  getManagerDashboardData() {
    return combineLatest({
      tasks: this.store.select(selectAllTasks),
      taskStats: this.store.select(selectTaskStats),
      teamKpi: this.store.select(selectTeamKpi),
      topPerformers: this.store.select(selectTopPerformers),
      kpiTrends: this.store.select(selectTeamKpiTrends),
      overdueTasks: this.store.select(selectOverdueTasks),
      upcomingDeadlines: this.store.select(selectUpcomingDeadlines)
    }).pipe(
      map(data => ({
        totalUsers: data.teamKpi.length,
        avgSystemKpi: data.teamKpi.length > 0
          ? data.teamKpi.reduce((sum, k) => sum + (k.kpiFinal || 0), 0) / data.teamKpi.length
          : 0,
        completionRate: data.taskStats.completionRate,
        statusBreakdown: this.statusBreakdown(data.tasks),
        overdueTasks: data.overdueTasks.length,
        overdueTasksList: this.overdueList(data.overdueTasks),
        upcomingDeadlines: data.upcomingDeadlines,
        topPerformers: data.topPerformers,
        kpiTrend: data.kpiTrends
      }))
    );
  }

  loadMemberDashboard() {
    this.store.dispatch(TaskActions.loadMyTasks({ page: 0, size: 50 }));
    this.store.dispatch(KpiActions.loadCurrentUserKpi());
    this.store.dispatch(EvaluationActions.loadEvaluations({}));
  }

  loadLeadDashboard() {
    this.store.dispatch(TaskActions.loadTasks({ page: 0, size: 100 }));
    this.store.dispatch(KpiActions.loadTeamKpiSummary({ page: 0, size: 50 }));
  }

  loadManagerDashboard() {
    this.store.dispatch(TaskActions.loadTasks({ page: 0, size: 200 }));
    this.store.dispatch(KpiActions.loadTeamKpiSummary({ page: 0, size: 100 }));
  }
}
