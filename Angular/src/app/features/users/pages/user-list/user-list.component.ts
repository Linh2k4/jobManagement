import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService, UserInfo } from '../../../../core/services/api/user.service';
import { RoleLabelPipe } from '../../../../shared/pipes/role-label.pipe';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RoleLabelPipe],
  template: `
    <div class="user-page animate-fade-in">
      <!-- HEADER -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Quản lý Nhân sự</h1>
          <p class="page-subtitle">Danh sách tài khoản nhân viên, phân quyền vai trò và nhóm làm việc</p>
        </div>
        <button class="btn btn-primary" routerLink="/users/new">
          <span class="icon">person_add</span> Thêm người dùng
        </button>
      </div>

      <!-- FILTER BAR -->
      <div class="filter-card">
        <div class="search-box">
          <span class="icon search-icon">search</span>
          <input
            type="text"
            placeholder="Tìm kiếm theo tên hoặc email..."
            [(ngModel)]="searchTerm"
            (keyup)="applyFilters()"
          >
        </div>
        <div class="filter-controls">
          <select [(ngModel)]="selectedRole" (change)="applyFilters()">
            <option value="">Tất cả vai trò</option>
            <option value="MANAGER">Quản lý</option>
            <option value="LEAD">Trưởng nhóm</option>
            <option value="MEMBER">Nhân viên</option>
          </select>
          <select [(ngModel)]="selectedStatus" (change)="applyFilters()">
            <option value="">Tất cả trạng thái</option>
            <option value="active">Đang hoạt động</option>
            <option value="inactive">Đã khoá</option>
          </select>
        </div>
      </div>

      <!-- LOADING & ERROR -->
      <div *ngIf="loading" class="state-card">
        <div class="spinner"></div>
        <p>Đang tải danh sách nhân sự...</p>
      </div>

      <div *ngIf="error" class="state-card error">
        <div class="state-icon-box error"><span class="icon">error</span></div>
        <h3>Không thể tải danh sách</h3>
        <p class="text-muted">{{ error }}</p>
      </div>

      <!-- TABLE -->
      <div class="table-wrapper" *ngIf="!loading && !error && filteredUsers.length > 0">
        <table class="table-clean">
          <thead>
            <tr>
              <th>Họ và tên</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Nhóm trực thuộc</th>
              <th>Trạng thái</th>
              <th>Ngày tham gia</th>
              <th style="width: 130px; text-align: right;">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let user of filteredUsers">
              <td>
                <div class="user-cell">
                  <div class="avatar-pill">{{ initials(user.fullName) }}</div>
                  <strong class="user-name">{{ user.fullName }}</strong>
                </div>
              </td>
              <td class="text-muted">{{ user.email }}</td>
              <td>
                <span class="role-badge" [ngClass]="'role-' + user.role">
                  {{ user.role | roleLabel }}
                </span>
              </td>
              <td>
                <span class="group-tag" *ngIf="user.groupName">{{ user.groupName }}</span>
                <span class="text-muted" *ngIf="!user.groupName">—</span>
              </td>
              <td>
                <span class="status-pill" [ngClass]="user.isActive ? 'active' : 'inactive'">
                  <span class="dot"></span>
                  {{ user.isActive ? 'Hoạt động' : 'Đã khoá' }}
                </span>
              </td>
              <td class="text-muted">{{ user.createdAt | date: 'dd/MM/yyyy' }}</td>
              <td class="text-right">
                <div class="row-actions">
                  <button [routerLink]="['/users', user.id, 'edit']" class="btn-action edit" title="Sửa">
                    <span class="icon">edit</span>
                  </button>
                  <button
                    (click)="toggleStatus(user)"
                    class="btn-action toggle"
                    [class.lock]="user.isActive"
                    [class.unlock]="!user.isActive"
                    [disabled]="togglingId === user.id"
                    [title]="user.isActive ? 'Khoá tài khoản' : 'Kích hoạt tài khoản'"
                  >
                    <span class="icon">{{ user.isActive ? 'lock' : 'lock_open' }}</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- EMPTY -->
      <div *ngIf="!loading && !error && filteredUsers.length === 0" class="state-card">
        <div class="state-icon-box">
          <span class="icon">person_search</span>
        </div>
        <h3>Không tìm thấy người dùng</h3>
        <p class="text-muted">Không có người dùng nào khớp với tiêu chí tìm kiếm.</p>
      </div>
    </div>
  `,
  styles: [`
    .user-page {
      max-width: 1300px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      gap: 16px;
      flex-wrap: wrap;
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

    .filter-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 12px 16px;
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      box-shadow: var(--shadow-sm);
      flex-wrap: wrap;
    }
    .search-box {
      flex: 1;
      min-width: 250px;
      position: relative;
      display: flex;
      align-items: center;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      color: #94a3b8;
      font-size: 20px;
    }
    .search-box input {
      padding-left: 40px !important;
    }
    .filter-controls {
      display: flex;
      gap: 10px;
    }
    .filter-controls select {
      width: 180px;
    }

    .table-wrapper {
      border-radius: var(--radius-md);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
    }
    .user-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .avatar-pill {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: var(--gradient-primary);
      color: white;
      font-weight: 800;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .user-name {
      font-size: 14px;
      color: var(--color-text);
    }

    .role-badge {
      display: inline-flex;
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 11.5px;
      font-weight: 700;
    }
    .role-MANAGER { background: #e0f2fe; color: #0284c7; border: 1px solid #bae6fd; }
    .role-LEAD { background: #f3e8ff; color: #7e22ce; border: 1px solid #e9d5ff; }
    .role-MEMBER { background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; }

    .group-tag {
      background: var(--color-primary-subtle);
      color: var(--color-primary);
      border: 1px solid var(--color-primary-border);
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
    }

    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 11.5px;
      font-weight: 700;
    }
    .status-pill.active { background: #dcfce7; color: #15803d; border: 1px solid #86efac; }
    .status-pill.inactive { background: #f1f5f9; color: #64748b; border: 1px solid #cbd5e1; }
    .dot { width: 7px; height: 7px; border-radius: 50%; }
    .status-pill.active .dot { background: #16a34a; }
    .status-pill.inactive .dot { background: #94a3b8; }

    .row-actions {
      display: flex;
      justify-content: flex-end;
      gap: 6px;
    }
    .btn-action {
      background: #f8fafc;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-xs);
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-muted);
      transition: all 0.15s ease;
    }
    .btn-action.edit:hover {
      background: var(--color-primary-light);
      border-color: var(--color-primary-border);
      color: var(--color-primary);
    }
    .btn-action.toggle.lock:hover {
      background: #fff1f2;
      border-color: #fecdd3;
      color: #e11d48;
    }
    .btn-action.toggle.unlock:hover {
      background: #ecfdf5;
      border-color: #a7f3d0;
      color: #059669;
    }

    .text-muted { color: var(--color-text-muted); }
    .text-right { text-align: right; }

    /* STATES */
    .state-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 48px;
      text-align: center;
      box-shadow: var(--shadow-sm);
    }
    .state-icon-box {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: var(--color-primary-light);
      color: var(--color-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px auto;
    }
    .state-icon-box.error { background: #fff1f2; color: #e11d48; }
    .state-icon-box .icon { font-size: 28px; }
    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid #e0f2fe;
      border-top-color: var(--color-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 12px auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class UserListComponent implements OnInit {
  private userService = inject(UserService);

  users: UserInfo[] = [];
  filteredUsers: UserInfo[] = [];
  loading = false;
  error: string | null = null;
  togglingId: number | null = null;

  searchTerm = '';
  selectedRole = '';
  selectedStatus = '';

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = null;
    this.userService.listUsers().subscribe({
      next: res => {
        this.users = res.data;
        this.loading = false;
        this.applyFilters();
      },
      error: () => {
        this.loading = false;
        this.error = 'Không thể tải danh sách người dùng. Bạn có thể không đủ quyền (chỉ Manager mới xem được).';
      }
    });
  }

  initials(name: string): string {
    return (name || '').split(' ').map(p => p[0]).slice(-2).join('').toUpperCase();
  }

  applyFilters() {
    this.filteredUsers = this.users.filter(user => {
      const matchSearch = !this.searchTerm ||
        user.fullName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchRole = !this.selectedRole || user.role === this.selectedRole;
      const matchStatus = !this.selectedStatus ||
        (this.selectedStatus === 'active' ? user.isActive : !user.isActive);
      return matchSearch && matchRole && matchStatus;
    });
  }

  toggleStatus(user: UserInfo) {
    this.togglingId = user.id;
    const action$ = user.isActive
      ? this.userService.deactivateUser(user.id)
      : this.userService.activateUser(user.id);

    action$.subscribe({
      next: () => {
        user.isActive = !user.isActive;
        this.togglingId = null;
        this.applyFilters();
      },
      error: () => { this.togglingId = null; }
    });
  }
}
