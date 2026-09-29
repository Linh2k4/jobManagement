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
    <div class="user-list-container">
      <div class="header">
        <h2>Quản lý Nhân sự</h2>
        <button class="btn btn-primary" routerLink="/users/new">+ Thêm người dùng</button>
      </div>

      <div class="filters">
        <input
          type="text"
          placeholder="Tìm kiếm theo tên hoặc email..."
          [(ngModel)]="searchTerm"
          (keyup)="applyFilters()"
        >
        <select [(ngModel)]="selectedRole" (change)="applyFilters()">
          <option value="">Tất cả vai trò</option>
          <option value="MANAGER">Quản lý</option>
          <option value="LEAD">Trưởng nhóm</option>
          <option value="MEMBER">Nhân viên</option>
        </select>
        <select [(ngModel)]="selectedStatus" (change)="applyFilters()">
          <option value="">Tất cả trạng thái</option>
          <option value="active">Đang hoạt động</option>
          <option value="inactive">Không hoạt động</option>
        </select>
      </div>

      <div *ngIf="loading" class="loading">Đang tải...</div>
      <div *ngIf="error" class="error">{{ error }}</div>

      <div class="table-container" *ngIf="!loading && !error">
        <table class="table-clean">
          <thead>
            <tr>
              <th>Tên</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Nhóm</th>
              <th>Trạng thái</th>
              <th>Ngày tham gia</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let user of filteredUsers">
              <td><strong>{{ user.fullName }}</strong></td>
              <td>{{ user.email }}</td>
              <td>
                <span class="role-badge" [ngClass]="'role-' + user.role">
                  {{ user.role | roleLabel }}
                </span>
              </td>
              <td>{{ user.groupName || '-' }}</td>
              <td>
                <span class="status-badge" [ngClass]="user.isActive ? 'status-active' : 'status-inactive'">
                  <span class="dot" [ngClass]="user.isActive ? 'dot-green' : 'dot-gray'"></span>
                  {{ user.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                </span>
              </td>
              <td>{{ user.createdAt | date: 'dd/MM/yyyy' }}</td>
              <td>
                <button [routerLink]="['/users', user.id, 'edit']" class="btn-edit">Sửa</button>
                <button (click)="toggleStatus(user)" class="btn-toggle" [disabled]="togglingId === user.id">
                  {{ user.isActive ? 'Khóa' : 'Mở' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="!loading && !error && filteredUsers.length === 0" class="no-data">
        Không tìm thấy người dùng phù hợp
      </div>
    </div>
  `,
  styles: [`
    .user-list-container { padding: 20px; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .filters { display: flex; gap: 10px; margin-bottom: 20px; }
    .filters input, .filters select { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .filters input { flex: 1; }
    .table-container { background: white; border: 1px solid #ddd; border-radius: 16px; overflow: auto; }
    .role-badge { padding: 4px 8px; border-radius: 10px; font-size: 12px; }
    .role-MANAGER { background: #e3f2fd; color: #2563eb; }
    .role-LEAD { background: #f3e5f5; color: #7b1fa2; }
    .role-MEMBER { background: #e8f5e9; color: #388e3c; }
    .status-badge { padding: 4px 8px; border-radius: 10px; font-size: 12px; display: inline-flex; align-items: center; gap: 6px; }
    .status-active { background: #c8e6c9; }
    .status-inactive { background: #f5f5f5; }
    .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    .dot-green { background: #43a047; }
    .dot-gray { background: #bdbdbd; }
    td button { margin-right: 8px; padding: 4px 8px; border: 1px solid #ddd; background: white; border-radius: 10px; cursor: pointer; }
    td button:hover { background: #f0f0f0; }
    td button:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-toggle { background: #fff3e0; }
    .no-data, .loading { text-align: center; color: #999; padding: 40px; }
    .error { text-align: center; color: #c62828; padding: 40px; }
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
