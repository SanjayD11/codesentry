import { useState, useEffect, useCallback, useMemo } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { listMyReports, deleteReport, downloadReport } from '../../api/reportApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import ConfirmDialog from '../../components/ui/ConfirmDialog'

const fmtDate = (iso) => iso ? new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'
const fmtSize = (b) => b > 1048576 ? `${(b/1048576).toFixed(1)} MB` : b > 1024 ? `${(b/1024).toFixed(0)} KB` : `${b??0} B`

const scoreColor = (s) => s >= 80 ? '#16a34a' : s >= 60 ? '#f59e0b' : '#dc2626'
const scoreBg    = (s) => s >= 80 ? '#f0fdf4' : s >= 60 ? '#fffbeb' : '#fef2f2'
const riskLabel  = (s) => s >= 80 ? 'Low Risk' : s >= 60 ? 'Medium' : s >= 40 ? 'High Risk' : 'Critical'

/* ── Compare Modal ── */
function CompareModal({ reports, onClose }) {
  const [a, setA] = useState(reports[0]?.id || '')
  const [b, setB] = useState(reports[1]?.id || '')
  const rA = reports.find(r => r.id === Number(a) || r.id === a)
  const rB = reports.find(r => r.id === Number(b) || r.id === b)

  const scoreDiff = (rA && rB && rA.securityScore != null && rB.securityScore != null)
    ? { d: rA.securityScore - rB.securityScore, improved: (rA.securityScore - rB.securityScore) > 0 }
    : null

  return createPortal(
    <div
      style={{ position:'fixed', inset:0, background:'rgba(15,23,42,0.45)', zIndex:9999, display:'flex', alignItems:'center', justifyContent:'center', padding:24 }}
      onClick={onClose}
    >
      <div
        style={{ background:'#ffffff', borderRadius:16, width:'100%', maxWidth:620, maxHeight:'90vh', overflow:'auto', boxShadow:'0 20px 40px rgba(15,23,42,0.15)', border:'1px solid #cbd5e1' }}
        onClick={e => e.stopPropagation()}
      >
        <style>{`
          @keyframes cdlgSlideUp {
            from { opacity: 0; transform: translateY(8px) scale(0.98); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
          .cmp-select:focus { border-color: #2563eb !important; box-shadow: 0 0 0 3px rgba(37,99,235,0.12) !important; outline: none; }
        `}</style>

        {/* Header */}
        <div style={{ padding:'20px 24px', borderBottom:'1px solid var(--snt-border-2)', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
          <div style={{ display:'flex', alignItems:'center', gap:12 }}>
            <div style={{ width:38, height:38, borderRadius:10, background:'#eff6ff', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
              <span className="material-symbols-outlined" style={{ fontSize:19, color:'#2563eb', fontVariationSettings:"'FILL' 1" }}>compare_arrows</span>
            </div>
            <div>
              <p style={{ margin:0, fontSize:15, fontWeight:700, color:'var(--snt-text-1)', letterSpacing:'-0.01em', fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Compare Reports</p>
              <p style={{ margin:'1px 0 0', fontSize:12, color:'var(--snt-text-4)', fontWeight:500 }}>Side-by-side security analysis</p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ width:30, height:30, border:'1px solid var(--snt-border)', borderRadius:8, background:'var(--snt-surface)', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', transition:'background 100ms ease, border-color 100ms ease' }}
            onMouseEnter={e=>{e.currentTarget.style.background='#f8fafc';e.currentTarget.style.borderColor='#cbd5e1'}}
            onMouseLeave={e=>{e.currentTarget.style.background='#fff';e.currentTarget.style.borderColor='#e2e8f0'}}
          >
            <span className="material-symbols-outlined" style={{ fontSize:16, color:'var(--snt-text-3)' }}>close</span>
          </button>
        </div>

        <div style={{ padding:'24px', display:'flex', flexDirection:'column', gap:20 }}>
          {/* Selectors */}
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:14 }}>
            {[{ label:'Baseline', val:a, set:setA }, { label:'Comparison', val:b, set:setB }].map(({ label, val, set }) => (
              <div key={label}>
                <p style={{ margin:'0 0 6px', fontSize:11, fontWeight:700, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.07em' }}>{label}</p>
                <select
                  className="cmp-select"
                  value={val}
                  onChange={e => set(e.target.value)}
                  style={{ width:'100%', padding:'9px 12px', border:'1px solid #cbd5e1', borderRadius:9, fontSize:13, color:'#0f172a', background:'#f8fafc', cursor:'pointer', transition:'border-color 100ms ease, box-shadow 100ms ease', fontFamily:"'Manrope',sans-serif", fontWeight:500 }}
                >
                  {reports.map(r => <option key={r.id} value={r.id}>{r.reportName || `Report #${r.id}`}</option>)}
                </select>
              </div>
            ))}
          </div>

          {rA && rB && (
            <>
              {/* Score cards + delta */}
              <div style={{ display:'grid', gridTemplateColumns:'1fr auto 1fr', gap:16, alignItems:'center' }}>
                {/* Baseline Card */}
                <div style={{
                  background: scoreBg(rA.securityScore ?? 0),
                  borderRadius:14, padding:'20px 16px', textAlign:'center',
                  border:`1px solid ${rA.securityScore >= 80 ? '#bbf7d0' : rA.securityScore >= 60 ? '#fde68a' : '#fecaca'}`,
                  display:'flex', flexDirection:'column', alignItems:'center', gap:6, minWidth:0
                }}>
                  <span style={{ fontSize:10, fontWeight:700, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.09em' }}>
                    Baseline
                  </span>
                  <span style={{ fontSize:36, fontWeight:800, color: scoreColor(rA.securityScore ?? 0), letterSpacing:'-0.03em', lineHeight:1 }}>
                    {rA.securityScore?.toFixed(0) ?? '—'}
                  </span>
                  <span style={{ fontSize:11, fontWeight:600, color: scoreColor(rA.securityScore ?? 0) }}>
                    {rA.securityScore != null ? riskLabel(rA.securityScore) : 'No score'}
                  </span>
                  <span style={{ fontSize:11, color:'#64748b', fontWeight:500, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', maxWidth:'100%' }}>
                    {rA.reportName || `Report #${rA.id}`}
                  </span>
                </div>

                {/* Delta Badge */}
                <div style={{ display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:4, padding:'0 4px' }}>
                  {scoreDiff ? (
                    <>
                      <div style={{ width:40, height:40, borderRadius:20, background: scoreDiff.improved ? '#f0fdf4' : '#fef2f2', border:`1px solid ${scoreDiff.improved ? '#bbf7d0' : '#fecaca'}`, display:'flex', alignItems:'center', justifyContent:'center' }}>
                        <span className="material-symbols-outlined" style={{ fontSize:22, color: scoreDiff.improved ? '#16a34a' : '#dc2626', fontVariationSettings:"'FILL' 1" }}>
                          {scoreDiff.improved ? 'trending_up' : 'trending_down'}
                        </span>
                      </div>
                      <span style={{ fontSize:13, fontWeight:800, color: scoreDiff.improved ? '#16a34a' : '#dc2626', marginTop:2 }}>
                        {scoreDiff.improved ? '+' : ''}{scoreDiff.d.toFixed(1)}
                      </span>
                      <span style={{ fontSize:10, color:'#64748b', fontWeight:600, textTransform:'uppercase', letterSpacing:'0.06em' }}>Delta</span>
                    </>
                  ) : (
                    <span style={{ fontSize:20, color:'#cbd5e1' }}>—</span>
                  )}
                </div>

                {/* Comparison Card */}
                <div style={{
                  background: scoreBg(rB.securityScore ?? 0),
                  borderRadius:14, padding:'20px 16px', textAlign:'center',
                  border:`1px solid ${rB.securityScore >= 80 ? '#bbf7d0' : rB.securityScore >= 60 ? '#fde68a' : '#fecaca'}`,
                  display:'flex', flexDirection:'column', alignItems:'center', gap:6, minWidth:0
                }}>
                  <span style={{ fontSize:10, fontWeight:700, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.09em' }}>
                    Comparison
                  </span>
                  <span style={{ fontSize:36, fontWeight:800, color: scoreColor(rB.securityScore ?? 0), letterSpacing:'-0.03em', lineHeight:1 }}>
                    {rB.securityScore?.toFixed(0) ?? '—'}
                  </span>
                  <span style={{ fontSize:11, fontWeight:600, color: scoreColor(rB.securityScore ?? 0) }}>
                    {rB.securityScore != null ? riskLabel(rB.securityScore) : 'No score'}
                  </span>
                  <span style={{ fontSize:11, color:'#64748b', fontWeight:500, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', maxWidth:'100%' }}>
                    {rB.reportName || `Report #${rB.id}`}
                  </span>
                </div>
              </div>

              {/* Detail table */}
              <div style={{ border:'1px solid #e2e8f0', borderRadius:12, overflow:'hidden' }}>
                {[
                  { label:'Generated', vA: fmtDate(rA.generatedAt), vB: fmtDate(rB.generatedAt) },
                  { label:'Project',   vA: rA.projectName || '—', vB: rB.projectName || '—' },
                  { label:'Risk',      vA: rA.securityScore != null ? riskLabel(rA.securityScore) : '—', vB: rB.securityScore != null ? riskLabel(rB.securityScore) : '—' },
                  { label:'File Size', vA: fmtSize(rA.reportSizeBytes), vB: fmtSize(rB.reportSizeBytes) },
                ].map((row, i) => (
                  <div key={row.label} style={{ display:'grid', gridTemplateColumns:'120px 1fr 1fr', borderBottom: i < 3 ? '1px solid #e2e8f0' : 'none', background: i % 2 === 0 ? '#fff' : '#f8fafc' }}>
                    <div style={{ padding:'11px 16px', fontSize:11, fontWeight:700, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.07em', display:'flex', alignItems:'center' }}>{row.label}</div>
                    <div style={{ padding:'11px 16px', fontSize:13, color:'#0f172a', borderLeft:'1px solid #e2e8f0', fontWeight:500, display:'flex', alignItems:'center' }}>{row.vA}</div>
                    <div style={{ padding:'11px 16px', fontSize:13, color:'#0f172a', borderLeft:'1px solid #e2e8f0', fontWeight:500, display:'flex', alignItems:'center' }}>{row.vB}</div>
                  </div>
                ))}
              </div>
            </>
          )}

          {/* Footer */}
          <div style={{ display:'flex', justifyContent:'flex-end', paddingTop:4 }}>
            <button
              onClick={onClose}
              style={{ height:38, padding:'0 22px', background:'var(--snt-surface-2)', color:'var(--snt-text-2)', border:'1px solid var(--snt-border)', borderRadius:9, fontSize:13, fontWeight:600, cursor:'pointer', fontFamily:"'Manrope',sans-serif", transition:'all 100ms ease' }}
              onMouseEnter={e=>{e.currentTarget.style.background='#f1f5f9'}}
              onMouseLeave={e=>{e.currentTarget.style.background='#f8fafc'}}
            >Close</button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  )
}

/* ── Main Component ── */
export default function Reports() {
  const [reports,  setReports]  = useState([])
  const [loading,  setLoading]  = useState(true)
  const [search,   setSearch]   = useState('')
  const [deleting, setDeleting] = useState(null)
  const [compare,  setCompare]  = useState(false)
  const [confirmState, setConfirmState] = useState(null) // { id }
  const [favs,     setFavs]     = useState(() => {
    try { return JSON.parse(localStorage.getItem('report_favs') || '[]') } catch { return [] }
  })
  const [filter,   setFilter]   = useState('ALL')
  const addToast = useToast()
  const navigate = useNavigate()

  const fetchReports = useCallback(async () => {
    setLoading(true)
    try {
      const res = await listMyReports()
      setReports(res.data.data || [])
    } catch {
      addToast('Failed to load reports', 'error')
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => { fetchReports() }, [fetchReports])

  const toggleFav = (id) => {
    setFavs(prev => {
      const next = prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
      localStorage.setItem('report_favs', JSON.stringify(next))
      return next
    })
  }

  const handleDownload = async (report) => {
    try {
      const res = await downloadReport(report.id)
      const url = window.URL.createObjectURL(new Blob([res.data], { type:'application/pdf' }))
      const a   = document.createElement('a')
      a.href    = url
      a.download = report.reportName?.endsWith('.pdf') ? report.reportName : `report-${report.id}.pdf`
      document.body.appendChild(a); a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      addToast('Report downloaded!', 'success')
    } catch {
      addToast('Failed to download report', 'error')
    }
  }

  const handleDelete = (id) => {
    setConfirmState({ id })
  }

  const handleDeleteConfirm = async () => {
    const id = confirmState?.id
    setDeleting(id)
    setConfirmState(null)
    try {
      await deleteReport(id)
      setReports(r => r.filter(x => x.id !== id))
      addToast('Report deleted', 'success')
    } catch {
      addToast('Failed to delete report', 'error')
    } finally {
      setDeleting(null)
    }
  }

  const handleAiExplain = (report) => {
    navigate('/chat', { state: { reportContext: { id: report.id, name: report.reportName, scanId: report.scanHistoryId } } })
  }

  const filtered = useMemo(() => {
    return reports.filter(r => {
      const matchSearch = !search ||
        r.reportName?.toLowerCase().includes(search.toLowerCase()) ||
        r.projectName?.toLowerCase().includes(search.toLowerCase())
      const matchFilter =
        filter === 'ALL' ? true :
        filter === 'FAVORITE' ? favs.includes(r.id) :
        filter === 'CRITICAL' ? (r.securityScore != null && r.securityScore < 40) :
        true
      return matchSearch && matchFilter
    })
  }, [reports, search, filter, favs])

  const stats = useMemo(() => ({
    total:    reports.length,
    critical: reports.filter(r => r.securityScore != null && r.securityScore < 40).length,
    avgScore: (() => {
      const s = reports.filter(r => r.securityScore != null)
      return s.length ? (s.reduce((a, r) => a + r.securityScore, 0) / s.length).toFixed(0) : '—'
    })(),
    latest: reports.length ? new Date(Math.max(...reports.map(r => new Date(r.generatedAt || 0)))).toLocaleDateString('en-US', { month:'short', day:'numeric' }) : '—',
  }), [reports])

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", paddingBottom: 64 }}>
      <style>{`
        .rp-card { box-shadow: 0 1px 3px rgba(15,23,42,0.04); transition: border-color 0.2s ease, box-shadow 0.2s ease; cursor: pointer; transform: none !important; }
        .rp-card:hover { border-color: #93c5fd !important; box-shadow: 0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04); transform: none !important; }
        .rp-act { height:36px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; color:#475569; cursor:pointer; display:flex; align-items:center; gap:6px; font-size:12px; font-weight:600; transition: border-color 0.15s ease, background 0.15s ease; flex: 1; min-width: 0; justify-content: center; transform: none !important; }
        .rp-act:hover { background:#f8fafc; color:#0f172a; border-color:#94a3b8; transform: none !important; }
        .rp-act.primary:hover { background:#eff6ff; color:#2563eb; border-color:#93c5fd; transform: none !important; }
        .rp-act.ai:hover { background:#f0fdf4; color:#16a34a; border-color:#86efac; transform: none !important; }
        .rp-act:disabled { opacity:0.4; cursor:not-allowed; transform: none !important; }
        /* Desktop */
        .rp-stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 24px; }
        .rp-hero-btns-row { display: flex; gap: 10px; }
        /* Tablet */
        @media (max-width: 1024px) {
          .rp-stats-grid { grid-template-columns: repeat(2, 1fr) !important; }
        }
        /* Mobile */
        @media (max-width: 640px) {
          .rp-stats-grid { grid-template-columns: 1fr !important; gap: 10px !important; }
          .rp-hero-mobile { border: none !important; box-shadow: none !important; background: transparent !important; padding: 0 !important; margin-bottom: 24px !important; }
          .rp-hero-inner { flex-direction: column !important; gap: 16px !important; }
          .rp-hero-left { gap: 14px !important; }
          .rp-hero-icon { display: none !important; }
          .rp-hero-title { font-size: 28px !important; line-height: 1.15 !important; }
          .rp-hero-subtitle { font-size: 13px !important; }
          .rp-hero-btns { flex-direction: column !important; width: 100% !important; gap: 12px !important; }
          .rp-hero-btns-row { display: flex; gap: 10px; width: 100%; }
          .rp-hero-btns-row > button { flex: 1; justify-content: center; padding: 0 12px !important; }
          .rp-icon-btn { flex: 0 0 44px !important; width: 44px !important; padding: 0 !important; }
          .rp-btn-full { width: 100% !important; justify-content: center !important; }
          
          .rp-stat-card { flex-direction: row !important; justify-content: space-between !important; align-items: center !important; padding: 16px !important; border-radius: 12px !important; border-color: #f1f5f9 !important; box-shadow: 0 1px 2px rgba(15,23,42,0.03) !important; }
          .rp-stat-card-left { margin-bottom: 0 !important; gap: 12px !important; }
          .rp-stat-card-left span.label { font-size: 14px !important; color: #0f172a !important; font-weight: 600 !important; }
          .rp-stat-card-val { font-size: 15px !important; display: flex !important; align-items: center !important; gap: 8px !important; }
        }
      `}</style>

      {/* ── Compact Page Header ── */}
      <div className="hero-card rp-hero-mobile" style={{ marginBottom: 24 }}>
        <div className="rp-hero-inner" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
          <div className="rp-hero-left" style={{ display: 'flex', alignItems: 'center', gap: 14, minWidth: 0, flex: 1 }}>
            <div className="rp-hero-icon" style={{ width: 40, height: 40, borderRadius: 10, background: '#eff6ff', border: '1px solid #bfdbfe', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#2563eb', fontVariationSettings: "'FILL' 1" }}>folder_special</span>
            </div>
            <div style={{ minWidth: 0 }}>
              <p style={{ margin: '0 0 1px', fontSize: 11, fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Documentation</p>
              <h1 className="rp-hero-title" style={{ margin: 0, fontSize: 'clamp(18px, 4vw, 22px)', fontWeight: 700, color: '#0f172a', letterSpacing: '-0.025em' }}>Security Reports</h1>
              <p className="rp-hero-subtitle" style={{ margin: '3px 0 0', fontSize: 13, color: '#64748b', fontWeight: 400, maxWidth: 420 }}>Review, download, and analyze your AI-generated security reports</p>
            </div>
          </div>
          <div className="hero-actions rp-hero-btns rp-hero-actions" style={{ flexShrink: 0 }}>
            <div className="rp-hero-btns-row">
              <button className="btn-responsive rp-icon-btn" style={{ height: 40, padding: '0 16px', background: '#f8fafc', color: '#0f172a', border: '1px solid #cbd5e1', borderRadius: 10, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, boxShadow: '0 1px 2px rgba(15,23,42,0.04)' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#2563eb', fontVariationSettings: "'FILL' 1" }}>folder</span>
              </button>
              <button onClick={fetchReports} className="btn-responsive"
                style={{ height: 40, padding: '0 16px', background: '#ffffff', color: '#0f172a', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, boxShadow: '0 1px 2px rgba(15,23,42,0.04)', transition: 'all 0.2s ease' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>refresh</span>Refresh
              </button>
              <button onClick={() => setCompare(true)} className="btn-responsive"
                style={{ height: 40, padding: '0 16px', background: '#ffffff', color: '#0f172a', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, boxShadow: '0 1px 2px rgba(15,23,42,0.04)', transition: 'all 0.2s ease', opacity: reports.length < 2 ? 0.5 : 1, pointerEvents: reports.length < 2 ? 'none' : 'auto' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>compare_arrows</span>Compare
              </button>
            </div>
            <button onClick={() => navigate('/scanner')} className="btn-responsive rp-btn-full"
              style={{ height: 40, padding: '0 20px', background: '#0058be', color: '#fff', border: '1px solid #0058be', borderRadius: 10, fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, boxShadow: '0 2px 6px rgba(0,88,190,0.2)', transition: 'all 0.2s ease' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>add</span>New Scan
            </button>
          </div>
        </div>
      </div>

      {/* ── Stats Strip ── */}
      <div className="rp-stats-grid">
        {[
          { label:'Total Reports', value: stats.total,    icon:'description',   color:'#2563eb', bg:'#eff6ff' },
          { label:'Critical Risk', value: stats.critical, icon:'warning',        color:'#dc2626', bg:'#fef2f2' },
          { label:'Avg Score',     value: stats.avgScore, icon:'speed',          color:'#16a34a', bg:'#f0fdf4' },
          { label:'Latest Report', value: stats.latest,   icon:'calendar_today', color:'#7c3aed', bg:'#f5f3ff' },
        ].map(st => (
          <div key={st.label} className="rp-stat-card" style={{
            background:'#ffffff', border:'1px solid #e2e8f0', borderRadius:14, padding:'20px 20px 18px', display: 'flex', flexDirection: 'column',
            boxShadow: '0 1px 3px rgba(15,23,42,0.04)', transition:'border-color 0.2s ease, box-shadow 0.2s ease'
          }}
          onMouseEnter={e => { e.currentTarget.style.borderColor = '#93c5fd'; e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)' }}
          onMouseLeave={e => { e.currentTarget.style.borderColor = '#e2e8f0'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)' }}
          >
            <div className="rp-stat-card-left" style={{ display:'flex', alignItems:'center', gap:8, marginBottom:10 }}>
              <div style={{ width:28, height:28, borderRadius:8, background:st.bg, display:'flex', alignItems:'center', justifyContent:'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize:15, color:st.color, fontVariationSettings:"'FILL' 1" }}>{st.icon}</span>
              </div>
              <span className="label" style={{ fontSize:12, color:'#64748b', fontWeight:600 }}>{st.label}</span>
            </div>
            <p className="rp-stat-card-val" style={{ margin:0, fontSize:28, fontWeight:700, color:'#0f172a', letterSpacing:'-0.03em', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif" }}>
              {st.label === 'Avg Score' ? `${st.value} / 100` : st.value}
            </p>
          </div>
        ))}
      </div>

      {/* ── Filter Bar ── */}
      <div className="filter-bar">
        <div style={{ position: 'relative', flex: '1 1 200px', maxWidth: 260 }}>
          <span className="material-symbols-outlined" style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', fontSize: 16, color: '#64748b' }}>search</span>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search reports…"
            style={{ paddingLeft: 36, paddingRight: 16, height: 40, width: '100%', background: '#ffffff', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: 13.5, color: '#0f172a', outline: 'none', boxShadow: '0 1px 2px rgba(15,23,42,0.04)', transition: 'border-color 0.2s, box-shadow 0.2s' }}
            onFocus={e => { e.target.style.borderColor = '#2563eb'; e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,0.1)' }}
            onBlur={e => { e.target.style.borderColor = '#cbd5e1'; e.target.style.boxShadow = '0 1px 2px rgba(15,23,42,0.04)' }}
          />
        </div>
        <div className="filter-tabs" style={{ display: 'flex', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: 10, padding: 3 }}>
          {[
            { key: 'ALL', label: 'All Reports' },
            { key: 'FAVORITE', label: 'Favorites' },
            { key: 'CRITICAL', label: 'Critical' },
          ].map(({ key, label }) => (
            <button key={key} onClick={() => setFilter(key)} style={{ padding: '6px 12px', borderRadius: 8, fontSize: 12.5, fontWeight: 600, border: filter === key ? '1px solid #cbd5e1' : '1px solid transparent', cursor: 'pointer', transition: 'all 0.15s ease',
              background: filter === key ? '#ffffff' : 'transparent', color: filter === key ? '#0f172a' : '#64748b',
              boxShadow: filter === key ? '0 1px 3px rgba(15,23,42,0.08)' : 'none', display: 'flex', alignItems: 'center', gap: 5, whiteSpace: 'nowrap' }}>
              {key === 'FAVORITE' && <span className="material-symbols-outlined" style={{ fontSize: 14, color: filter === key ? '#f59e0b' : 'inherit', fontVariationSettings: filter === key ? "'FILL' 1" : "'FILL' 0" }}>star</span>}
              {key === 'CRITICAL' && <span className="material-symbols-outlined" style={{ fontSize: 14, color: filter === key ? '#dc2626' : 'inherit', fontVariationSettings: filter === key ? "'FILL' 1" : "'FILL' 0" }}>error</span>}
              {label}
            </button>
          ))}
        </div>
        <span style={{ marginLeft: 'auto', fontSize: 12.5, color: '#64748b', fontWeight: 600, whiteSpace: 'nowrap' }}>{filtered.length} report{filtered.length !== 1 ? 's' : ''}</span>
      </div>

      {/* ── Cards Grid ── */}
      {loading ? (
        <div style={{ display:'flex', justifyContent:'center', padding:80 }}><LoadingSpinner size="lg" /></div>
      ) : filtered.length === 0 ? (
        <div style={{ background:'var(--snt-surface)', border:'1px solid var(--snt-border)', borderRadius:16, padding:'64px 32px', textAlign:'center' }}>
          <div style={{ width:56, height:56, borderRadius:16, background:'var(--snt-surface-3)', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}>
            <span className="material-symbols-outlined" style={{ fontSize:28, color:'var(--snt-text-4)' }}>folder_special</span>
          </div>
          <p style={{ margin:'0 0 6px', fontSize:18, fontWeight:600, color:'var(--snt-text-1)' }}>No reports found</p>
          <p style={{ margin:'0 0 24px', fontSize:14, color:'var(--snt-text-4)' }}>{search || filter !== 'ALL' ? 'Try adjusting your filters.' : 'Run a scan and generate a report to see it here.'}</p>
          <button onClick={() => search || filter !== 'ALL' ? (setSearch(''), setFilter('ALL')) : navigate('/scanner')} style={{ height:40, padding:'0 24px', background:'#2563eb', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>
            {search || filter !== 'ALL' ? 'Clear Filters' : 'Start Scanning'}
          </button>
        </div>
      ) : (
        <div className="reports-grid">
          {filtered.map(r => {
            const isFav = favs.includes(r.id)
            const score = r.securityScore
            return (
              <div key={r.id} className="rp-card" style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 16,
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                boxShadow: '0 1px 3px rgba(15,23,42,0.04)',
                transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
              }}
              onMouseEnter={e => { e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)'; e.currentTarget.style.borderColor = '#93c5fd' }}
              onMouseLeave={e => { e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)'; e.currentTarget.style.borderColor = '#e2e8f0' }}
              >
                <div style={{ padding:'20px', flex:1, display:'flex', flexDirection:'column', gap:16 }}>
                  {/* Top row */}
                  <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:8 }}>
                    <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                      <div style={{ width:40, height:40, borderRadius:10, background:'#eff6ff', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                        <span className="material-symbols-outlined" style={{ fontSize:20, color:'#2563eb', fontVariationSettings:"'FILL' 1" }}>description</span>
                      </div>
                      <div style={{ minWidth:0 }}>
                        <p style={{ margin:0, fontSize:15, fontWeight:600, color:'var(--snt-text-1)', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', letterSpacing:'-0.01em' }}>{r.reportName || `Report #${r.id}`}</p>
                        <p style={{ margin:'2px 0 0', fontSize:12, color:'var(--snt-text-3)', fontWeight:500 }}>{r.projectName || 'Unknown Project'}</p>
                      </div>
                    </div>
                    <button onClick={() => toggleFav(r.id)} title={isFav ? 'Remove from favorites' : 'Add to favorites'} style={{ border:'none', background:'none', cursor:'pointer', padding:4, borderRadius:6, flexShrink:0, display:'flex', alignItems:'center', justifyContent:'center' }}>
                      <span className="material-symbols-outlined" style={{ fontSize:20, color: isFav ? '#f59e0b' : '#d1d5db', fontVariationSettings: isFav ? "'FILL' 1" : "'FILL' 0", transition:'color 0.2s' }}>star</span>
                    </button>
                  </div>

                  {/* Score + Risk row */}
                  <div style={{ display:'flex', alignItems:'center', gap:12, background:'var(--snt-surface-2)', border:'1px solid #f3f4f6', borderRadius:12, padding:'12px 16px' }}>
                    {score != null ? (
                      <>
                        <div style={{ width:40, height:40, borderRadius:10, background:scoreBg(score), border:`1px solid ${score>=80?'#bbf7d0':score>=60?'#fde68a':'#fecaca'}`, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                          <span style={{ fontSize:15, fontWeight:700, color:scoreColor(score), letterSpacing:'-0.02em' }}>{score.toFixed(0)}</span>
                        </div>
                        <div>
                          <p style={{ margin:0, fontSize:13, fontWeight:600, color:scoreColor(score) }}>{riskLabel(score)}</p>
                          <p style={{ margin:0, fontSize:11, color:'var(--snt-text-4)', fontWeight:500 }}>Security Score</p>
                        </div>
                      </>
                    ) : (
                      <span style={{ fontSize:12, color:'var(--snt-text-4)', fontWeight:500 }}>No score available</span>
                    )}
                    <div style={{ marginLeft:'auto', textAlign:'right' }}>
                      <p style={{ margin:0, fontSize:12, color:'var(--snt-text-3)', fontWeight:500 }}>{fmtDate(r.generatedAt)}</p>
                      <p style={{ margin:'2px 0 0', fontSize:11, color:'var(--snt-text-4)', fontWeight:500 }}>{fmtSize(r.reportSizeBytes)}</p>
                    </div>
                  </div>

                  {/* Actions */}
                  <div style={{ display:'flex', gap:8, marginTop:'auto' }}>
                    <button onClick={() => navigate(`/reports/${r.scanHistoryId}`)} title="View Report" className="rp-act primary">
                      <span className="material-symbols-outlined" style={{ fontSize:16 }}>visibility</span>
                      View
                    </button>
                    <button onClick={() => handleDownload(r)} title="Download PDF" className="rp-act">
                      <span className="material-symbols-outlined" style={{ fontSize:16 }}>download</span>
                      PDF
                    </button>
                    <button onClick={() => handleAiExplain(r)} title="AI Explain" className="rp-act ai">
                      <span className="material-symbols-outlined" style={{ fontSize:16 }}>psychology</span>
                      Explain
                    </button>
                    <button onClick={() => handleDelete(r.id)} disabled={deleting === r.id} title="Delete" className="rp-act" style={{ flex:'none', width:36, padding:0, color:'#ef4444' }}>
                      {deleting === r.id ? <LoadingSpinner size="sm" /> : <span className="material-symbols-outlined" style={{ fontSize:16 }}>delete</span>}
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {compare && <CompareModal reports={reports} onClose={() => setCompare(false)} />}

      <ConfirmDialog
        open={!!confirmState}
        title="Delete Report"
        message="This report will be permanently deleted. This action cannot be undone."
        confirmLabel="Delete Report"
        variant="danger"
        loading={deleting === confirmState?.id}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setConfirmState(null)}
      />
    </div>
  )
}
