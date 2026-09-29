import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from '../../services/dashboard.service';
import { AuthStore } from '../../../../core/stores/auth.store';

type MemberDashboard = ReturnType<DashboardService['getMemberDashboardData']> extends import('rxjs').Observable<infer T> ? T : never;
type LeadDashboard = ReturnType<DashboardService['getLeadDashboardData']> extends import('rxjs').Observable<infer T> ? T : never;
type ManagerDashboard = ReturnType<DashboardService['getManagerDashboardData']> extends import('rxjs').Observable<infer T> ? T : never;

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard-home.component.html',
  styleUrls: ['./dashboard-home.component.scss']
})
export class DashboardHomeComponent implements OnInit {
  readonly loading = signal(true);
  readonly memberData = signal<MemberDashboard | null>(null);
  readonly leadData = signal<LeadDashboard | null>(null);
  readonly managerData = signal<ManagerDashboard | null>(null);

  private dashboardService = inject(DashboardService);
  private authStore = inject(AuthStore);

  readonly isManager = this.authStore.isManager;
  readonly isLead = this.authStore.isLead;
  readonly isMember = this.authStore.isMember;

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading.set(true);

    if (this.isManager()) {
      this.dashboardService.loadManagerDashboard();
      this.dashboardService.getManagerDashboardData().subscribe({
        next: (data) => { this.managerData.set(data); this.loading.set(false); },
        error: (error) => { console.error('Failed to load manager dashboard:', error); this.loading.set(false); }
      });
    } else if (this.isLead()) {
      this.dashboardService.loadLeadDashboard();
      this.dashboardService.getLeadDashboardData().subscribe({
        next: (data) => { this.leadData.set(data); this.loading.set(false); },
        error: (error) => { console.error('Failed to load lead dashboard:', error); this.loading.set(false); }
      });
    } else {
      this.dashboardService.loadMemberDashboard();
      this.dashboardService.getMemberDashboardData().subscribe({
        next: (data) => { this.memberData.set(data); this.loading.set(false); },
        error: (error) => { console.error('Failed to load member dashboard:', error); this.loading.set(false); }
      });
    }
  }

  getCompletionColor(rate: number): string {
    if (rate >= 80) return 'primary';
    if (rate >= 50) return 'accent';
    return 'warn';
  }

  /** Initials for the small avatar next to an overdue task's assignee. */
  initials(name: string | undefined): string {
    if (!name) return '?';
    return name.split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  daysOverdue(dueDate: string | undefined): number {
    if (!dueDate) return 0;
    const diffMs = new Date().setHours(0, 0, 0, 0) - new Date(dueDate).setHours(0, 0, 0, 0);
    return Math.max(0, Math.round(diffMs / (24 * 60 * 60 * 1000)));
  }

  /** CSS conic-gradient stops for the task-progress donut, in a fixed status order. */
  donutGradient(breakdown: { pending: number; inProgress: number; done: number; closedLate: number; cancelled: number }): string {
    const total = breakdown.pending + breakdown.inProgress + breakdown.done + breakdown.closedLate + breakdown.cancelled;
    if (!total) return 'conic-gradient(#e5e7eb 0 100%)';
    const segments: Array<[number, string]> = [
      [breakdown.done, '#22c55e'],
      [breakdown.inProgress, '#2563eb'],
      [breakdown.pending, '#f59e0b'],
      [breakdown.closedLate, '#ef4444'],
      [breakdown.cancelled, '#9ca3af']
    ];
    let acc = 0;
    const stops = segments
      .filter(([count]) => count > 0)
      .map(([count, color]) => {
        const start = (acc / total) * 100;
        acc += count;
        const end = (acc / total) * 100;
        return `${color} ${start}% ${end}%`;
      });
    return `conic-gradient(${stops.join(', ')})`;
  }
}
