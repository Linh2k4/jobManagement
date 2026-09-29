export type KpiRanking = 'Xuất sắc' | 'Tốt' | 'Đạt' | 'Cần cải thiện' | 'Không đạt';
export type Trend = 'UP' | 'DOWN' | 'STABLE';

export interface KPI {
  userId: number;
  userFullName?: string;
  periodMonth: string; // YYYY-MM format
  wcr: number; // Weighted Completion Rate (0-100)
  vi: number; // Volume Index (0-120)
  ea: number; // Estimate Accuracy (0-100)
  autoScore: number; // Calculated: (WCR×60% + VI×25% + EA×15%) × 100
  leadScore: number; // 0-100: avg of lead evaluation criteria
  managerScore: number; // 0-100: avg of manager evaluation criteria
  kpiFinal: number; // Final: (auto×40% + lead×35% + mgr×25%)
  ranking: KpiRanking;
  trend: Trend;
  percentageChange: number; // Compare to previous month
  isCached: boolean;
  cacheExpiry?: string; // ISO timestamp
  createdAt: string;
}

export interface KpiConfig {
  periodMonth: string;
  // Weights for auto-score components
  wcrWeight: number; // Default 60%, range 40-80%
  viWeight: number; // Default 25%, range 10-40%
  eaWeight: number; // Default 15%, range 0-30%
  // Weights for final score
  autoScoreWeight: number; // Default 40%, range 20-70%
  leadScoreWeight: number; // Default 35%, range 10-60%
  managerScoreWeight: number; // Default 25%, range 10-50%
  // Difficulty multipliers (1-5 scale)
  difficultyMultipliers: {
    1: number; // Very Easy, default 1.0
    2: number; // Easy, default 2.0
    3: number; // Medium, default 3.0
    4: number; // Hard, default 4.0
    5: number; // Very Hard, default 5.0
  };
  // Ranking thresholds
  thresholds: {
    outstanding: number; // Default 90
    good: number; // Default 75
    satisfactory: number; // Default 60
    needsImprovement: number; // Default 45
  };
  // Other settings
  workingHoursPerDay: number; // Default 8
  effectiveHoursCap: number; // Default 8 (cap per task)
  viCap: number; // Default 120 (max %)
  createdAt: string;
  updatedAt: string;
}

export interface KpiComponent {
  userId: number;
  periodMonth: string;
  wcr: number;
  vi: number;
  ea: number;
  autoScore: number;
  leadScore: number;
  managerScore: number;
  kpiFinal: number;
  ranking: KpiRanking;
  previousMonthKpi?: number;
  trend: Trend;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors Spring Data's Page<T> JSON shape (GET /kpi/team/summary). */
export interface KpiTeamSummary {
  content: KPI[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface UpdateKpiConfigRequest {
  wcrWeight?: number;
  viWeight?: number;
  eaWeight?: number;
  autoScoreWeight?: number;
  leadScoreWeight?: number;
  managerScoreWeight?: number;
  difficultyMultipliers?: {
    1?: number;
    2?: number;
    3?: number;
    4?: number;
    5?: number;
  };
  thresholds?: {
    outstanding?: number;
    good?: number;
    satisfactory?: number;
    needsImprovement?: number;
  };
  workingHoursPerDay?: number;
  effectiveHoursCap?: number;
  viCap?: number;
}

export interface KpiResponse {
  data: KPI;
  success: boolean;
}

export interface KpiConfigResponse {
  data: KpiConfig;
  success: boolean;
}

export interface KpiTeamResponse {
  data: KpiTeamSummary;
  success: boolean;
}

export interface KpiHistoryResponse {
  data: KPI[];
  success: boolean;
}

// KPI Calculation helpers
export const KpiCalculator = {
  calculateWCR: (wcr: number): number => Math.min(Math.max(wcr, 0), 100),

  calculateVI: (vi: number, cap: number = 120): number => Math.min(Math.max(vi, 0), cap),

  calculateEA: (ea: number): number => Math.min(Math.max(ea, 0), 100),

  calculateAutoScore: (wcr: number, vi: number, ea: number, config: {
    wcrWeight: number;
    viWeight: number;
    eaWeight: number;
  }): number => {
    const normalized = (vi / config.viWeight) * 100; // Normalize VI from 0-120 to 0-100
    return (
      wcr * (config.wcrWeight / 100) +
      Math.min(normalized, 100) * (config.viWeight / 100) +
      ea * (config.eaWeight / 100)
    );
  },

  calculateFinalKPI: (auto: number, lead: number, manager: number, config: {
    autoScoreWeight: number;
    leadScoreWeight: number;
    managerScoreWeight: number;
  }): number => {
    return (
      auto * (config.autoScoreWeight / 100) +
      lead * (config.leadScoreWeight / 100) +
      manager * (config.managerScoreWeight / 100)
    );
  },

  getRanking: (kpi: number, thresholds: {
    outstanding: number;
    good: number;
    satisfactory: number;
    needsImprovement: number;
  }): KpiRanking => {
    if (kpi >= thresholds.outstanding) return 'Xuất sắc';
    if (kpi >= thresholds.good) return 'Tốt';
    if (kpi >= thresholds.satisfactory) return 'Đạt';
    if (kpi >= thresholds.needsImprovement) return 'Cần cải thiện';
    return 'Không đạt';
  },

  calculateTrend: (current: number, previous?: number): Trend => {
    if (!previous) return 'STABLE';
    if (current > previous) return 'UP';
    if (current < previous) return 'DOWN';
    return 'STABLE';
  }
};

export type MemberKpiStatus = 'DAT' | 'CANH_BAO' | 'NGUY_HIEM';

/** Mirrors the real backend MemberKpiSummaryResponse (GET /kpi/members).
 * Scope.md §13 — one row per (member, group) pair; a member in N groups appears N times.
 */
export interface MemberKpiSummary {
  userId: number;
  fullName: string;
  role: string;
  groupId?: number; // null for members with no group membership
  groupName?: string; // name of group this row's KPI is scoped to
  kpi: number | null;
  kpiStatus: MemberKpiStatus;
  fastDone: number;
  fastTotal: number;
  multiStepDone: number;
  multiStepTotal: number;
  totalDone: number;
  overdueCount: number;
}

export interface KpiTypeBreakdown {
  done: number;
  total: number;
  completionPercent: number;
}

export interface KpiTaskLine {
  taskId: number;
  title: string;
  timeCategory: string;
  status: string;
  dueDate?: string;
  completedAt?: string;
}

/** Mirrors the real backend MemberKpiDetailResponse (GET /kpi/members/{id}).
 * Scope.md §13 — shows KPI breakdown for one (member, group) pair.
 */
export interface MemberKpiDetail {
  userId: number;
  fullName: string;
  role: string;
  periodMonth: string;
  groupId?: number; // null for members with no group membership
  groupName?: string; // name of group this breakdown is scoped to
  kpiFinal: number | null;
  kpiStatus: MemberKpiStatus;
  previousMonthKpi: number | null;
  changeVsPreviousMonth: number | null;
  autoScore: number | null;
  leadScore: number | null;
  managerScore: number | null;
  leadHasScored: boolean;
  managerHasFinalized: boolean;
  fastTask: KpiTypeBreakdown;
  multiStep: KpiTypeBreakdown;
  tasks: KpiTaskLine[];
}
