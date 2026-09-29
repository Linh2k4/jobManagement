import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { map } from 'rxjs';
import {
  selectAllCategories,
  selectCategoryLoading,
  selectSystemCategories,
  selectCustomCategories
} from '../../../../store/category/category.selectors';
import * as CategoryActions from '../../../../store/category/category.actions';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="category-page animate-fade-in">
      <!-- HEADER -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Loại công việc</h1>
          <p class="page-subtitle">Quản lý các danh mục phân loại công việc và định mức thời gian</p>
        </div>
        <button class="btn btn-primary" (click)="newCategory()">
          <span class="icon">add</span> Danh mục mới
        </button>
      </div>

      <!-- TABS & FILTERS -->
      <div class="control-panel">
        <div class="tab-group">
          <button [class.active]="activeTab === 'all'" (click)="setTab('all')">Tất cả</button>
          <button [class.active]="activeTab === 'system'" (click)="setTab('system')">Hệ thống</button>
          <button [class.active]="activeTab === 'custom'" (click)="setTab('custom')">Tuỳ chỉnh</button>
        </div>

        <div class="filters-row">
          <div class="search-box">
            <span class="icon search-icon">search</span>
            <input
              type="text"
              placeholder="Tìm theo tên danh mục..."
              [(ngModel)]="searchTerm"
              (keyup)="onSearch()"
            >
          </div>
          <div class="select-box">
            <select [(ngModel)]="selectedTier" (change)="onFilterChange()">
              <option value="">Tất cả phạm vi</option>
              <option value="SYSTEM">Hệ thống</option>
              <option value="CUSTOM">Tuỳ chỉnh</option>
            </select>
          </div>
        </div>
      </div>

      <!-- LOADING -->
      <div *ngIf="isLoading$ | async" class="state-card">
        <div class="spinner"></div>
        <p>Đang tải danh mục...</p>
      </div>

      <!-- CATEGORIES GRID -->
      <div class="categories-grid" *ngIf="!(isLoading$ | async)">
        <div *ngFor="let category of filteredCategories$ | async" class="category-card" [style.border-top-color]="category.color || '#0284c7'">
          <div class="card-head">
            <div class="title-wrap">
              <span class="color-dot" [style.background]="category.color || '#0284c7'"></span>
              <h3>{{ category.name }}</h3>
            </div>
            <span class="tier-pill" [class.system]="category.tier === 'SYSTEM'">
              {{ category.tier === 'SYSTEM' ? 'Hệ thống' : 'Tuỳ chỉnh' }}
            </span>
          </div>

          <p class="desc">{{ category.description || 'Không có mô tả cho danh mục này.' }}</p>

          <div class="card-foot">
            <div class="task-count">
              <span class="icon">checklist</span>
              <span><strong>{{ category.taskCount || 0 }}</strong> công việc</span>
            </div>
            <div class="action-buttons">
              <button (click)="editCategory(category.id)" class="btn-card-action edit" title="Chỉnh sửa">
                <span class="icon">edit</span>
              </button>
              <button (click)="deleteCategory(category.id)" class="btn-card-action delete" *ngIf="category.tier !== 'SYSTEM'" title="Xoá">
                <span class="icon">delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- EMPTY STATE -->
      <div *ngIf="!(filteredCategories$ | async)?.length && !(isLoading$ | async)" class="state-card">
        <div class="state-icon-box">
          <span class="icon">category</span>
        </div>
        <h3>Không tìm thấy danh mục</h3>
        <p class="text-muted">Không có danh mục nào phù hợp với điều kiện tìm kiếm.</p>
      </div>
    </div>
  `,
  styles: [`
    .category-page {
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

    .control-panel {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 14px 18px;
      margin-bottom: 24px;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .tab-group {
      display: flex;
      gap: 6px;
      border-bottom: 1px solid var(--color-border);
      padding-bottom: 12px;
    }
    .tab-group button {
      padding: 6px 14px;
      border: 1px solid transparent;
      border-radius: var(--radius-xs);
      background: none;
      font-size: 13px;
      font-weight: 700;
      color: var(--color-text-muted);
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .tab-group button:hover {
      background: var(--color-primary-light);
      color: var(--color-primary);
    }
    .tab-group button.active {
      background: var(--color-primary-subtle);
      color: var(--color-primary);
      border-color: var(--color-primary-border);
    }

    .filters-row {
      display: flex;
      gap: 12px;
    }
    .search-box {
      flex: 1;
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
    .select-box {
      width: 200px;
    }

    /* CARDS */
    .categories-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 18px;
    }
    .category-card {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-top-width: 4px;
      border-radius: var(--radius-md);
      padding: 20px;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .category-card:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow-md);
    }
    .card-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 10px;
      margin-bottom: 10px;
    }
    .title-wrap {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .color-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      flex-shrink: 0;
      box-shadow: 0 0 6px rgba(0, 0, 0, 0.15);
    }
    .card-head h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 800;
      color: var(--color-text);
    }
    .tier-pill {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 999px;
      background: var(--color-primary-subtle);
      color: var(--color-primary);
      border: 1px solid var(--color-primary-border);
    }
    .tier-pill.system {
      background: #ecfdf5;
      color: #059669;
      border-color: #a7f3d0;
    }

    .desc {
      color: var(--color-text-muted);
      font-size: 13px;
      line-height: 1.5;
      margin: 0 0 16px 0;
      flex: 1;
    }

    .card-foot {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 14px;
      border-top: 1px solid var(--color-border);
    }
    .task-count {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12.5px;
      color: var(--color-text-muted);
    }
    .task-count .icon { font-size: 16px; color: var(--color-primary); }

    .action-buttons {
      display: flex;
      gap: 6px;
    }
    .btn-card-action {
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
    .btn-card-action.edit:hover {
      background: var(--color-primary-light);
      border-color: var(--color-primary-border);
      color: var(--color-primary);
    }
    .btn-card-action.delete:hover {
      background: var(--color-danger-bg);
      border-color: var(--color-danger-border);
      color: var(--color-danger);
    }

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
export class CategoryListComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);

  allCategories$ = this.store.select(selectAllCategories);
  systemCategories$ = this.store.select(selectSystemCategories);
  customCategories$ = this.store.select(selectCustomCategories);
  isLoading$ = this.store.select(selectCategoryLoading);

  filteredCategories$ = this.allCategories$;
  activeTab = 'all';
  searchTerm = '';
  selectedTier = '';

  ngOnInit() {
    this.store.dispatch(CategoryActions.loadCategories({
      page: 0,
      size: 50
    }));
    this.filteredCategories$ = this.allCategories$;
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.onFilterChange();
  }

  onSearch() {
    this.onFilterChange();
  }

  onFilterChange() {
    const search = this.searchTerm.trim().toLowerCase();
    const tier = this.selectedTier;
    const base$ = this.activeTab === 'system' ? this.systemCategories$
      : this.activeTab === 'custom' ? this.customCategories$
      : this.allCategories$;

    this.filteredCategories$ = base$.pipe(
      map(list => list.filter(c =>
        (!search || c.name.toLowerCase().includes(search)) &&
        (!tier || c.tier === tier)
      ))
    );
  }

  editCategory(id: number) {
    this.router.navigate(['/category', id, 'edit']);
  }

  deleteCategory(id: number) {
    if (confirm('Bạn có chắc muốn xoá danh mục này?')) {
      this.store.dispatch(CategoryActions.deleteCategory({ id }));
    }
  }

  newCategory() {
    this.router.navigate(['/category/new']);
  }
}
