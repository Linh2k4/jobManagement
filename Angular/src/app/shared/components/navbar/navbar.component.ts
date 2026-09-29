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
        <button (click)="ui.toggleSidebar()" class="btn-hamburger icon" aria-label="Menu">menu</button>

        <div class="navbar-brand">
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
      background: var(--color-surface);
      color: var(--color-text);
      border-bottom: 1px solid var(--color-border);
      box-shadow: var(--shadow-sm);
      position: sticky;
      top: 0;
      z-index: 200;
    }
    .navbar-content {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 0 20px;
      height: var(--navbar-height);
      max-width: 1400px;
      margin: 0 auto;
    }
    .btn-hamburger {
      display: none;
      background: none; border: none; color: var(--color-text);
      cursor: pointer; font-size: 20px; padding: 8px; border-radius: var(--radius-sm);
    }
    .btn-hamburger:hover { background: var(--color-surface-hover); }
    .navbar-brand { display: flex; align-items: center; gap: 8px; }
    .brand-logo { width: 28px; height: 28px; border-radius: 8px; }
    .menu-item { display: flex; align-items: center; gap: 8px; }
    .menu-item .icon { font-size: 18px; }
    .navbar-brand h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text); }
    .navbar-center { flex: 1; text-align: center; }
    .user-welcome { font-size: 14px; margin-right: 10px; color: var(--color-text); }
    .user-role { background: var(--color-primary-light); color: var(--color-primary); padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
    .navbar-end { position: relative; margin-left: auto; }
    .btn-avatar {
      width: 36px; height: 36px; border-radius: 50%; border: none;
      background: var(--color-primary); color: white; font-weight: 600; font-size: 13px;
      cursor: pointer; display: flex; align-items: center; justify-content: center;
    }
    .user-menu {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: var(--color-surface);
      color: var(--color-text);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-md);
      min-width: 180px;
      z-index: 1000;
      overflow: hidden;
    }
    .menu-item { width: 100%; padding: 10px 16px; border: none; background: none; text-align: left; cursor: pointer; font-size: 14px; color: var(--color-text); }
    .menu-item:hover { background: var(--color-surface-hover); }
    .menu-item.logout { color: var(--color-danger); }
    .menu-item.logout:hover { background: var(--color-danger-bg); }
    hr { margin: 4px 0; border: none; border-top: 1px solid var(--color-border); }

    @media (max-width: 900px) {
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
