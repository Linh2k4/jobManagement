import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import * as TaskActions from '../../../../store/task/task.actions';
import { CreateTaskRequest, TaskTypeSummary } from '../../../../core/models';
import { TaskTypeService } from '../../../../core/services/api/task-type.service';

@Component({
  selector: 'app-task-create-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="close()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Tạo công việc mới</h2>
          <button class="close-btn" (click)="close()"><span class="icon">close</span></button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label>Loại công việc</label>
            <select formControlName="taskTypeId">
              <option [ngValue]="null" disabled>Chọn loại công việc</option>
              <option *ngFor="let t of taskTypes" [ngValue]="t.id">{{ t.name }} ({{ t.timeCategory }})</option>
            </select>
          </div>

          <div class="form-group">
            <label>Tiêu đề</label>
            <input formControlName="title" type="text" placeholder="Nhập tiêu đề công việc">
          </div>

          <div class="form-group">
            <label>Mô tả</label>
            <textarea formControlName="description" placeholder="Mô tả công việc"></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Ước lượng (phút)</label>
              <input formControlName="estimateMinutes" type="number" min="1">
            </div>

            <div class="form-group">
              <label>Hạn chót</label>
              <input formControlName="dueDate" type="date">
            </div>
          </div>

          <div class="form-group" *ngIf="selectedIsOften">
            <label>Bộ phận (dành cho việc lặp lại)</label>
            <input formControlName="section" type="text" placeholder="VD: Kế toán">
          </div>

          <p class="error" *ngIf="errorMessage">{{ errorMessage }}</p>

          <div class="form-actions">
            <button type="button" (click)="close()" class="btn btn-secondary">Huỷ</button>
            <button type="submit" [disabled]="!form.valid || submitting" class="btn btn-primary">
              {{ submitting ? 'Đang tạo...' : 'Tạo công việc' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center;
      z-index: 1000;
    }
    .modal-content {
      background: white; border-radius: 16px; padding: 30px; width: 500px; max-width: 90vw;
      max-height: 90vh; overflow-y: auto;
      box-shadow: 0 4px 16px rgba(0,0,0,0.2);
    }
    .modal-header { display: flex; justify-content: space-between; margin-bottom: 20px; }
    .close-btn { background: none; border: none; font-size: 24px; cursor: pointer; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; margin-bottom: 5px; font-weight: bold; }
    .form-group input, .form-group select, .form-group textarea {
      width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 10px; box-sizing: border-box;
    }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
    .error { color: #c62828; margin: 0 0 15px; }
    .form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
  `]
})
export class TaskCreateModalComponent implements OnInit {
  @Output() created = new EventEmitter<void>();

  private taskTypeService = inject(TaskTypeService);

  isOpen = false;
  submitting = false;
  errorMessage: string | null = null;
  taskTypes: TaskTypeSummary[] = [];
  form: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private store: Store
  ) {
    this.form = this.formBuilder.group({
      taskTypeId: [null, Validators.required],
      title: ['', Validators.required],
      description: [''],
      estimateMinutes: [null],
      dueDate: [''],
      section: ['']
    });
  }

  ngOnInit() {
    this.taskTypeService.getTaskTypes().subscribe(types => this.taskTypes = types);
  }

  get selectedIsOften(): boolean {
    const type = this.taskTypes.find(t => t.id === this.form.value.taskTypeId);
    return type?.timeCategory === 'OFTEN';
  }

  open() {
    this.isOpen = true;
    this.errorMessage = null;
  }

  close() {
    this.isOpen = false;
    this.form.reset();
  }

  onSubmit() {
    if (!this.form.valid) return;

    const request: CreateTaskRequest = {
      taskTypeId: this.form.value.taskTypeId,
      title: this.form.value.title,
      description: this.form.value.description || undefined,
      estimateMinutes: this.form.value.estimateMinutes || undefined,
      dueDate: this.form.value.dueDate || undefined,
      section: this.form.value.section || undefined
    };

    this.submitting = true;
    this.errorMessage = null;
    this.store.dispatch(TaskActions.createTask({ request }));
    // Optimistic close — the list refreshes via createTaskSuccess in the
    // reducer; a create failure surfaces through the store's error state,
    // not here, since createTask's effect doesn't return a promise.
    this.submitting = false;
    this.created.emit();
    this.close();
  }
}
