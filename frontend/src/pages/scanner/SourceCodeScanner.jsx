import { useState, useEffect, useCallback, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { getProjects } from '../../api/projectApi'
import { uploadFiles, getProjectFiles, deleteFile } from '../../api/uploadApi'
import { triggerScan, getScan } from '../../api/scanApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

// ─── Duplicate Confirmation Modal ────────────────────────────────────────────
function DuplicateConfirmModal({ fileName, onUploadAnyway, onCancel }) {
  return createPortal(
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'rgba(15,23,42,0.45)'
    }}>
        <div style={{
          background: '#ffffff', borderRadius: 16, padding: '32px 36px',
          maxWidth: 440, width: '90%', boxShadow: '0 24px 64px rgba(15,23,42,0.18)',
          border: '1px solid #e2e8f0',
          animation: 'fadeInScale 0.25s cubic-bezier(0.16,1,0.3,1)'
        }}>
          <style>{`
            @keyframes fadeInScale {
              from { opacity: 0; transform: scale(0.95); }
              to   { opacity: 1; transform: scale(1); }
            }
          `}</style>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
            <div style={{
              width: 44, height: 44, borderRadius: '50%',
              background: '#fff7ed', border: '1px solid #ffedd5', display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0
            }}>
              <span className="material-symbols-outlined" style={{ color: '#f59e0b', fontSize: 24 }}>content_copy</span>
            </div>
            <div>
              <div style={{ fontSize: 17, fontWeight: 700, color: '#0f172a', fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Duplicate File Detected</div>
              <div style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>Same content already exists in this project</div>
            </div>
          </div>

          <div style={{
            background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 10,
            padding: '12px 16px', marginBottom: 24
          }}>
            <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 4, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>File</div>
            <div style={{ fontSize: 13, color: '#334155', fontWeight: 600, wordBreak: 'break-all' }}>{fileName}</div>
          </div>

          <p style={{ fontSize: 14, color: '#475569', lineHeight: 1.6, marginBottom: 24 }}>
            Would you like to upload this file anyway? A <strong>fresh scan</strong> will be performed
            on the identical code, and the previous scan history will be preserved.
          </p>

          <div style={{ display: 'flex', gap: 12 }}>
            <button
              id="duplicate-cancel-btn"
              onClick={onCancel}
              style={{
                flex: 1, height: 44, background: '#ffffff', color: '#475569',
                border: '1px solid #cbd5e1', borderRadius: 10, fontSize: 14,
                fontWeight: 600, cursor: 'pointer', display: 'flex',
                alignItems: 'center', justifyContent: 'center', gap: 6,
                transition: 'all 0.15s ease'
              }}
              onMouseEnter={e => e.currentTarget.style.background = '#f8fafc'}
              onMouseLeave={e => e.currentTarget.style.background = '#ffffff'}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 18 }}>close</span>
              Cancel
            </button>
            <button
              id="duplicate-upload-anyway-btn"
              onClick={onUploadAnyway}
              style={{
                flex: 2, height: 44, background: '#2563eb',
                color: '#fff', border: 'none', borderRadius: 10, fontSize: 14,
                fontWeight: 600, cursor: 'pointer', display: 'flex',
                alignItems: 'center', justifyContent: 'center', gap: 6,
                boxShadow: '0 4px 12px rgba(37,99,235,0.25)',
                transition: 'all 0.15s ease'
              }}
              onMouseEnter={e => e.currentTarget.style.background = '#1d4ed8'}
              onMouseLeave={e => e.currentTarget.style.background = '#2563eb'}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 18 }}>refresh</span>
              Upload Anyway
            </button>
          </div>
        </div>
    </div>,
    document.body
  )
}
// ─────────────────────────────────────────────────────────────────────────────

const DEFAULT_CONFIG = {
  owasp: true,
  cwe: true,
  secrets: true,
  sqlInjection: true,
  xss: true,
  commandInjection: true,
  pathTraversal: true,
  jwtIssues: true,
  insecureDeserialization: true,
  weakCryptography: true,
  directoryTraversal: true,
  promptInjection: false,
  aiExplanation: true,
  aiRootCause: true,
  aiBusinessImpact: true,
  aiSecureFix: true,
  confidenceThreshold: 70,
  explanationDepth: 'detailed',
  maxFileSize: 10, // MB
  maxFolderDepth: 5,
  skipGenerated: true,
  ignoreDirs: 'node_modules, .git, target, build',
  allowedExts: '.java, .py, .js, .ts, .go',
  timeout: 300 // seconds
}

