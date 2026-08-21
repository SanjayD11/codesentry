import { useState, useEffect, useCallback, useMemo } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { getProjects, createProject, deleteProject } from '../../api/projectApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import ConfirmDialog from '../../components/ui/ConfirmDialog'

const fmtDate = (iso) => iso ? new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'
const scoreColor = (s) => s >= 80 ? '#16a34a' : s >= 60 ? '#f59e0b' : '#dc2626'
const scoreBg    = (s) => s >= 80 ? '#f0fdf4' : s >= 60 ? '#fffbeb' : '#fef2f2'
const riskLabel  = (s) => s >= 80 ? 'Low Risk' : s >= 60 ? 'Medium' : s >= 40 ? 'High Risk' : 'Critical'

function CreateProjectModal({ onClose, onSuccess }) {
  const [name, setName] = useState('')
  const [desc, setDesc] = useState('')
  const [type, setType] = useState('WEB_APPLICATION')
  const [loading, setLoading] = useState(false)
  const addToast = useToast()

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!name.trim()) return addToast('Project name is required', 'error')
    setLoading(true)
    try {
      const res = await createProject({ projectName: name, description: desc, projectType: type })
      addToast('Project created successfully', 'success')
      onSuccess(res.data.data)
    } catch (err) {
      addToast(err?.response?.data?.message || 'Failed to create project', 'error')
    } finally {
      setLoading(false)
    }
  }

  return createPortal(
    <div style={{ position:'fixed', inset:0, background:'rgba(15,23,42,0.45)', zIndex:9999, display:'flex', alignItems:'center', justifyContent:'center', padding:24 }} onClick={onClose}>
      <div style={{ background:'#ffffff', borderRadius:16, width:'100%', maxWidth:500, boxShadow:'0 20px 40px rgba(15,23,42,0.15)', border:'1px solid #cbd5e1' }} onClick={e => e.stopPropagation()}>
        <div style={{ padding:'24px 28px 20px', borderBottom:'1px solid #e2e8f0', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
          <div style={{ display:'flex', alignItems:'center', gap:12 }}>
            <div style={{ width:40, height:40, borderRadius:12, background:'#eff6ff', border:'1px solid #bfdbfe', display:'flex', alignItems:'center', justifyContent:'center' }}>
              <span className="material-symbols-outlined" style={{ fontSize:20, color:'#2563eb', fontVariationSettings:"'FILL' 1" }}>add_circle</span>
            </div>
            <p style={{ margin:0, fontSize:17, fontWeight:700, color:'#0f172a', letterSpacing:'-0.01em' }}>New Project</p>
          </div>
          <button onClick={onClose} style={{ width:32, height:32, border:'1px solid #cbd5e1', borderRadius:8, background:'#ffffff', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center' }}>
            <span className="material-symbols-outlined" style={{ fontSize:18, color:'#64748b' }}>close</span>
          </button>
        </div>
        <form onSubmit={handleSubmit} style={{ padding:28, display:'flex', flexDirection:'column', gap:20 }}>
          <div>
            <label style={{ display:'block', margin:'0 0 8px', fontSize:13, fontWeight:600, color:'#334155' }}>Project Name *</label>
            <input value={name} onChange={e=>setName(e.target.value)} placeholder="e.g. Core Payment API" style={{ width:'100%', padding:'10px 14px', border:'1px solid #cbd5e1', borderRadius:10, fontSize:14, color:'#0f172a', background:'#ffffff', outline:'none', transition:'all 0.2s', boxShadow:'0 1px 2px rgba(0,0,0,0.05)' }}
              onFocus={e=>{e.target.style.borderColor='#2563eb';e.target.style.boxShadow='0 0 0 3px rgba(37,99,235,0.12)'}}
              onBlur={e=>{e.target.style.borderColor='#cbd5e1';e.target.style.boxShadow='0 1px 2px rgba(0,0,0,0.05)'}} autoFocus />
          </div>
          <div>
            <label style={{ display:'block', margin:'0 0 8px', fontSize:13, fontWeight:600, color:'#334155' }}>Description</label>
            <textarea value={desc} onChange={e=>setDesc(e.target.value)} placeholder="Brief purpose of the project..." style={{ width:'100%', padding:'10px 14px', border:'1px solid #cbd5e1', borderRadius:10, fontSize:14, color:'#0f172a', background:'#ffffff', outline:'none', transition:'all 0.2s', minHeight:80, resize:'vertical', boxShadow:'0 1px 2px rgba(0,0,0,0.05)' }}
              onFocus={e=>{e.target.style.borderColor='#2563eb';e.target.style.boxShadow='0 0 0 3px rgba(37,99,235,0.12)'}}
              onBlur={e=>{e.target.style.borderColor='#cbd5e1';e.target.style.boxShadow='0 1px 2px rgba(0,0,0,0.05)'}} />
          </div>
          <div>
            <label style={{ display:'block', margin:'0 0 8px', fontSize:13, fontWeight:600, color:'#334155' }}>Project Type</label>
            <select value={type} onChange={e=>setType(e.target.value)} style={{ width:'100%', padding:'10px 14px', border:'1px solid #cbd5e1', borderRadius:10, fontSize:14, color:'#0f172a', background:'#ffffff', outline:'none', cursor:'pointer', boxShadow:'0 1px 2px rgba(0,0,0,0.05)' }}>
              <option value="WEB_APPLICATION">Web Application</option>
              <option value="API_SERVICE">API Service</option>
              <option value="MOBILE_APP">Mobile App</option>
              <option value="LIBRARY">Library / Package</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div style={{ display:'flex', gap:12, justifyContent:'flex-end', marginTop:8 }}>
            <button type="button" onClick={onClose} style={{ height:40, padding:'0 20px', background:'#ffffff', color:'#0f172a', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', transition:'all 0.15s' }}>Cancel</button>
            <button type="submit" disabled={loading} style={{ height:40, padding:'0 20px', background:'#2563eb', color:'#fff', border:'1px solid #1d4ed8', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, opacity:loading?0.7:1, boxShadow:'0 2px 6px rgba(37,99,235,0.2)' }}>
              {loading ? <LoadingSpinner size="sm" /> : 'Create Project'}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  )
}

export default function Projects() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [view, setView] = useState('grid')
  const [showCreate, setShowCreate] = useState(false)
  const addToast = useToast()
  const navigate = useNavigate()

  const fetchProjects = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getProjects({ size: 100 })
      const list = res.data.data?.projects || res.data.data?.content || []
      setProjects(list)
    } catch {
      addToast('Failed to load projects', 'error')
    } finally {
      setLoading(false)
    }
  }, [addToast])

  useEffect(() => { fetchProjects() }, [fetchProjects])

  const [confirmState, setConfirmState] = useState(null) // { id, e }

  const handleDelete = (e, id) => {
    e.stopPropagation()
    setConfirmState({ id })
  }

  const handleDeleteConfirm = async () => {
    const id = confirmState?.id
    setConfirmState(null)
    try {
      await deleteProject(id)
      setProjects(prev => prev.filter(p => p.id !== id))
      addToast('Project deleted', 'success')
    } catch {
      addToast('Failed to delete project', 'error')
    }
  }

  const filtered = useMemo(() => {
    return projects.filter(p => !search || p.projectName?.toLowerCase().includes(search.toLowerCase()) || p.description?.toLowerCase().includes(search.toLowerCase()))
  }, [projects, search])

  const stats = useMemo(() => {
    const scanned = projects.filter(p => p.lastScanTime != null)
    const avg = scanned.length ? scanned.reduce((acc, p) => acc + (100 - (p.overallRiskScore||0)), 0) / scanned.length : 0
    return {
      total: projects.length,
      activeScans: scanned.length,
      avgScore: scanned.length ? avg.toFixed(0) : '—'
    }
  }, [projects])

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", paddingBottom: 64 }}>
      <style>{`
        .pj-card { transition: border-color 0.2s ease, box-shadow 0.2s ease; cursor: pointer; transform: none !important; }
        .pj-card:hover { border-color: #93c5fd !important; box-shadow: 0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04); transform: none !important; }
        .pj-act { width:32px; height:32px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; color:#64748b; cursor:pointer; display:flex; align-items:center; justify-content:center; transition: border-color 0.15s ease, background 0.15s ease; transform: none !important; }
        .pj-act:hover { border-color:#2563eb; color:#2563eb; background:#eff6ff; transform: none !important; }
        .pj-act.danger:hover { border-color:#dc2626; color:#dc2626; background:#fef2f2; transform: none !important; }
      `}</style>

      {/* ── Header ── */}
      <div style={{ background:'var(--snt-surface)', border:'1px solid #e2e6f0', borderRadius:16, padding:'28px 32px', marginBottom:24, display:'flex', alignItems:'center', justifyContent:'space-between', flexWrap:'wrap', gap:16 }}>
        <div style={{ display:'flex', alignItems:'center', gap:16 }}>
          <div style={{ width:44, height:44, borderRadius:12, background:'#f0f3ff', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
            <span className="material-symbols-outlined" style={{ fontSize:22, color:'#0058be', fontVariationSettings:"'FILL' 1" }}>folder</span>
          </div>
          <div>
            <p style={{ margin:'0 0 2px', fontSize:11, fontWeight:700, color:'#727785', textTransform:'uppercase', letterSpacing:'0.08em' }}>Workspace</p>
            <h1 style={{ margin:0, fontSize:24, fontWeight:700, color:'var(--snt-text-1)', letterSpacing:'-0.025em', fontFamily:"'Plus Jakarta Sans', sans-serif" }}>Projects</h1>
            <p style={{ margin:'4px 0 0', fontSize:14, color:'#505f76', fontWeight:400 }}>Manage your repositories, target applications, and security context.</p>
          </div>
        </div>
        <div style={{ display:'flex', gap:8 }}>
          <button onClick={fetchProjects} style={{ height:40, padding:'0 16px', background:'#ffffff', color:'#0f172a', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, boxShadow:'0 1px 2px rgba(15,23,42,0.04)', transition:'all 0.2s ease' }}
            onMouseEnter={e=>{e.currentTarget.style.borderColor='#94a3b8';e.currentTarget.style.background='#f8fafc'}}
            onMouseLeave={e=>{e.currentTarget.style.borderColor='#cbd5e1';e.currentTarget.style.background='#ffffff'}}>
            <span className="material-symbols-outlined" style={{ fontSize:16 }}>refresh</span>Refresh
          </button>
          <button onClick={() => setShowCreate(true)} style={{ height:40, padding:'0 20px', background:'#0058be', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, transition:'all 0.2s ease' }}
            onMouseEnter={e=>{e.currentTarget.style.background='#004a9f'}}
            onMouseLeave={e=>{e.currentTarget.style.background='#0058be'}}>
            <span className="material-symbols-outlined" style={{ fontSize:16 }}>add</span>New Project
          </button>
        </div>
      </div>

      {/* ── Stats Strip ── */}
      <style>{`
        .pj-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
        .pj-stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
        @media (max-width: 768px) {
          .pj-stats-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
      <div className="pj-stats-grid">
        {[
          { label:'Total Projects', value: stats.total,       icon:'folder',       color:'#0058be', bg:'#f0f3ff' },
          { label:'Active Status',  value: stats.activeScans, icon:'check_circle', color:'#00855b', bg:'#f5fff6' },
          { label:'Avg Score',      value: stats.avgScore,    icon:'speed',        color:'#0058be', bg:'#f0f3ff' },
        ].map(st => (
          <div key={st.label} style={{
            background:'#ffffff', border:'1px solid #e2e8f0', borderRadius:14, padding:'20px',
            boxShadow:'0 1px 3px rgba(15,23,42,0.04)', transition:'border-color 0.2s ease, box-shadow 0.2s ease'
          }}
          onMouseEnter={e => { e.currentTarget.style.borderColor = '#93c5fd'; e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)' }}
          onMouseLeave={e => { e.currentTarget.style.borderColor = '#e2e8f0'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)' }}
          >
            <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:10 }}>
              <div style={{ width:28, height:28, borderRadius:8, background:st.bg, display:'flex', alignItems:'center', justifyContent:'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize:15, color:st.color, fontVariationSettings:"'FILL' 1" }}>{st.icon}</span>
              </div>
              <span style={{ fontSize:12, color:'#727785', fontWeight:600 }}>{st.label}</span>
            </div>
            <p style={{ margin:0, fontSize:28, fontWeight:700, color:'#0f172a', letterSpacing:'-0.03em' }}>{st.value}</p>
          </div>
        ))}
      </div>

      {/* ── Filter Bar ── */}
      <div style={{ display:'flex', gap:12, alignItems:'center', marginBottom:20, flexWrap:'wrap' }}>
        <div style={{ position:'relative' }}>
          <span className="material-symbols-outlined" style={{ position:'absolute', left:12, top:'50%', transform:'translateY(-50%)', fontSize:16, color:'#64748b' }}>search</span>
          <input
            value={search} onChange={e => setSearch(e.target.value)} placeholder="Search projects…"
            style={{ paddingLeft:36, paddingRight:16, height:40, width:280, background:'#ffffff', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13.5, color:'#0f172a', outline:'none', boxShadow:'0 1px 2px rgba(15,23,42,0.04)', transition:'all 0.2s' }}
            onFocus={e=>{e.target.style.borderColor='#2563eb';e.target.style.boxShadow='0 0 0 3px rgba(37,99,235,0.1)'}}
            onBlur={e=>{e.target.style.borderColor='#cbd5e1';e.target.style.boxShadow='0 1px 2px rgba(15,23,42,0.04)'}}
          />
        </div>
        <div style={{ marginLeft:'auto', display:'flex', background:'#f1f5f9', border:'1px solid #cbd5e1', borderRadius:10, padding:3 }}>
          {['grid', 'list'].map(v => (
            <button key={v} onClick={() => setView(v)} style={{ width:34, height:34, borderRadius:8, border: view === v ? '1px solid #cbd5e1' : '1px solid transparent', cursor:'pointer', transition:'all 0.2s', display:'flex', alignItems:'center', justifyContent:'center',
              background:view === v ? '#ffffff' : 'transparent', color:view === v ? '#0f172a' : '#64748b',
              boxShadow:view === v ? '0 1px 3px rgba(15,23,42,0.08)' : 'none' }}>
              <span className="material-symbols-outlined" style={{ fontSize:18 }}>{v === 'grid' ? 'grid_view' : 'view_list'}</span>
            </button>
          ))}
        </div>
      </div>

      {/* ── Grid/List ── */}
      {loading ? (
        <div style={{ display:'flex', justifyContent:'center', padding:80 }}><LoadingSpinner size="lg" /></div>
      ) : filtered.length === 0 ? (
        <div style={{ background:'var(--snt-surface)', border:'1px solid #e2e6f0', borderRadius:16, padding:'64px 32px', textAlign:'center' }}>
          <div style={{ width:56, height:56, borderRadius:16, background:'#f0f3ff', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}>
            <span className="material-symbols-outlined" style={{ fontSize:28, color:'#0058be' }}>folder_open</span>
          </div>
          <p style={{ margin:'0 0 6px', fontSize:18, fontWeight:600, color:'var(--snt-text-1)' }}>No projects found</p>
          <p style={{ margin:'0 0 24px', fontSize:14, color:'#727785' }}>{search ? 'Try adjusting your search.' : 'Create your first project workspace to begin scanning.'}</p>
          <button onClick={() => search ? setSearch('') : setShowCreate(true)} style={{ height:40, padding:'0 24px', background:'#0058be', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>
            {search ? 'Clear Search' : 'Create Project'}
          </button>
        </div>
      ) : view === 'grid' ? (
        <div className="pj-grid">
          {filtered.map(p => {
            const score = p.lastScanTime ? (100 - (p.overallRiskScore||0)) : null;
            return (
            <div key={p.id} className="pj-card" onClick={() => navigate(`/projects/${p.id}`)} style={{
              background:'#ffffff', border:'1px solid #e2e8f0', borderRadius:16, display:'flex', flexDirection:'column', cursor:'pointer', position:'relative', padding:'24px',
              boxShadow: '0 1px 3px rgba(15,23,42,0.04)', transition: 'border-color 0.2s ease, box-shadow 0.2s ease'
            }}
            onMouseEnter={e => { e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)'; e.currentTarget.style.borderColor = '#93c5fd' }}
            onMouseLeave={e => { e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)'; e.currentTarget.style.borderColor = '#e2e8f0' }}
            >
              {/* Header: Title and Type */}
              <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:12, marginBottom:24 }}>
                <div style={{ display:'flex', alignItems:'flex-start', gap:14, minWidth:0 }}>
                  <div style={{ width:42, height:42, borderRadius:12, background:p.lastScanTime ? scoreBg(score) : '#f0f3ff', border:`1px solid ${p.lastScanTime ? scoreColor(score)+'30' : '#d0e1fb'}`, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                    <span className="material-symbols-outlined" style={{ fontSize:20, color:p.lastScanTime ? scoreColor(score) : '#0058be', fontVariationSettings:"'FILL' 1" }}>
                      {p.projectType === 'WEB_APPLICATION' ? 'language' : p.projectType === 'API_SERVICE' ? 'api' : 'folder'}
                    </span>
                  </div>
                  <div style={{ minWidth:0, paddingTop:2 }}>
                    <h3 style={{ margin:'0 0 4px', fontSize:16, fontWeight:700, color:'var(--snt-text-1)', letterSpacing:'-0.01em', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', fontFamily:"'Plus Jakarta Sans', sans-serif" }}>{p.projectName}</h3>
                    <p style={{ margin:0, fontSize:13, color:'#505f76', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{p.description || 'No description provided'}</p>
                  </div>
                </div>
              </div>

              {/* Metrics Strip */}
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12, marginBottom:24, marginTop:'auto' }}>
                <div style={{ padding:'12px', background:'#f9f9ff', borderRadius:12, border:'1px solid #f0f3ff' }}>
                  <p style={{ margin:'0 0 4px', fontSize:11, color:'#727785', fontWeight:600, textTransform:'uppercase', letterSpacing:'0.04em' }}>Security Score</p>
                  {p.lastScanTime ? (
                    <div style={{ display:'flex', alignItems:'baseline', gap:4 }}>
                        <span style={{ fontSize:20, fontWeight:700, color:scoreColor(score), letterSpacing:'-0.02em', lineHeight:1 }}>{score.toFixed(0)}</span>
                        <span style={{ fontSize:12, fontWeight:600, color:scoreColor(score), opacity:0.8 }}>/ 100</span>
                    </div>
                  ) : (
                    <span style={{ fontSize:13, color:'#727785', fontWeight:500 }}>No scans</span>
                  )}
                </div>
                <div style={{ padding:'12px', background:'#f9f9ff', borderRadius:12, border:'1px solid #f0f3ff' }}>
                  <p style={{ margin:'0 0 4px', fontSize:11, color:'#727785', fontWeight:600, textTransform:'uppercase', letterSpacing:'0.04em' }}>Files</p>
                  <div style={{ display:'flex', alignItems:'baseline', gap:6 }}>
                      <span style={{ fontSize:20, fontWeight:700, color:'var(--snt-text-1)', letterSpacing:'-0.02em', lineHeight:1 }}>{p.totalFiles}</span>
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', borderTop:'1px solid #f0f3ff', paddingTop:16 }}>
                <span style={{ fontSize:12, color:'#727785', fontWeight:500 }}>Created {fmtDate(p.createdAt)}</span>
                <div style={{ display:'flex', gap:6 }}>
                  <button className="pj-act" onClick={e => { e.stopPropagation(); navigate(`/scanner?project=${p.id}`) }} title="New Scan">
                    <span className="material-symbols-outlined" style={{ fontSize:16 }}>shield</span>
                  </button>
                  <button className="pj-act danger" onClick={e => handleDelete(e, p.id)} title="Delete Project">
                    <span className="material-symbols-outlined" style={{ fontSize:16 }}>delete</span>
                  </button>
                </div>
              </div>
            </div>
            )
          })}
        </div>
      ) : (
        <div style={{ background:'#ffffff', border:'1px solid #e2e8f0', borderRadius:16, overflow:'hidden', boxShadow: '0 1px 3px rgba(15,23,42,0.04)' }}>
          {/* Desktop/Tablet Table */}
          <div className="table-scroll-wrap hidden-on-mobile">
            <div className="table-scroll-inner">
              <table style={{ width:'100%', borderCollapse:'collapse' }}>
                <thead>
                  <tr style={{ borderBottom:'1px solid #cbd5e1', background:'#f8fafc' }}>
                    <th style={{ padding:'14px 20px', textAlign:'left', fontSize:11, fontWeight:700, color:'#475569', textTransform:'uppercase', letterSpacing:'0.06em' }}>Project Name</th>
                    <th style={{ padding:'14px 20px', textAlign:'left', fontSize:11, fontWeight:700, color:'#475569', textTransform:'uppercase', letterSpacing:'0.06em' }}>Type</th>
                    <th style={{ padding:'14px 20px', textAlign:'left', fontSize:11, fontWeight:700, color:'#475569', textTransform:'uppercase', letterSpacing:'0.06em' }}>Files</th>
                    <th style={{ padding:'14px 20px', textAlign:'left', fontSize:11, fontWeight:700, color:'#475569', textTransform:'uppercase', letterSpacing:'0.06em' }}>Score</th>
                    <th style={{ padding:'14px 20px', textAlign:'right', fontSize:11, fontWeight:700, color:'#475569', textTransform:'uppercase', letterSpacing:'0.06em' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(p => (
                    <tr key={p.id} onClick={() => navigate(`/projects/${p.id}`)} style={{ borderBottom:'1px solid #e2e8f0', cursor:'pointer', transition: 'background 0.15s ease' }} onMouseEnter={e=>e.currentTarget.style.background='#f8fafc'} onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
                      <td style={{ padding:'16px 20px' }}>
                        <p style={{ margin:0, fontSize:14, fontWeight:600, color:'#0f172a' }}>{p.projectName}</p>
                        <p style={{ margin:'2px 0 0', fontSize:12, color:'#64748b' }}>Created {fmtDate(p.createdAt)}</p>
                      </td>
                      <td style={{ padding:'16px 20px' }}>
                        <span style={{ padding:'4px 8px', borderRadius:6, background:'var(--snt-surface-3)', fontSize:10, fontWeight:700, color:'var(--snt-text-3)', letterSpacing:'0.05em', textTransform:'uppercase' }}>{p.projectType?.replace('_',' ')}</span>
                      </td>
                      <td style={{ padding:'16px 20px', fontSize:14, color:'var(--snt-text-2)', fontWeight:500 }}>{p.totalFiles}</td>
                      <td style={{ padding:'16px 20px' }}>
                        {p.lastScanTime ? (
                          <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                            <span style={{ fontSize:15, fontWeight:700, color:scoreColor(100-(p.overallRiskScore||0)) }}>{(100-(p.overallRiskScore||0)).toFixed(0)}</span>
                            <span style={{ fontSize:11, color:scoreColor(100-(p.overallRiskScore||0)), fontWeight:600, opacity:0.8 }}>{riskLabel(100-(p.overallRiskScore||0))}</span>
                          </div>
                        ) : <span style={{ color:'var(--snt-text-4)' }}>—</span>}
                      </td>
                      <td style={{ padding:'16px 20px' }}>
                        <div style={{ display:'flex', alignItems:'center', justifyContent:'flex-end', gap:6 }}>
                          <button className="pj-act" onClick={e => { e.stopPropagation(); navigate(`/scanner?project=${p.id}`) }} title="New Scan">
                            <span className="material-symbols-outlined" style={{ fontSize:16 }}>shield</span>
                          </button>
                          <button className="pj-act danger" onClick={e => handleDelete(e, p.id)} title="Delete Project">
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

          {/* Adaptive Mobile Card List for < 768px */}
          <div className="visible-on-mobile" style={{ display:'flex', flexDirection:'column', gap:10, padding:12 }}>
            {filtered.map(p => {
              const score = p.lastScanTime ? (100 - (p.overallRiskScore||0)) : null;
              return (
                <div key={p.id} onClick={() => navigate(`/projects/${p.id}`)} style={{
                  background:'#f8fafc', border:'1px solid #e2e8f0', borderRadius:12, padding:'14px', display:'flex', flexDirection:'column', gap:10, cursor:'pointer'
                }}>
                  <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:8 }}>
                    <div style={{ minWidth:0 }}>
                      <p style={{ margin:0, fontSize:14, fontWeight:700, color:'#0f172a' }}>{p.projectName}</p>
                      <span style={{ display:'inline-block', marginTop:4, padding:'2px 6px', borderRadius:4, background:'#e2e8f0', fontSize:9.5, fontWeight:700, color:'#475569', textTransform:'uppercase' }}>{p.projectType?.replace('_',' ')}</span>
                    </div>
                    {score != null ? (
                      <div style={{ textAlign:'right', flexShrink:0 }}>
                        <span style={{ fontSize:15, fontWeight:800, color:scoreColor(score) }}>{score.toFixed(0)}</span>
                        <p style={{ margin:0, fontSize:10, fontWeight:600, color:scoreColor(score) }}>{riskLabel(score)}</p>
                      </div>
                    ) : <span style={{ fontSize:11, color:'#94a3b8' }}>No scans</span>}
                  </div>
                  <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', borderTop:'1px solid #e2e8f0', paddingTop:8 }}>
                    <span style={{ fontSize:11.5, color:'#64748b' }}>{p.totalFiles} files · {fmtDate(p.createdAt)}</span>
                    <div style={{ display:'flex', gap:6 }}>
                      <button className="pj-act" onClick={e => { e.stopPropagation(); navigate(`/scanner?project=${p.id}`) }} title="New Scan">
                        <span className="material-symbols-outlined" style={{ fontSize:15 }}>shield</span>
                      </button>
                      <button className="pj-act danger" onClick={e => handleDelete(e, p.id)} title="Delete Project">
                        <span className="material-symbols-outlined" style={{ fontSize:15 }}>delete</span>
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {showCreate && <CreateProjectModal onClose={() => setShowCreate(false)} onSuccess={(newProj) => { setShowCreate(false); fetchProjects(); navigate(`/projects/${newProj.id}`) }} />}

      <ConfirmDialog
        open={!!confirmState}
        title="Delete Project"
        message="This project and all its scans, files, and reports will be permanently deleted. This action cannot be undone."
        confirmLabel="Delete Project"
        variant="danger"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setConfirmState(null)}
      />
    </div>
  )
}
