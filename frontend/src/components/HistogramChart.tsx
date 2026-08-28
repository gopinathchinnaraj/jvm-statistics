import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import type { HistogramBucket } from '../api/plannerApi'

interface Props {
  column: string
  buckets: HistogramBucket[]
}

const formatK = (v: number) => v >= 1000 ? `${(v/1000).toFixed(0)}k` : `${v}`

export default function HistogramChart({ column, buckets }: Props) {
  const data = buckets.map(b => ({
    name: `${formatK(b.bucketLo)}–${formatK(b.bucketHi)}`,
    rows: b.rowCount,
    pct:  +(b.pct * 100).toFixed(2),
  }))

  const max = Math.max(...data.map(d => d.rows))

  return (
    <div style={{ marginBottom: 24 }}>
      <div className="text-xs text-muted mb-2" style={{ fontWeight:600, textTransform:'uppercase', letterSpacing:'0.05em' }}>
        Histogram — {column}
      </div>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data} margin={{ top:4, right:8, bottom:30, left:8 }}>
          <XAxis dataKey="name" tick={{ fontSize:10, fill:'#475569' }}
            angle={-45} textAnchor="end" interval={1} />
          <YAxis tickFormatter={formatK} tick={{ fontSize:10, fill:'#475569' }} />
          <Tooltip
            contentStyle={{ background:'#0f1629', border:'1px solid var(--border)', borderRadius:8, fontSize:12 }}
            formatter={(v: any) => [Number(v ?? 0).toLocaleString(), 'rows']}
          />
          <Bar dataKey="rows" radius={[3,3,0,0]}>
            {data.map((entry, i) => (
              <Cell key={i}
                fill={entry.rows === max ? '#a855f7' : entry.rows > max * 0.5 ? '#7c3aed' : '#4c1d95'}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
      <p className="text-xs text-muted" style={{ textAlign:'center', marginTop:4 }}>
        Most rows concentrated in the low-value buckets (skewed, not uniform)
      </p>
    </div>
  )
}
