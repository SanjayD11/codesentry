import { useState, useEffect, useCallback, useMemo } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { getAllUserScans, triggerScan, deleteScan } from '../../api/scanApi'
import { generateReport } from '../../api/reportApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import ConfirmDialog from '../../components/ui/ConfirmDialog'

const fmt = (iso) => iso ? new Date(iso).toLocaleString('en-US', { month:'short', day:'numeric', year:'numeric', hour:'2-digit', minute:'2-digit' }) : '—'
const dur = (s, e) => { if (!s||!e) return '—'; const sec = Math.round((new Date(e)-new Date(s))/1000); return sec<60?`${sec}s`:`${Math.floor(sec/60)}m ${sec%60}s` }
const scoreClr = s => s>=80?'#16a34a':s>=60?'#f59e0b':'#dc2626'
const riskLbl  = s => s>=80?'Low':s>=60?'Medium':s>=40?'High':'Critical'

const ST = {
  COMPLETED:  { bg:'#f0fdf4', clr:'#16a34a', bdr:'#bbf7d0', icon:'check_circle',  lbl:'Completed' },
  RUNNING:    { bg:'#eff6ff', clr:'#2563eb', bdr:'#bfdbfe', icon:'sync',           lbl:'Running' },
  PENDING:    { bg:'#fffbeb', clr:'#d97706', bdr:'#fde68a', icon:'schedule',       lbl:'Pending' },
  FAILED:     { bg:'#fef2f2', clr:'#dc2626', bdr:'#fecaca', icon:'error',          lbl:'Failed' },
  CANCELLED:  { bg:'#f9fafb', clr:'#6b7280', bdr:'#e5e7eb', icon:'cancel',         lbl:'Cancelled' },
  IN_PROGRESS:{ bg:'#eff6ff', clr:'#2563eb', bdr:'#bfdbfe', icon:'sync',           lbl:'In Progress' },
}

function Badge({ status }) {
  const c = ST[status]||ST.PENDING
  return <span style={{ display:'inline-flex', alignItems:'center', gap:4, padding:'4px 10px', borderRadius:99, fontSize:12, fontWeight:600, background:c.bg, color:c.clr, border:`1px solid ${c.bdr}` }}>
    <span className="material-symbols-outlined" style={{ fontSize:13, fontVariationSettings:"'FILL' 1" }}>{c.icon}</span>{c.lbl}
  </span>
}

