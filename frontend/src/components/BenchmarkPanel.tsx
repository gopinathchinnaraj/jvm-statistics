import { useState, useEffect } from 'react'
import { plannerApi, statisticsApi, type QueryPlan } from '../api/plannerApi'

const COUNTRIES = ['India','USA','Germany','Japan','UK','Iceland','France','Brazil','Canada','Australia']
const STATUSES  = ['COMPLETED','PENDING','CANCELLED','FAILED','REFUNDED']

const PlanCard = ({ plan, label }: { plan: QueryPlan; label: string }) => {
  const icon  = plan.planType === 'INDEX_SCAN' ? '🟢' : plan.planType === 'FILTER_SCAN' ? '🟡' : '🔴'
  const cls   = plan.planType === 'INDEX_SCAN' ? 'badge-index-scan' : plan.planType === 'FILTER_SCAN' ? 'badge-filter-scan' : 'badge-full-scan'
  const modeC = plan.statisticsMode === 'GOOD' ? 'badge-index-scan' : plan.statisticsMode === 'CORRUPTED' ? 'badge-full-scan' : 'badge-stats-none'

  return (
    <div style={{ flex:1, background:'rgba(255,255,255,0.03)', borderRadius:12, padding:16, border:'1px solid var(--border)' }}>
      <div className="flex items-center justify-between mb-3">
        <span style={{ fontSize:12, fontWeight:700, color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.05em' }}>{label}</span>
        <span className={`badge ${modeC}`} style={{ fontSize:10 }}>STATS: {plan.statisticsMode}</span>
      </div>
      <div className={`badge ${cls}`} style={{ marginBottom:12 }}>{icon} {plan.planType.replace('_',' ')}</div>
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8 }}>
        <div style={{ textAlign:'center', padding:'8px 4px', background:'rgba(0,0,0,0.2)', borderRadius:6 }}>
          <div style={{ fontSize:10, color:'var(--text-muted)', marginBottom:4, textTransform:'uppercase', letterSpacing:'0.05em' }}>Estimated</div>
          <div style={{ fontFamily:'JetBrains Mono, monospace', fontSize:18, fontWeight:700, color: plan.optimalDecision ? '#34d399' : '#f87171' }}>
            {plan.estimatedRows.toLocaleString()}
          </div>
        </div>
        <div style={{ textAlign:'center', padding:'8px 4px', background:'rgba(0,0,0,0.2)', borderRadius:6 }}>
          <div style={{ fontSize:10, color:'var(--text-muted)', marginBottom:4, textTransform:'uppercase', letterSpacing:'0.05em' }}>Actual</div>
          <div style={{ fontFamily:'JetBrains Mono, monospace', fontSize:18, fontWeight:700, color:'#34d399' }}>
            {plan.actualRows.toLocaleString()}
          </div>
        </div>
      </div>
      <div style={{ marginTop:8, fontSize:11, color: plan.optimalDecision ? '#34d399' : '#f87171', textAlign:'center' }}>
        {plan.optimalDecision ? '✓ Optimal decision' : '⚠ Suboptimal decision'}
      </div>
    </div>
  )
}

