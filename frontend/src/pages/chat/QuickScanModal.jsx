import React, { useState, useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import Editor from '@monaco-editor/react'
import api from '../../api/axiosConfig'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

// ─── Constants ────────────────────────────────────────────────────────────────

const DEFAULT_CONFIG = {
  owasp: true, cwe: true, secrets: true, sqlInjection: true, xss: true,
  commandInjection: true, pathTraversal: true, jwtIssues: true,
  insecureDeserialization: true, weakCryptography: true, directoryTraversal: true,
  promptInjection: false, aiExplanation: true, aiRootCause: true,
  aiBusinessImpact: true, aiSecureFix: true, confidenceThreshold: 70,
  explanationDepth: 'detailed', maxFileSize: 10, maxFolderDepth: 5,
  skipGenerated: true, ignoreDirs: 'node_modules, .git, target, build',
  allowedExts: '.java, .py, .js, .ts, .go', timeout: 300
}

const LANGUAGES = [
  { value: 'java',       label: 'Java',        ext: '.java' },
  { value: 'javascript', label: 'JavaScript',   ext: '.js'   },
  { value: 'typescript', label: 'TypeScript',   ext: '.ts'   },
  { value: 'python',     label: 'Python',       ext: '.py'   },
  { value: 'c',          label: 'C',            ext: '.c'    },
  { value: 'cpp',        label: 'C++',          ext: '.cpp'  },
  { value: 'csharp',     label: 'C#',           ext: '.cs'   },
  { value: 'go',         label: 'Go',           ext: '.go'   },
  { value: 'php',        label: 'PHP',          ext: '.php'  },
  { value: 'ruby',       label: 'Ruby',         ext: '.rb'   },
  { value: 'kotlin',     label: 'Kotlin',       ext: '.kt'   },
  { value: 'swift',      label: 'Swift',        ext: '.swift'},
  { value: 'rust',       label: 'Rust',         ext: '.rs'   },
  { value: 'html',       label: 'HTML',         ext: '.html' },
  { value: 'css',        label: 'CSS',          ext: '.css'  },
  { value: 'xml',        label: 'XML',          ext: '.xml'  },
  { value: 'yaml',       label: 'YAML',         ext: '.yaml' },
  { value: 'json',       label: 'JSON',         ext: '.json' },
  { value: 'dockerfile', label: 'Dockerfile',   ext: ''      },
  { value: 'shell',      label: 'Shell / Bash', ext: '.sh'   },
  { value: 'powershell', label: 'PowerShell',   ext: '.ps1'  },
]

// Stripped-down Monaco options — syntax highlighting only, zero IDE behaviour
const EDITOR_OPTIONS = {
  // Appearance
  minimap:               { enabled: false },
  fontSize:              13.5,
  fontFamily:            "'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace",
  fontLigatures:         true,
  lineHeight:            1.6,
  padding:               { top: 16, bottom: 16 },
  scrollBeyondLastLine:  false,
  smoothScrolling:       true,
  cursorSmoothCaretAnimation: 'on',
  renderLineHighlight:   'none',
  lineNumbersMinChars:   3,

  // Disable ALL IDE / IntelliSense features
  quickSuggestions:              false,
  suggestOnTriggerCharacters:    false,
  acceptSuggestionOnEnter:       'off',
  tabCompletion:                 'off',
  wordBasedSuggestions:          'off',
  parameterHints:                { enabled: false },
  hover:                         { enabled: false },
  contextmenu:                   false,
  folding:                       false,
  glyphMargin:                   false,
  lightbulb:                     { enabled: false },
  codeLens:                      false,
  links:                         false,
  occurrencesHighlight:          false,
  selectionHighlight:            false,
  matchBrackets:                 'never',
  inlayHints:                    { enabled: 'off' },
  renderValidationDecorations:   'off',
  bracketPairColorization:       { enabled: false },
  guides:                        { bracketPairs: false, indentation: true },
  autoIndent:                    'full',
  formatOnPaste:                 false,
  formatOnType:                  false,
  snippetSuggestions:            'none',
  showDeprecated:                false,
  showUnused:                    false,
  // Keep find widget accessible for Ctrl+F
  find:                          { addExtraSpaceOnTop: false },
  automaticLayout:               true,
}

function mapConfigForApi(cfg) {
  return {
    // Must match ScanConfigurationDto field names exactly (Java camelCase)
    owasp:                    cfg.owasp,
    cwe:                      cfg.cwe,
    secrets:                  cfg.secrets,
    sqlInjection:             cfg.sqlInjection,
    xss:                      cfg.xss,
    commandInjection:         cfg.commandInjection,
    pathTraversal:            cfg.pathTraversal,
    jwtIssues:                cfg.jwtIssues,
    insecureDeserialization:  cfg.insecureDeserialization,
    weakCryptography:         cfg.weakCryptography,
    directoryTraversal:       cfg.directoryTraversal,
    enableExplanation:        cfg.aiExplanation,
    enableRootCause:          cfg.aiRootCause,
    enableBusinessImpact:     cfg.aiBusinessImpact,
    enableSecureFix:          cfg.aiSecureFix,
    confidenceThreshold:      cfg.confidenceThreshold,
    maxFileSizeMB:            cfg.maxFileSize,
    skipGeneratedFiles:       cfg.skipGenerated,
    ignoreDirectories:        cfg.ignoreDirs || 'node_modules, .git, target, build',
    timeoutSeconds:           cfg.timeout,
  }
}

// ─── Scan Configuration Drawer ───────────────────────────────────────────────

function ConfigDrawer({ isOpen, onClose, config, onSave }) {
  const [local, setLocal] = useState(config)
  useEffect(() => { setLocal(config) }, [config])
  if (!isOpen) return null
  const toggle = k => setLocal(p => ({ ...p, [k]: !p[k] }))
  const save   = () => { onSave(local); onClose() }

  const detectionKeys = [
    'owasp', 'cwe', 'secrets', 'sqlInjection', 'xss',
    'commandInjection', 'pathTraversal', 'jwtIssues',
    'insecureDeserialization', 'weakCryptography', 'directoryTraversal', 'promptInjection',
  ]
  const aiKeys = ['aiExplanation', 'aiRootCause', 'aiBusinessImpact', 'aiSecureFix']
  const label  = k => k.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase())
                       .replace(/^Ai /, '')

  return (
    <div style={{ position:'fixed', inset:0, zIndex:10000, display:'flex', justifyContent:'flex-end' }}>
      <div style={{ position:'absolute', inset:0, background:'rgba(15,23,42,0.45)' }} onClick={onClose} />
      <div style={{ width:400, background:'#ffffff', height:'100%', position:'relative', display:'flex', flexDirection:'column', boxShadow:'-8px 0 32px rgba(15,23,42,0.15)', animation:'slideInRight 0.28s cubic-bezier(0.16,1,0.3,1)', zIndex:1 }}>
        <style>{`@keyframes slideInRight{from{transform:translateX(100%)}to{transform:translateX(0)}} .d-row{display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #f3f4f6} .d-row:last-child{border-bottom:none} .d-chk{appearance:none;width:38px;height:22px;background:#e5e7eb;border-radius:11px;position:relative;cursor:pointer;transition:background .18s;flex-shrink:0} .d-chk:checked{background:#0058be} .d-chk::after{content:'';position:absolute;top:2px;left:2px;width:18px;height:18px;background:#fff;border-radius:50%;transition:transform .18s;box-shadow:0 1px 2px rgba(0,0,0,.12)} .d-chk:checked::after{transform:translateX(16px)}`}</style>
        {/* Header */}
        <div style={{ padding:'18px 22px', borderBottom:'1px solid var(--snt-border)', display:'flex', alignItems:'center', justifyContent:'space-between', flexShrink:0 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10 }}>
            <div style={{ width:34, height:34, borderRadius:9, background:'#e0eaff', display:'flex', alignItems:'center', justifyContent:'center' }}>
              <span className="material-symbols-outlined" style={{ fontSize:18, color:'#0058be' }}>tune</span>
            </div>
            <div>
              <p style={{ margin:0, fontSize:15, fontWeight:700, color:'var(--snt-text-1)' }}>Scan Configuration</p>
              <p style={{ margin:0, fontSize:12, color:'var(--snt-text-4)' }}>Same settings as full scans</p>
            </div>
          </div>
          <button onClick={onClose} style={{ border:'none', background:'none', cursor:'pointer', color:'var(--snt-text-4)', padding:4 }}>
            <span className="material-symbols-outlined" style={{ fontSize:20 }}>close</span>
          </button>
        </div>

        {/* Body */}
        <div style={{ flex:1, overflowY:'auto' }}>
          {/* Detection */}
          <div style={{ padding:'18px 22px' }}>
            <p style={{ margin:'0 0 14px', fontSize:11, fontWeight:700, color:'var(--snt-text-4)', textTransform:'uppercase', letterSpacing:'0.07em', display:'flex', alignItems:'center', gap:6 }}>
              <span className="material-symbols-outlined" style={{ fontSize:14, color:'#0058be' }}>shield</span> Detection Scope
            </p>
            {detectionKeys.map(k => (
              <label key={k} className="d-row" style={{ cursor:'pointer' }}>
                <span style={{ fontSize:13.5, color:'var(--snt-text-2)', fontWeight:500 }}>{label(k)}</span>
                <input type="checkbox" className="d-chk" checked={local[k]} onChange={() => toggle(k)} />
              </label>
            ))}
          </div>
          {/* AI */}
          <div style={{ padding:'18px 22px', borderTop:'1px solid #f3f4f6' }}>
            <p style={{ margin:'0 0 14px', fontSize:11, fontWeight:700, color:'var(--snt-text-4)', textTransform:'uppercase', letterSpacing:'0.07em', display:'flex', alignItems:'center', gap:6 }}>
              <span className="material-symbols-outlined" style={{ fontSize:14, color:'#7c3aed' }}>psychology</span> AI Enrichment
            </p>
            {aiKeys.map(k => (
              <label key={k} className="d-row" style={{ cursor:'pointer' }}>
                <span style={{ fontSize:13.5, color:'var(--snt-text-2)', fontWeight:500 }}>{label(k)}</span>
                <input type="checkbox" className="d-chk" checked={local[k]} onChange={() => toggle(k)} />
              </label>
            ))}
            <div style={{ marginTop:16 }}>
              <p style={{ margin:'0 0 6px', fontSize:13, fontWeight:500, color:'var(--snt-text-2)' }}>Confidence Threshold — {local.confidenceThreshold}%</p>
              <input type="range" min="0" max="100" value={local.confidenceThreshold}
                onChange={e => setLocal({ ...local, confidenceThreshold: Number(e.target.value) })}
                style={{ width:'100%', accentColor:'#0058be' }} />
            </div>
          </div>
        </div>

        {/* Footer */}
        <div style={{ padding:'16px 22px', borderTop:'1px solid #e5e7eb', display:'flex', gap:10, flexShrink:0 }}>
          <button onClick={() => setLocal(DEFAULT_CONFIG)} style={{ flex:1, height:40, background:'var(--snt-surface-2)', color:'var(--snt-text-2)', border:'1px solid var(--snt-border)', borderRadius:9, fontSize:13, fontWeight:600, cursor:'pointer' }}>Reset</button>
          <button onClick={save} style={{ flex:2, height:40, background:'#0058be', color:'#fff', border:'none', borderRadius:9, fontSize:13, fontWeight:600, cursor:'pointer' }}>Save Configuration</button>
        </div>
      </div>
    </div>
  )
}

