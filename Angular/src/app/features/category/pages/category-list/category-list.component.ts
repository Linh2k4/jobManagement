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
    <div class="category-list-container">
      <div class="header">
        <h2>Danh mục đầu việc</h2>
        <button class="btn-new" (click)="newCategory()">+ Danh mục mới</button>
      </div>

      <div class="tabs">
        <button [class.active]="activeTab === 'all'" (click)="activeTab = 'all'">Tất cả</button>
        <button [class.active]="activeTab === 'system'" (click)="activeTab = 'system'">Hệ thống</button>
        <button [class.active]="activeTab === 'custom'" (click)="activeTab = 'custom'">Tuỳ chỉnh</button>
      </div>

      <div class="filters">
        <input
          type="text"
          placeholder="Tìm theo tên..."
          [(ngModel)]="searchTerm"
          (keyup)="onSearch()"
        >
        <select [(ngModel)]="selectedTier" (change)="onFilterChange()">
          <option value="">Tất cả phạm vi</option>
          <option value="SYSTEM">Hệ thống</option>
          <option value="CUSTOM">Tuỳ chỉnh</option>
        </select>
      </div>

      <div *ngIf="isLoading$ | async" class="loading">Đang tải...</div>

      <div class="categories-grid" *ngIf="!(isLoading$ | async)">
        <ng-container *ngIf="activeTab === 'all'">
          <div *ngFor="let category of filteredCategories$ | async" class="category-card" [ngClass]="'tier-' + category.tier">
            <div class="card-header">
              <h3>{{ category.name }}</h3>
              <span class="tier-badge" [class.system]="category.tier === 'SYSTEM'">{{ category.tier === 'SYSTEM' ? 'Hệ thống' : 'Tuỳ chỉnh' }}</span>
            </div>
            <p class="description">{{ category.description }}</p>
            <div class="stats">
              <span>Màu: <span class="color-swatch" [style.background]="category.color"></span></span>
              <span>Công việc: {{ category.taskCount || 0 }}</span>
            </div>
            <div class="actions">
              <button (click)="editCategory(category.id)" class="btn-edit">Sửa</button>
              <button (click)="deleteCategory(category.id)" class="btn-delete" *ngIf="category.tier !== 'SYSTEM'">Xoá</button>
            </div>
          </div>
        </ng-container>

        <ng-container *ngIf="activeTab === 'system'">
          <div *ngFor="let category of systemCategories$ | async" class="category-card" [ngClass]="'tier-' + category.tier">
            <div class="card-header">
              <h3>{{ category.name }}</h3>
              <span class="tier-badge system">Hệ thống</span>
            </div>
            <p class="description">{{ category.description }}</p>
            <div class="stats">
              <span>Màu: <span class="color-swatch" [style.background]="category.color"></span></span>
              <span>Công việc: {{ category.taskCount || 0 }}</span>
            </div>
          </div>
        </ng-container>

        <ng-container *ngIf="activeTab === 'custom'">
          <div *ngFor="let category of customCategories$ | async" class="category-card" [ngClass]="'tier-' + category.tier">
            <div class="card-header">
              <h3>{{ category.name }}</h3>
              <span class="tier-badge">Tuỳ chỉnh</span>
            </div>
            <p class="description">{{ category.description }}</p>
            <div class="stats">
              <span>Màu: <span class="color-swatch" [style.background]="category.color"></span></span>
              <span>Công việc: {{ category.taskCount || 0 }}</span>
            </div>
            <div class="actions">
              <button (click)="editCategory(category.id)" class="btn-edit">Sửa</button>
              <button (click)="deleteCategory(category.id)" class="btn-delete">Xoá</button>
            </div>
          </div>
        </ng-container>
      </div>

      <div *ngIf="!(filteredCategories$ | async)?.length && !(isLoading$ | async)" class="no-data">
        Không có danh mục nào
      </div>
    </div>
  `,
  styles: [`
    .category-list-container { padding: 20px; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .btn-new { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 10px; cursor: pointer; }
    .tabs { display: flex; gap: 10px; margin-bottom: 20px; border-bottom: 1px solid #ddd; }
    .tabs button { padding: 10px 15px; border: none; background: none; cursor: pointer; border-bottom: 2px solid transparent; }
    .tabs button.active { border-bottom-color: #2563eb; color: #2563eb; font-weight: bold; }
    .filters { display: flex; gap: 10px; margin-bottom: 20px; }
    .filters input, .filters select { padding: 8px; border: 1px solid #ddd; border-radius: 10px; }
    .filters input { flex: 1; }
    .categories-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
    .category-card { background: white; border: 1px solid #ddd; border-radius: 16px; padding: 15px; }
    .tier-SYSTEM { border-left: 4px solid #2e7d32; }
    .tier-CUSTOM { border-left: 4px solid #1d4ed8; }
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
    .card-header h3 { margin: 0; }
    .tier-badge { background: #e0e0e0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: bold; }
    .tier-badge.system { background: #c8e6c9; color: #2e7d32; }
    .description { color: #666; margin: 10px 0; }
    .stats { display: flex; gap: 15px; font-size: 12px; margin: 10px 0; }
    .color-swatch { display: inline-block; width: 16px; height: 16px; border-radius: 2px; margin: 0 4px; }
    .actions { display: flex; gap: 8px; margin-top: 10px; }
    .btn-edit, .btn-delete { padding: 4px 8px; border: 1px solid #ddd; border-radius: 10px; cursor: pointer; }
    .btn-edit { background: white; }
    .btn-delete { background: #ffebee; color: #d32f2f; }
    .loading { text-align: center; color: #2563eb; padding: 40px; }
    .no-data { text-align: center; color: #999; padding: 40px; }
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

  onSearch() {
    this.onFilterChange();
  }

  onFilterChange() {
    const search = this.searchTerm.trim().toLowerCase();
    const tier = this.selectedTier;
    this.filteredCategories$ = this.allCategories$.pipe(
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
