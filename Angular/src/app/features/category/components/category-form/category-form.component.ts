import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { forkJoin, Observable } from 'rxjs';
import { selectSelectedCategory, selectCategoryLoading } from '../../../../store/category/category.selectors';
import * as CategoryActions from '../../../../store/category/category.actions';
import { AuthStore } from '../../../../core/stores/auth.store';
import { CategoryService } from '../../../../core/services/api/category.service';
import { CreateCategoryRequest, FieldType, CategoryTier } from '../../../../core/models';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="category-form-container">
      <h2>{{ isEditMode ? 'Sửa danh mục' : 'Danh mục mới' }}</h2>
      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Tên danh mục</label>
          <input formControlName="name" type="text" placeholder="Nhập tên danh mục" required>
          <p class="field-error" *ngIf="nameTaken">Tên này đã tồn tại trong phạm vi hiện tại.</p>
        </div>

        <div class="form-group">
          <label>Mô tả</label>
          <textarea formControlName="description" placeholder="Mô tả danh mục" rows="3"></textarea>
        </div>

        <div class="form-grid">
          <div class="form-group" *ngIf="!isEditMode">
            <label>Phạm vi</label>
            <select formControlName="tier">
              <option *ngIf="authStore.isManager()" value="SYSTEM">Hệ thống</option>
              <option *ngIf="authStore.isLead()" value="CUSTOM">Tuỳ chỉnh (nhóm của tôi)</option>
            </select>
          </div>
          <div class="form-group" *ngIf="isEditMode">
            <label>Phạm vi</label>
            <p class="readonly-value">{{ form.value.tier === 'SYSTEM' ? 'Hệ thống' : 'Tuỳ chỉnh' }}</p>
          </div>

          <div class="form-group">
            <label>Icon</label>
            <input formControlName="icon" type="text" placeholder="vd: calendar, document">
          </div>

          <div class="form-group">
            <label>Màu</label>
            <div class="color-picker">
              <input formControlName="color" type="color">
              <span class="color-preview" [style.background]="form.get('color')?.value"></span>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>Áp dụng cho loại công việc</label>
          <div class="checkbox-row">
            <label class="checkbox-label"><input type="checkbox" [checked]="hasTaskType('FAST')" (change)="toggleTaskType('FAST')"> Fast</label>
            <label class="checkbox-label"><input type="checkbox" [checked]="hasTaskType('OFTEN')" (change)="toggleTaskType('OFTEN')"> Often</label>
            <label class="checkbox-label"><input type="checkbox" [checked]="hasTaskType('MULTI_STEP')" (change)="toggleTaskType('MULTI_STEP')"> Multi-step</label>
          </div>
        </div>

        <div class="form-group">
          <label>Trường tuỳ chỉnh (tối đa 10)</label>
          <div class="extra-fields">
            <div *ngFor="let field of extraFields.controls; let i = index" [formGroup]="asFormGroup(field)" class="field-row">
              <input formControlName="label" placeholder="Tên trường">
              <select formControlName="fieldType" [disabled]="hasTaskData && field.get('id')?.value">
                <option *ngFor="let t of fieldTypes" [value]="t.value">{{ t.label }}</option>
              </select>
              <input *ngIf="isChoiceType(field.get('fieldType')?.value)" formControlName="optionsText" placeholder="Các lựa chọn, cách nhau bởi dấu phẩy">
              <label class="checkbox-label small">
                <input type="checkbox" formControlName="required" [attr.disabled]="hasTaskData ? true : null"> Bắt buộc
              </label>
              <button type="button" (click)="removeExtraField(i)" class="btn-remove">Xoá</button>
            </div>
            <button type="button" (click)="addExtraField()" class="btn-add-field" [disabled]="extraFields.length >= 10">+ Thêm trường</button>
            <p class="hint" *ngIf="hasTaskData">Danh mục đã có công việc sử dụng — không thể đổi kiểu hoặc đặt bắt buộc cho trường đã tồn tại.</p>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" (click)="cancel()">Huỷ</button>
          <button type="submit" [disabled]="!form.valid || saving" class="btn btn-primary">
            {{ isEditMode ? 'Cập nhật' : 'Tạo danh mục' }}
          </button>
        </div>
      </form>

      <div *ngIf="isLoading$ | async" class="loading">Đang xử lý...</div>
    </div>
  `,
  styles: [`
    .category-form-container { padding: 20px; max-width: 900px; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; font-weight: bold; margin-bottom: 5px; }
    .form-group input, .form-group select, .form-group textarea {
      width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 10px; box-sizing: border-box;
    }
    .readonly-value { margin: 0; padding: 8px 0; color: #555; }
    .field-error { color: #c62828; font-size: 12px; margin: 4px 0 0; }
    .form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; }
    .color-picker { display: flex; align-items: center; gap: 10px; }
    .color-picker input { width: 50px; height: 40px; }
    .color-preview { display: block; width: 40px; height: 40px; border-radius: 10px; border: 1px solid #ddd; }
    .checkbox-row { display: flex; gap: 16px; }
    .checkbox-label { display: flex; align-items: center; gap: 6px; font-weight: normal; }
    .checkbox-label.small { font-size: 13px; white-space: nowrap; }
    .extra-fields { background: #f9f9f9; padding: 12px; border-radius: 10px; }
    .btn-add-field { padding: 6px 12px; background: #e0e0e0; border: none; border-radius: 10px; cursor: pointer; }
    .btn-add-field:disabled { opacity: 0.5; cursor: not-allowed; }
    .field-row { display: grid; grid-template-columns: 1.3fr 1.2fr 1.3fr 100px 60px; gap: 10px; margin-bottom: 8px; align-items: center; }
    .field-row input, .field-row select { padding: 6px; border: 1px solid #ddd; border-radius: 10px; width: 100%; box-sizing: border-box; }
    .field-row .checkbox-label.small { font-size: 12px; white-space: normal; }
    .btn-remove { padding: 4px 8px; background: #ffcdd2; color: #d32f2f; border: none; border-radius: 10px; cursor: pointer; }
    .hint { font-size: 12px; color: #ef6c00; margin: 8px 0 0; }
    .form-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }    .loading { margin-top: 15px; color: #2563eb; text-align: center; }
  `]
})
export class CategoryFormComponent implements OnInit {
  private formBuilder = inject(FormBuilder);
  private store = inject(Store);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private categoryService = inject(CategoryService);
  authStore = inject(AuthStore);

  form: FormGroup;
  selectedCategory$ = this.store.select(selectSelectedCategory);
  isLoading$ = this.store.select(selectCategoryLoading);

  isEditMode = false;
  categoryId: number | null = null;
  saving = false;
  nameTaken = false;
  hasTaskData = false;
  private originalFieldIds = new Set<number>();

  readonly fieldTypes: { value: FieldType; label: string }[] = [
    { value: 'TEXT', label: 'Văn bản 1 dòng' },
    { value: 'TEXTAREA', label: 'Văn bản nhiều dòng' },
    { value: 'NUMBER', label: 'Số' },
    { value: 'DATE', label: 'Ngày' },
    { value: 'DATETIME', label: 'Ngày giờ' },
    { value: 'SELECT', label: 'Chọn 1 giá trị' },
    { value: 'MULTISELECT', label: 'Chọn nhiều giá trị' },
    { value: 'CHECKBOX', label: 'Hộp tích' }
  ];

  get extraFields() {
    return this.form.get('extraFields') as FormArray;
  }

  asFormGroup(control: any): FormGroup {
    return control as FormGroup;
  }

  isChoiceType(type: string): boolean {
    return type === 'SELECT' || type === 'MULTISELECT';
  }

  constructor() {
    this.form = this.formBuilder.group({
      name: ['', Validators.required],
      description: [''],
      icon: [''],
      color: ['#2563eb'],
      tier: ['CUSTOM' as CategoryTier],
      allowedTaskTypes: [[] as string[]],
      extraFields: this.formBuilder.array([])
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.categoryId = Number(idParam);
      this.store.dispatch(CategoryActions.loadCategory({ id: this.categoryId }));
    } else {
      // Default tier to whatever the current role is actually allowed to create
      this.form.patchValue({ tier: this.authStore.isManager() ? 'SYSTEM' : 'CUSTOM' });
    }

    this.selectedCategory$.subscribe(category => {
      if (category && this.isEditMode) {
        this.hasTaskData = category.taskCount > 0;
        this.form.patchValue({
          name: category.name,
          description: category.description,
          icon: category.icon,
          color: category.color,
          tier: category.tier,
          allowedTaskTypes: category.allowedTaskTypes || []
        });
        this.extraFields.clear();
        this.originalFieldIds.clear();
        for (const f of category.extraFields || []) {
          this.originalFieldIds.add(f.id);
          this.extraFields.push(this.buildFieldGroup(f));
        }
      }
    });

    this.form.get('name')?.valueChanges.subscribe(() => this.nameTaken = false);
  }

  private buildFieldGroup(f?: { id?: number; label?: string; fieldType?: FieldType; required?: boolean; options?: string[] }) {
    return this.formBuilder.group({
      id: [f?.id ?? null],
      label: [f?.label ?? '', Validators.required],
      fieldType: [f?.fieldType ?? 'TEXT', Validators.required],
      required: [f?.required ?? false],
      optionsText: [(f?.options ?? []).join(', ')]
    });
  }

  hasTaskType(type: string): boolean {
    return (this.form.value.allowedTaskTypes || []).includes(type);
  }

  toggleTaskType(type: string) {
    const current: string[] = this.form.value.allowedTaskTypes || [];
    const next = current.includes(type) ? current.filter(t => t !== type) : [...current, type];
    this.form.patchValue({ allowedTaskTypes: next });
  }

  addExtraField() {
    if (this.extraFields.length >= 10) return;
    this.extraFields.push(this.buildFieldGroup());
  }

  removeExtraField(index: number) {
    this.extraFields.removeAt(index);
  }

  onSubmit() {
    if (!this.form.valid || this.saving) return;

    this.saving = true;
    const v = this.form.value;
    const fieldPayloads = (v.extraFields || []).map((f: any) => ({
      id: f.id as number | null,
      label: f.label,
      fieldType: f.fieldType,
      required: f.required,
      options: this.isChoiceType(f.fieldType)
        ? String(f.optionsText || '').split(',').map((s: string) => s.trim()).filter((s: string) => s.length > 0)
        : []
    }));

    if (this.isEditMode && this.categoryId) {
      const id = this.categoryId;
      this.categoryService.updateCategory(id, {
        name: v.name, description: v.description, icon: v.icon, color: v.color, allowedTaskTypes: v.allowedTaskTypes
      }).subscribe({
        next: () => this.syncExtraFields(id, fieldPayloads),
        error: (err) => this.handleError(err)
      });
    } else {
      const request: CreateCategoryRequest = {
        name: v.name, description: v.description, icon: v.icon, color: v.color,
        tier: v.tier, allowedTaskTypes: v.allowedTaskTypes
      };
      this.categoryService.createCategory(request).subscribe({
        next: (res) => this.syncExtraFields(res.data.id, fieldPayloads),
        error: (err) => this.handleError(err)
      });
    }
  }

  /** Create/update fields still in the form, delete ones removed since load, then leave. */
  private syncExtraFields(categoryId: number, fields: Array<{ id: number | null; label: string; fieldType: FieldType; required: boolean; options: string[] }>) {
    const keptIds = new Set(fields.map(f => f.id).filter((id): id is number => id != null));
    const deletedIds = [...this.originalFieldIds].filter(id => !keptIds.has(id));

    const calls: Observable<unknown>[] = [
      ...fields.map(f => f.id
        ? this.categoryService.updateExtraField(categoryId, f.id, { label: f.label, fieldType: f.fieldType, required: f.required, options: f.options })
        : this.categoryService.addExtraField(categoryId, { label: f.label, fieldType: f.fieldType, required: f.required, options: f.options, sortOrder: 0 })
      ),
      ...deletedIds.map(id => this.categoryService.deleteExtraField(categoryId, id))
    ];

    const done = () => {
      this.saving = false;
      this.store.dispatch(CategoryActions.loadCategories({ page: 0, size: 50 }));
      this.router.navigate(['/category']);
    };

    if (calls.length === 0) {
      done();
      return;
    }
    forkJoin(calls).subscribe({
      next: done,
      error: (err: any) => this.handleError(err)
    });
  }

  private handleError(err: any) {
    this.saving = false;
    const message: string = err.error?.message || 'Không thể lưu danh mục';
    if (message.toLowerCase().includes('already exists')) {
      this.nameTaken = true;
    } else {
      alert(message);
    }
  }

  cancel() {
    this.router.navigate(['/category']);
  }
}
