import { Injectable, signal } from '@angular/core';

/**
 * Sidebar open/closed state, shared between Navbar (hamburger toggle) and
 * Sidebar (mobile drawer) without prop-drilling through MainLayout.
 */
@Injectable({
  providedIn: 'root'
})
export class UiStore {
  readonly sidebarOpen = signal(false);

  toggleSidebar() {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
  }
}
