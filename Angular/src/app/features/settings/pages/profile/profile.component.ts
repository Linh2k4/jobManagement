import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../../../core/stores/auth.store';
import { RoleLabelPipe } from '../../../../shared/pipes/role-label.pipe';

interface ProfileInfo {
  phone?: string;
  birthDate?: string;
  address?: string;
  bio?: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RoleLabelPipe],
  template: `
    <div class="profile-container">
      <div class="profile-header">
        <div class="avatar-section">
          <div class="avatar icon">person</div>
          <div class="avatar-info" *ngIf="authStore.user() as user">
            <h2>{{ user.fullName }}</h2>
            <p class="role">{{ user.role | roleLabel }}</p>
            <p class="email">{{ user.email }}</p>
          </div>
        </div>
      </div>

      <form [formGroup]="profileForm" (ngSubmit)="onUpdateProfile()">
        <div class="form-grid">
          <div class="form-group">
            <label>Họ tên</label>
            <input formControlName="name" type="text" placeholder="Nhập họ tên" required>
          </div>

          <div class="form-group">
            <label>Email</label>
            <input formControlName="email" type="email" placeholder="Nhập email" required readonly>
          </div>
        </div>

        <div class="form-grid">
          <div class="form-group">
            <label>Số điện thoại</label>
            <input formControlName="phone" type="tel" placeholder="Nhập số điện thoại">
          </div>

          <div class="form-group">
            <label>Ngày sinh (tùy chọn)</label>
            <input formControlName="birthDate" type="date">
          </div>
        </div>

        <div class="form-group">
          <label>Địa chỉ</label>
          <textarea formControlName="address" placeholder="Nhập địa chỉ" rows="3"></textarea>
        </div>

        <div class="form-group">
          <label>Tiểu sử / Giới thiệu</label>
          <textarea formControlName="bio" placeholder="Nhập thông tin giới thiệu" rows="4"></textarea>
        </div>

        <button type="submit" [disabled]="!profileForm.valid || saving" class="btn btn-primary">
          <ng-container *ngIf="saving; else profileIdle">Đang lưu...</ng-container>
          <ng-template #profileIdle><span class="icon">check</span> Cập nhật hồ sơ</ng-template>
        </button>
      </form>

      <div *ngIf="success" class="success-message">
        <span class="icon">check</span> Cập nhật thành công!
      </div>
      <div *ngIf="error" class="error-message">
        {{ error }}
      </div>
    </div>
  `,
  styles: [`
    .profile-container { padding: 20px; max-width: 800px; }
    .profile-header { background: white; border-radius: 16px; padding: 30px; margin-bottom: 20px; }
    .avatar-section { display: flex; align-items: center; gap: 20px; }
    .avatar { width: 100px; height: 100px; border-radius: 50%; background: #e0e0e0; display: flex; align-items: center; justify-content: center; font-size: 48px; }
    .avatar-info h2 { margin: 0 0 5px 0; }
    .avatar-info .role { color: var(--color-primary); font-weight: bold; margin: 5px 0; }
    .avatar-info .email { color: #999; margin: 5px 0; }
    form { background: white; border-radius: 16px; padding: 20px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; font-weight: bold; margin-bottom: 5px; }
    .form-group input, .form-group textarea {
      width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 10px;
    }
    .form-group input:readonly { background: #f5f5f5; }
    .success-message { background: #c8e6c9; color: #2e7d32; padding: 15px; border-radius: 16px; margin-top: 20px; }
    .error-message { background: #ffcdd2; color: #c62828; padding: 15px; border-radius: 16px; margin-top: 20px; }
  `]
})
export class ProfileComponent implements OnInit {
  protected authStore = inject(AuthStore);
  private http = inject(HttpClient);
  profileForm: FormGroup;
  success = false;
  error: string | null = null;
  saving = false;

  constructor(
    private fb: FormBuilder
  ) {
    this.profileForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      birthDate: [''],
      address: [''],
      bio: ['']
    });
  }

  ngOnInit() {
    const user = this.authStore.user();
    if (user) {
      this.profileForm.patchValue({
        name: user.fullName,
        email: user.email
      });
    }

    this.http.get<{ data: ProfileInfo }>('/api/v1/users/me').subscribe({
      next: res => this.profileForm.patchValue({
        phone: res.data.phone || '',
        birthDate: res.data.birthDate || '',
        address: res.data.address || '',
        bio: res.data.bio || ''
      }),
      error: () => { /* fall back to whatever authStore already had */ }
    });
  }

  onUpdateProfile() {
    if (!this.profileForm.valid) return;
    this.saving = true;
    this.error = null;
    this.success = false;

    const { phone, birthDate, address, bio } = this.profileForm.value;
    this.http.put<{ data: ProfileInfo }>('/api/v1/users/me', { phone, birthDate: birthDate || null, address, bio }).subscribe({
      next: () => {
        this.saving = false;
        this.success = true;
        setTimeout(() => this.success = false, 3000);
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Cập nhật hồ sơ thất bại.';
      }
    });
  }

}
