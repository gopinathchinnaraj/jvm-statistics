import { useState } from 'react'
import { plannerApi } from '../api/plannerApi'
import type { QueryPlan } from '../api/plannerApi'

const PLAN_ICONS: Record<string, string> = {
  FULL_SCAN:   '🔴',
  FILTER_SCAN: '🟡',
  INDEX_SCAN:  '🟢',
}

const COUNTRIES = ['India', 'USA', 'Germany', 'Japan', 'UK', 'Iceland', 'France', 'Brazil', 'Canada', 'Australia']
const STATUSES  = ['COMPLETED', 'PENDING', 'CANCELLED', 'FAILED', 'REFUNDED']

export default function NaivePlannerPanel() {
  const [filterType, setFilterType]   = useState<'country' | 'status' | 'amount'>('country')
  const [country, setCountry]         = useState('Iceland')
  const [status, setStatus]           = useState('REFUNDED')
  const [minAmount, setMinAmount]     = useState('90000')
  const [plan, setPlan]               = useState<QueryPlan | null>(null)
  const [loading, setLoading]         = useState(false)
  const [error, setError]             = useState<string | null>(null)

  const run = async () => {
    setLoading(true); setError(null); setPlan(null)
    try {
      let res
      if (filterType === 'country') res = await plannerApi.naive.byCountry(country)
      else if (filterType === 'status') res = await plannerApi.naive.byStatus(status)
      else res = await plannerApi.naive.byAmount(minAmount)
      setPlan(res.data)
    } catch {
      setError('Backend unreachable. Start the Spring Boot app first.')
    } finally {
      setLoading(false)
    }
  }

  const planBadgeClass = plan
    ? plan.planType === 'INDEX_SCAN' ? 'badge badge-index-scan'
    : plan.planType === 'FILTER_SCAN' ? 'badge badge-filter-scan'
    : 'badge badge-full-scan'
    : ''

  return (
    <div className="card fade-in">
      <div className="card-header">
        <span style={{ fontSize: 18 }}>🔍</span>
        <span className="card-title">Phase 1 — Naive Planner (No Statistics)</span>
        <span className="badge badge-stats-none" style={{ marginLeft: 'auto' }}>STATS: NONE</span>
      </div>

      {/* Filter type selector */}
      <div className="mb-4">
        <label className="text-xs text-muted mb-2" style={{ display:'block', fontWeight:600, letterSpacing:'0.05em', textTransform:'uppercase' }}>
          Filter Column
        </label>
        <div className="flex gap-2">
          {(['country','status','amount'] as const).map(t => (
            <button key={t} onClick={() => setFilterType(t)}
              className={`btn ${filterType === t ? 'btn-primary' : 'btn-ghost'}`}
              style={{ padding:'6px 14px', fontSize:13 }}>
              {t}
            </button>
          ))}
        </div>
      </div>

      {/* Filter value */}
      <div className="mb-4">
        <label className="text-xs text-muted mb-2" style={{ display:'block', fontWeight:600, letterSpacing:'0.05em', textTransform:'uppercase' }}>
          Filter Value
        </label>
        {filterType === 'country' && (
          <select className="select" value={country} onChange={e => setCountry(e.target.value)}>
            {COUNTRIES.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        )}
        {filterType === 'status' && (
          <select className="select" value={status} onChange={e => setStatus(e.target.value)}>
            {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        )}
        {filterType === 'amount' && (
          <input className="input" type="number" value={minAmount}
            onChange={e => setMinAmount(e.target.value)}
            placeholder="e.g. 90000" />
        )}
      </div>

      {/* SQL preview */}
      <div className="mb-4" style={{ background:'rgba(0,0,0,0.3)', borderRadius:8, padding:'10px 14px', border:'1px solid var(--border)' }}>
        <code className="font-mono text-sm" style={{ color:'#a78bfa' }}>
          SELECT * FROM orders WHERE{' '}
          {filterType === 'country' && `country = '${country}'`}
          {filterType === 'status' && `status = '${status}'`}
          {filterType === 'amount' && `amount > ${minAmount}`}
        </code>
      </div>

      <button className="btn btn-primary w-full mb-4" onClick={run} disabled={loading}>
        {loading ? <><span className="spinner" /> Running...</> : '▶ Run Naive Planner'}
      </button>

      {error && (
        <div style={{ padding:'10px 14px', background:'rgba(239,68,68,0.1)', border:'1px solid rgba(239,68,68,0.3)', borderRadius:8, color:'#f87171', fontSize:13 }}>
          {error}
        </div>
      )}

      {plan && (
        <div className="fade-in">
          {/* Plan type badge */}
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs text-muted" style={{ fontWeight:600, textTransform:'uppercase', letterSpacing:'0.05em' }}>Plan Chosen</span>
            <span className={planBadgeClass}>
              {PLAN_ICONS[plan.planType]} {plan.planType.replace('_', ' ')}
            </span>
          </div>

          {/* Estimated vs Actual */}
          <div className="metric-row mb-4">
            <div className="metric-cell">
              <div className="metric-label">Estimated Rows</div>
              <div className={`metric-value ${plan.optimalDecision ? '' : 'poor'}`}>
                {plan.estimatedRows.toLocaleString()}
              </div>
            </div>
            <div className="metric-divider" />
            <div className="metric-cell">
              <div className="metric-label">Actual Rows</div>
              <div className="metric-value good">{plan.actualRows.toLocaleString()}</div>
            </div>
          </div>

          {/* Cost */}
          <div className="metric-row mb-4">
            <div className="metric-cell">
              <div className="metric-label">Est. Cost</div>
              <div className="metric-value text-sm">{plan.estimatedCostMs} ms</div>
            </div>
            <div className="metric-divider" />
            <div className="metric-cell">
              <div className="metric-label">Actual Time</div>
              <div className="metric-value text-sm">{plan.actualCostMs} ms</div>
            </div>
          </div>

          {/* Optimal decision indicator */}
          <div style={{
            padding:'10px 14px', borderRadius:8, fontSize:13,
            background: plan.optimalDecision ? 'rgba(16,185,129,0.08)' : 'rgba(239,68,68,0.08)',
            border: `1px solid ${plan.optimalDecision ? 'rgba(16,185,129,0.25)' : 'rgba(239,68,68,0.25)'}`,
            color: plan.optimalDecision ? '#34d399' : '#f87171',
          }}>
            {plan.optimalDecision ? '✓ Plan is reasonable' : '⚠ Suboptimal plan — estimation error caused this'}
          </div>

          {/* Explanation */}
          <div className="mt-4" style={{ padding:'12px 14px', background:'rgba(255,255,255,0.03)', borderRadius:8, border:'1px solid var(--border)' }}>
            <div className="text-xs text-muted mb-2" style={{ fontWeight:600, textTransform:'uppercase', letterSpacing:'0.05em' }}>Planner Explanation</div>
            <p className="text-sm" style={{ color:'var(--text-secondary)', lineHeight:1.7 }}>{plan.explanation}</p>
          </div>
        </div>
      )}
    </div>
  )
}
