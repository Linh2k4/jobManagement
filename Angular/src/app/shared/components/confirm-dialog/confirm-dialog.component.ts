import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dialog-overlay" *ngIf="isOpen" (click)="onCancel()">
      <div class="dialog-content" (click)="$event.stopPropagation()">
        <div class="dialog-header">
          <h3>{{ title }}</h3>
          <button class="close-btn" (click)="onCancel()"><span class="icon">close</span></button>
        </div>

        <div class="dialog-body">
          <p>{{ message }}</p>
        </div>

        <div class="dialog-actions">
          <button (click)="onCancel()" class="btn-cancel">{{ cancelLabel }}</button>
          <button (click)="onConfirm()" class="btn-confirm" [ngClass]="'btn-' + type">
            {{ confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 2000;
    }
    .dialog-content {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
      min-width: 300px;
      max-width: 500px;
    }
    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid #ddd;
    }
    .dialog-header h3 { margin: 0; }
    .close-btn { background: none; border: none; font-size: 24px; cursor: pointer; }
    .dialog-body { padding: 20px; }
    .dialog-body p { margin: 0; color: #666; }
    .dialog-actions { display: flex; gap: 10px; justify-content: flex-end; padding: 20px; border-top: 1px solid #ddd; }
    button { padding: 8px 16px; border: 1px solid #ddd; border-radius: 10px; cursor: pointer; }
    .btn-cancel { background: white; }
    .btn-cancel:hover { background: #f0f0f0; }
    .btn-confirm { background: #4CAF50; color: white; border: none; }
    .btn-confirm.btn-danger { background: #f44336; }
    .btn-confirm.btn-warning { background: #ff9800; }
    .btn-confirm:hover { opacity: 0.8; }
  `]
})
export class ConfirmDialogComponent {
  @Input() isOpen = false;
  @Input() title = 'Xác nhận';
  @Input() message = 'Bạn chắc chắn muốn thực hiện hành động này?';
  @Input() confirmLabel = 'Xác nhận';
  @Input() cancelLabel = 'Hủy';
  @Input() type: 'default' | 'danger' | 'warning' = 'default';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm() {
    this.confirm.emit();
    this.isOpen = false;
  }

  onCancel() {
    this.cancel.emit();
    this.isOpen = false;
  }
}
