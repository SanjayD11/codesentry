import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { getProject } from '../../api/projectApi'
import { getProjectScans, triggerScan } from '../../api/scanApi'
import { getProjectReports, generateReport } from '../../api/reportApi'
import { getProjectFiles } from '../../api/uploadApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

const fmtDate = (iso) => iso ? new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'
const scoreColor = (s) => s >= 80 ? '#16a34a' : s >= 60 ? '#f59e0b' : '#dc2626'
const scoreBg    = (s) => s >= 80 ? '#f0fdf4' : s >= 60 ? '#fffbeb' : '#fef2f2'

export default function ProjectDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const addToast = useToast()

  const [project, setProject] = useState(null)
  const [scans, setScans] = useState([])
  const [reports, setReports] = useState([])
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(true)
  const [scanning, setScanning] = useState(false)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const [projRes, scanRes, repRes, fileRes] = await Promise.all([
        getProject(id),
        getProjectScans(id).catch(() => ({ data: { data: [] } })),
        getProjectReports(id).catch(() => ({ data: { data: [] } })),
        getProjectFiles(id).catch(() => ({ data: { data: { content: [] } } }))
      ])
      setProject(projRes.data.data)
      setScans(scanRes.data.data || [])
      setReports(repRes.data.data || [])
      setFiles(fileRes.data.data?.content || [])
    } catch (err) {
      addToast('Failed to load project details', 'error')
      navigate('/projects')
    } finally {
      setLoading(false)
    }
  }, [id, navigate, addToast])

  useEffect(() => { fetchData() }, [fetchData])

  const handleStartScan = async () => {
    setScanning(true)
    try {
      await triggerScan(id)
      addToast('Scan triggered successfully. It is running in the background.', 'success')
      fetchData() // Refresh to show running status if applicable
    } catch {
      addToast('Failed to trigger scan', 'error')
    } finally {
      setScanning(false)
    }
  }

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><LoadingSpinner size="lg" /></div>
  if (!project) return null

  const hasScans = project.lastScanTime != null
  const s = hasScans ? (100 - (project.overallRiskScore || 0)) : null
  const totalFindings = project.totalVulnerabilities || 0

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", paddingBottom: 64 }}>
      <style>{`
        .pjd-card { background:#fff; border:1px solid #e5e7eb; border-radius:16px; overflow:hidden; }
        .pjd-head { padding:20px 24px; border-bottom:1px solid #f3f4f6; display:flex; align-items:center; justify-content:space-between; }
        .pjd-head h3 { margin:0; font-size:16px; font-weight:700; color:#111827; letter-spacing:-0.01em; }
        .pjd-row { padding:16px 24px; border-bottom:1px solid #f3f4f6; display:flex; align-items:center; justify-content:space-between; font-size:14px; color:#374151; transition:background 0.15s; }
        .pjd-row:hover { background:#f9fafb; }
        .pjd-row:last-child { border-bottom:none; }
      `}</style>

      {/* ── Header ── */}
      <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', marginBottom:24, flexWrap:'wrap', gap:16 }}>
        <div>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:8 }}>
            <Link to="/projects" style={{ color:'var(--snt-text-3)', textDecoration:'none', fontSize:13, fontWeight:600, display:'flex', alignItems:'center', gap:4 }}><span className="material-symbols-outlined" style={{ fontSize:16 }}>arrow_back</span> Projects</Link>
            <span style={{ color:'#d1d5db' }}>/</span>
            <span style={{ fontSize:13, fontWeight:600, color:'#2563eb', background:'#eff6ff', padding:'2px 8px', borderRadius:6 }}>{project.projectType?.replace('_',' ')}</span>
          </div>
          <h1 style={{ margin:0, fontSize:28, fontWeight:800, color:'#0f172a', letterSpacing:'-0.03em' }}>{project.projectName}</h1>
          <p style={{ margin:'6px 0 0', fontSize:14, color:'#64748b', maxWidth:600 }}>{project.description || 'No description provided.'}</p>
        </div>
        <div style={{ display:'flex', gap:12 }}>
          <button onClick={() => navigate(`/scanner?project=${id}`)} style={{ height:40, padding:'0 16px', background:'#ffffff', color:'#0f172a', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, boxShadow:'0 1px 2px rgba(15,23,42,0.04)', transition:'all 0.2s ease' }}
            onMouseEnter={e=>{e.currentTarget.style.borderColor='#94a3b8';e.currentTarget.style.background='#f8fafc'}}
            onMouseLeave={e=>{e.currentTarget.style.borderColor='#cbd5e1';e.currentTarget.style.background='#ffffff'}}>
            <span className="material-symbols-outlined" style={{ fontSize:16 }}>settings</span> Configure Scan
          </button>
          <button onClick={handleStartScan} disabled={scanning} style={{ height:40, padding:'0 24px', background:'#2563eb', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, boxShadow:'0 1px 2px rgba(37,99,235,0.2)', transition:'all 0.2s ease', opacity:scanning?0.7:1 }}
            onMouseEnter={e=>{if(!scanning){e.currentTarget.style.background='#1d4ed8'}}}
            onMouseLeave={e=>{if(!scanning){e.currentTarget.style.background='#2563eb'}}}>
            {scanning ? <LoadingSpinner size="sm" /> : <><span className="material-symbols-outlined" style={{ fontSize:16 }}>play_arrow</span> Start Scan</>}
          </button>
        </div>
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'2fr 1fr', gap:24, alignItems:'start' }}>
        
        {/* LEFT COL */}
        <div style={{ display:'flex', flexDirection:'column', gap:24 }}>
          
          {/* Quick Metrics */}
          <div style={{ display:'grid', gridTemplateColumns:'repeat(3, 1fr)', gap:16 }}>
            <div className="pjd-card" style={{ padding:24 }}>
              <p style={{ margin:0, fontSize:12, fontWeight:600, color:'var(--snt-text-3)', textTransform:'uppercase', letterSpacing:'0.05em', marginBottom:12 }}>Overall Risk Score</p>
              <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                <div style={{ width:48, height:48, borderRadius:12, background:hasScans ? scoreBg(s) : '#f3f4f6', border:hasScans ? `1px solid ${s>=80?'#bbf7d0':s>=60?'#fde68a':'#fecaca'}` : '1px solid #e5e7eb', display:'flex', alignItems:'center', justifyContent:'center' }}>
                  <span style={{ fontSize:18, fontWeight:700, color:hasScans ? scoreColor(s) : '#9ca3af' }}>{hasScans ? s.toFixed(0) : '—'}</span>
                </div>
                <div>
                  <p style={{ margin:0, fontSize:18, fontWeight:700, color:hasScans ? scoreColor(s) : '#6b7280' }}>{hasScans ? (s >= 80 ? 'Low Risk' : s >= 60 ? 'Medium Risk' : 'High Risk') : 'Not Scanned'}</p>
                  <p style={{ margin:'2px 0 0', fontSize:12, color:'var(--snt-text-4)' }}>{hasScans ? 'Based on recent scans' : 'Run a scan to get score'}</p>
                </div>
              </div>
            </div>
            <div className="pjd-card" style={{ padding:24 }}>
              <p style={{ margin:0, fontSize:12, fontWeight:600, color:'var(--snt-text-3)', textTransform:'uppercase', letterSpacing:'0.05em', marginBottom:12 }}>Total Findings</p>
              <div style={{ display:'flex', alignItems:'baseline', gap:8 }}>
                <span style={{ fontSize:32, fontWeight:800, color:'var(--snt-text-1)', letterSpacing:'-0.03em', lineHeight:1 }}>{totalFindings}</span>
                <span style={{ fontSize:13, color:'#ef4444', fontWeight:600 }}>issues</span>
              </div>
            </div>
            <div className="pjd-card" style={{ padding:24 }}>
              <p style={{ margin:0, fontSize:12, fontWeight:600, color:'var(--snt-text-3)', textTransform:'uppercase', letterSpacing:'0.05em', marginBottom:12 }}>Scans Executed</p>
              <div style={{ display:'flex', alignItems:'baseline', gap:8 }}>
                <span style={{ fontSize:32, fontWeight:800, color:'var(--snt-text-1)', letterSpacing:'-0.03em', lineHeight:1 }}>{project.totalScans || 0}</span>
                <span style={{ fontSize:13, color:'var(--snt-text-3)', fontWeight:600 }}>total</span>
              </div>
            </div>
          </div>

          {/* Recent Scans */}
          <div className="pjd-card">
            <div className="pjd-head">
              <h3>Recent Scans</h3>
              <Link to="/history" style={{ fontSize:13, fontWeight:600, color:'#2563eb', textDecoration:'none' }}>View All →</Link>
            </div>
            <div>
              {scans.length > 0 ? scans.slice(0, 5).map(scan => (
                <div key={scan.scanId} className="pjd-row">
                  <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                    <div style={{ width:32, height:32, borderRadius:8, background:'var(--snt-surface-3)', display:'flex', alignItems:'center', justifyContent:'center' }}>
                      <span className="material-symbols-outlined" style={{ fontSize:16, color:'var(--snt-text-2)' }}>shield</span>
                    </div>
                    <div>
                      <p style={{ margin:0, fontWeight:600, color:'var(--snt-text-1)' }}>Scan #{scan.scanId}</p>
                      <p style={{ margin:0, fontSize:12, color:'var(--snt-text-3)' }}>{fmtDate(scan.scanStart)}</p>
                    </div>
                  </div>
                  <div style={{ display:'flex', gap:24, alignItems:'center' }}>
                    <div style={{ textAlign:'right' }}>
                      <span style={{ fontSize:11, color:'var(--snt-text-4)', display:'block', marginBottom:2 }}>SCORE</span>
                      <span style={{ fontWeight:700, color:scoreColor(scan.securityScore||0) }}>{Math.round(scan.securityScore||0)}</span>
                    </div>
                    <div style={{ textAlign:'right' }}>
                      <span style={{ fontSize:11, color:'var(--snt-text-4)', display:'block', marginBottom:2 }}>FINDINGS</span>
                      <span style={{ fontWeight:700, color:scan.totalVulnerabilities>0?'#ef4444':'#16a34a' }}>{scan.totalVulnerabilities||0}</span>
                    </div>
                    <span style={{ fontSize:12, fontWeight:700, padding:'4px 10px', borderRadius:20, background:scan.status==='COMPLETED'?'#f0fdf4':'#fef2f2', color:scan.status==='COMPLETED'?'#16a34a':'#dc2626' }}>{scan.status}</span>
                  </div>
                </div>
              )) : (
                <div style={{ padding:40, textAlign:'center', color:'var(--snt-text-3)', fontSize:14 }}>No scans found. Start a scan to see results.</div>
              )}
            </div>
          </div>

          {/* Uploaded Files */}
          <div className="pjd-card">
            <div className="pjd-head">
              <h3>Source Files</h3>
              <button onClick={() => navigate(`/scanner?project=${id}`)} style={{ background:'transparent', border:'none', color:'#2563eb', fontSize:13, fontWeight:600, cursor:'pointer' }}>Manage Files →</button>
            </div>
            <div>
              {files.length > 0 ? files.slice(0, 5).map(f => (
                <div key={f.id} className="pjd-row">
                  <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                    <span className="material-symbols-outlined" style={{ fontSize:20, color:'var(--snt-text-4)' }}>description</span>
                    <span style={{ fontWeight:500, color:'var(--snt-text-1)' }}>{f.originalFilename}</span>
                  </div>
                  <span style={{ fontSize:12, color:'var(--snt-text-3)' }}>{(f.fileSize / 1024).toFixed(1)} KB</span>
                </div>
              )) : (
                <div style={{ padding:40, textAlign:'center', color:'var(--snt-text-3)', fontSize:14 }}>No files uploaded yet.</div>
              )}
            </div>
          </div>
        </div>

        {/* RIGHT COL */}
        <div style={{ display:'flex', flexDirection:'column', gap:24 }}>
          
          {/* Project Info */}
          <div className="pjd-card">
            <div className="pjd-head">
              <h3>Project Info</h3>
            </div>
            <div style={{ padding:24, display:'flex', flexDirection:'column', gap:16 }}>
              <div>
                <p style={{ margin:0, fontSize:12, color:'var(--snt-text-3)', fontWeight:600, textTransform:'uppercase' }}>Type</p>
                <p style={{ margin:'4px 0 0', fontSize:14, color:'var(--snt-text-1)', fontWeight:500 }}>{project.projectType?.replace('_',' ')}</p>
              </div>
              <div>
                <p style={{ margin:0, fontSize:12, color:'var(--snt-text-3)', fontWeight:600, textTransform:'uppercase' }}>Owner</p>
                <p style={{ margin:'4px 0 0', fontSize:14, color:'var(--snt-text-1)', fontWeight:500 }}>{project.ownerEmail}</p>
              </div>
              <div>
                <p style={{ margin:0, fontSize:12, color:'var(--snt-text-3)', fontWeight:600, textTransform:'uppercase' }}>Created On</p>
                <p style={{ margin:'4px 0 0', fontSize:14, color:'var(--snt-text-1)', fontWeight:500 }}>{fmtDate(project.createdAt)}</p>
              </div>
              <div>
                <p style={{ margin:0, fontSize:12, color:'var(--snt-text-3)', fontWeight:600, textTransform:'uppercase' }}>Total Files</p>
                <p style={{ margin:'4px 0 0', fontSize:14, color:'var(--snt-text-1)', fontWeight:500 }}>{project.totalFiles}</p>
              </div>
            </div>
          </div>

          {/* Security Assistant Promo */}
          <div className="pjd-card" style={{ background:'linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%)', border:'1px solid #c7d2fe' }}>
            <div style={{ padding:24 }}>
              <div style={{ display:'flex', alignItems:'center', gap:12, marginBottom:16 }}>
                <div style={{ width:40, height:40, borderRadius:10, background:'#4f46e5', display:'flex', alignItems:'center', justifyContent:'center' }}>
                  <span className="material-symbols-outlined" style={{ fontSize:20, color:'#fff', fontVariationSettings:"'FILL' 1" }}>psychology</span>
                </div>
                <h3 style={{ margin:0, fontSize:16, fontWeight:700, color:'var(--snt-text-1)' }}>Sentinel AI</h3>
              </div>
              <p style={{ margin:'0 0 20px', fontSize:14, color:'#4338ca', lineHeight:1.5 }}>Use our AI assistant to analyze the latest scan results for this project and get guided remediation steps.</p>
              <button onClick={() => navigate('/chat')} style={{ width:'100%', height:40, background:'var(--snt-surface)', color:'#4f46e5', border:'1px solid #c7d2fe', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', gap:6, transition:'background 0.2s' }} onMouseEnter={e=>e.currentTarget.style.background='#f8fafc'} onMouseLeave={e=>e.currentTarget.style.background='#fff'}>
                Open AI Assistant
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  )
}
