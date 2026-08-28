import { useState } from 'react'
import { statisticsApi } from '../api/plannerApi'
import type { ColumnStats } from '../api/plannerApi'
import HistogramChart from './HistogramChart'
import MCVChart from './MCVChart'

export default function StatisticsExplorer() {
  const [stats, setStats]       = useState<Record<string, ColumnStats>>({})
  const [loading, setLoading]   = useState(false)
  const [selected, setSelected] = useState<string | null>(null)
  const [error, setError]       = useState<string | null>(null)

  const collect = async () => {
    setLoading(true); setError(null)
    try {
      const res = await statisticsApi.collect()
      setStats(res.data)
      setSelected(Object.keys(res.data)[0] ?? null)
    } catch {
      setError('Failed to collect. Make sure backend is running and DB has data.')
    } finally { setLoading(false) }
  }

  const corrupt  = async () => { await statisticsApi.corrupt(); await refresh() }
  const restore  = async () => { await statisticsApi.restore(); await refresh() }
  const refresh  = async () => {
    const res = await statisticsApi.getAll(); setStats(res.data)
  }

  const columns = Object.values(stats)
  const active  = selected ? stats[selected] : null

  return (
    <div className="card fade-in">
      <div className="card-header">
        <span style={{ fontSize:18 }}>📊</span>
        <span className="card-title">Phase 2 — Statistics Explorer</span>
        <div className="flex gap-2" style={{ marginLeft:'auto' }}>
          <button className="btn btn-danger" onClick={corrupt} disabled={loading || columns.length === 0} style={{ padding:'6px 12px', fontSize:12 }}>
            💥 Corrupt Stats
          </button>
          <button className="btn btn-success" onClick={restore} disabled={loading || columns.length === 0} style={{ padding:'6px 12px', fontSize:12 }}>
            ✅ Restore
          </button>
        </div>
      </div>

      {columns.length === 0 ? (
        <div style={{ textAlign:'center', padding:'40px 0' }}>
          <div style={{ fontSize:40, marginBottom:12 }}>🗂</div>
          <p className="text-muted mb-4">No statistics collected yet.</p>
          <button className="btn btn-primary" onClick={collect} disabled={loading}>
            {loading ? <><span className="spinner" /> Collecting...</> : '⚡ Collect Statistics'}
          </button>
          {error && <p style={{ color:'#f87171', marginTop:12, fontSize:13 }}>{error}</p>}
        </div>
      ) : (
        <>
          {/* Column summary table */}
          <div style={{ overflowX:'auto', marginBottom:20 }}>
            <table style={{ width:'100%', borderCollapse:'collapse', fontSize:13 }}>
              <thead>
                <tr style={{ borderBottom:'1px solid var(--border)' }}>
                  {['Column','Rows','Distinct','Null','Min','Max','Mode'].map(h => (
                    <th key={h} style={{ padding:'8px 12px', textAlign:'left', color:'var(--text-muted)', fontWeight:600, fontSize:11, letterSpacing:'0.05em', textTransform:'uppercase' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {columns.map(col => (
                  <tr key={col.columnName}
                    onClick={() => setSelected(col.columnName)}
                    style={{
                      borderBottom:'1px solid var(--border)',
                      cursor:'pointer',
                      background: selected === col.columnName ? 'rgba(139,92,246,0.08)' : 'transparent',
                      transition:'background 0.15s',
                    }}>
                    <td style={{ padding:'10px 12px', fontFamily:'JetBrains Mono, monospace', color:'var(--text-accent)', fontWeight:600 }}>{col.columnName}</td>
                    <td style={{ padding:'10px 12px' }}>{col.rowCount.toLocaleString()}</td>
                    <td style={{ padding:'10px 12px' }}>{col.distinctCount.toLocaleString()}</td>
                    <td style={{ padding:'10px 12px' }}>{col.nullCount}</td>
                    <td style={{ padding:'10px 12px', fontSize:12, color:'var(--text-secondary)' }}>{col.minValue ?? '—'}</td>
                    <td style={{ padding:'10px 12px', fontSize:12, color:'var(--text-secondary)' }}>{col.maxValue ?? '—'}</td>
                    <td style={{ padding:'10px 12px' }}>
                      <span className={`badge ${col.mode === 'GOOD' ? 'badge-index-scan' : col.mode === 'CORRUPTED' ? 'badge-full-scan' : 'badge-stats-none'}`}>
                        {col.mode}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Detail for selected column */}
          {active && (
            <div className="fade-in">
              <div className="text-xs text-muted mb-4" style={{ fontWeight:600, textTransform:'uppercase', letterSpacing:'0.05em' }}>
                Detail — {active.columnName}
              </div>

              {active.histogramBuckets && active.histogramBuckets.length > 0 && (
                <HistogramChart column={active.columnName} buckets={active.histogramBuckets} />
              )}

              {active.mostCommonValues && active.mostCommonValues.length > 0 && (
                <MCVChart column={active.columnName} values={active.mostCommonValues} />
              )}
            </div>
          )}
        </>
      )}
    </div>
  )
}
