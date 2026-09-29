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
      padding-top: 16px;
      z-index: 250;
      transition: transform 0.2s ease;
    }
    .sidebar-menu { padding: 0 12px; }
    .menu-section { margin-bottom: 20px; }
    .menu-section h4 { margin: 12px 10px 8px 10px; font-size: 11px; color: var(--color-sidebar-text-muted); text-transform: uppercase; letter-spacing: 0.04em; }
    .nav-list { display: flex; flex-direction: column; gap: 2px; }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: var(--radius-sm);
      text-decoration: none;
      color: var(--color-sidebar-text);
      transition: background 0.15s;
      cursor: pointer;
      position: relative;
      font-size: 14px;
    }
    .nav-item:hover { background: var(--color-sidebar-hover); }
    .nav-item.active { background: var(--color-sidebar-active-bg); color: white; font-weight: 600; }
    .icon { font-size: 17px; min-width: 22px; }
    .label { flex: 1; }
    .badge {
      background: var(--color-danger);
      color: white;
      border-radius: 18px;
      padding: 2px 8px;
      font-size: 11px;
      font-weight: bold;
    }

    .backdrop { display: none; }

    @media (max-width: 900px) {
      .sidebar {
        transform: translateX(-100%);
        box-shadow: var(--shadow-md);
        height: calc(100vh - var(--navbar-height));
      }
      .sidebar.open { transform: translateX(0); }
      .backdrop {
        display: block;
        position: fixed;
        inset: var(--navbar-height) 0 0 0;
        background: rgba(15, 23, 42, 0.35);
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

  getMenuItems(): MenuItem[] {
    const role = this.authStore.user()?.role || '';
    return this.allMenuItems.filter(item => item.roles.includes(role));
  }
}
