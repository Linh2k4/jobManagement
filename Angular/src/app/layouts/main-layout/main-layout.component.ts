import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    <app-navbar></app-navbar>
    <div class="main-container">
      <app-sidebar></app-sidebar>
      <main class="main-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .main-container {
      display: flex;
    }
    .main-content {
      flex: 1;
      min-width: 0;
      margin-left: var(--sidebar-width);
      padding: 24px;
      background: var(--color-bg);
      min-height: calc(100vh - var(--navbar-height));
    }

    @media (max-width: 900px) {
      .main-content {
        margin-left: 0;
        padding: 16px;
      }
    }
  `]
})
export class MainLayoutComponent {}