// ─── Main Modal ───────────────────────────────────────────────────────────────

export default function QuickScanModal({ isOpen, onClose, onScanComplete }) {
  const [code,        setCode]        = useState('')
  const [language,    setLanguage]    = useState('')          // '' = no selection yet (required)
  const [filename,    setFilename]    = useState('')
  const [config,      setConfig]      = useState(DEFAULT_CONFIG)
  const [drawerOpen,  setDrawerOpen]  = useState(false)
  const [scanning,    setScanning]    = useState(false)
  const [lineCount,   setLineCount]   = useState(0)
  const addToast = useToast()
  const editorRef = useRef(null)

  // Reload saved config each time modal opens
  useEffect(() => {
    if (!isOpen) return
    try {
      const saved = localStorage.getItem('quick_scan_cfg')
      if (saved) setConfig(JSON.parse(saved))
    } catch { /* ignore */ }
  }, [isOpen])

  if (!isOpen) return null

  const handleSaveConfig = (newConfig) => {
    setConfig(newConfig)
    localStorage.setItem('quick_scan_cfg', JSON.stringify(newConfig))
    addToast('Configuration saved', 'success')
  }

  const handleEditorMount = (editor, monaco) => {
    editorRef.current = editor
    // Disable TypeScript / JavaScript diagnostics (red squiggles)
    try {
      monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({ noSemanticValidation: true, noSyntaxValidation: true })
      monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({ noSemanticValidation: true, noSyntaxValidation: true })
    } catch { /* languages may not be registered for all targets */ }
    // Clear any initial markers
    monaco.editor.setModelMarkers(editor.getModel(), 'owner', [])
  }

  const handleCodeChange = (value = '') => {
    setCode(value)
    setLineCount(value ? value.split('\n').length : 0)
  }

  const handleScan = async () => {
    if (!language) {
      addToast('Please select a language before scanning', 'error'); return
    }
    if (!code.trim()) {
      addToast('Paste some code to scan', 'error'); return
    }
    setScanning(true)
    try {
      const payload = {
        sourceCode:    code,
        language:      language,
        filename:      filename.trim() || undefined,
        configuration: mapConfigForApi(config),
      }
      const res = await api.post('/scan/quick', payload)
      addToast('Quick Scan completed — results saved to history', 'success')
      onScanComplete?.(res.data?.data)
      onClose()
    } catch (err) {
      addToast(err?.response?.data?.message || 'Quick scan failed. Please try again.', 'error')
    } finally {
      setScanning(false)
    }
  }

  const selectedLang = LANGUAGES.find(l => l.value === language)

  return createPortal(
    <>
      {/* ── Backdrop ── */}
      <div style={{
        position:'fixed', inset:0, zIndex:9999,
        display:'flex', alignItems:'center', justifyContent:'center',
        background:'rgba(15,23,42,0.45)',
      }}>
        <style>{`
          @keyframes qsFadeIn{from{opacity:0;transform:scale(0.97)}to{opacity:1;transform:scale(1)}}
          .qs-lang-sel{
            appearance:none; height:34px; padding:0 32px 0 10px;
            background:#1e2030 url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%236b7280'/%3E%3C/svg%3E") no-repeat right 10px center;
            border:1px solid #373b52; border-radius:7px;
            color:#c9cde2; font-size:13px; font-weight:500; cursor:pointer;
            font-family:'Manrope',sans-serif; outline:none; transition:border-color .18s;
          }
          .qs-lang-sel:focus{border-color:#4f86f7;}
          .qs-lang-sel option{background:#1e2030; color:#c9cde2;}
          .qs-fname-inp{
            height:34px; padding:0 10px;
            background:#1e2030; border:1px solid #373b52; border-radius:7px;
            color:#c9cde2; font-size:13px; outline:none; width:180px;
            font-family:'Manrope',sans-serif; transition:border-color .18s;
          }
          @media (max-width: 640px) {
            .qs-fname-inp { width: 140px !important; }
            .qs-card-modal { width: 96% !important; max-height: 94vh !important; border-radius: 12px !important; }
          }
          .qs-fname-inp::placeholder{color:#4b5563;}
          .qs-fname-inp:focus{border-color:#4f86f7;}
          .qs-cfg-btn{
            height:34px; padding:0 12px; background:#262a3d; border:1px solid #373b52;
            border-radius:7px; color:#a0a8c3; font-size:13px; font-weight:500;
            cursor:pointer; display:flex; align-items:center; gap:5px; flex-shrink:0;
            font-family:'Manrope',sans-serif; transition:background .15s, border-color .15s;
          }
          .qs-cfg-btn:hover{background:#30364d; color:#e2e8f0; border-color:#4f86f7;}
          .qs-action-btn {
            height: 36px; padding: 0 14px; border-radius: 7px; font-size: 13.5px; font-weight: 600;
            display: flex; align-items: center; justify-content: center; gap: 6px; cursor: pointer;
            font-family: 'Manrope', sans-serif; transition: opacity .15s, background .15s; outline: none;
          }
          .qs-action-btn:hover:not(:disabled) { opacity: 0.85; }
          .qs-ctrls-bar {
            padding: 12px 20px; background: #171a29; border-bottom: 1px solid #282c40;
            display: flex; align-items: center; flex-wrap: wrap; gap: 12px; flex-shrink: 0;
          }
          .qs-ctrls-group { display: flex; align-items: center; gap: 8px; }
          .qs-ctrls-label { font-size: 12px; font-weight: 600; color: #5c6380; text-transform: uppercase; letter-spacing: 0.06em; white-space: nowrap; }
          
          @media (max-width: 640px) {
            .qs-fname-inp { width: 100% !important; }
            .qs-lang-sel { width: 100% !important; }
            .qs-card-modal { width: 96% !important; max-height: 94vh !important; border-radius: 12px !important; }
            .qs-ctrls-bar { padding: 12px 14px; gap: 10px; }
            .qs-ctrls-group { width: 100%; justify-content: space-between; }
            .qs-divider { display: none; }
            .qs-cfg-btn { width: 100%; justify-content: center; }
            .qs-footer-actions { width: 100%; display: flex; justify-content: space-between; gap: 8px; margin-top: 8px; }
            .qs-footer-actions button { flex: 1; }
            .qs-card-modal-footer { flex-direction: column !important; align-items: stretch !important; gap: 0 !important; }
          }
        `}</style>

        {/* ── Main Modal Card ── */}
        <div className="qs-card-modal" style={{
          width: '92%', maxWidth: 840,
          height: '85vh', maxHeight: 800, minHeight: 500,
          background: '#141724', border: '1px solid #282c40',
          borderRadius: 16, boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
          overflow: 'hidden', display: 'flex', flexDirection: 'column',
          animation: 'qsFadeIn .2s cubic-bezier(0.16,1,0.3,1)'
        }}>
          {/* Header */}
          <div style={{
            padding: '16px 20px', background: '#1a1d2e',
            borderBottom: '1px solid #282c40',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 32, height: 32, borderRadius: 8, background: '#1d2c4e', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#4f86f7' }}>bolt</span>
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: 15, fontWeight: 700, color: '#f1f5f9', fontFamily: "'Plus Jakarta Sans',sans-serif" }}>Quick Scan</h3>
                <span style={{ fontSize: 12, color: '#6b7280' }}>Paste code or snippet for instant AI vulnerability analysis</span>
              </div>
            </div>
            <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: '#6b7280', cursor: 'pointer', display: 'flex', padding: 4 }}>
              <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
            </button>
          </div>

          {/* Controls Bar */}
          <div className="qs-ctrls-bar">
            {/* Language (required) */}
            <div className="qs-ctrls-group" style={{ flex: 1, minWidth: 120 }}>
              <label className="qs-ctrls-label">
                Language <span style={{ color:'#ef4444' }}>*</span>
              </label>
              <select
                className="qs-lang-sel"
                value={language}
                onChange={e => setLanguage(e.target.value)}
                style={{ flex: 1 }}
              >
                <option value="" disabled>Select…</option>
                {LANGUAGES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
              </select>
            </div>

            {/* Divider */}
            <div className="qs-divider" style={{ width:1, height:22, background:'rgba(255,255,255,0.07)' }} />

            {/* Filename */}
            <div className="qs-ctrls-group" style={{ flex: 1, minWidth: 140 }}>
              <label className="qs-ctrls-label">
                Filename
              </label>
              <input
                type="text"
                className="qs-fname-inp"
                placeholder={selectedLang ? `e.g. snippet${selectedLang.ext}` : 'e.g. snippet.java'}
                value={filename}
                onChange={e => setFilename(e.target.value)}
                style={{ flex: 1 }}
              />
            </div>

            {/* Spacer */}
            <div className="qs-divider" style={{ flex:1 }} />

            {/* Line count */}
            {lineCount > 0 && (
              <span className="qs-divider" style={{ fontSize:12, color:'#3d4258', fontFamily:'monospace', whiteSpace: 'nowrap' }}>
                {lineCount} {lineCount === 1 ? 'line' : 'lines'}
              </span>
            )}

            {/* Config button */}
            <button className="qs-cfg-btn" onClick={() => setDrawerOpen(true)}>
              <span className="material-symbols-outlined" style={{ fontSize:15 }}>tune</span>
              Config
            </button>
          </div>

          {/* ── Code Editor (dark, snippet-only) ── */}
          <div style={{ flex: 1, minHeight: 0, position: 'relative' }}>
            <Editor
              height="100%"
              language={language || 'plaintext'}
              theme="vs-dark"
              value={code}
              onChange={handleCodeChange}
              onMount={handleEditorMount}
              options={EDITOR_OPTIONS}
            />
            {/* Empty state overlay */}
            {!code && (
              <div style={{
                position:'absolute', top:0, left:0, right:0, bottom:0,
                display:'flex', alignItems:'center', justifyContent:'center',
                pointerEvents:'none',
              }}>
                <div style={{ textAlign:'center' }}>
                  <span className="material-symbols-outlined" style={{ fontSize:36, color:'#2a2d3e', display:'block', marginBottom:8 }}>code</span>
                  <p style={{ margin:0, fontSize:13, color:'#2a2d3e' }}>
                    {language ? 'Paste your code here' : 'Select a language, then paste your code'}
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* ── Footer ── */}
          <div className="qs-card-modal-footer" style={{
            display:'flex', alignItems:'center', justifyContent:'space-between',
            padding:'12px 16px', background:'#1a1d2e',
            borderTop:'1px solid rgba(255,255,255,0.06)', flexShrink:0,
          }}>
            {/* Left: clear */}
            <button
              className="qs-action-btn"
              onClick={() => setCode('')}
              style={{ background:'#262a3d', color:'#a0a8c3', border:'1px solid #373b52' }}
            >
              <span className="material-symbols-outlined" style={{ fontSize:16 }}>delete_sweep</span>
              Clear
            </button>

            {/* Right: cancel + scan */}
            <div className="qs-footer-actions" style={{ display:'flex', gap:10, alignItems:'center' }}>
              <button
                className="qs-action-btn"
                onClick={onClose}
                style={{ background:'transparent', color:'#a0a8c3', border:'1px solid #373b52' }}
              >
                Cancel
              </button>
              <button
                className="qs-action-btn"
                onClick={handleScan}
                disabled={scanning || !language || !code.trim()}
                style={{
                  background: scanning ? '#2563eb99' : '#0058be',
                  color:'#fff', padding:'0 24px', border: 'none',
                  opacity: (!language || !code.trim()) && !scanning ? 0.45 : 1,
                  cursor: (!language || !code.trim()) && !scanning ? 'not-allowed' : 'pointer',
                }}
              >
                {scanning
                  ? <><LoadingSpinner size={14} color="#fff" /> Scanning…</>
                  : <><span className="material-symbols-outlined" style={{ fontSize:16 }}>shield</span> Quick Scan</>
                }
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* ── Config Drawer (rendered at high z-index, above modal) ── */}
      <ConfigDrawer
        isOpen={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        config={config}
        onSave={handleSaveConfig}
      />
    </>,
    document.body
  )
}
