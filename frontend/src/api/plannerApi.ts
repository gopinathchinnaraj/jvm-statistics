import axios from 'axios'

const BASE = import.meta.env.VITE_API_URL 
  ? `${import.meta.env.VITE_API_URL.replace(/\/$/, '')}/api` 
  : 'http://localhost:8080/api'

// ── Types ─────────────────────────────────────────────────────────

export interface QueryPlan {
  planType: 'FULL_SCAN' | 'FILTER_SCAN' | 'INDEX_SCAN'
  estimatedRows: number
  actualRows: number
  estimatedCostMs: number
  actualCostMs: number
  statisticsMode: 'NONE' | 'GOOD' | 'CORRUPTED'
  explanation: string
  selectivity: number
  columnFilter: string
  optimalDecision: boolean
}

export interface MostCommonValue {
  value: string
  frequency: number
  pct: number
}

export interface HistogramBucket {
  bucketIndex: number
  bucketLo: number
  bucketHi: number
  rowCount: number
  pct: number
}

export interface ColumnStats {
  columnName: string
  rowCount: number
  distinctCount: number
  nullCount: number
  minValue?: string
  maxValue?: string
  avgValue?: number
  computedAt: string
  mostCommonValues?: MostCommonValue[]
  histogramBuckets?: HistogramBucket[]
  mode: 'NONE' | 'GOOD' | 'CORRUPTED'
}

export interface BenchmarkResult {
  columnFilter: string
  naivePlan: QueryPlan
  smartPlan: QueryPlan
  actualRows: number
  actualCostMs: number
  estimationErrorNaive: number
  estimationErrorSmart: number
  smartIsBetter: boolean
  summary: string
}

// ── Planner API ───────────────────────────────────────────────────

export const plannerApi = {
  health: () => axios.get(`${BASE}/planner/health`),

  naive: {
    byCountry: (country: string) =>
      axios.post<QueryPlan>(`${BASE}/planner/naive/country`, { country }),
    byStatus: (status: string) =>
      axios.post<QueryPlan>(`${BASE}/planner/naive/status`, { status }),
    byAmount: (minAmount: string) =>
      axios.post<QueryPlan>(`${BASE}/planner/naive/amount`, { minAmount }),
  },

  smart: {
    byCountry: (country: string) =>
      axios.post<QueryPlan>(`${BASE}/planner/smart/country`, { country }),
    byStatus: (status: string) =>
      axios.post<QueryPlan>(`${BASE}/planner/smart/status`, { status }),
    byAmount: (minAmount: string) =>
      axios.post<QueryPlan>(`${BASE}/planner/smart/amount`, { minAmount }),
  },

  benchmark: {
    byCountry: (country: string) =>
      axios.post<BenchmarkResult>(`${BASE}/planner/benchmark/country`, { country }),
    byStatus: (status: string) =>
      axios.post<BenchmarkResult>(`${BASE}/planner/benchmark/status`, { status }),
    byAmount: (minAmount: string) =>
      axios.post<BenchmarkResult>(`${BASE}/planner/benchmark/amount`, { minAmount }),
  },
}

// ── Statistics API ────────────────────────────────────────────────

export const statisticsApi = {
  collect: () => axios.post<Record<string, ColumnStats>>(`${BASE}/statistics/collect`),
  getAll:  () => axios.get<Record<string, ColumnStats>>(`${BASE}/statistics/columns`),
  status:  () => axios.get<{ hasStats: boolean; isCorrupted: boolean }>(`${BASE}/statistics/status`),
  corrupt: () => axios.post(`${BASE}/statistics/corrupt`),
  restore: () => axios.post(`${BASE}/statistics/restore`),
}
