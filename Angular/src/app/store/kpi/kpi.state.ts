import { KPI, KpiConfig } from '../../core/models';

/**
 * KPI Feature State
 * Manages KPI calculations, caching, and team rankings
 */
export interface KpiState {
  // Current user KPI
  currentUserKpi: KPI | null;
  lastUpdated: string | null;
  cacheExpiry: string | null;

  // Team KPI summary (paginated)
  teamKpi: KPI[];
  teamKpiPage: number;
  teamKpiSize: number;
  teamKpiTotal: number;

  // KPI history (last 12 months)
  kpiHistory: KPI[];

  // KPI configuration
  config: KpiConfig | null;

  // Loading states
  loading: boolean;
  loadingCurrentKpi: boolean;
  loadingTeamKpi: boolean;
  loadingHistory: boolean;
  loadingConfig: boolean;
  updatingConfig: boolean;

  // Error handling
  error: string | null;
  errorType: 'FETCH' | 'CALCULATE' | 'CONFIG_UPDATE' | null;

  // Cache status
  isCached: boolean;
}

/**
 * Initial state for KPI feature
 */
export const initialKpiState: KpiState = {
  currentUserKpi: null,
  lastUpdated: null,
  cacheExpiry: null,
  teamKpi: [],
  teamKpiPage: 0,
  teamKpiSize: 10,
  teamKpiTotal: 0,
  kpiHistory: [],
  config: null,
  loading: false,
  loadingCurrentKpi: false,
  loadingTeamKpi: false,
  loadingHistory: false,
  loadingConfig: false,
  updatingConfig: false,
  error: null,
  errorType: null,
  isCached: false
};
