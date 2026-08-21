import { useState, useEffect } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { getDashboard } from '../../api/dashboardApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatRelativeTime(dateString) {
  if (!dateString) return '—'
  const diff = Date.now() - new Date(dateString).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'Just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

function formatDate(dateString) {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function getScanStatusStyle(status) {
  switch ((status || '').toUpperCase()) {
    case 'COMPLETED': return { color: '#006947', bg: 'rgba(0,133,91,0.08)', dot: '#00855b' }
    case 'FAILED':    return { color: '#ba1a1a', bg: 'rgba(186,26,26,0.08)', dot: '#ba1a1a' }
    case 'RUNNING':   return { color: '#0058be', bg: 'rgba(0,88,190,0.08)', dot: '#0058be' }
    default:          return { color: 'var(--snt-text-2)', bg: 'rgba(66,71,84,0.08)', dot: '#424754' }
  }
}

function getScoreColor(score) {
  if (score >= 80) return '#006947'
  if (score >= 60) return '#b45309'
  return '#ba1a1a'
}

function SeverityBar({ label, count, total, color }) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10 }}>
      <div style={{ width: 64, fontSize: 12, fontWeight: 600, color: 'var(--snt-text-2)', textAlign: 'right', flexShrink: 0 }}>{label}</div>
      <div style={{ flex: 1, height: 5, background: '#f0f3ff', borderRadius: 99, overflow: 'hidden' }}>
        <div style={{
          width: `${pct}%`, height: '100%', background: color, borderRadius: 99,
          transition: 'width 0.7s cubic-bezier(0.4,0,0.2,1)',
        }} />
      </div>
      <div style={{ width: 28, fontSize: 12, fontWeight: 700, color: 'var(--snt-text-1)', textAlign: 'right', flexShrink: 0 }}>{count}</div>
    </div>
  )
}

