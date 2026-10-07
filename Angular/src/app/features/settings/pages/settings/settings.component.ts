import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { AuthStore } from '../../../../core/stores/auth.store';
import { UiStore, AppTheme, AppLanguage } from '../../../../core/stores/ui.store';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="settings-page animate-fade-in">
      <!-- PAGE HEADER -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Cài đặt Hệ thống</h1>
          <p class="page-subtitle">Tuỳ chỉnh giao diện, thông báo, bảo mật và cấu hình hệ thống</p>
        </div>
      </div>

      <div class="settings-grid">
        <!-- THEME & LANGUAGE -->
        <div class="card settings-card">
          <div class="card-header">
            <div class="header-icon-box sky"><span class="icon">palette</span></div>
            <div>
              <h3>Giao diện & Ngôn ngữ</h3>
              <p class="card-subtitle">Tuỳ chỉnh chế độ màu sắc và ngôn ngữ hiển thị</p>
            </div>
          </div>

          <div class="card-body">
            <div class="form-group">
              <label>Chế độ hiển thị (Theme)</label>
              <select [(ngModel)]="settings.theme" (change)="onThemeChange()" class="form-select">
                <option value="light">☀️ Giao diện Sáng (Light Sky)</option>
                <option value="dark">🌙 Giao diện Tối (Dark Navy)</option>
                <option value="auto">🖥️ Theo hệ thống (Auto)</option>
              </select>
            </div>

            <div class="form-group">
              <label>Ngôn ngữ giao diện</label>
              <select [(ngModel)]="settings.language" (change)="onLanguageChange()" class="form-select">
                <option value="vi">🇻🇳 Tiếng Việt (Mặc định)</option>
                <option value="en">🇬🇧 English</option>
              </select>
            </div>
          </div>

          <div class="card-footer">
            <button (click)="saveThemeSettings()" class="btn btn-primary btn-sm">
              <span class="icon">check</span> Lưu giao diện
            </button>
            <span class="saved-badge" *ngIf="themeSaved">
              <span class="icon">check_circle</span> Đã áp dụng thành công!
            </span>
          </div>
        </div>

        <!-- KPI CONFIG (MANAGER ONLY) -->
        <div class="card settings-card" *ngIf="auth.isManager()">
          <div class="card-header">
            <div class="header-icon-box blue"><span class="icon">tune</span></div>
            <div>
              <h3>Cấu hình KPI Hệ thống</h3>
              <p class="card-subtitle">Quản trị viên cấu hình trọng số công thức tính điểm</p>
            </div>
          </div>

          <div class="card-body">
            <p class="desc-text">
              Thiết lập trọng số tự động (40%), Lead đánh giá (35%), và Manager chốt điểm (25%) theo quy chuẩn doanh nghiệp.
            </p>
          </div>

          <div class="card-footer">
            <a routerLink="/settings/kpi-config" class="btn btn-secondary btn-sm">
              <span class="icon">settings</span> Mở bảng cấu hình
            </a>
          </div>
        </div>

        <!-- NOTIFICATIONS -->
        <div class="card settings-card">
          <div class="card-header">
            <div class="header-icon-box amber"><span class="icon">notifications</span></div>
            <div>
              <h3>Tuỳ chọn Thông báo</h3>
              <p class="card-subtitle">Quản lý nhận email và thông báo đẩy khi có việc mới</p>
            </div>
          </div>

          <div class="card-body">
            <label class="custom-checkbox">
              <input type="checkbox" [(ngModel)]="settings.emailNotifications">
              <span class="checkmark"></span>
              <div class="checkbox-label">
                <strong>Thông báo qua email</strong>
                <span class="checkbox-sub">Nhận email khi được giao task hoặc có thay đổi deadline</span>
              </div>
            </label>

            <label class="custom-checkbox">
              <input type="checkbox" [(ngModel)]="settings.taskNotifications">
              <span class="checkmark"></span>
              <div class="checkbox-label">
                <strong>Công việc mới</strong>
                <span class="checkbox-sub">Thông báo trên thanh menu khi có phân công mới</span>
              </div>
            </label>

            <label class="custom-checkbox">
              <input type="checkbox" [(ngModel)]="settings.deadlineNotifications">
              <span class="checkmark"></span>
              <div class="checkbox-label">
                <strong>Cảnh báo sắp đến hạn (30 phút)</strong>
                <span class="checkbox-sub">Gửi cảnh báo khẩn cấp trước khi hết hạn nhiệm vụ</span>
              </div>
            </label>
          </div>

          <div class="card-footer">
            <button (click)="saveNotificationSettings()" class="btn btn-primary btn-sm">
              <span class="icon">check</span> Lưu thông báo
            </button>
            <span class="saved-badge" *ngIf="notifSaved">
              <span class="icon">check_circle</span> Đã lưu cài đặt!
            </span>
          </div>
        </div>

        <!-- CHANGE PASSWORD -->
        <div class="card settings-card">
          <div class="card-header">
            <div class="header-icon-box emerald"><span class="icon">lock</span></div>
            <div>
              <h3>Đổi Mật khẩu</h3>
              <p class="card-subtitle">Cập nhật mật khẩu bảo mật cho tài khoản của bạn</p>
            </div>
          </div>

          <div class="card-body">
            <form [formGroup]="passwordForm" (ngSubmit)="onChangePassword()">
              <div class="form-group">
                <label>Mật khẩu hiện tại</label>
                <input
                  formControlName="currentPassword"
                  type="password"
                  placeholder="Nhập mật khẩu hiện tại"
                  class="form-input"
                >
              </div>

              <div class="form-group">
                <label>Mật khẩu mới</label>
                <input
                  formControlName="newPassword"
                  type="password"
                  placeholder="Tối thiểu 6 ký tự"
                  class="form-input"
                >
              </div>

              <div class="form-group">
                <label>Xác nhận mật khẩu mới</label>
                <input
                  formControlName="confirmPassword"
                  type="password"
                  placeholder="Nhập lại mật khẩu mới"
                  class="form-input"
                >
              </div>

              <button type="submit" [disabled]="!passwordForm.valid || passwordSaving" class="btn btn-primary btn-sm">
                <ng-container *ngIf="passwordSaving; else pwdIdle">
                  <span class="icon spinner">sync</span> Đang đổi...
                </ng-container>
                <ng-template #pwdIdle>
                  <span class="icon">lock_reset</span> Đổi mật khẩu
                </ng-template>
              </button>
            </form>

            <div *ngIf="passwordSuccess" class="alert-box success">
              <span class="icon">check_circle</span> Đổi mật khẩu thành công!
            </div>
            <div *ngIf="passwordError" class="alert-box error">
              <span class="icon">error</span> {{ passwordError }}
            </div>
          </div>
        </div>

        <!-- ACCOUNT ACTIONS -->
        <div class="card settings-card danger-card">
          <div class="card-header">
            <div class="header-icon-box rose"><span class="icon">security</span></div>
            <div>
              <h3>Tài khoản & Phiên làm việc</h3>
              <p class="card-subtitle">Đăng xuất hoặc quản lý quyền truy cập</p>
            </div>
          </div>

          <div class="card-body">
            <p class="desc-text">
              Bạn đang đăng nhập với tài khoản <strong>{{ auth.user()?.email }}</strong> ({{ auth.user()?.role }}).
            </p>
          </div>

          <div class="card-footer">
            <button (click)="logout()" class="btn btn-danger btn-sm">
              <span class="icon">logout</span> Đăng xuất phiên hiện tại
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .settings-page {
      max-width: 1200px;
      margin: 0 auto;
    }

    .page-header {
      margin-bottom: 24px;
    }
    .page-title {
      font-size: 26px;
      font-weight: 800;
      color: var(--color-text);
      margin: 0 0 4px 0;
      letter-spacing: -0.02em;
    }
    .page-subtitle {
      color: var(--color-text-muted);
      font-size: 14px;
      margin: 0;
    }

    .settings-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
      gap: 20px;
    }

    .settings-card {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      height: 100%;
    }
    .danger-card {
      border-color: var(--color-danger-border);
      background: var(--color-danger-bg);
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: 14px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--color-border);
      margin-bottom: 16px;
    }
    .header-icon-box {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .header-icon-box.sky { background: var(--color-primary-light); color: var(--color-primary); }
    .header-icon-box.blue { background: #dbeafe; color: #2563eb; }
    .header-icon-box.amber { background: #fef3c7; color: #d97706; }
    .header-icon-box.emerald { background: #dcfce7; color: #059669; }
    .header-icon-box.rose { background: #ffe4e6; color: #e11d48; }
    .header-icon-box .icon { font-size: 24px; }

    .card-header h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 800;
      color: var(--color-text);
    }
    .card-subtitle {
      margin: 2px 0 0 0;
      font-size: 12px;
      color: var(--color-text-muted);
    }

    .card-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .desc-text {
      font-size: 13.5px;
      color: var(--color-text-muted);
      line-height: 1.5;
      margin: 0;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .form-group label {
      font-size: 12.5px;
      font-weight: 700;
      color: var(--color-text);
    }

    /* CUSTOM CHECKBOX */
    .custom-checkbox {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      cursor: pointer;
      user-select: none;
      padding: 8px 10px;
      border-radius: var(--radius-xs);
      transition: background 0.15s ease;
    }
    .custom-checkbox:hover {
      background: var(--color-surface-hover);
    }
    .custom-checkbox input {
      width: 18px;
      height: 18px;
      margin-top: 2px;
      cursor: pointer;
    }
    .checkbox-label {
      display: flex;
      flex-direction: column;
    }
    .checkbox-label strong {
      font-size: 13.5px;
      color: var(--color-text);
    }
    .checkbox-sub {
      font-size: 11.5px;
      color: var(--color-text-muted);
    }

    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 16px;
      border-top: 1px solid var(--color-border);
      margin-top: 16px;
      flex-wrap: wrap;
      gap: 10px;
    }

    .saved-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      font-weight: 700;
      color: #059669;
      background: #dcfce7;
      padding: 4px 10px;
      border-radius: 999px;
      animation: fadeIn 0.2s ease;
    }
    .saved-badge .icon { font-size: 15px; }

    .alert-box {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border-radius: var(--radius-xs);
      font-size: 12.5px;
      font-weight: 600;
      margin-top: 10px;
    }
    .alert-box.success { background: #dcfce7; color: #15803d; border: 1px solid #86efac; }
    .alert-box.error { background: #ffe4e6; color: #be123c; border: 1px solid #fecdd3; }
  `]
})
export class SettingsComponent implements OnInit {
  auth = inject(AuthStore);
  protected ui = inject(UiStore);
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  passwordForm: FormGroup;
  passwordSuccess = false;
  passwordError = '';
  passwordSaving = false;

  notifSaved = false;
  themeSaved = false;

  settings = {
    emailNotifications: true,
    taskNotifications: true,
    deadlineNotifications: true,
    theme: 'light',
    language: 'vi'
  };

  constructor() {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit() {
    this.settings.theme = this.ui.theme();
    this.settings.language = this.ui.language();
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: any } | null {
    const password = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { 'passwordMismatch': true };
  }

  onThemeChange() {
    this.ui.setTheme(this.settings.theme as AppTheme);
  }

  onLanguageChange() {
    this.ui.setLanguage(this.settings.language as AppLanguage);
  }

  saveThemeSettings() {
    this.ui.setTheme(this.settings.theme as AppTheme);
    this.ui.setLanguage(this.settings.language as AppLanguage);
    this.themeSaved = true;
    setTimeout(() => this.themeSaved = false, 3000);
  }

  saveNotificationSettings() {
    this.notifSaved = true;
    setTimeout(() => this.notifSaved = false, 3000);
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

  logout() {
    this.authService.logout();
  }
}
