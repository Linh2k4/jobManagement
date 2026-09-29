import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TaskService } from '../../../../core/services/api/task.service';
import { TimelineMember, TimelineTask } from '../../../../core/models';

type ViewMode = 'week' | 'month' | 'quarter';

interface DayColumn {
  date: Date;
  label: string;
  isMonthStart: boolean;
  monthLabel: string;
}

interface PositionedBar extends TimelineTask {
  leftPx: number;
  widthPx: number;
  clampedLeft: boolean;
  clampedRight: boolean;
}

const DAY_WIDTH = 28;

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="timeline-controls">
      <button (click)="scrollToToday()">Hôm nay</button>
      <label class="toggle">
        <input type="checkbox" [(ngModel)]="includeCompleted" (change)="load()">
        Xem tất cả (kể cả đã hoàn thành)
      </label>
      <select [(ngModel)]="viewMode" (change)="rebuildColumns()">
        <option value="week">Tuần</option>
        <option value="month">Tháng</option>
        <option value="quarter">Quý</option>
      </select>
    </div>

    <div class="gantt" *ngIf="!loading; else loadingTpl">
      <div class="gantt-header">
        <div class="member-col-header"></div>
        <div class="days-scroll" #daysHeader>
          <div class="days-row">
            <div class="day-cell" *ngFor="let d of days" [style.width.px]="DAY_WIDTH" [class.today]="isToday(d.date)">
              <span class="month-label" *ngIf="d.isMonthStart">{{ d.monthLabel }}</span>
              <span class="day-label">{{ d.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="gantt-body">
        <div class="member-row" *ngFor="let m of members">
          <div class="member-col">
            <span class="name">{{ m.fullName }}</span>
            <span class="role">{{ m.role === 'LEAD' ? 'Lead' : 'Member' }}</span>
            <button class="view-btn" (click)="openMemberModal(m)">Xem</button>
          </div>
          <div class="bars-scroll">
            <div class="bars-track" [style.width.px]="days.length * DAY_WIDTH">
              <div class="today-line" [style.left.px]="todayOffsetPx()"></div>
              <div class="bar" *ngFor="let bar of positionedBars(m)"
                   [style.left.px]="bar.leftPx" [style.width.px]="bar.widthPx"
                   [ngClass]="barClass(bar)"
                   [title]="bar.title"
                   (click)="router.navigate(['/tasks', bar.id])">
                <span class="arrow left" *ngIf="bar.clampedLeft">◄</span>
                <span class="bar-label">{{ bar.title }}</span>
                <span class="arrow right" *ngIf="bar.clampedRight">▶</span>
              </div>
            </div>
          </div>
        </div>
        <div *ngIf="members.length === 0" class="empty">Không có thành viên nào.</div>
      </div>
    </div>
    <ng-template #loadingTpl><div class="loading">Đang tải...</div></ng-template>

    <!-- MEMBER FULL TIMELINE POPUP -->
    <div class="modal-overlay" *ngIf="modalMember" (click)="modalMember = null">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <strong>{{ modalMember.fullName }}</strong>
          <button class="close-btn" (click)="modalMember = null"><span class="icon">close</span></button>
        </div>
        <h4>Fast Task</h4>
        <div class="task-line" *ngFor="let t of fastTasksOf(modalMember)" (click)="router.navigate(['/tasks', t.id]); modalMember = null">
          <span>{{ t.title }}</span>
          <span class="status-badge">{{ t.status }}</span>
          <span class="muted">{{ t.endDate | date:'shortDate' }}</span>
        </div>
        <p *ngIf="fastTasksOf(modalMember).length === 0" class="muted">Không có Fast Task.</p>

        <h4>Multi-step Task</h4>
        <div class="task-line" *ngFor="let t of multiStepTasksOf(modalMember)" (click)="router.navigate(['/tasks', t.id]); modalMember = null">
          <span>{{ t.title }}</span>
          <span class="status-badge">{{ t.status }}</span>
          <span class="muted">{{ t.endDate | date:'shortDate' }}</span>
        </div>
        <p *ngIf="multiStepTasksOf(modalMember).length === 0" class="muted">Không có Multi-step Task.</p>
      </div>
    </div>
  `,
  styles: [`
    .timeline-controls { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
    .timeline-controls button, .timeline-controls select { padding: 8px 12px; border: 1px solid #ddd; border-radius: 10px; background: white; cursor: pointer; }
    .toggle { display: flex; align-items: center; gap: 6px; font-size: 14px; }
    .loading, .empty { text-align: center; color: #999; padding: 40px; }

    .gantt { border: 1px solid #eee; border-radius: 16px; overflow: hidden; }
    .gantt-header { display: flex; border-bottom: 2px solid #ddd; background: #fafafa; }
    .member-col-header { width: 180px; flex-shrink: 0; }
    .days-scroll { overflow-x: auto; flex: 1; }
    .days-row { display: flex; }
    .day-cell { flex-shrink: 0; text-align: center; font-size: 10px; color: #777; padding: 4px 0; border-left: 1px solid #f0f0f0; position: relative; }
    .day-cell.today { background: #e3f2fd; }
    .month-label { position: absolute; top: -14px; left: 2px; font-weight: bold; color: #333; font-size: 11px; white-space: nowrap; }

    .gantt-body { max-height: 500px; overflow-y: auto; }
    .member-row { display: flex; border-bottom: 1px solid #f0f0f0; }
    .member-col { width: 180px; flex-shrink: 0; padding: 10px; display: flex; flex-direction: column; gap: 2px; }
    .member-col .name { font-weight: 600; font-size: 13px; }
    .member-col .role { font-size: 11px; color: #999; }
    .view-btn { align-self: flex-start; margin-top: 4px; font-size: 11px; padding: 2px 8px; border: 1px solid #ddd; border-radius: 10px; background: white; cursor: pointer; }
    .bars-scroll { overflow-x: auto; flex: 1; }
    .bars-track { position: relative; min-height: 44px; padding: 6px 0; }
    .today-line { position: absolute; top: 0; bottom: 0; width: 2px; background: #2563eb; z-index: 1; }
    .bar {
      position: absolute; height: 22px; border-radius: 10px; background: #F59E0B; color: white;
      font-size: 11px; display: flex; align-items: center; padding: 0 6px; overflow: hidden;
      white-space: nowrap; cursor: pointer; top: 6px;
    }
    .bar.multi-step { background: #8B5CF6; }
    .bar.done { background: #22C55E; }
    .bar.overdue { background: #DC2626; }
    .bar-label { overflow: hidden; text-overflow: ellipsis; }
    .arrow { flex-shrink: 0; }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-content { background: white; border-radius: 16px; padding: 24px; width: 700px; max-width: 90vw; max-height: 80vh; overflow-y: auto; }
    .modal-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
    .close-btn { background: none; border: none; font-size: 20px; cursor: pointer; }
    .task-line { display: flex; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; font-size: 13px; }
    .task-line:hover { background: #f9f9f9; }
    .task-line span:first-child { flex: 1; }
    .status-badge { font-size: 11px; background: #eee; padding: 2px 8px; border-radius: 16px; }
    .muted { color: #999; font-size: 13px; }
  `]
})
export class TimelineViewComponent implements OnInit {
  router = inject(Router);
  private taskService = inject(TaskService);

  DAY_WIDTH = DAY_WIDTH;
  members: TimelineMember[] = [];
  days: DayColumn[] = [];
  rangeStart!: Date;
  loading = false;
  includeCompleted = false;
  viewMode: ViewMode = 'week';
  modalMember: TimelineMember | null = null;

  ngOnInit() {
    this.rebuildColumns();
    this.load();
  }

  rebuildColumns() {
    const now = new Date();
    this.rangeStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const monthsAhead = this.viewMode === 'week' ? 2 : this.viewMode === 'month' ? 4 : 9;
    const rangeEnd = new Date(now.getFullYear(), now.getMonth() + monthsAhead, 0);

    const days: DayColumn[] = [];
    const cursor = new Date(this.rangeStart);
    while (cursor <= rangeEnd) {
      days.push({
        date: new Date(cursor),
        label: cursor.getDate().toString(),
        isMonthStart: cursor.getDate() === 1,
        monthLabel: `Th${cursor.getMonth() + 1}/${cursor.getFullYear()}`
      });
      cursor.setDate(cursor.getDate() + 1);
    }
    this.days = days;
  }

  load() {
    this.loading = true;
    this.taskService.getTimeline(this.includeCompleted).subscribe({
      next: members => { this.members = members; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  isToday(d: Date): boolean {
    const now = new Date();
    return d.toDateString() === now.toDateString();
  }

  todayOffsetPx(): number {
    const diff = this.daysBetween(this.rangeStart, new Date());
    return diff * DAY_WIDTH;
  }

  scrollToToday() {
    // Viewport auto-includes today since range always starts this month;
    // native scroll wiring is out of scope for this pass.
  }

  private daysBetween(a: Date, b: Date): number {
    const msPerDay = 24 * 60 * 60 * 1000;
    const utcA = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate());
    const utcB = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate());
    return Math.round((utcB - utcA) / msPerDay);
  }

  positionedBars(member: TimelineMember): PositionedBar[] {
    const rangeEnd = this.days[this.days.length - 1]?.date ?? this.rangeStart;
    return member.tasks.map(t => {
      const start = new Date(t.startDate);
      const end = new Date(t.endDate);
      const clampedLeft = start < this.rangeStart;
      const clampedRight = end > rangeEnd;
      const visibleStart = clampedLeft ? this.rangeStart : start;
      const visibleEnd = clampedRight ? rangeEnd : end;
      const leftDays = Math.max(0, this.daysBetween(this.rangeStart, visibleStart));
      const widthDays = Math.max(1, this.daysBetween(visibleStart, visibleEnd) + 1);
      return {
        ...t,
        leftPx: leftDays * DAY_WIDTH,
        widthPx: widthDays * DAY_WIDTH,
        clampedLeft,
        clampedRight
      };
    });
  }

  barClass(bar: PositionedBar): string {
    if (bar.isOverdue) return 'overdue';
    if (bar.isDone) return 'done';
    return bar.timeCategory === 'MULTI_STEP' ? 'multi-step' : '';
  }

  openMemberModal(member: TimelineMember) {
    this.modalMember = member;
  }

  fastTasksOf(member: TimelineMember): TimelineTask[] {
    return member.tasks.filter(t => t.timeCategory === 'FAST');
  }

  multiStepTasksOf(member: TimelineMember): TimelineTask[] {
    return member.tasks.filter(t => t.timeCategory !== 'FAST');
  }
}