function KpiCard({ icon, iconBg, iconColor, label, value, meta }) {
  const [hovered, setHovered] = useState(false)
  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      className="kpi-card-compact kpi-card-mobile"
      style={{
        border: `1px solid ${hovered ? '#93c5fd' : '#e2e8f0'}`,
        boxShadow: hovered
          ? '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)'
          : '0 1px 3px rgba(15,23,42,0.04)',
        transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
        cursor: 'default',
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div style={{
          width: 38, height: 38, borderRadius: 9,
          background: hovered ? (iconBg.replace('0.08', '0.14')) : iconBg,
          color: iconColor,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transition: 'background 0.2s ease',
        }}>
          <span className="material-symbols-outlined" style={{ fontSize: 20, fontVariationSettings: "'FILL' 1" }}>{icon}</span>
        </div>
        {meta && (
          <span style={{ fontSize: 11.5, fontWeight: 600, color: '#424754', background: '#f0f3ff', padding: '3px 8px', borderRadius: 99 }}>
            {meta}
          </span>
        )}
      </div>
      <div>
        <p style={{ margin: 0, fontSize: 13, fontWeight: 500, color: 'var(--snt-text-2)' }}>{label}</p>
        <p className="dash-kpi-num" style={{ margin: '4px 0 0', fontWeight: 800, color: 'var(--snt-text-1)', letterSpacing: '-0.03em', lineHeight: 1, fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif" }}>
          {value}
        </p>
      </div>
    </div>
  )
}

function ScanRow({ scan }) {
  const st = getScanStatusStyle(scan.status)
  const score = scan.securityScore ?? 0
  const scoreColor = getScoreColor(score)
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '1fr 100px 90px 90px 90px',
        alignItems: 'center',
        padding: '13px 20px',
        borderBottom: '1px solid #e2e8f0',
        color: 'inherit',
        transition: 'all 0.15s ease',
        gap: 12,
        cursor: 'pointer',
      }}
      onMouseEnter={e => e.currentTarget.style.background = '#f8fafc'}
      onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
        <div style={{
          width: 32, height: 32, borderRadius: 8,
          background: '#f0f3ff', display: 'flex', alignItems: 'center',
          justifyContent: 'center', flexShrink: 0,
        }}>
          <span className="material-symbols-outlined" style={{ fontSize: 15, color: '#0058be' }}>
            {scan.language === 'PYTHON' ? 'code' : scan.language === 'JAVA' ? 'terminal' : 'insert_drive_file'}
          </span>
        </div>
        <div style={{ minWidth: 0 }}>
          <p style={{ margin: 0, fontSize: 13.5, fontWeight: 600, color: '#111c2d', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {scan.scanType === 'QUICK_SCAN' ? `Quick Scan ${scan.snippetFilename ? '(' + scan.snippetFilename + ')' : ''}` : `${scan.language || 'File'} Scan #${scan.scanId}`}
          </p>
          <p style={{ margin: 0, fontSize: 12, color: '#424754' }}>
            {scan.scanType === 'QUICK_SCAN' ? `${scan.snippetLines || 0} lines` : `${scan.scannedFiles ?? 0} files`} · {typeof scan.durationSeconds === 'number' ? scan.durationSeconds.toFixed(2) : (scan.durationSeconds ?? 0)}s
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
        <span style={{ width: 6, height: 6, borderRadius: '50%', background: st.dot, flexShrink: 0 }} />
        <span style={{ fontSize: 12.5, fontWeight: 600, color: st.color }}>{scan.status}</span>
      </div>

      <div>
        <span style={{ fontSize: 12.5, fontWeight: 600, color: scan.totalVulnerabilities > 0 ? '#ba1a1a' : '#006947' }}>
          {scan.totalVulnerabilities ?? 0} found
        </span>
      </div>

      <div>
        <span style={{ fontSize: 13, fontWeight: 700, color: scoreColor }}>{Math.round(score)}</span>
        <span style={{ fontSize: 11, color: 'var(--snt-text-2)' }}>/100</span>
      </div>

      <div>
        <span style={{ fontSize: 12, color: 'var(--snt-text-2)' }}>{formatRelativeTime(scan.scanStart)}</span>
      </div>
    </div>
  )
}

// ─── Main Component ────────────────────────────────────────────────────────────

export default function Dashboard() {
  const { user } = useAuth()
  // ── All hooks must be declared before any conditional return (React Rules of Hooks) ──
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const addToast = useToast()

  const firstName = user?.firstName || 'there'

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await getDashboard()
        setData(res.data.data)
      } catch {
        addToast('Failed to load dashboard data', 'error')
      } finally {
        setLoading(false)
      }
    }
    fetchDashboard()
  }, [addToast])

  // Admin users are redirected to their own dashboard — placed AFTER all hooks
  if (user?.role === 'ADMIN') {
    return <Navigate to="/admin/dashboard" replace />
  }

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 400 }}>
      <LoadingSpinner size="lg" />
    </div>
  )

  if (!data) return null

  const totalVulns = (data.criticalCount || 0) + (data.highCount || 0) + (data.mediumCount || 0) + (data.lowCount || 0)
  const recentScans = data.recentScans || []
  const lastScan = recentScans[0]
  const avgScore = Math.round(data.averageSecurityScore || 100)
  const scoreColor = getScoreColor(avgScore)
  const lastScanTime = lastScan ? formatRelativeTime(lastScan.scanStart) : 'No scans yet'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 28, fontFamily: "'Manrope', sans-serif", color: 'var(--snt-text-1)' }}>

      <style>{`
        /* Desktop grids */
        .stats-grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
        .dash-bottom-grid-v2 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 20px; align-items: stretch; }
        .dash-kpi-num { font-size: 28px; }
        .dash-table-wrap { width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch; }
        .dash-table-inner { min-width: 580px; }
        /* Tablet */
        @media (max-width: 1024px) {
          .stats-grid-4 { grid-template-columns: repeat(2, 1fr) !important; }
          .dash-bottom-grid-v2 { grid-template-columns: 1fr !important; }
        }
        
        /* Mobile */
        @media (max-width: 640px) {
          .stats-grid-4 { grid-template-columns: repeat(2, 1fr) !important; gap: 10px !important; }
          .dash-kpi-num { font-size: 24px !important; }
          .dash-hero-mobile { border: none !important; box-shadow: none !important; background: transparent !important; padding: 0 !important; margin-bottom: 4px !important; }
          .dash-hero-inner { flex-direction: column !important; gap: 12px !important; }
          .dash-hero-title { font-size: 28px !important; line-height: 1.15 !important; }
          .dash-hero-subtitle { font-size: 13px !important; }
          
          .dash-hero-btns { flex-direction: row !important; gap: 10px !important; width: 100% !important; }
          .dash-hero-btns a { flex: 1 !important; justify-content: center !important; }
          .dash-overview-header { display: flex !important; }
          .kpi-card-mobile { border: none !important; background: #f5f8ff !important; box-shadow: none !important; padding: 14px !important; border-radius: 14px !important; }
        }
        /* Desktop: hide overview header */
        .dash-overview-header { display: none; }
      `}</style>

      {/* ── Hero Section ── */}
      <section className="dash-hero-mobile" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="dash-hero-inner" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 20, flexWrap: 'wrap' }}>
          <div style={{ minWidth: 0, flex: 1 }}>
            <p style={{ margin: '0 0 4px', fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: '#0058be' }}>
              Security Dashboard
            </p>
            <h1 className="text-responsive-h1 dash-hero-title" style={{ margin: 0, fontWeight: 800, letterSpacing: '-0.025em', color: '#0f172a', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif" }}>
              Good {new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 18 ? 'afternoon' : 'evening'}, {firstName}.
            </h1>
            <p className="dash-hero-subtitle" style={{ margin: '5px 0 0', fontSize: 13, color: '#475569', lineHeight: 1.5, fontWeight: 500, maxWidth: 480 }}>
              {data.totalScans > 0
                ? `${data.totalScans} scan${data.totalScans !== 1 ? 's' : ''} completed · ${totalVulns} vulnerabilit${totalVulns !== 1 ? 'ies' : 'y'} identified · Last activity ${lastScanTime}`
                : 'No scans yet. Upload a file or start a source code analysis.'}
            </p>
          </div>
          <div className="hero-actions dash-hero-btns" style={{ display: 'flex', gap: 10, flexShrink: 0 }}>
            <Link to="/reports" style={{ ...btnOutlineStyle, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>View Reports</Link>
            <Link to="/scanner" style={{ ...btnPrimaryStyle, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 16, fontVariationSettings: "'FILL' 1" }}>shield</span>
              Start Analysis
            </Link>
          </div>
        </div>
      </section>

      {/* ── Overview header (mobile only) ── */}
      <div className="dash-overview-header" style={{ alignItems: 'center', justifyContent: 'space-between', padding: '4px 0' }}>
        <span style={{ fontSize: 15, fontWeight: 700, color: '#0f172a' }}>Overview</span>
        <span style={{ fontSize: 12.5, color: '#64748b', fontWeight: 500 }}>All time</span>
      </div>

      {/* ── KPI Cards ── */}
      <section className="stats-grid-4">
        <KpiCard icon="shield" iconBg="rgba(0,88,190,0.08)" iconColor="#0058be" label="Total Scans" value={data.totalScans || 0} meta={`Last ${lastScanTime}`} />
        <KpiCard icon="upload_file" iconBg="rgba(80,95,118,0.08)" iconColor="#505f76" label="Files Analyzed" value={data.totalFilesUploaded || 0} meta={`${data.activeProjects || 0} project${data.activeProjects !== 1 ? 's' : ''}`} />
        <KpiCard icon="warning" iconBg="rgba(186,26,26,0.08)" iconColor="#ba1a1a" label="Vulnerabilities" value={totalVulns} meta={data.criticalCount > 0 ? `${data.criticalCount} critical` : 'None critical'} />
        <KpiCard icon="verified_user" iconBg={`${scoreColor}14`} iconColor={scoreColor} label="Avg. Score" value={`${avgScore}/100`} meta={avgScore >= 80 ? 'Good' : avgScore >= 60 ? 'Fair' : 'Poor'} />
      </section>

      <div style={panelStyle}>
        <div style={panelHeaderStyle}>
          <div>
            <p style={eyebrowStyle}>Recent Activity</p>
            <h2 style={panelTitleStyle}>Scan Timeline</h2>
          </div>
          <Link to="/history" style={linkButtonStyle}>View all →</Link>
        </div>
        {recentScans.length > 0 ? (
          <div className="dash-table-wrap">
            <div className="dash-table-inner">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px 90px 90px 90px', padding: '10px 20px', background: '#f8fafc', borderBottom: '1px solid #cbd5e1', gap: 12 }}>
                {['Source', 'Status', 'Findings', 'Score', 'Time'].map(h => (
                  <span key={h} style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.07em', textTransform: 'uppercase', color: '#475569' }}>{h}</span>
                ))}
              </div>
              {recentScans.map(scan => <ScanRow key={scan.scanId} scan={scan} />)}
            </div>
          </div>
        ) : (
          <EmptyState icon="shield" message="No scans yet." action={<Link to="/scanner" style={btnPrimarySmStyle}>Run your first scan</Link>} />
        )}
      </div>

      <div className="dash-bottom-grid-v2">

        <div style={{ ...panelStyle, display: 'flex', flexDirection: 'column' }}>
          <div style={panelHeaderStyle}>
            <div>
              <p style={eyebrowStyle}>Findings</p>
              <h2 style={panelTitleStyle}>By Severity</h2>
            </div>
          </div>
          <div style={{ padding: '0 20px 20px', flex: 1, display: 'flex', flexDirection: 'column' }}>
            {totalVulns === 0 ? (
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 8, textAlign: 'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 36, color: '#00855b', fontVariationSettings: "'FILL' 1" }}>shield</span>
                <p style={{ margin: 0, fontSize: 13.5, color: '#006947', fontWeight: 600 }}>No vulnerabilities found</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                <div>
                  <SeverityBar label="Critical" count={data.criticalCount || 0} total={totalVulns} color="#ba1a1a" />
                  <SeverityBar label="High"     count={data.highCount || 0}     total={totalVulns} color="#c2410c" />
                  <SeverityBar label="Medium"   count={data.mediumCount || 0}   total={totalVulns} color="#b45309" />
                  <SeverityBar label="Low"      count={data.lowCount || 0}      total={totalVulns} color="#0058be" />
                </div>
                <div style={{ marginTop: 'auto', paddingTop: 16, borderTop: '1px solid #f0f3ff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: 13, color: 'var(--snt-text-2)' }}>Total findings</span>
                  <span style={{ fontSize: 15, fontWeight: 800, color: 'var(--snt-text-1)', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif" }}>{totalVulns}</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        <div style={panelStyle}>
          <div style={{ ...panelHeaderStyle, paddingBottom: 0 }}>
            <div>
              <p style={eyebrowStyle}>Workflow</p>
              <h2 style={panelTitleStyle}>Quick Actions</h2>
            </div>
          </div>
          <div style={{ padding: '16px 20px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            <ActionRow icon="code" to="/scanner" label="Source Code Scanner" desc="Paste or upload source code for AI analysis" />
            <ActionRow icon="description" to="/reports" label="Analysis Reports" desc="Browse and export generated vulnerability reports" />
            <ActionRow icon="history" to="/history" label="Scan History" desc="View all past scans and detailed findings" />
            <ActionRow image="/ai-bot.png" to="/chat" label="AI Security Assistant" desc="Get contextual guidance on vulnerabilities" />
          </div>
        </div>

        {/* Sentinel AI Card ── full-width on tablet, 1-col on lg */}
        <div className="dash-ai-card" style={{ ...panelStyle, display: 'flex', flexDirection: 'column', position: 'relative', overflow: 'hidden', background: '#fafcff' }}>
          {/* Subtle animated/glow background element */}
          <div style={{
            position: 'absolute', top: '-10%', right: '-10%',
            width: '60%', height: '60%',
            background: 'radial-gradient(circle, rgba(0,88,190,0.06) 0%, transparent 70%)',
            pointerEvents: 'none',
          }} />

          <div style={{ padding: '28px', flex: 1, display: 'flex', flexDirection: 'column', position: 'relative', zIndex: 1 }}>
            
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 20 }}>
              <img src="/ai-bot.png" alt="Sentinel AI" style={{
                width: 44, height: 44, borderRadius: 12, flexShrink: 0,
                objectFit: 'cover',
                boxShadow: '0 4px 14px rgba(0,88,190,0.18)',
              }} />
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#16a34a' }} />
                  <p style={{ margin: 0, fontSize: 11, fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--snt-text-3)' }}>Sentinel AI Active</p>
                </div>
                <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: 'var(--snt-text-1)', letterSpacing: '-0.02em' }}>
                  Contextual Security Engine
                </h3>
              </div>
            </div>

            {/* Description */}
            <p style={{ margin: '0 0 20px', fontSize: 13.5, color: 'var(--snt-text-2)', lineHeight: 1.6, flex: 1 }}>
              Dynamically analyze your codebase architecture. Ask it to explain complex vulnerabilities, generate tailored remediation patches, or compare security risks across your recent scans.
            </p>

            {/* Feature pills */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 24 }}>
              {['Vulnerability Explanation', 'Patch Generation', 'Risk Comparison'].map(tag => (
                <span key={tag} style={{
                  fontSize: 11.5, fontWeight: 500, color: 'var(--snt-text-2)',
                  background: 'var(--snt-surface)', border: '1px solid var(--snt-border)',
                  borderRadius: 6, padding: '4px 10px',
                  boxShadow: '0 1px 2px rgba(17,28,45,0.02)'
                }}>{tag}</span>
              ))}
            </div>

            {/* CTA Button */}
            <Link to="/chat"
              style={{
                display: 'inline-flex', alignItems: 'center', justifySelf: 'flex-start', alignSelf: 'flex-start', gap: 8,
                padding: '0 24px', height: 40,
                background: '#111c2d',
                borderRadius: 8, color: '#ffffff',
                fontSize: 13, fontWeight: 600,
                textDecoration: 'none',
                transition: 'background 0.2s ease',
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#273449'}
              onMouseLeave={(e) => e.currentTarget.style.background = '#111c2d'}
            >
              Open AI Assistant
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>arrow_forward</span>
            </Link>
          </div>
        </div>

      </div>
    </div>
  )
}

// ─── Micro-components ──────────────────────────────────────────────────────────

function ActionRow({ icon, to, label, desc, image }) {
  const [hovered, setHovered] = useState(false)
  return (
    <Link
      to={to}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '10px 12px', borderRadius: 8,
        background: hovered ? '#f0f3ff' : 'transparent',
        border: '1px solid ' + (hovered ? '#adc6ff' : 'transparent'),
        textDecoration: 'none', color: 'inherit',
        transition: 'background 0.18s ease, border-color 0.18s ease',
      }}
    >
      <div style={{
        width: 34, height: 34, borderRadius: 8, flexShrink: 0,
        background: hovered ? 'rgba(0,88,190,0.1)' : '#f0f3ff',
        color: '#0058be', display: 'flex', alignItems: 'center', justifyContent: 'center',
        transition: 'background 0.18s ease', overflow: 'hidden',
      }}>
        {image ? (
          <img src={image} alt="" style={{ width: 26, height: 26, borderRadius: 6, objectFit: 'cover' }} />
        ) : (
          <span className="material-symbols-outlined" style={{ fontSize: 17 }}>{icon}</span>
        )}
      </div>
      <div style={{ minWidth: 0 }}>
        <p style={{ margin: 0, fontSize: 13.5, fontWeight: 600, color: '#111c2d' }}>{label}</p>
        <p style={{ margin: 0, fontSize: 12, color: '#424754', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{desc}</p>
      </div>
      <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#c2c6d6', marginLeft: 'auto', flexShrink: 0, opacity: hovered ? 1 : 0, transition: 'opacity 0.18s ease' }}>arrow_forward</span>
    </Link>
  )
}

function EmptyState({ icon, message, action }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '40px 20px', gap: 12 }}>
      <div style={{ width: 44, height: 44, borderRadius: 12, background: '#f0f3ff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#0058be' }}>{icon}</span>
      </div>
      <p style={{ margin: 0, fontSize: 14, color: 'var(--snt-text-2)', fontWeight: 500 }}>{message}</p>
      {action}
    </div>
  )
}

// ─── Style constants ──────────────────────────────────────────────────────────

const panelStyle = {
  background: '#ffffff',
  border: '1px solid #e2e8f0',
  borderRadius: 14,
  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.04)',
  overflow: 'hidden',
}

const panelHeaderStyle = {
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  padding: '20px 20px 14px',
}

const panelTitleStyle = {
  margin: 0, fontSize: 15, fontWeight: 700, color: 'var(--snt-text-1)',
  letterSpacing: '-0.01em', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif",
}

const eyebrowStyle = {
  margin: '0 0 2px', fontSize: 11, fontWeight: 700,
  letterSpacing: '0.08em', textTransform: 'uppercase', color: '#0058be',
}

const linkButtonStyle = {
  fontSize: 13, fontWeight: 600, color: '#0058be',
  textDecoration: 'none', padding: '5px 10px', borderRadius: 6,
  transition: 'all 0.18s cubic-bezier(0.4, 0, 0.2, 1)',
  whiteSpace: 'nowrap',
  cursor: 'pointer',
}

const btnPrimaryStyle = {
  display: 'inline-flex', alignItems: 'center', gap: 6,
  background: '#0058be', color: '#fff',
  padding: '9px 18px', borderRadius: 8,
  fontSize: 13.5, fontWeight: 600,
  textDecoration: 'none',
  boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.15), 0 1px 3px rgba(0,88,190,0.2)',
  transition: 'all 0.18s cubic-bezier(0.4, 0, 0.2, 1)',
  whiteSpace: 'nowrap',
  cursor: 'pointer',
}

const btnOutlineStyle = {
  display: 'inline-flex', alignItems: 'center',
  background: '#ffffff', color: '#0f172a',
  border: '1px solid #cbd5e1',
  padding: '8px 18px', borderRadius: 8,
  fontSize: 13.5, fontWeight: 600,
  textDecoration: 'none',
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
  transition: 'all 0.18s cubic-bezier(0.4, 0, 0.2, 1)',
  whiteSpace: 'nowrap',
  cursor: 'pointer',
}

const btnPrimarySmStyle = {
  display: 'inline-flex', alignItems: 'center', gap: 6,
  background: '#0058be', color: '#fff',
  padding: '7px 14px', borderRadius: 7,
  fontSize: 13, fontWeight: 600,
  textDecoration: 'none',
  cursor: 'pointer',
  transition: 'all 0.18s cubic-bezier(0.4, 0, 0.2, 1)',
}

