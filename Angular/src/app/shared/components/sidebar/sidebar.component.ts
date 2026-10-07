import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthStore } from '../../../core/stores/auth.store';
import { UiStore } from '../../../core/stores/ui.store';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
  badge?: number;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <div class="backdrop" *ngIf="ui.sidebarOpen()" (click)="ui.closeSidebar()"></div>

    <aside class="sidebar" [class.open]="ui.sidebarOpen()">
      <div class="sidebar-menu">
        <div class="menu-section">
          <h4>Dashboard</h4>
          <nav class="nav-list">
            <a
              *ngFor="let item of getMenuItems() | slice:0:3"
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: isExact(item.route) }"
              class="nav-item"
              (click)="ui.closeSidebar()"
            >
              <span class="icon">{{ item.icon }}</span>
              <span class="label">{{ item.label }}</span>
              <span *ngIf="item.badge" class="badge">{{ item.badge }}</span>
            </a>
          </nav>
        </div>

        <div class="menu-section">
          <h4>Quản lý Công việc</h4>
          <nav class="nav-list">
            <a
              *ngFor="let item of getMenuItems() | slice:3:6"
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: isExact(item.route) }"
              class="nav-item"
              (click)="ui.closeSidebar()"
            >
              <span class="icon">{{ item.icon }}</span>
              <span class="label">{{ item.label }}</span>
              <span *ngIf="item.badge" class="badge">{{ item.badge }}</span>
            </a>
          </nav>
        </div>

        <div class="menu-section">
          <h4>Quản lý Khác</h4>
          <nav class="nav-list">
            <a
              *ngFor="let item of getMenuItems() | slice:6"
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: isExact(item.route) }"
              class="nav-item"
              (click)="ui.closeSidebar()"
            >
              <span class="icon">{{ item.icon }}</span>
              <span class="label">{{ item.label }}</span>
              <span *ngIf="item.badge" class="badge">{{ item.badge }}</span>
            </a>
          </nav>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      background: var(--color-sidebar-bg);
      color: var(--color-sidebar-text);
      width: var(--sidebar-width);
      height: calc(100vh - var(--navbar-height));
      overflow-y: auto;
      -webkit-overflow-scrolling: touch;
      position: fixed;
      left: 0;
      top: var(--navbar-height);
      padding: 20px 14px;
      z-index: 250;
      transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), background-color 0.2s ease;
      border-right: 1px solid var(--color-border);
      box-shadow: 2px 0 12px rgba(14, 165, 233, 0.04);
    }
    .sidebar-menu { display: flex; flex-direction: column; gap: 20px; }
    .menu-section { display: flex; flex-direction: column; gap: 4px; }
    .menu-section h4 {
      margin: 0 10px 8px 10px;
      font-size: 11px;
      font-weight: 800;
      color: var(--color-text-light);
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }
    .nav-list { display: flex; flex-direction: column; gap: 4px; }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      border-radius: var(--radius-sm);
      text-decoration: none;
      color: var(--color-sidebar-text);
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      cursor: pointer;
      position: relative;
      font-size: 13.5px;
      font-weight: 600;
    }
    .nav-item .icon {
      font-size: 20px;
      min-width: 22px;
      color: var(--color-primary);
      transition: transform 0.2s ease;
    }
    .nav-item:hover {
      background: var(--color-sidebar-hover);
      color: var(--color-primary);
      transform: translateX(3px);
    }
    .nav-item.active {
      background: var(--gradient-primary);
      color: #ffffff;
      font-weight: 700;
      box-shadow: 0 4px 14px rgba(14, 165, 233, 0.35);
    }
    .nav-item.active .icon {
      color: #ffffff;
    }
    .nav-item:hover .icon {
      transform: scale(1.1);
    }
    .label { flex: 1; }
    .badge {
      background: #f43f5e;
      color: white;
      border-radius: 999px;
      padding: 2px 8px;
      font-size: 11px;
      font-weight: 700;
      box-shadow: 0 2px 6px rgba(244, 63, 94, 0.3);
    }

    .backdrop { display: none; }

    @media (max-width: 900px) {
      .sidebar {
        transform: translateX(-100%);
        box-shadow: var(--shadow-lg);
        height: calc(100vh - var(--navbar-height));
      }
      .sidebar.open { transform: translateX(0); }
      .backdrop {
        display: block;
        position: fixed;
        inset: var(--navbar-height) 0 0 0;
        background: rgba(15, 23, 42, 0.5);
        backdrop-filter: blur(4px);
        z-index: 240;
      }
    }
  `]
})
export class SidebarComponent {
  private authStore = inject(AuthStore);
  protected ui = inject(UiStore);

  private allMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: ['MEMBER', 'LEAD', 'MANAGER'] },
    { label: 'Công việc', icon: 'checklist', route: '/tasks', roles: ['MEMBER', 'LEAD', 'MANAGER'] },
    { label: 'Đánh giá', icon: 'rate_review', route: '/evaluations', roles: ['MEMBER', 'LEAD', 'MANAGER'] },

    { label: 'Thông báo', icon: 'notifications', route: '/notifications', roles: ['MEMBER', 'LEAD', 'MANAGER'] },
    { label: 'KPI nhóm', icon: 'leaderboard', route: '/kpi', roles: ['LEAD', 'MANAGER'] },
    { label: 'Thành viên & KPI', icon: 'group', route: '/kpi/members', roles: ['LEAD', 'MANAGER'] },
    { label: 'Duyệt gia hạn', icon: 'hourglass_top', route: '/tasks/deadline-extensions', roles: ['LEAD', 'MANAGER'] },
    { label: 'Loại công việc', icon: 'sell', route: '/category', roles: ['MEMBER', 'LEAD', 'MANAGER'] },

    { label: 'Nhân sự', icon: 'badge', route: '/users', roles: ['LEAD', 'MANAGER'] },
    { label: 'Cài đặt', icon: 'settings', route: '/settings', roles: ['MEMBER', 'LEAD', 'MANAGER'] }
  ];

  isExact(route: string): boolean {
    return route === '/tasks' || route === '/kpi' || route === '/settings';
  }

  getMenuItems(): MenuItem[] {
    const role = this.authStore.user()?.role || '';
    return this.allMenuItems.filter(item => item.roles.includes(role));
  }
}
