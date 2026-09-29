import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="error-container">
      <div class="error-card">
        <div class="error-icon icon">block</div>
        <h1>403 - Truy cập bị từ chối</h1>
        <p>Bạn không có quyền truy cập trang này.</p>
        <p class="error-message">
          Nếu bạn cho rằng đây là một lỗi, vui lòng liên hệ với quản trị viên hệ thống.
        </p>
        <div class="actions">
          <a routerLink="/dashboard/member" class="btn-back"><span class="icon">arrow_back</span> Quay về Dashboard</a>
          <a routerLink="/" class="btn-home"><span class="icon">home</span> Trang chủ</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .error-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    .error-card {
      background: white;
      border-radius: 18px;
      padding: 40px;
      text-align: center;
      max-width: 500px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
    }
    .error-icon { font-size: 64px; margin-bottom: 20px; }
    h1 { margin: 0 0 10px 0; color: #d32f2f; }
    p { color: #666; margin: 10px 0; }
    .error-message { color: #999; font-size: 14px; }
    .actions { display: flex; gap: 10px; justify-content: center; margin-top: 30px; }
    a { padding: 10px 20px; border-radius: 10px; text-decoration: none; font-weight: bold; }
    .btn-back { background: #2563eb; color: white; }
    .btn-home { background: #f0f0f0; color: #333; }
    a:hover { opacity: 0.8; }
  `]
})
export class AccessDeniedComponent {}