// Drawer Component
function ConfigureDrawer({ isOpen, onClose, config, setConfig, onSave }) {
  const [local, setLocal] = useState(config)
  useEffect(() => { setLocal(config) }, [config])

  if (!isOpen) return null

  const toggle = (k) => setLocal(p => ({ ...p, [k]: !p[k] }))
  const handleSave = () => { onSave(local); onClose() }

  return createPortal(
    <div style={{ position:'fixed', inset:0, zIndex:9999, display:'flex', justifyContent:'flex-end' }}>
      <div style={{ position:'absolute', inset:0, background:'rgba(15,23,42,0.45)' }} onClick={onClose} />
      <div style={{ width:420, background:'#ffffff', height:'100%', position:'relative', zIndex:2001, display:'flex', flexDirection:'column', boxShadow:'-12px 0 36px rgba(15,23,42,0.15)', animation:'slideInRight 0.25s cubic-bezier(0.16,1,0.3,1)' }}>
        <style>{`
          @keyframes slideInRight { from { transform: translateX(100%); } to { transform: translateX(0); } }
          .cfg-sec { padding:24px; border-bottom:1px solid #f1f5f9; }
          .cfg-title { font-size:11.5px; font-weight:700; color:#64748b; text-transform:uppercase; letter-spacing:0.06em; margin-bottom:16px; display:flex; align-items:center; gap:8px; }
          .cfg-row { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; }
          .cfg-row:last-child { margin-bottom:0; }
          .cfg-lbl { font-size:13.5px; color:#1e293b; font-weight:500; }
          .cfg-chk { appearance:none; width:40px; height:24px; background:#cbd5e1; border-radius:12px; position:relative; cursor:pointer; transition:all 0.2s; outline:none; }
          .cfg-chk:checked { background:#2563eb; }
          .cfg-chk::after { content:''; position:absolute; top:2px; left:2px; width:20px; height:20px; background:#fff; border-radius:50%; transition:all 0.2s; box-shadow:0 1px 3px rgba(0,0,0,0.15); }
          .cfg-chk:checked::after { transform:translateX(16px); }
          .cfg-inp { width:100%; padding:8px 12px; border:1px solid #cbd5e1; border-radius:8px; font-size:13px; outline:none; color:#0f172a; transition:border-color 0.2s; }
          .cfg-inp:focus { border-color:#2563eb; box-shadow:0 0 0 3px rgba(37,99,235,0.1); }
        `}</style>

        <div style={{ padding:'20px 24px', borderBottom:'1px solid #e2e8f0', display:'flex', alignItems:'center', justifyContent:'space-between', background: '#ffffff' }}>
          <h2 style={{ margin:0, fontSize:18, fontWeight:700, color:'#0f172a', fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Scan Configuration</h2>
          <button onClick={onClose} style={{ background:'transparent', border:'none', cursor:'pointer', color:'#64748b', display: 'flex', alignItems: 'center', padding: 4, borderRadius: 6 }}><span className="material-symbols-outlined">close</span></button>
        </div>

        <div style={{ flex:1, overflowY:'auto', background: '#ffffff' }}>
          <div className="cfg-sec">
            <div className="cfg-title"><span className="material-symbols-outlined" style={{ fontSize:16, color:'#2563eb', fontVariationSettings: "'FILL' 1" }}>shield</span> Detection Scope</div>
            {['owasp', 'cwe', 'secrets', 'sqlInjection', 'xss', 'commandInjection', 'pathTraversal', 'jwtIssues', 'insecureDeserialization', 'weakCryptography', 'directoryTraversal'].map(k => (
              <label key={k} className="cfg-row">
                <span className="cfg-lbl">{k.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}</span>
                <input type="checkbox" className="cfg-chk" checked={local[k]} onChange={() => toggle(k)} />
              </label>
            ))}
            <label className="cfg-row" style={{ opacity:0.5 }}>
              <span className="cfg-lbl">Prompt Injection (Future)</span>
              <input type="checkbox" className="cfg-chk" disabled checked={local.promptInjection} />
            </label>
          </div>

          <div className="cfg-sec">
            <div className="cfg-title"><span className="material-symbols-outlined" style={{ fontSize:16, color:'#8b5cf6', fontVariationSettings: "'FILL' 1" }}>psychology</span> AI Configuration</div>
            {['aiExplanation', 'aiRootCause', 'aiBusinessImpact', 'aiSecureFix'].map(k => (
              <label key={k} className="cfg-row">
                <span className="cfg-lbl">{k.replace('ai', 'Enable ').replace(/([A-Z])/g, ' $1')}</span>
                <input type="checkbox" className="cfg-chk" checked={local[k]} onChange={() => toggle(k)} />
              </label>
            ))}
            <div style={{ marginTop:16 }}>
              <label style={{ display:'block', fontSize:13, fontWeight:500, color:'#475569', marginBottom:6 }}>Confidence Threshold ({local.confidenceThreshold}%)</label>
              <input type="range" min="0" max="100" value={local.confidenceThreshold} onChange={e => setLocal({...local, confidenceThreshold: Number(e.target.value)})} style={{ width:'100%', accentColor: '#2563eb' }} />
            </div>
          </div>

          <div className="cfg-sec">
            <div className="cfg-title"><span className="material-symbols-outlined" style={{ fontSize:16, color:'#f59e0b', fontVariationSettings: "'FILL' 1" }}>settings</span> Scan Behaviour</div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12, marginBottom:12 }}>
              <div>
                <label style={{ display:'block', fontSize:12, color:'#64748b', marginBottom:4, fontWeight: 500 }}>Max File Size (MB)</label>
                <input type="number" className="cfg-inp" value={local.maxFileSize} onChange={e => setLocal({...local, maxFileSize: e.target.value})} />
              </div>
              <div>
                <label style={{ display:'block', fontSize:12, color:'#64748b', marginBottom:4, fontWeight: 500 }}>Timeout (s)</label>
                <input type="number" className="cfg-inp" value={local.timeout} onChange={e => setLocal({...local, timeout: e.target.value})} />
              </div>
            </div>
            <div style={{ marginBottom:12 }}>
              <label style={{ display:'block', fontSize:12, color:'#64748b', marginBottom:4, fontWeight: 500 }}>Ignore Directories</label>
              <input type="text" className="cfg-inp" value={local.ignoreDirs} onChange={e => setLocal({...local, ignoreDirs: e.target.value})} />
            </div>
            <label className="cfg-row">
              <span className="cfg-lbl">Skip Generated Files</span>
              <input type="checkbox" className="cfg-chk" checked={local.skipGenerated} onChange={() => toggle('skipGenerated')} />
            </label>
          </div>
        </div>

        <div style={{ padding:20, borderTop:'1px solid #e2e8f0', display:'flex', gap:12, background: '#ffffff' }}>
          <button onClick={() => setLocal(DEFAULT_CONFIG)} style={{ flex:1, height:40, background:'#ffffff', color:'#475569', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>Reset</button>
          <button onClick={handleSave} style={{ flex:2, height:40, background:'#2563eb', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', boxShadow: '0 2px 6px rgba(37,99,235,0.2)' }}>Save Configuration</button>
        </div>
      </div>
    </div>,
    document.body
  )
}

export default function SourceCodeScanner() {
  const [projects, setProjects] = useState([])
  const [selectedProjectId, setSelectedProjectId] = useState('')
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [completedScanId, setCompletedScanId] = useState(null)
  const [drawerOpen, setDrawerOpen] = useState(false)

  // Duplicate detection state
  const [duplicateModal, setDuplicateModal] = useState(null) // { file, formData }
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const addToast = useToast()
  const fileInputRef = useRef(null)

  // Config State
  const [config, setConfig] = useState(DEFAULT_CONFIG)

  const loadProjects = useCallback(async () => {
    try {
      const res = await getProjects({ size: 100 })
      const list = res.data.data?.projects || res.data.data?.content || []
      setProjects(list)
      const pId = searchParams.get('project') || list[0]?.id || ''
      if (pId) {
        setSelectedProjectId(pId.toString())
        loadFiles(pId.toString())
        // Load persisted config
        const saved = localStorage.getItem(`scan_cfg_${pId}`)
        if (saved) setConfig(JSON.parse(saved))
      }
    } catch {
      addToast('Failed to load projects', 'error')
    } finally {
      setLoading(false)
    }
  }, [searchParams, addToast])

  useEffect(() => { loadProjects() }, [loadProjects])

  const loadFiles = async (pId) => {
    try {
      const res = await getProjectFiles(pId)
      setFiles(res.data.data?.content || [])
    } catch {
      // Ignore
    }
  }

  const handleProjectChange = (e) => {
    const id = e.target.value
    setSelectedProjectId(id)
    setCompletedScanId(null) // Reset on change
    if (id) {
      loadFiles(id)
      const saved = localStorage.getItem(`scan_cfg_${id}`)
      if (saved) setConfig(JSON.parse(saved))
      else setConfig(DEFAULT_CONFIG)
    } else {
      setFiles([])
    }
  }

  const handleUploadClick = () => {
    if (!selectedProjectId) return addToast('Please select a project first', 'warning')
    fileInputRef.current?.click()
  }

  const handleFileChange = async (e) => {
    const picked = e.target.files?.[0]
    if (!picked) return
    setUploading(true)
    const formData = new FormData()
    formData.append('files', picked)
    try {
      const res = await uploadFiles(selectedProjectId, formData, false)
      const fileRes = res.data?.data?.[0]
      if (fileRes && !fileRes.success && fileRes.message === 'DUPLICATE_FILE_DETECTED') {
        // Surface the confirmation dialog — do NOT show an error toast
        setDuplicateModal({ pickedFile: picked, fileName: picked.name })
      } else if (fileRes && !fileRes.success) {
        addToast(fileRes.message || 'Failed to upload file', 'error')
      } else {
        addToast('File uploaded successfully', 'success')
        loadFiles(selectedProjectId)
      }
    } catch (err) {
      addToast(err?.response?.data?.message || 'Failed to upload file', 'error')
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const handleDuplicateUploadAnyway = async () => {
    if (!duplicateModal) return
    const { pickedFile, fileName } = duplicateModal
    setDuplicateModal(null)
    setUploading(true)
    const formData = new FormData()
    formData.append('files', pickedFile)
    try {
      const res = await uploadFiles(selectedProjectId, formData, true)
      const fileRes = res.data?.data?.[0]
      if (fileRes && !fileRes.success) {
        addToast(fileRes.message || 'Failed to re-upload file', 'error')
      } else {
        addToast(`'${fileName}' re-uploaded — ready for a fresh scan`, 'success')
        loadFiles(selectedProjectId)
      }
    } catch (err) {
      addToast(err?.response?.data?.message || 'Failed to re-upload file', 'error')
    } finally {
      setUploading(false)
    }
  }

  const handleDeleteFile = async (fileId) => {
    try {
      await deleteFile(fileId)
      setFiles(prev => prev.filter(f => f.id !== fileId))
      addToast('File deleted', 'success')
    } catch {
      addToast('Failed to delete file', 'error')
    }
  }

  const handleSaveConfig = (newConfig) => {
    setConfig(newConfig)
    if (selectedProjectId) {
      localStorage.setItem(`scan_cfg_${selectedProjectId}`, JSON.stringify(newConfig))
    }
    addToast('Configuration saved to project', 'success')
  }

  const handleStartScan = async () => {
    if (!selectedProjectId) return addToast('Select a project to scan', 'warning')
    if (files.length === 0) return addToast('Upload at least one file to scan', 'warning')
    
    setScanning(true)
    setCompletedScanId(null)
    try {
      // Map frontend config keys → Java ScanConfigurationDto field names
      const backendConfig = {
        owasp:                  config.owasp,
        cwe:                    config.cwe,
        secrets:                config.secrets,
        sqlInjection:           config.sqlInjection,
        xss:                    config.xss,
        commandInjection:       config.commandInjection,
        pathTraversal:          config.pathTraversal,
        jwtIssues:              config.jwtIssues,
        insecureDeserialization:config.insecureDeserialization,
        weakCryptography:       config.weakCryptography,
        directoryTraversal:     config.directoryTraversal,
        enableExplanation:      config.aiExplanation,
        enableRootCause:        config.aiRootCause,
        enableBusinessImpact:   config.aiBusinessImpact,
        enableSecureFix:        config.aiSecureFix,
        confidenceThreshold:    config.confidenceThreshold,
        maxFileSizeMB:          config.maxFileSize,
        timeoutSeconds:         config.timeout,
        ignoreDirectories:      config.ignoreDirs,
        skipGeneratedFiles:     config.skipGenerated,
      }
      const res = await triggerScan(selectedProjectId, backendConfig)

      const scanId = res.data?.data?.scanId
      addToast('Analysis started! Please wait...', 'success')
      
      // Poll for completion
      let isDone = false
      while (!isDone) {
        await new Promise(r => setTimeout(r, 2000))
        try {
          const statusRes = await getScan(scanId)
          const currentScan = statusRes.data?.data
          if (currentScan && (currentScan.status === 'COMPLETED' || currentScan.status === 'FAILED')) {
            isDone = true
            if (currentScan.status === 'COMPLETED') {
              addToast('Analysis complete!', 'success')
              setCompletedScanId(scanId)
            } else {
              addToast('Analysis failed.', 'error')
            }
          }
        } catch (e) {
          // ignore transient polling errors
        }
      }
    } catch (err) {
      addToast(err?.response?.data?.message || 'Failed to trigger scan', 'error')
    } finally {
      setScanning(false)
    }
  }

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><LoadingSpinner size="lg" /></div>

  if (projects.length === 0) {
    return (
      <div style={{ background:'var(--snt-surface)', border:'1px solid var(--snt-border)', borderRadius:16, padding:'64px 32px', textAlign:'center', maxWidth:600, margin:'40px auto' }}>
        <div style={{ width:56, height:56, borderRadius:16, background:'#eff6ff', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}>
          <span className="material-symbols-outlined" style={{ fontSize:28, color:'#2563eb' }}>folder_off</span>
        </div>
        <p style={{ margin:'0 0 6px', fontSize:18, fontWeight:600, color:'var(--snt-text-1)' }}>No projects available</p>
        <p style={{ margin:'0 0 24px', fontSize:14, color:'var(--snt-text-3)' }}>You must create a project before scanning files. A project acts as a workspace for your source code and scan history.</p>
        <button onClick={() => navigate('/projects')} style={{ height:40, padding:'0 24px', background:'#2563eb', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer' }}>
          Create Project
        </button>
      </div>
    )
  }

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", paddingBottom: 64, maxWidth:1000, margin:'0 auto' }}>
      {/* Duplicate file confirmation modal */}
      {duplicateModal && (
        <DuplicateConfirmModal
          fileName={duplicateModal.fileName}
          onUploadAnyway={handleDuplicateUploadAnyway}
          onCancel={() => setDuplicateModal(null)}
        />
      )}
      
      {/* ── Header ── */}
      <div style={{ background:'var(--snt-surface)', border:'1px solid var(--snt-border)', borderRadius:16, padding:'28px 32px', marginBottom:24, display:'flex', alignItems:'center', justifyContent:'space-between', flexWrap:'wrap', gap:16 }}>
        <div style={{ display:'flex', alignItems:'center', gap:16 }}>
          <div style={{ width:44, height:44, borderRadius:12, background:'#eff6ff', border:'1px solid #dbeafe', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
            <span className="material-symbols-outlined" style={{ fontSize:22, color:'#2563eb', fontVariationSettings: "'FILL' 1" }}>shield</span>
          </div>
          <div>
            <p style={{ margin:'0 0 2px', fontSize:11, fontWeight:700, color:'#64748b', textTransform:'uppercase', letterSpacing:'0.08em' }}>Execution</p>
            <h1 style={{ margin:0, fontSize:24, fontWeight:700, color:'#0f172a', letterSpacing:'-0.025em' }}>Scan Engine</h1>
          </div>
        </div>
        <div style={{ display:'flex', gap:12 }}>
          <button onClick={() => setDrawerOpen(true)} disabled={scanning} style={{ height:40, padding:'0 16px', background:'#ffffff', color:'#0f172a', border:'1px solid #cbd5e1', borderRadius:10, fontSize:13, fontWeight:600, cursor:scanning?'not-allowed':'pointer', display:'flex', alignItems:'center', gap:6, boxShadow:'0 1px 2px rgba(15,23,42,0.04)', transition:'all 0.2s', opacity:scanning?0.5:1 }} onMouseEnter={e=>!scanning&&(e.currentTarget.style.borderColor='#94a3b8', e.currentTarget.style.background='#f8fafc')} onMouseLeave={e=>!scanning&&(e.currentTarget.style.borderColor='#cbd5e1', e.currentTarget.style.background='#ffffff')}>
            <span className="material-symbols-outlined" style={{ fontSize:16 }}>settings</span> Configure
          </button>
          {completedScanId ? (
            <button onClick={() => navigate(`/reports/${completedScanId}`)} style={{ height:40, padding:'0 24px', background:'#10b981', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, boxShadow:'0 1px 2px rgba(16,185,129,0.2)', transition:'all 0.2s ease' }} onMouseEnter={e=>e.currentTarget.style.background='#059669'} onMouseLeave={e=>e.currentTarget.style.background='#10b981'}>
              <span className="material-symbols-outlined" style={{ fontSize:16 }}>analytics</span> View Report
            </button>
          ) : (
            <button onClick={handleStartScan} disabled={scanning} style={{ height:40, padding:'0 24px', background:'#2563eb', color:'#fff', border:'none', borderRadius:10, fontSize:13, fontWeight:600, cursor:scanning?'not-allowed':'pointer', display:'flex', alignItems:'center', gap:6, boxShadow:'0 1px 2px rgba(37,99,235,0.2)', transition:'all 0.2s ease', opacity:scanning?0.7:1 }} onMouseEnter={e=>!scanning&&(e.currentTarget.style.background='#1d4ed8')} onMouseLeave={e=>!scanning&&(e.currentTarget.style.background='#2563eb')}>
              {scanning ? <><LoadingSpinner size="sm" /> <span style={{ marginLeft: 6 }}>Analyzing Code...</span></> : <><span className="material-symbols-outlined" style={{ fontSize:16 }}>play_arrow</span> Start Analysis</>}
            </button>
          )}
        </div>
      </div>

      <div style={{ display:'flex', flexDirection:'column', gap:24 }}>
        
        {/* ── Step 1: Project Selection ── */}
        <div style={{
          background: '#ffffff',
          border: '1px solid #e2e8f0',
          borderRadius: 16,
          padding: 32,
          boxShadow: '0 1px 3px rgba(15,23,42,0.04)',
          transition: 'border-color 0.15s ease, box-shadow 0.15s ease',
        }}
        onMouseEnter={e => { e.currentTarget.style.boxShadow = '0 4px 16px rgba(15,23,42,0.06)'; e.currentTarget.style.borderColor = '#cbd5e1' }}
        onMouseLeave={e => { e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)'; e.currentTarget.style.borderColor = '#e2e8f0' }}
        >
          <div style={{ display:'flex', alignItems:'center', gap:12, marginBottom:20 }}>
            <div style={{ width:28, height:28, borderRadius:'50%', background:'#2563eb', color:'#ffffff', display:'flex', alignItems:'center', justifyContent:'center', fontSize:13, fontWeight:700 }}>1</div>
            <h2 style={{ margin:0, fontSize:18, fontWeight:700, color:'#0f172a', fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Select Project Workspace</h2>
          </div>
          <select value={selectedProjectId} onChange={handleProjectChange} style={{ width:'100%', padding:'12px 16px', border:'1px solid #cbd5e1', borderRadius:10, fontSize:14, color:'#0f172a', background:'#f8fafc', outline:'none', cursor:'pointer', fontWeight:500, fontFamily: "'Manrope', sans-serif" }}>
            <option value="" disabled>-- Choose a project workspace --</option>
            {projects.map(p => <option key={p.id} value={p.id}>{p.projectName} ({p.projectType?.replace('_',' ')})</option>)}
          </select>
        </div>

        {/* ── Step 2: Source Files ── */}
        <div style={{
          background: '#ffffff',
          border: '1px solid #e2e8f0',
          borderRadius: 16,
          padding: 32,
          opacity: selectedProjectId ? 1 : 0.5,
          pointerEvents: selectedProjectId ? 'auto' : 'none',
          boxShadow: '0 1px 3px rgba(15,23,42,0.04)',
          transition: 'border-color 0.15s ease, box-shadow 0.15s ease',
        }}
        onMouseEnter={e => selectedProjectId && (e.currentTarget.style.boxShadow = '0 4px 16px rgba(15,23,42,0.06)', e.currentTarget.style.borderColor = '#cbd5e1')}
        onMouseLeave={e => selectedProjectId && (e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)', e.currentTarget.style.borderColor = '#e2e8f0')}
        >
          <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:20 }}>
            <div style={{ display:'flex', alignItems:'center', gap:12 }}>
              <div style={{ width:28, height:28, borderRadius:'50%', background: selectedProjectId ? '#2563eb' : '#94a3b8', color:'#ffffff', display:'flex', alignItems:'center', justifyContent:'center', fontSize:13, fontWeight:700 }}>2</div>
              <h2 style={{ margin:0, fontSize:18, fontWeight:700, color:'#0f172a', fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Source Files</h2>
            </div>
            <button onClick={handleUploadClick} disabled={uploading} style={{ height:36, padding:'0 16px', background:'#eff6ff', color:'#2563eb', border:'1px solid #bfdbfe', borderRadius:8, fontSize:13, fontWeight:600, cursor:'pointer', display:'flex', alignItems:'center', gap:6, transition: 'all 0.15s ease' }} onMouseEnter={e=>e.currentTarget.style.background='#dbeafe'} onMouseLeave={e=>e.currentTarget.style.background='#eff6ff'}>
              {uploading ? <LoadingSpinner size="sm" /> : <><span className="material-symbols-outlined" style={{ fontSize:16 }}>upload</span> Upload File</>}
            </button>
            <input type="file" ref={fileInputRef} onChange={handleFileChange} style={{ display:'none' }} />
          </div>

          {files.length > 0 ? (
            <div style={{ border:'1px solid #cbd5e1', borderRadius:12, overflow:'hidden', boxShadow: '0 1px 2px rgba(15,23,42,0.04)' }}>
              {files.map((f, i) => (
                <div key={f.id} style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'16px 20px', background:'#ffffff', borderBottom: i !== files.length-1 ? '1px solid #e2e8f0' : 'none', transition: 'background 0.15s ease' }} onMouseEnter={e=>e.currentTarget.style.background='#f8fafc'} onMouseLeave={e=>e.currentTarget.style.background='#ffffff'}>
                  <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                    <div style={{ width: 34, height: 34, borderRadius: 8, background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <span className="material-symbols-outlined" style={{ fontSize:20, color:'#2563eb' }}>description</span>
                    </div>
                    <div>
                      <p style={{ margin:0, fontSize:14, fontWeight:600, color:'#0f172a' }}>{f.originalFilename}</p>
                      <p style={{ margin:0, fontSize:12, color:'#64748b' }}>{(f.fileSize / 1024).toFixed(1)} KB · Uploaded {new Date(f.createdAt).toLocaleDateString()}</p>
                    </div>
                  </div>
                  <button onClick={() => handleDeleteFile(f.id)} style={{ width:32, height:32, border:'none', background:'transparent', color:'#94a3b8', cursor:'pointer', borderRadius:8, transition:'all 0.2s' }} onMouseEnter={e=>{e.currentTarget.style.background='#fef2f2';e.currentTarget.style.color='#ef4444'}} onMouseLeave={e=>{e.currentTarget.style.background='transparent';e.currentTarget.style.color='#94a3b8'}}>
                    <span className="material-symbols-outlined" style={{ fontSize:18 }}>delete</span>
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div 
              onClick={handleUploadClick}
              style={{ border:'2px dashed #cbd5e1', borderRadius:12, padding:40, textAlign:'center', background:'#f8fafc', cursor:'pointer', transition:'all 0.18s cubic-bezier(0.4, 0, 0.2, 1)' }}
              onMouseEnter={e => { e.currentTarget.style.borderColor = '#2563eb'; e.currentTarget.style.background = '#eff6ff'; e.currentTarget.style.boxShadow = '0 4px 12px rgba(37,99,235,0.06)'; }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.background = '#f8fafc'; e.currentTarget.style.boxShadow = 'none'; }}
            >
              <div style={{ width: 48, height: 48, borderRadius: '50%', background: '#eff6ff', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <span className="material-symbols-outlined" style={{ fontSize:24, color:'#2563eb' }}>upload_file</span>
              </div>
              <p style={{ margin:'0 0 6px', fontSize:15, fontWeight:700, color:'#0f172a', fontFamily: "'Plus Jakarta Sans', sans-serif" }}>No files uploaded yet</p>
              <p style={{ margin:0, fontSize:13, color:'#64748b' }}>Click to upload your source code files or ZIP archives to begin scanning.</p>
            </div>
          )}
        </div>

      </div>

      <ConfigureDrawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} config={config} setConfig={setConfig} onSave={handleSaveConfig} />
    </div>
  )
}