export default function BenchmarkPanel() {
  const [filterType, setFilterType] = useState<'country'|'status'|'amount'>('country')
  const [country, setCountry]       = useState('Iceland')
  const [status, setStatus]         = useState('REFUNDED')
  const [minAmount, setMinAmount]   = useState('90000')
  const [naive, setNaive]           = useState<QueryPlan | null>(null)
  const [smart, setSmart]           = useState<QueryPlan | null>(null)
  const [loading, setLoading]       = useState(false)
  const [error, setError]           = useState<string | null>(null)

  useEffect(() => {
    // Auto-ensure GOOD stats and display difference values directly on page mount
    const autoLoadDifference = async () => {
      setLoading(true); setError(null)
      try {
        const statusRes = await statisticsApi.getStatus()
        if (!statusRes.data.hasStats || statusRes.data.isCorrupted) {
          await statisticsApi.collect()
        }
        const [nRes, sRes] = await Promise.all([plannerApi.naive.byCountry('Iceland'), plannerApi.smart.byCountry('Iceland')])
        setNaive(nRes.data)
        setSmart(sRes.data)
      } catch {
        setError('Backend unreachable. Start Spring Boot first.')
      } finally {
        setLoading(false)
      }
    }
    autoLoadDifference()
  }, [])

  const run = async () => {
    setLoading(true); setError(null); setNaive(null); setSmart(null)
    try {
      let nRes, sRes
      if (filterType === 'country') {
        [nRes, sRes] = await Promise.all([plannerApi.naive.byCountry(country), plannerApi.smart.byCountry(country)])
      } else if (filterType === 'status') {
        [nRes, sRes] = await Promise.all([plannerApi.naive.byStatus(status), plannerApi.smart.byStatus(status)])
      } else {
        [nRes, sRes] = await Promise.all([plannerApi.naive.byAmount(minAmount), plannerApi.smart.byAmount(minAmount)])
      }

      // If stats missing, auto collect and retry to ensure good stats difference is shown
      if (sRes.data.statisticsMode === 'NONE') {
        await statisticsApi.collect()
        if (filterType === 'country') {
          [nRes, sRes] = await Promise.all([plannerApi.naive.byCountry(country), plannerApi.smart.byCountry(country)])
        } else if (filterType === 'status') {
          [nRes, sRes] = await Promise.all([plannerApi.naive.byStatus(status), plannerApi.smart.byStatus(status)])
        } else {
          [nRes, sRes] = await Promise.all([plannerApi.naive.byAmount(minAmount), plannerApi.smart.byAmount(minAmount)])
        }
      }

      setNaive(nRes.data); setSmart(sRes.data)
    } catch {
      setError('Backend unreachable. Start Spring Boot first.')
    } finally { setLoading(false) }
  }

  const handleRestore = async () => {
    try {
      await statisticsApi.restore()
      await run()
    } catch {
      setError('Failed to restore stats.')
    }
  }

  return (
    <div className="card fade-in">
      <div className="card-header" style={{ display:'flex', alignItems:'center', justifyContent:'space-between', flexWrap:'wrap', gap:12 }}>
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          <span style={{ fontSize:18 }}>⚡</span>
          <span className="card-title">Phase 4 — Before vs After Benchmark</span>
        </div>
        <div style={{ display:'flex', gap:8 }}>
          <button className="btn btn-success" onClick={handleRestore} style={{ padding:'4px 10px', fontSize:11 }}>
            ✅ Restore Good Stats (See Difference)
          </button>
        </div>
      </div>

      <div className="flex gap-2 mb-4">
        {(['country','status','amount'] as const).map(t => (
          <button key={t} onClick={() => setFilterType(t)}
            className={`btn ${filterType === t ? 'btn-primary' : 'btn-ghost'}`}
            style={{ padding:'6px 14px', fontSize:13 }}>
            {t}
          </button>
        ))}
      </div>

      <div className="mb-4">
        {filterType === 'country' && (
          <select className="select" value={country} onChange={e => setCountry(e.target.value)}>
            {COUNTRIES.map(c => <option key={c}>{c}</option>)}
          </select>
        )}
        {filterType === 'status' && (
          <select className="select" value={status} onChange={e => setStatus(e.target.value)}>
            {STATUSES.map(s => <option key={s}>{s}</option>)}
          </select>
        )}
        {filterType === 'amount' && (
          <input className="input" type="number" value={minAmount} onChange={e => setMinAmount(e.target.value)} />
        )}
      </div>

      <button className="btn btn-primary w-full mb-4" onClick={run} disabled={loading}>
        {loading ? <><span className="spinner"/>Running both planners...</> : '⚡ Run Both Planners & Compare'}
      </button>

      {error && <p style={{ color:'#f87171', fontSize:13, marginBottom:12 }}>{error}</p>}

      {naive && smart && (
        <div className="fade-in">
          <div className="flex gap-4" style={{ marginBottom:16 }}>
            <PlanCard plan={naive} label="BEFORE — Naive Planner" />
            <PlanCard plan={smart} label="AFTER — Smart Planner" />
          </div>

          {/* Summary comparison table */}
          <div style={{ background:'rgba(0,0,0,0.25)', borderRadius:10, overflow:'hidden', border:'1px solid var(--border)', fontSize:13 }}>
            {[
              ['Metric','Naive (No Stats)','Smart (With Stats)'],
              ['Estimated Rows', naive.estimatedRows.toLocaleString(), smart.estimatedRows.toLocaleString()],
              ['Actual Rows', naive.actualRows.toLocaleString(), smart.actualRows.toLocaleString()],
              ['Plan Chosen', naive.planType, smart.planType],
              ['Estimation Error', Math.abs(naive.estimatedRows-naive.actualRows).toLocaleString(), Math.abs(smart.estimatedRows-smart.actualRows).toLocaleString()],
              ['Optimal?', naive.optimalDecision ? '✓ Yes' : '✗ No', smart.optimalDecision ? '✓ Yes' : '✗ No'],
            ].map(([label, naiveVal, smartVal], i) => (
              <div key={i} className="flex" style={{
                borderBottom: i < 5 ? '1px solid var(--border)' : 'none',
                background: i === 0 ? 'rgba(255,255,255,0.03)' : 'transparent',
              }}>
                <div style={{ flex:1, padding:'10px 14px', color: i===0 ? 'var(--text-muted)' : 'var(--text-secondary)', fontWeight: i===0 ? 700 : 400, fontSize: i===0 ? 11 : 13, textTransform: i===0 ? 'uppercase' : 'none', letterSpacing: i===0 ? '0.05em' : 'normal' }}>{label}</div>
                <div style={{ flex:1, padding:'10px 14px', borderLeft:'1px solid var(--border)', fontFamily: i>0 ? 'JetBrains Mono, monospace' : 'inherit', color: i===0 ? 'var(--text-muted)' : '#f87171', fontWeight: i===0 ? 700 : 600 }}>{naiveVal}</div>
                <div style={{ flex:1, padding:'10px 14px', borderLeft:'1px solid var(--border)', fontFamily: i>0 ? 'JetBrains Mono, monospace' : 'inherit', color: i===0 ? 'var(--text-muted)' : '#34d399', fontWeight: i===0 ? 700 : 600 }}>{smartVal}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
