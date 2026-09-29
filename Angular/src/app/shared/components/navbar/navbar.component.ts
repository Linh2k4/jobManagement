import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStore } from '../../../core/stores/auth.store';
import { UiStore } from '../../../core/stores/ui.store';
import { RoleLabelPipe } from '../../pipes/role-label.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RoleLabelPipe],
  template: `
    <nav class="navbar">
      <div class="navbar-content">
        <div class="navbar-brand" routerLink="/dashboard">
          <button (click)="$event.stopPropagation(); ui.toggleSidebar()" class="btn-hamburger icon" aria-label="Menu">menu</button>
          <img src="logo.svg" alt="JobManagement" class="brand-logo">
          <h2>JobManagement</h2>
        </div>

        <div class="navbar-center" *ngIf="authStore.user() as user">
          <span class="user-welcome">Xin chào, <strong>{{ user.fullName }}</strong></span>
          <span class="user-role">{{ user.role | roleLabel }}</span>
        </div>

        <div class="navbar-end">
          <button (click)="toggleMenu()" class="btn-avatar" aria-label="Tài khoản">
            {{ initials() }}
          </button>
          <div class="user-menu" *ngIf="menuOpen">
            <button (click)="goToProfile()" class="menu-item"><span class="icon">person</span> Hồ sơ</button>
            <button (click)="goToSettings()" class="menu-item"><span class="icon">settings</span> Cài đặt</button>
            <hr>
            <button (click)="logout()" class="menu-item logout"><span class="icon">logout</span> Đăng xuất</button>
          </div>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      color: var(--color-text);
      border-bottom: 1px solid var(--color-border);
      box-shadow: 0 2px 12px rgba(14, 165, 233, 0.06);
      position: sticky;
      top: 0;
      z-index: 200;
      width: 100%;
    }
    .navbar-content {
      display: flex;
      align-items: center;
      padding: 0 24px 0 0;
      height: var(--navbar-height);
      width: 100%;
    }
    .navbar-brand {
      width: var(--sidebar-width);
      min-width: var(--sidebar-width);
      height: var(--navbar-height);
      padding: 0 16px 0 20px;
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      border-right: 1px solid var(--color-border);
      box-sizing: border-box;
      user-select: none;
      transition: background 0.15s ease;
    }
    .navbar-brand:hover {
      background: rgba(240, 249, 255, 0.6);
    }
    .brand-logo {
      width: 32px;
      height: 32px;
      border-radius: 9px;
      box-shadow: 0 3px 8px rgba(14, 165, 233, 0.35);
      flex-shrink: 0;
    }
    .navbar-brand h2 {
      margin: 0;
      font-size: 18px;
      font-weight: 800;
      background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      letter-spacing: -0.03em;
      white-space: nowrap;
    }
    .btn-hamburger {
      display: none;
      background: none;
      border: 1px solid var(--color-border);
      color: var(--color-text);
      cursor: pointer;
      font-size: 20px;
      padding: 6px;
      border-radius: var(--radius-sm);
      transition: all 0.2s ease;
      margin-right: 4px;
    }
    .btn-hamburger:hover {
      background: var(--color-primary-light);
      border-color: var(--color-primary-border);
      color: var(--color-primary);
    }
    .navbar-center {
      flex: 1;
      padding: 0 20px;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .user-welcome {
      font-size: 13.5px;
      color: var(--color-text-muted);
    }
    .user-welcome strong {
      color: var(--color-text);
      font-weight: 700;
    }
    .user-role {
      background: var(--color-primary-subtle);
      color: var(--color-primary);
      border: 1px solid var(--color-primary-border);
      padding: 3px 12px;
      border-radius: 999px;
      font-size: 11.5px;
      font-weight: 700;
      letter-spacing: 0.02em;
    }
    .navbar-end {
      position: relative;
      margin-left: auto;
      padding-right: 8px;
    }
    .btn-avatar {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      border: 2px solid #ffffff;
      background: var(--gradient-primary);
      color: white;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .btn-avatar:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(14, 165, 233, 0.45);
    }
    .user-menu {
      position: absolute;
      top: calc(100% + 10px);
      right: 0;
      background: var(--color-surface);
      color: var(--color-text);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-lg);
      min-width: 200px;
      z-index: 1000;
      overflow: hidden;
      padding: 6px;
      animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .menu-item {
      width: 100%;
      padding: 10px 14px;
      border: none;
      background: none;
      text-align: left;
      cursor: pointer;
      font-size: 14px;
      font-weight: 600;
      color: var(--color-text);
      border-radius: var(--radius-sm);
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .menu-item:hover {
      background: var(--color-primary-light);
      color: var(--color-primary);
    }
    .menu-item.logout {
      color: var(--color-danger);
    }
    .menu-item.logout:hover {
      background: var(--color-danger-bg);
      color: var(--color-danger);
    }
    hr {
      margin: 6px 0;
      border: none;
      border-top: 1px solid var(--color-border);
    }

    @media (max-width: 900px) {
      .navbar-brand {
        width: auto;
        min-width: auto;
        border-right: none;
        padding-left: 16px;
      }
      .btn-hamburger { display: inline-flex; align-items: center; justify-content: center; }
      .navbar-center { display: none; }
    }
    @media (max-width: 480px) {
      .navbar-brand h2 { display: none; }
    }
  `]
})
export class NavbarComponent {
  protected authStore = inject(AuthStore);
  protected ui = inject(UiStore);
  private authService = inject(AuthService);
  private router = inject(Router);

  menuOpen = false;

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  initials(): string {
    const name = this.authStore.user()?.fullName || '';
    return name.split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  goToProfile() {
    this.menuOpen = false;
    this.router.navigate(['/settings/profile']);
  }

  goToSettings() {
    this.menuOpen = false;
    this.router.navigate(['/settings']);
  }

  logout() {
    this.authService.logout();
    this.menuOpen = false;
    this.router.navigate(['/auth/login']);
  }
}
