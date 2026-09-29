import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { AuthStore } from '../../../../core/stores/auth.store';

const LOCAL_SETTINGS_KEY = 'jobmanagement_local_settings';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="settings-container">
      <h2>Cài đặt Hệ thống</h2>

      <div class="settings-grid">
        <div class="settings-card" *ngIf="auth.isManager()">
          <h3><span class="icon">settings</span> Cấu hình KPI</h3>
          <p>Trọng số công thức tính KPI toàn hệ thống (Scope.md §7.6)</p>
          <a routerLink="/settings/kpi-config" class="btn btn-primary link-btn">Mở cấu hình</a>
        </div>

        <div class="settings-card">
          <h3><span class="icon">lock</span> Đổi Mật khẩu</h3>
          <form [formGroup]="passwordForm" (ngSubmit)="onChangePassword()">
            <div class="form-group">
              <label>Mật khẩu hiện tại</label>
              <input
                formControlName="currentPassword"
                type="password"
                placeholder="Nhập mật khẩu hiện tại"
                required
              >
            </div>

            <div class="form-group">
              <label>Mật khẩu mới</label>
              <input
                formControlName="newPassword"
                type="password"
                placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)"
                required
              >
            </div>

            <div class="form-group">
              <label>Xác nhận mật khẩu mới</label>
              <input
                formControlName="confirmPassword"
                type="password"
                placeholder="Nhập lại mật khẩu mới"
                required
              >
            </div>

            <button type="submit" [disabled]="!passwordForm.valid || passwordSaving" class="btn btn-primary">
              <ng-container *ngIf="passwordSaving; else pwdIdle">Đang đổi...</ng-container>
              <ng-template #pwdIdle><span class="icon">check</span> Đổi mật khẩu</ng-template>
            </button>
          </form>
          <div *ngIf="passwordSuccess" class="success-message">
            <span class="icon">check</span> Đổi mật khẩu thành công!
          </div>
          <div *ngIf="passwordError" class="error-message">
            {{ passwordError }}
          </div>
        </div>

        <div class="settings-card">
          <h3><span class="icon">notifications</span> Thông báo</h3>
          <div class="form-group checkbox">
            <input type="checkbox" id="email-notif" [(ngModel)]="settings.emailNotifications">
            <label for="email-notif">Nhận thông báo qua email</label>
          </div>
          <div class="form-group checkbox">
            <input type="checkbox" id="task-notif" [(ngModel)]="settings.taskNotifications">
            <label for="task-notif">Thông báo về công việc mới</label>
          </div>
          <div class="form-group checkbox">
            <input type="checkbox" id="deadline-notif" [(ngModel)]="settings.deadlineNotifications">
            <label for="deadline-notif">Thông báo gần deadline (30 phút trước)</label>
          </div>
          <button (click)="saveNotificationSettings()" class="btn btn-primary">
            <span class="icon">check</span> Lưu cài đặt
          </button>
          <span class="saved-hint" *ngIf="notifSaved">Đã lưu</span>
        </div>

        <div class="settings-card">
          <h3><span class="icon">palette</span> Giao diện</h3>
          <div class="form-group">
            <label>Chế độ hiển thị</label>
            <select [(ngModel)]="settings.theme">
              <option value="light">Sáng (Light)</option>
              <option value="dark">Tối (Dark)</option>
              <option value="auto">Tự động</option>
            </select>
          </div>
          <div class="form-group">
            <label>Ngôn ngữ</label>
            <select [(ngModel)]="settings.language">
              <option value="vi">🇻🇳 Tiếng Việt</option>
              <option value="en">🇬🇧 English</option>
            </select>
          </div>
          <button (click)="saveThemeSettings()" class="btn btn-primary">
            <span class="icon">check</span> Lưu cài đặt
          </button>
          <span class="saved-hint" *ngIf="themeSaved">Đã lưu</span>
        </div>

        <div class="settings-card danger">
          <h3><span class="icon">warning</span> Nguy hiểm</h3>
          <p>Các hành động này không thể hoàn tác</p>
          <button (click)="logout()" class="btn-danger">
            <span class="icon">logout</span> Đăng xuất
          </button>
          <button (click)="deleteAccount()" class="btn-delete">
            <span class="icon">cancel</span> Xóa tài khoản
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .settings-container { padding: 20px; max-width: 1000px; }
    .settings-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; }
    .settings-card { background: white; border: 1px solid #ddd; border-radius: 16px; padding: 20px; }
    .settings-card h3 { margin: 0 0 15px 0; }
    .settings-card.danger { border-color: #ffebee; background: #fff5f5; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; font-weight: bold; margin-bottom: 5px; }
    .form-group input[type="text"],
    .form-group input[type="password"],
    .form-group select {
      width: 100%;
      padding: 8px;
      border: 1px solid #ddd;
      border-radius: 10px;
    }
    .form-group.checkbox { display: flex; align-items: center; }
    .form-group.checkbox input { margin: 0; margin-right: 10px; }
    .form-group.checkbox label { margin: 0; }
    .link-btn { display: inline-block; text-decoration: none; text-align: center; }
    .btn-danger { padding: 8px 16px; background: var(--color-primary); color: white; border: none; border-radius: 10px; cursor: pointer; margin-bottom: 8px; width: 100%; }
    .btn-delete { padding: 8px 16px; background: #f44336; color: white; border: none; border-radius: 10px; cursor: pointer; width: 100%; }
    .success-message { background: #c8e6c9; color: #2e7d32; padding: 10px; border-radius: 10px; margin-top: 10px; }
    .saved-hint { margin-left: 10px; font-size: 13px; color: var(--color-success, #2e7d32); }
    .error-message { background: #ffcdd2; color: #c62828; padding: 10px; border-radius: 10px; margin-top: 10px; }
  `]
})
export class SettingsComponent {
  auth = inject(AuthStore);
  private http = inject(HttpClient);
  passwordForm: FormGroup;
  passwordSuccess = false;
  passwordError = '';
  passwordSaving = false;

  notifSaved = false;
  themeSaved = false;

  // Notification/theme prefs have no backend column to persist to yet —
  // stored in localStorage so they're at least real (survive reload) rather
  // than a console.log that pretended to save and forgot on refresh.
  settings = {
    emailNotifications: true,
    taskNotifications: true,
    deadlineNotifications: true,
    theme: 'light',
    language: 'vi'
  };

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });

    const stored = localStorage.getItem(LOCAL_SETTINGS_KEY);
    if (stored) {
      try { this.settings = { ...this.settings, ...JSON.parse(stored) }; } catch { /* ignore corrupt value */ }
    }
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: any } | null {
    const password = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { 'passwordMismatch': true };
  }

  onChangePassword() {
    if (!this.passwordForm.valid) return;
    if (this.passwordForm.hasError('passwordMismatch')) {
      this.passwordError = 'Mật khẩu xác nhận không khớp.';
      return;
    }

    this.passwordSaving = true;
    this.passwordError = '';
    this.passwordSuccess = false;

    const { currentPassword, newPassword } = this.passwordForm.value;
    this.http.put<{ success: boolean; message: string }>('/api/v1/users/me/password', { currentPassword, newPassword }).subscribe({
      next: () => {
        this.passwordSaving = false;
        this.passwordSuccess = true;
        setTimeout(() => this.passwordSuccess = false, 3000);
        this.passwordForm.reset();
      },
      error: (err) => {
        this.passwordSaving = false;
        this.passwordError = err?.error?.message || 'Đổi mật khẩu thất bại.';
      }
    });
  }

  saveNotificationSettings() {
    localStorage.setItem(LOCAL_SETTINGS_KEY, JSON.stringify(this.settings));
    this.notifSaved = true;
    setTimeout(() => this.notifSaved = false, 2000);
  }

  saveThemeSettings() {
    localStorage.setItem(LOCAL_SETTINGS_KEY, JSON.stringify(this.settings));
    this.themeSaved = true;
    setTimeout(() => this.themeSaved = false, 2000);
  }

  logout() {
    this.authService.logout();
  }

  deleteAccount() {
    // No account-deletion endpoint exists (only Manager-initiated
    // deactivate) — a real "delete my account" action needs that backend
    // capability designed first, so this stays a dead end rather than
    // faking a destructive action that doesn't actually happen.
    alert('Tính năng xóa tài khoản chưa khả dụng. Vui lòng liên hệ Quản lý để vô hiệu hoá tài khoản.');
  }
}
