import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import type { MostCommonValue } from '../api/plannerApi'

interface Props { column: string; values: MostCommonValue[] }

const COLORS = ['#6366f1','#8b5cf6','#a855f7','#c084fc','#d8b4fe','#e9d5ff','#7c3aed','#4c1d95','#2e1065','#5b21b6']

export default function MCVChart({ column, values }: Props) {
  const top = values.slice(0, 10)
  const data = top.map(v => ({ name: v.value, pct: +(v.pct * 100).toFixed(3), rows: v.frequency }))

  return (
    <div style={{ marginBottom: 24 }}>
      <div className="text-xs text-muted mb-2" style={{ fontWeight:600, textTransform:'uppercase', letterSpacing:'0.05em' }}>
        Most Common Values — {column}
      </div>
      <ResponsiveContainer width="100%" height={180}>
        <BarChart data={data} layout="vertical" margin={{ top:4, right:60, bottom:4, left:80 }}>
          <XAxis type="number" unit="%" tick={{ fontSize:10, fill:'#475569' }} />
          <YAxis type="category" dataKey="name" tick={{ fontSize:11, fill:'#94a3b8' }} width={75} />
          <Tooltip
            contentStyle={{ background:'#0f1629', border:'1px solid var(--border)', borderRadius:8, fontSize:12 }}
            formatter={(v: any, _: any, props: any) => [
              `${v}% (${Number(props?.payload?.rows ?? 0).toLocaleString()} rows)`, ''
            ]}
          />
          <Bar dataKey="pct" radius={[0,4,4,0]}>
            {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
