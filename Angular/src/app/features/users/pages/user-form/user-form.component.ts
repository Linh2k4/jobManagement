import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../../core/services/api/user.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="user-form-container">
      <h2>{{ isEdit ? 'Chỉnh sửa Người dùng' : 'Thêm Người dùng Mới' }}</h2>

      <div *ngIf="loading" class="loading">Đang tải...</div>

      <form *ngIf="!loading" [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="form-grid">
          <div class="form-group">
            <label>Họ tên</label>
            <input formControlName="fullName" type="text" placeholder="Nhập họ tên" required>
          </div>

          <div class="form-group">
            <label>Email</label>
            <input formControlName="email" type="email" placeholder="Nhập email" required [readonly]="isEdit">
          </div>
        </div>

        <div class="form-group">
          <label>Vai trò</label>
          <select formControlName="role" required>
            <option value="MEMBER">Nhân viên</option>
            <option value="LEAD">Trưởng nhóm</option>
            <option value="MANAGER">Quản lý</option>
          </select>
        </div>

        <div class="form-group" *ngIf="!isEdit">
          <label>Mật khẩu tạm thời (tối thiểu 8 ký tự)</label>
          <input formControlName="password" type="password" placeholder="Nhập mật khẩu tạm" required>
        </div>

        <p class="hint" *ngIf="isEdit">Trạng thái hoạt động và mật khẩu được quản lý riêng ở trang Nhân sự / Cài đặt.</p>

        <div class="error" *ngIf="error">{{ error }}</div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" (click)="cancel()">Hủy</button>
          <button type="submit" [disabled]="!form.valid || saving" class="btn btn-primary">
            {{ saving ? 'Đang lưu...' : (isEdit ? 'Cập nhật' : 'Tạo') }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .user-form-container { padding: 20px; max-width: 600px; background: white; border-radius: 16px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; font-weight: bold; margin-bottom: 5px; }
    .form-group input, .form-group select {
      width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 10px;
    }
    .form-group input[readonly] { background: #f5f5f5; }
    .hint { font-size: 13px; color: #777; }
    .error { background: #ffcdd2; color: #c62828; padding: 10px; border-radius: 10px; margin-bottom: 12px; }
    .loading { color: #999; padding: 20px 0; }
    .form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }  `]
})
export class UserFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);

  form: FormGroup;
  isEdit = false;
  userId: number | null = null;
  loading = false;
  saving = false;
  error: string | null = null;

  constructor() {
    this.form = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      role: ['MEMBER', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.params['id'];
    if (idParam) {
      this.isEdit = true;
      this.userId = +idParam;
      this.form.removeControl('password');
      this.loadUser(this.userId);
    }
  }

  private loadUser(id: number) {
    this.loading = true;
    this.userService.getUser(id).subscribe({
      next: res => {
        this.form.patchValue({ fullName: res.data.fullName, email: res.data.email, role: res.data.role });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Không thể tải thông tin người dùng.';
      }
    });
  }

  onSubmit() {
    if (!this.form.valid) return;
    this.saving = true;
    this.error = null;

    if (this.isEdit && this.userId != null) {
      this.userService.updateUser(this.userId, { fullName: this.form.value.fullName, role: this.form.value.role }).subscribe({
        next: () => { this.saving = false; this.router.navigate(['/users']); },
        error: (err) => { this.saving = false; this.error = err?.error?.message || 'Cập nhật thất bại.'; }
      });
    } else {
      this.userService.createUser(this.form.value).subscribe({
        next: () => { this.saving = false; this.router.navigate(['/users']); },
        error: (err) => { this.saving = false; this.error = err?.error?.message || 'Tạo người dùng thất bại.'; }
      });
    }
  }

  cancel() {
    this.router.navigate(['/users']);
  }
}
