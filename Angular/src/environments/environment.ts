export const environment = {
  production: false,
  apiUrl: '/api/v1',
  apiBaseUrl: '/api/v1',
  apiTimeout: 30000,
  
  // JWT Configuration
  tokenKey: 'access_token',
  refreshTokenKey: 'refresh_token',
  
  // Feature Flags
  features: {
    enableAnalytics: false,
    enableNotifications: true,
    enableExport: true,
    enableBulkActions: true
  },

  // Pagination
  defaultPageSize: 20,
  pageSizeOptions: [10, 20, 50, 100],

  // Cache
  cacheEnabled: true,
  cacheDurationMs: 300000, // 5 minutes
};
