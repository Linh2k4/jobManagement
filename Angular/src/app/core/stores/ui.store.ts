import { Injectable, signal } from '@angular/core';

export type AppTheme = 'light' | 'dark' | 'auto';
export type AppLanguage = 'vi' | 'en';

const LOCAL_SETTINGS_KEY = 'jobmanagement_local_settings';

@Injectable({
  providedIn: 'root'
})
export class UiStore {
  readonly sidebarOpen = signal(false);
  readonly theme = signal<AppTheme>('light');
  readonly language = signal<AppLanguage>('vi');

  constructor() {
    this.initFromStorage();
  }

  toggleSidebar() {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
  }

  setTheme(theme: AppTheme) {
    this.theme.set(theme);
    this.applyTheme(theme);
    this.persistSettings();
  }

  setLanguage(lang: AppLanguage) {
    this.language.set(lang);
    document.documentElement.lang = lang;
    this.persistSettings();
  }

  private applyTheme(theme: AppTheme) {
    let effectiveTheme = theme;
    if (theme === 'auto') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      effectiveTheme = prefersDark ? 'dark' : 'light';
    }

    if (effectiveTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      document.body.classList.add('dark-theme');
    } else {
      document.documentElement.removeAttribute('data-theme');
      document.body.classList.remove('dark-theme');
    }
  }

  private initFromStorage() {
    try {
      const raw = localStorage.getItem(LOCAL_SETTINGS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed.theme) {
          this.theme.set(parsed.theme);
          this.applyTheme(parsed.theme);
        }
        if (parsed.language) {
          this.language.set(parsed.language);
          document.documentElement.lang = parsed.language;
        }
      }
    } catch {
      // ignore JSON parse error
    }
  }

  private persistSettings() {
    try {
      const raw = localStorage.getItem(LOCAL_SETTINGS_KEY);
      const existing = raw ? JSON.parse(raw) : {};
      const updated = {
        ...existing,
        theme: this.theme(),
        language: this.language()
      };
      localStorage.setItem(LOCAL_SETTINGS_KEY, JSON.stringify(updated));
    } catch {
      // ignore
    }
  }
}
