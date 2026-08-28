import React, { useEffect, useState } from 'react'

interface Step {
  icon: string
  label: string
  sub: string
  color: string
}

const GOOD_STEPS: Step[] = [
  { icon:'📦', label:'1M Rows', sub:'Skewed data: Iceland = 0.012%', color:'#6366f1' },
  { icon:'📊', label:'Good Statistics', sub:'Histogram + MCV collected', color:'#8b5cf6' },
  { icon:'🧠', label:'Smart Estimate', sub:'Iceland → 0.012% → ~120 rows', color:'#a855f7' },
  { icon:'📌', label:'INDEX SCAN chosen', sub:'Selectivity < 5% threshold', color:'#10b981' },
  { icon:'⚡', label:'18 ms', sub:'Only 120 rows read', color:'#34d399' },
]

const BAD_STEPS: Step[] = [
  { icon:'📦', label:'1M Rows', sub:'Skewed data: Iceland = 0.012%', color:'#6366f1' },
  { icon:'❌', label:'No / Bad Statistics', sub:'Uniform distribution assumed', color:'#ef4444' },
  { icon:'💀', label:'Bad Estimate', sub:'Iceland → 10% → 100,000 rows', color:'#dc2626' },
  { icon:'🐢', label:'FULL SCAN chosen', sub:'Selectivity ≥ 5% threshold', color:'#ef4444' },
  { icon:'🔥', label:'850 ms', sub:'1,000,000 rows read for 120 results', color:'#f87171' },
]

function FlowStep({ step, index, active }: { step: Step; index: number; active: boolean }) {
  return (
    <div style={{
      display:'flex', flexDirection:'column', alignItems:'center', flex:1,
      opacity: active ? 1 : 0.3,
      transition: `opacity 0.4s ease ${index * 0.15}s`,
    }}>
      <div style={{
        width:52, height:52, borderRadius:'50%', display:'flex', alignItems:'center',
        justifyContent:'center', fontSize:22,
        background: active ? `${step.color}22` : 'rgba(255,255,255,0.04)',
        border: `2px solid ${active ? step.color : 'rgba(255,255,255,0.08)'}`,
        marginBottom:8, transition:`all 0.4s ease ${index * 0.15}s`,
        boxShadow: active ? `0 0 16px ${step.color}44` : 'none',
      }}>
        {step.icon}
      </div>
      <div style={{ fontSize:11, fontWeight:700, color: active ? step.color : 'var(--text-muted)', textAlign:'center', lineHeight:1.3, marginBottom:3 }}>
        {step.label}
      </div>
      <div style={{ fontSize:10, color:'var(--text-muted)', textAlign:'center', lineHeight:1.4, maxWidth:90 }}>
        {step.sub}
      </div>
    </div>
  )
}

export default function FlowDiagram() {
  const [mode, setMode]         = useState<'good'|'bad'>('bad')
  const [activeStep, setActive] = useState(0)
  const [running, setRunning]   = useState(false)

  const steps = mode === 'good' ? GOOD_STEPS : BAD_STEPS

  const animate = () => {
    setRunning(true); setActive(0)
    let i = 0
    const timer = setInterval(() => {
      i++; setActive(i)
      if (i >= steps.length - 1) { clearInterval(timer); setRunning(false) }
    }, 600)
  }

  useEffect(() => { animate() }, [mode])

  return (
    <div className="card fade-in">
      <div className="card-header">
        <span style={{ fontSize:18 }}>🎬</span>
        <span className="card-title">Phase 3 — The Core Problem Visualized</span>
        <div className="flex gap-2" style={{ marginLeft:'auto' }}>
          <button className="btn btn-danger" onClick={() => setMode('bad')} style={{ padding:'6px 12px', fontSize:12, opacity: mode==='bad' ? 1 : 0.5 }}>
            😈 Bad Stats
          </button>
          <button className="btn btn-success" onClick={() => setMode('good')} style={{ padding:'6px 12px', fontSize:12, opacity: mode==='good' ? 1 : 0.5 }}>
            😊 Good Stats
          </button>
        </div>
      </div>

      {/* Mode headline */}
      <div style={{
        textAlign:'center', padding:'12px 20px', borderRadius:10, marginBottom:20,
        background: mode === 'good' ? 'rgba(16,185,129,0.08)' : 'rgba(239,68,68,0.08)',
        border: `1px solid ${mode === 'good' ? 'rgba(16,185,129,0.25)' : 'rgba(239,68,68,0.25)'}`,
        color: mode === 'good' ? '#34d399' : '#f87171',
        fontWeight:700, fontSize:14, letterSpacing:'0.02em',
      }}>
        {mode === 'good'
          ? '✅ GOOD STATISTICS → ACCURATE ESTIMATE → OPTIMAL PLAN → FAST QUERY'
          : '❌ BAD STATISTICS → WRONG ESTIMATE → WRONG PLAN → SLOW QUERY'}
      </div>

      {/* Flow steps */}
      <div style={{ display:'flex', alignItems:'flex-start', gap:8, padding:'8px 0' }}>
        {steps.map((step, i) => (
          <React.Fragment key={i}>
            <FlowStep step={step} index={i} active={i <= activeStep} />
            {i < steps.length - 1 && (
              <div style={{
                fontSize:18, color: i < activeStep ? (mode === 'good' ? '#34d399' : '#f87171') : 'var(--border)',
                marginTop:14, transition:`color 0.4s ease ${i * 0.15}s`, flexShrink:0,
              }}>→</div>
            )}
          </React.Fragment>
        ))}
      </div>

      <div style={{ textAlign:'center', marginTop:16 }}>
        <button className="btn btn-ghost" onClick={animate} disabled={running} style={{ fontSize:12 }}>
          {running ? '▶ Animating...' : '🔁 Replay'}
        </button>
      </div>
    </div>
  )
}