/* ── Detail Modal ── */
function DetailModal({ scan, onClose, onGen, genPdf, navigate, onDelete }) {
  if (!scan) return null
  const vulns = scan.vulnerabilities||[]
  const sev = s => vulns.filter(v=>v.severity===s).length
  return createPortal(
    <div onClick={onClose} style={{ position:'fixed', inset:0, background:'rgba(15,23,42,0.45)', zIndex:9999, display:'flex', alignItems:'center', justifyContent:'center', padding:24 }}>
      <div onClick={e=>e.stopPropagation()} style={{ background:'#ffffff', borderRadius:16, width:'100%', maxWidth:640, maxHeight:'90vh', display:'flex', flexDirection:'column', boxShadow:'0 20px 40px rgba(15,23,42,0.15)', border:'1px solid #cbd5e1' }}>
        {/* Header - Fixed */}
        <div style={{ padding:'20px 24px', borderBottom:'1px solid #e2e8f0', display:'flex', alignItems:'center', justifyContent:'space-between', flexShrink:0 }}>
          <div style={{ display:'flex', alignItems:'center', gap:12 }}>
            <div style={{ width:40, height:40, borderRadius:12, background:'#eff6ff', border:'1px solid #bfdbfe', display:'flex', alignItems:'center', justifyContent:'center' }}>
              <span className="material-symbols-outlined" style={{ fontSize:20, color:'#2563eb', fontVariationSettings:"'FILL' 1" }}>shield</span>
            </div>
            <div>
              <p style={{ margin:0, fontSize:17, fontWeight:700, color:'#0f172a', letterSpacing:'-0.01em' }}>Scan #{scan.scanId}</p>
              <p style={{ margin:0, fontSize:13, color:'#64748b' }}>{scan.scanType === 'QUICK_SCAN' ? `Quick Scan: ${scan.snippetFilename || scan.snippetLanguage || 'Snippet'}` : `Project #${scan.projectId}`}</p>
            </div>
          </div>
          <button onClick={onClose} style={{ width:32, height:32, border:'1px solid #cbd5e1', borderRadius:8, background:'#ffffff', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
            <span className="material-symbols-outlined" style={{ fontSize:18, color:'#64748b' }}>close</span>
          </button>
        </div>
        
        {/* Body - Scrollable */}
        <div style={{ padding:24, display:'flex', flexDirection:'column', gap:24, overflowY:'auto' }}>
          <div className="sh-detail-grid">
            {[
              { l:'Status', v:<Badge status={scan.status}/> },
              { l:'Score',  v:(scan.status==='COMPLETED' && scan.securityScore!=null)?<span style={{ fontSize:22, fontWeight:800, color:scoreClr(scan.securityScore), letterSpacing:'-0.02em' }}>{scan.securityScore.toFixed(0)}</span>:'—' },
              { l:'Findings', v:<span style={{ fontSize:20, fontWeight:700, color:'#0f172a' }}>{scan.status==='COMPLETED'?scan.totalVulnerabilities??'—':'—'}</span> },
              { l:'Duration', v:<span style={{ fontSize:14, fontWeight:600, color:'#0f172a' }}>{dur(scan.scanStart,scan.scanEnd)}</span> },
            ].map(({l,v})=>(
              <div key={l} style={{ background:'#f8fafc', border:'1px solid #e2e8f0', borderRadius:12, padding:'14px 16px' }}>
                <p style={{ margin:'0 0 6px', fontSize:11, fontWeight:600, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.06em' }}>{l}</p>
                <div>{v}</div>
              </div>
            ))}
          </div>
          <div>
            <p style={{ margin:'0 0 12px', fontSize:13, fontWeight:600, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.05em' }}>Timeline</p>
            <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
              {[{ icon:'play_circle', l:'Started', v:fmt(scan.scanStart), c:'#2563eb' },{ icon:'check_circle', l:'Completed', v:fmt(scan.scanEnd), c:'#16a34a' }].map(r=>(
                <div key={r.l} style={{ display:'flex', alignItems:'center', gap:12, padding:'12px 16px', background:'#f8fafc', borderRadius:10, border:'1px solid #e2e8f0' }}>
                  <span className="material-symbols-outlined" style={{ fontSize:18, color:r.c, fontVariationSettings:"'FILL' 1" }}>{r.icon}</span>
                  <span style={{ fontSize:13, color:'#475569', fontWeight:500, flex:1 }}>{r.l}</span>
                  <span style={{ fontSize:13, color:'#0f172a', fontFamily:"'JetBrains Mono',monospace", fontWeight:500 }}>{r.v}</span>
                </div>
              ))}
            </div>
          </div>
          {scan.status==='COMPLETED' && (
            <div>
              <p style={{ margin:'0 0 12px', fontSize:13, fontWeight:600, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.05em' }}>Severity Distribution</p>
              <div className="sh-sev-grid">
                {[{l:'Critical',s:'CRITICAL',bg:'#fef2f2',c:'#dc2626'},{l:'High',s:'HIGH',bg:'#fff7ed',c:'#ea580c'},{l:'Medium',s:'MEDIUM',bg:'#fffbeb',c:'#d97706'},{l:'Low',s:'LOW',bg:'#f0fdf4',c:'#16a34a'}].map(({l,s,bg,c})=>(
                  <div key={s} style={{ background:bg, borderRadius:10, padding:'12px', textAlign:'center', border:`1px solid ${c}30` }}>
                    <p style={{ margin:0, fontSize:24, fontWeight:800, color:c, letterSpacing:'-0.02em' }}>{sev(s)}</p>
                    <p style={{ margin:'2px 0 0', fontSize:11, color:c, fontWeight:600, opacity:0.8 }}>{l}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer - Fixed */}
        <div className="sh-modal-footer" style={{ padding:'16px 24px', borderTop:'1px solid #e2e8f0', background: '#fafbfc', borderBottomLeftRadius: 16, borderBottomRightRadius: 16, flexShrink:0 }}>
          <button onClick={()=>onDelete(scan.scanId)} style={{ display:'flex', alignItems:'center', gap:6, height:40, padding:'0 20px', background:'#fef2f2', color:'#dc2626', border:'1px solid #fecaca', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>
            <span className="material-symbols-outlined" style={{ fontSize:16 }}>delete</span>
            Delete Scan
          </button>
          <div className="sh-modal-actions-right">
            {scan.status==='COMPLETED' && (
              <>
                <button onClick={()=>navigate(`/reports/${scan.scanId}`)} style={{ display:'flex', alignItems:'center', gap:6, height:40, padding:'0 20px', background:'#eff6ff', color:'#2563eb', border:'1px solid #bfdbfe', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>
                  <span className="material-symbols-outlined" style={{ fontSize:16 }}>analytics</span>
                  View Report
                </button>
                <button onClick={()=>onGen(scan)} disabled={genPdf===scan.scanId} style={{ display:'flex', alignItems:'center', gap:6, height:40, padding:'0 20px', background:'#2563eb', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', opacity:genPdf===scan.scanId?0.6:1 }}>
                  <span className="material-symbols-outlined" style={{ fontSize:16 }}>picture_as_pdf</span>
                  {genPdf===scan.scanId?'Generating…':'Download PDF'}
                </button>
              </>
            )}
            <button onClick={onClose} style={{ height:40, padding:'0 20px', background:'#ffffff', color:'#0f172a', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>Close</button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  )
}

const FILTERS = ['ALL','COMPLETED','RUNNING','FAILED','CANCELLED','PENDING']

export default function ScanHistory() {
  const [scans, setScans] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [genPdf, setGenPdf] = useState(null)
  const [scanning, setScanning] = useState(null)
  const [detail, setDetail] = useState(null)
  const [confirmState, setConfirmState] = useState(null) // { type: 'rescan'|'delete', scan, scanId }
  const addToast = useToast()
  const navigate = useNavigate()

  const fetchScans = useCallback(async () => {
    setLoading(true)
    try { const res = await getAllUserScans(); setScans(res.data.data||[]) }
    catch { addToast('Failed to load scan history','error') }
    finally { setLoading(false) }
  }, [addToast])

  useEffect(() => { fetchScans() }, [fetchScans])

  const handleGenReport = async (scan) => {
    setGenPdf(scan.scanId)
    try {
      const res = await generateReport(scan.scanId)
      const reportId = res.data.data?.id
      addToast('Report generated!','success'); setDetail(null)
      if (reportId) navigate(`/reports/${reportId}`); else navigate('/reports')
    } catch { addToast('Failed to generate report','error') }
    finally { setGenPdf(null) }
  }

  const handleScanAgain = (scan) => {
    setConfirmState({ type: 'rescan', scan })
  }

  const handleScanAgainConfirm = async () => {
    const scan = confirmState?.scan
    setConfirmState(null)
    setScanning(scan.scanId)
    try { await triggerScan(scan.projectId); addToast('New scan started!', 'success'); fetchScans() }
    catch (err) { addToast(err?.response?.data?.message || 'Failed to start scan', 'error') }
    finally { setScanning(null) }
  }

  const handleDeleteScan = (scanId) => {
    setConfirmState({ type: 'delete', scanId })
  }

  const handleDeleteScanConfirm = async () => {
    const scanId = confirmState?.scanId
    setConfirmState(null)
    try {
      await deleteScan(scanId)
      addToast('Scan deleted successfully!', 'success')
      if (detail && detail.scanId === scanId) setDetail(null)
      fetchScans()
    } catch {
      addToast('Failed to delete scan.', 'error')
    }
  }

  const filtered = useMemo(() => scans.filter(s => {
    const ms = filter==='ALL'||s.status===filter
    const mq = !search||s.scanId?.toString().includes(search)||s.projectId?.toString().includes(search)||s.status?.toLowerCase().includes(search.toLowerCase())
    return ms&&mq
  }), [scans, filter, search])

  const stats = useMemo(() => ({
    total: scans.length,
    completed: scans.filter(s=>s.status==='COMPLETED').length,
    failed: scans.filter(s=>s.status==='FAILED').length,
    running: scans.filter(s=>['RUNNING','IN_PROGRESS','PENDING'].includes(s.status)).length,
    avgScore: (()=>{ const d=scans.filter(s=>s.status==='COMPLETED'&&s.securityScore!=null); return d.length?(d.reduce((a,s)=>a+s.securityScore,0)/d.length).toFixed(0):'—' })()
  }), [scans])

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif" }}>
      <style>{`
        .sh-row { transition: background 0.15s ease; }
        .sh-row:hover { background: #f9fbff !important; }
        .sh-act { width:32px; height:32px; border:1px solid #e5e7eb; border-radius:8px; background:#fff; color:#9ca3af; cursor:pointer; display:flex; align-items:center; justify-content:center; transition: all 0.2s ease; }
        .sh-act:hover { border-color:#2563eb; color:#2563eb; background:#eff6ff; }
        .sh-act:disabled { opacity:0.4; cursor:not-allowed; transform:none; }
        @keyframes spin { to { transform:rotate(360deg) } }
        /* Desktop */
        .sh-stats-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 24px; }
        .sh-kpi-num { font-size: 28px; }
        /* Tablet */
        @media (min-width: 768px) and (max-width: 1279px) { .sh-stats-grid { grid-template-columns: repeat(3, 1fr) !important; } }
        /* Mobile */
        @media (max-width: 640px) {
          .sh-stats-grid { grid-template-columns: repeat(2, 1fr) !important; gap: 10px !important; }
          .sh-kpi-num { font-size: 24px !important; }
          .sh-stat-card { border-radius: 12px !important; padding: 14px 16px !important; border-color: #f1f5f9 !important; box-shadow: 0 1px 2px rgba(15,23,42,0.03) !important; }
          .sh-stat-card.sh-stat-last { grid-column: span 2; display: flex; flex-direction: row; justify-content: space-between; align-items: center; }
          .sh-stat-card.sh-stat-last .sh-stat-left { margin-bottom: 0 !important; gap: 12px !important; }
          .sh-stat-card.sh-stat-last .sh-kpi-num { font-size: 16px !important; margin: 0 !important; }
          
          .sh-hero-mobile { border: none !important; box-shadow: none !important; background: transparent !important; padding: 0 !important; margin-bottom: 24px !important; }
          .sh-hero-inner { flex-direction: column !important; gap: 16px !important; }
          .sh-hero-left { gap: 14px !important; }
          .sh-hero-icon { display: none !important; }
          .sh-hero-title { font-size: 28px !important; line-height: 1.15 !important; }
          .sh-subtitle { display: block !important; margin-bottom: 16px !important; }
          
          .sh-hero-btns { flex-direction: row !important; width: 100% !important; gap: 10px !important; }
          .sh-hero-btns button { flex: 1 !important; justify-content: center !important; }
        }
        
        .sh-detail-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
        @media (max-width: 600px) { .sh-detail-grid { grid-template-columns: repeat(2, 1fr) !important; } }
        .sh-sev-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
        @media (max-width: 600px) { .sh-sev-grid { grid-template-columns: repeat(2, 1fr) !important; } }
        
        /* Modal Footer Responsive */
        .sh-modal-footer { display: flex; gap: 8px; justify-content: space-between; }
        .sh-modal-actions-right { display: flex; gap: 8px; }
        @media (max-width: 640px) {
          .sh-modal-footer { flex-direction: column-reverse; }
          .sh-modal-actions-right { flex-direction: column; width: 100%; }
          .sh-modal-footer button { width: 100%; justify-content: center; }
        }
      `}</style>

      {/* ── Header ── */}
      <div className="hero-card sh-hero-mobile" style={{ marginBottom: 24 }}>
        <div className="sh-hero-inner" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
          <div className="sh-hero-left" style={{ display: 'flex', alignItems: 'center', gap: 14, minWidth: 0, flex: 1 }}>
            <div className="sh-hero-icon" style={{ width: 40, height: 40, borderRadius: 10, background: '#eff6ff', border: '1px solid #bfdbfe', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#2563eb', fontVariationSettings: "'FILL' 1" }}>manage_history</span>
            </div>
            <div style={{ minWidth: 0 }}>
              <p style={{ margin: '0 0 1px', fontSize: 11, fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Operations</p>
              <h1 className="sh-hero-title" style={{ margin: 0, fontSize: 'clamp(18px, 4vw, 22px)', fontWeight: 700, color: '#0f172a', letterSpacing: '-0.025em' }}>Scan History</h1>
              <p className="sh-subtitle" style={{ margin: '3px 0 0', fontSize: 13, color: '#64748b', fontWeight: 400, display: 'none' }}>Complete history of all security scans</p>
            </div>
          </div>
          <div className="hero-actions sh-hero-btns sh-hero-actions" style={{ flexShrink: 0 }}>
            <button onClick={fetchScans}
              className="btn-responsive"
              style={{ height: 40, padding: '0 16px', background: '#ffffff', color: '#0f172a', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, boxShadow: '0 1px 2px rgba(15,23,42,0.04)', transition: 'all 0.2s ease' }}
              onMouseEnter={e => { e.currentTarget.style.borderColor = '#94a3b8'; e.currentTarget.style.background = '#f8fafc' }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.background = '#ffffff' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>refresh</span>Refresh
            </button>
            <button onClick={() => navigate('/scanner')}
              className="btn-responsive"
              style={{ height: 40, padding: '0 20px', background: '#0058be', color: '#fff', border: '1px solid #0058be', borderRadius: 10, fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, boxShadow: '0 2px 6px rgba(0,88,190,0.2)', transition: 'all 0.2s ease' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>add</span>New Scan
            </button>
          </div>
        </div>
      </div>

      {/* ── Stats Row ── */}
      <div className="sh-stats-grid">
        {[
          { l:'Total Scans', v:stats.total, icon:'shield', c:'#2563eb', bg:'#eff6ff' },
          { l:'Completed',   v:stats.completed, icon:'check_circle', c:'#16a34a', bg:'#f0fdf4' },
          { l:'Failed',      v:stats.failed, icon:'error', c:'#dc2626', bg:'#fef2f2' },
          { l:'Active',      v:stats.running, icon:'sync', c:'#d97706', bg:'#fffbeb' },
          { l:'Avg Score',   v:stats.avgScore, icon:'speed', c:'#7c3aed', bg:'#f5f3ff' },
        ].map((s, i) => (
          <div key={s.l} className={`sh-stat-card ${i === 4 ? 'sh-stat-last' : ''}`} style={{ background:'#ffffff', border:'1px solid #cbd5e1', borderRadius:14, padding:'16px', boxShadow: '0 1px 4px rgba(17,28,45,0.04)', transition:'border-color 0.2s ease, box-shadow 0.2s ease' }}
            onMouseEnter={e=>{e.currentTarget.style.borderColor='#93c5fd';e.currentTarget.style.boxShadow='0 4px 16px rgba(37,99,235,0.1)'}}
            onMouseLeave={e=>{e.currentTarget.style.borderColor='#cbd5e1';e.currentTarget.style.boxShadow='0 1px 4px rgba(17,28,45,0.04)'}}
          >
            <div className="sh-stat-left" style={{ display:'flex', alignItems:'center', gap:8, marginBottom:12 }}>
              <div style={{ width:24, height:24, borderRadius:6, background:s.bg, display:'flex', alignItems:'center', justifyContent:'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize:14, color:s.c, fontVariationSettings:"'FILL' 1" }}>{s.icon}</span>
              </div>
              <span style={{ fontSize:12, color:'#64748b', fontWeight:600 }}>{s.l}</span>
            </div>
            <div className="sh-kpi-num" style={{ fontWeight:700, color:'#0f172a', letterSpacing:'-0.02em', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif" }}>
              {s.l === 'Avg Score' ? `${s.v} / 100` : s.v}
            </div>
          </div>
        ))}
      </div>

      {/* ── Filter Bar ── */}
      <div className="filter-bar">
        <div style={{ position: 'relative', flex: '1 1 200px', maxWidth: 260 }}>
          <span className="material-symbols-outlined" style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', fontSize: 16, color: '#64748b' }}>search</span>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search scans…"
            style={{ paddingLeft: 36, paddingRight: 16, height: 40, width: '100%', background: '#ffffff', border: '1px solid #cbd5e1', borderRadius: 10, fontSize: 13.5, color: '#0f172a', outline: 'none', boxShadow: '0 1px 2px rgba(15,23,42,0.04)', transition: 'border-color 0.2s, box-shadow 0.2s' }}
            onFocus={e => { e.target.style.borderColor = '#2563eb'; e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,0.1)' }}
            onBlur={e => { e.target.style.borderColor = '#cbd5e1'; e.target.style.boxShadow = '0 1px 2px rgba(15,23,42,0.04)' }} />
        </div>
        <div className="filter-tabs" style={{ display: 'flex', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: 10, padding: 3 }}>
          {FILTERS.map(f => (
            <button key={f} onClick={() => setFilter(f)} style={{ padding: '6px 12px', borderRadius: 8, fontSize: 12.5, fontWeight: 600, border: filter === f ? '1px solid #cbd5e1' : '1px solid transparent', cursor: 'pointer', transition: 'all 0.15s ease',
              background: filter === f ? '#ffffff' : 'transparent', color: filter === f ? '#0f172a' : '#64748b',
              boxShadow: filter === f ? '0 1px 3px rgba(15,23,42,0.08)' : 'none', whiteSpace: 'nowrap' }}>
              {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        <span style={{ marginLeft: 'auto', fontSize: 12.5, color: '#64748b', fontWeight: 600, whiteSpace: 'nowrap' }}>{filtered.length} scan{filtered.length !== 1 ? 's' : ''}</span>
      </div>

      {/* ── Table ── */}
      {loading ? (
        <div style={{ display:'flex', justifyContent:'center', padding:80 }}><LoadingSpinner size="lg"/></div>
      ) : filtered.length===0 ? (
        <div style={{ background:'#ffffff', border:'1px solid #cbd5e1', borderRadius:16, padding:'64px 32px', textAlign:'center', boxShadow:'0 1px 3px rgba(0,0,0,0.04)' }}>
          <div style={{ width:56, height:56, borderRadius:16, background:'#eff6ff', border:'1px solid #bfdbfe', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}>
            <span className="material-symbols-outlined" style={{ fontSize:28, color:'#2563eb' }}>manage_history</span>
          </div>
          <p style={{ margin:'0 0 6px', fontSize:18, fontWeight:600, color:'#0f172a' }}>No scans found</p>
          <p style={{ margin:'0 0 24px', fontSize:14, color:'#64748b' }}>{search||filter!=='ALL'?'Try adjusting your filters.':'Run your first scan to see it here.'}</p>
          <button onClick={()=>search||filter!=='ALL'?(setSearch(''),setFilter('ALL')):navigate('/scanner')}
            style={{ height:40, padding:'0 24px', background:'#2563eb', color:'#fff', border:'1px solid #1d4ed8', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', boxShadow:'0 2px 6px rgba(37,99,235,0.2)' }}>
            {search||filter!=='ALL'?'Clear Filters':'Start Scanning'}
          </button>
        </div>
      ) : (
        <div style={{ background:'#ffffff', border:'1px solid #e2e8f0', borderRadius:16, overflow:'hidden', boxShadow: '0 1px 3px rgba(15,23,42,0.04)' }}>
          <div className="table-scroll-wrap">
            <div className="table-scroll-inner">
              <table style={{ width:'100%', borderCollapse:'collapse' }}>
                <thead>
                  <tr style={{ borderBottom:'1px solid #cbd5e1', background:'#f8fafc' }}>
                    {['Scan','Status','Timeline','Score','Findings','Actions'].map((h,i)=>(
                      <th key={i} style={{ padding:'14px 20px', textAlign:i===5?'right':'left', fontSize:11, fontWeight:700, color:'#475569', textTransform:'uppercase', letterSpacing:'0.06em', background:'#f8fafc' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(s=>(
                    <tr key={s.scanId} className="sh-row" style={{ borderBottom:'1px solid #e2e8f0', cursor:'pointer', transition: 'background 0.15s ease' }} onClick={()=>setDetail(s)} onMouseEnter={e=>e.currentTarget.style.background='#f8fafc'} onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
                      <td style={{ padding:'16px 20px' }}>
                        <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                          <div style={{ width:36, height:36, borderRadius:10, background:'#eff6ff', border:'1px solid #bfdbfe', display:'flex', alignItems:'center', justifyContent:'center' }}>
                            <span className="material-symbols-outlined" style={{ fontSize:18, color:'#2563eb' }}>shield</span>
                          </div>
                          <div>
                            <p style={{ margin:0, fontSize:14, fontWeight:600, color:'var(--snt-text-1)' }}>Scan #{s.scanId}</p>
                            <p style={{ margin:0, fontSize:12, color:'var(--snt-text-4)' }}>{s.scanType === 'QUICK_SCAN' ? 'Quick Scan' : `Project #${s.projectId}`}</p>
                          </div>
                        </div>
                      </td>
                      <td style={{ padding:'16px 20px' }} onClick={e=>e.stopPropagation()}><Badge status={s.status}/></td>
                      <td style={{ padding:'16px 20px' }}>
                        <p style={{ margin:0, fontSize:13, color:'var(--snt-text-2)' }}>{fmt(s.scanStart)}</p>
                        <p style={{ margin:'2px 0 0', fontSize:12, color:'var(--snt-text-4)' }}>{dur(s.scanStart,s.scanEnd)}</p>
                      </td>
                      <td style={{ padding:'16px 20px' }}>
                        {s.status==='COMPLETED'&&s.securityScore!=null ? (
                          <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                            <span style={{ fontSize:16, fontWeight:700, color:scoreClr(s.securityScore), letterSpacing:'-0.02em' }}>{s.securityScore.toFixed(0)}</span>
                            <span style={{ fontSize:11, fontWeight:600, color:scoreClr(s.securityScore), opacity:0.7 }}>{riskLbl(s.securityScore)}</span>
                          </div>
                        ):<span style={{ color:'#d1d5db' }}>—</span>}
                      </td>
                      <td style={{ padding:'16px 20px' }}>
                        {s.status==='COMPLETED'?<span style={{ fontSize:15, fontWeight:700, color:'var(--snt-text-1)' }}>{s.totalVulnerabilities??0}</span>:<span style={{ color:'#d1d5db' }}>—</span>}
                      </td>
                      <td style={{ padding:'16px 20px' }} onClick={e=>e.stopPropagation()}>
                        <div style={{ display:'flex', alignItems:'center', justifyContent:'flex-end', gap:4 }}>
                          <button className="sh-act" title="View Details" onClick={()=>setDetail(s)}>
                            <span className="material-symbols-outlined" style={{ fontSize:16 }}>open_in_new</span>
                          </button>
                          {s.status==='COMPLETED'&&(
                            <button className="sh-act" title="Generate Report" onClick={()=>handleGenReport(s)} disabled={genPdf===s.scanId}>
                              <span className="material-symbols-outlined" style={{ fontSize:16 }}>{genPdf===s.scanId?'hourglass_empty':'picture_as_pdf'}</span>
                            </button>
                          )}
                          <button className="sh-act" title="Scan Again" onClick={()=>handleScanAgain(s)} disabled={scanning===s.scanId}>
                            <span className="material-symbols-outlined" style={{ fontSize:16, ...(scanning===s.scanId?{animation:'spin 1s linear infinite'}:{}) }}>replay</span>
                          </button>
                          {s.status==='COMPLETED'&&(
                            <button className="sh-act" title="View Report" onClick={(e)=>{e.stopPropagation(); navigate(`/reports/${s.scanId}`)}}>
                              <span className="material-symbols-outlined" style={{ fontSize:16 }}>description</span>
                            </button>
                          )}
                          <button className="sh-act" style={{ color: '#dc2626' }} title="Delete Scan" onClick={(e)=>{e.stopPropagation(); handleDeleteScan(s.scanId)}}>
                            <span className="material-symbols-outlined" style={{ fontSize:16 }}>delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {detail && <DetailModal scan={detail} onClose={()=>setDetail(null)} onGen={handleGenReport} genPdf={genPdf} navigate={navigate} onDelete={handleDeleteScan} />}

      {/* Delete scan confirm */}
      <ConfirmDialog
        open={confirmState?.type === 'delete'}
        title="Delete Scan"
        message={`Scan #${confirmState?.scanId} will be permanently deleted. This action cannot be undone.`}
        confirmLabel="Delete Scan"
        variant="danger"
        onConfirm={handleDeleteScanConfirm}
        onCancel={() => setConfirmState(null)}
      />

      {/* Re-scan confirm */}
      <ConfirmDialog
        open={confirmState?.type === 'rescan'}
        title="Start New Scan"
        message={`Start a new scan for Project #${confirmState?.scan?.projectId}? This will not affect existing scan history.`}
        confirmLabel="Start Scan"
        variant="info"
        onConfirm={handleScanAgainConfirm}
        onCancel={() => setConfirmState(null)}
      />
    </div>
  )
}
