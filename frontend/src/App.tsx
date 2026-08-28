import { useState, useEffect } from 'react'
import './index.css'
import NaivePlannerPanel from './components/NaivePlannerPanel'
import StatisticsExplorer from './components/StatisticsExplorer'
import BenchmarkPanel from './components/BenchmarkPanel'
import FlowDiagram from './components/FlowDiagram'
import { plannerApi } from './api/plannerApi'

type Tab = 'phase1' | 'phase2' | 'phase3' | 'phase4'

const TABS: { id: Tab; label: string; icon: string; desc: string }[] = [
  { id:'phase1', icon:'🔴', label:'Phase 1 — Naive Planner', desc:'Reproduce the problem' },
  { id:'phase2', icon:'📊', label:'Phase 2 — Statistics',    desc:'Investigate & collect' },
  { id:'phase3', icon:'🎬', label:'Phase 3 — Flow Diagram',  desc:'Visualize the cause' },
  { id:'phase4', icon:'⚡', label:'Phase 4 — Benchmark',     desc:'Before vs After' },
]

export default function App() {
  const [tab, setTab]           = useState<Tab>('phase1')
  const [connected, setConnected] = useState<boolean | null>(null)

  useEffect(() => {
    plannerApi.health()
      .then(() => setConnected(true))
      .catch(() => setConnected(false))
  }, [])

  return (
    <div style={{ minHeight:'100vh' }}>

      {/* ── Header ─────────────────────────────────────────────── */}
      <header style={{
        borderBottom:'1px solid var(--border)',
        background:'rgba(10,14,26,0.9)',
        backdropFilter:'blur(20px)',
        position:'sticky', top:0, zIndex:100,
      }}>
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 24px', display:'flex', alignItems:'center', gap:16, height:60 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10 }}>
            <div style={{
              width:32, height:32, borderRadius:8,
              background:'linear-gradient(135deg,#6366f1,#a855f7)',
              display:'flex', alignItems:'center', justifyContent:'center', fontSize:16,
            }}>🧪</div>
            <div>
              <div style={{ fontSize:14, fontWeight:800, letterSpacing:'-0.02em', lineHeight:1 }}>
                JVM Query Statistics Lab
              </div>
              <div style={{ fontSize:10, color:'var(--text-muted)', letterSpacing:'0.03em' }}>
                1,000,000 rows · 4 phases · bad stats → bad plans
              </div>
            </div>
          </div>

          {/* Backend status indicator */}
          <div style={{ marginLeft:'auto', display:'flex', alignItems:'center', gap:8, fontSize:12 }}>
            {connected === null && <><span className="spinner" style={{ width:10,height:10 }}/><span style={{ color:'var(--text-muted)' }}>Checking backend...</span></>}
            {connected === true  && <><span className="live-dot"/><span style={{ color:'#34d399' }}>Backend connected</span></>}
            {connected === false && <><span style={{ width:8,height:8,borderRadius:'50%',background:'#ef4444',display:'inline-block' }}/><span style={{ color:'#f87171' }}>Backend offline — start Spring Boot</span></>}
          </div>
        </div>
      </header>

      {/* ── Tab bar ───────────────────────────────────────────── */}
      <div style={{ borderBottom:'1px solid var(--border)', background:'rgba(15,22,41,0.6)', backdropFilter:'blur(10px)' }}>
        <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 24px', display:'flex', gap:4 }}>
          {TABS.map(t => (
            <button key={t.id} onClick={() => setTab(t.id)} style={{
              padding:'14px 18px', background:'transparent', border:'none',
              borderBottom:`2px solid ${tab === t.id ? '#8b5cf6' : 'transparent'}`,
              color: tab === t.id ? 'var(--text-primary)' : 'var(--text-muted)',
              cursor:'pointer', fontSize:13, fontWeight: tab === t.id ? 600 : 400,
              fontFamily:'inherit', display:'flex', alignItems:'center', gap:8,
              transition:'all 0.2s', whiteSpace:'nowrap',
            }}>
              <span>{t.icon}</span>
              <span>{t.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* ── Main content ──────────────────────────────────────── */}
      <main style={{ maxWidth:1200, margin:'0 auto', padding:'32px 24px' }}>

        {/* Phase 1 */}
        {tab === 'phase1' && (
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:24, alignItems:'start' }}>
            <NaivePlannerPanel />
            <div className="card fade-in" style={{ padding:24 }}>
              <div className="card-header">
                <span style={{ fontSize:18 }}>💡</span>
                <span className="card-title">What's happening here?</span>
              </div>
              <div style={{ fontSize:13, color:'var(--text-secondary)', lineHeight:1.8 }}>
                <p className="mb-4">The <strong style={{ color:'var(--text-primary)' }}>naive planner</strong> has NO statistics about the data distribution. It assumes every country appears equally often.</p>
                <div style={{ background:'rgba(0,0,0,0.3)', borderRadius:8, padding:14, fontFamily:'JetBrains Mono, monospace', fontSize:12, marginBottom:16, lineHeight:2 }}>
                  <div style={{ color:'#94a3b8' }}>// Planner assumption (wrong!)</div>
                  <div>Countries assumed: <span style={{ color:'#a78bfa' }}>10</span></div>
                  <div>Selectivity: <span style={{ color:'#a78bfa' }}>1 / 10 = 10%</span></div>
                  <div>Estimated rows: <span style={{ color:'#f87171' }}>100,000</span></div>
                  <div style={{ color:'#94a3b8', marginTop:8 }}>// Reality for Iceland</div>
                  <div>Actual rows: <span style={{ color:'#34d399' }}>~120</span></div>
                  <div>Decision: <span style={{ color:'#f87171' }}>FULL SCAN ← WRONG!</span></div>
                </div>
                <p>Try <strong style={{ color:'#f87171' }}>Iceland</strong> to see the worst case: 100,000 estimated, 120 actual. Then try <strong style={{ color:'#34d399' }}>India</strong> to see when the estimate accidentally works.</p>
              </div>
            </div>
          </div>
        )}

        {/* Phase 2 */}
        {tab === 'phase2' && <StatisticsExplorer />}

        {/* Phase 3 */}
        {tab === 'phase3' && <FlowDiagram />}

        {/* Phase 4 */}
        {tab === 'phase4' && <BenchmarkPanel />}
      </main>
    </div>
  )
}
