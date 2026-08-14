import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import {
  FiDownload, FiArrowLeft, FiMessageSquare, FiCode, FiCopy, FiCheckCircle, FiShield
} from 'react-icons/fi'
import { getScan } from '../../api/scanApi'
import { downloadReport, generateReport } from '../../api/reportApi'
import { enrichVulnerability, enrichScan as enrichAllVulnerabilities, retryVulnerability } from '../../api/aiApi'
import { useToast } from '../../hooks/useToast'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import EmptyState from '../../components/ui/EmptyState'
import ReactMarkdown from 'react-markdown'

const cleanAiText = (text) => {
  if (!text) return '';
  return text.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
}

const SEV_STYLE = {
  CRITICAL: 'bg-error text-on-error',
  HIGH:     'bg-[#ffb4a9] text-[#93000a] border border-[#ffb4a9]/20',
  MEDIUM:   'bg-secondary-container text-on-secondary-container border border-secondary/20',
  LOW:      'bg-surface-variant text-on-surface-variant border border-outline-variant',
}

export default function ReportDetail() {
  const { id }    = useParams()
  const navigate  = useNavigate()
  const addToast  = useToast()

  const [scan,         setScan]         = useState(null)
  const [loading,      setLoading]      = useState(true)
  const [downloading,  setDownloading]  = useState(false)
  const [enriching,    setEnriching]    = useState(null)
  const [isResolved,   setIsResolved]   = useState(false)
  const [filter,       setFilter]       = useState('ALL')
  const [selectedVuln, setSelectedVuln] = useState(null)

  const fetchScan = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getScan(id)
      setScan(res.data.data)
      if (res.data.data?.vulnerabilities?.length > 0) {
        setSelectedVuln(res.data.data.vulnerabilities[0])
      }
    } catch {
      addToast('Failed to load report details', 'error')
      navigate('/reports')
    } finally {
      setLoading(false)
    }
  }, [id, addToast, navigate])

  useEffect(() => { fetchScan() }, [fetchScan])

  const handleDownload = async () => {
    setDownloading(true)
    try {
      const genRes = await generateReport(id)
      const reportId = genRes.data.data?.id
      
      const res = await downloadReport(reportId)
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      const a   = document.createElement('a')
      a.href    = url
      a.download = `aegis-report-${id}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
    } catch {
      addToast('Failed to generate and download PDF report.', 'error')
    } finally {
      setDownloading(false)
    }
  }

  const handleEnrichOne = async (vulnId) => {
    setEnriching(vulnId)
    try {
      const res = await enrichVulnerability(vulnId)
      const enriched = res.data.data
      setScan(prev => ({
        ...prev,
        vulnerabilities: prev.vulnerabilities.map(v =>
          v.id === vulnId ? { ...v, ...enriched } : v
        )
      }))
      if (selectedVuln?.id === vulnId) {
        setSelectedVuln(prev => ({ ...prev, ...enriched }))
      }
      addToast('AI enrichment applied', 'success')
    } catch {
      addToast('AI enrichment failed', 'error')
    } finally {
      setEnriching(null)
    }
  }

  const handleRetryOne = async (vulnId) => {
    setEnriching(vulnId)
    try {
      const res = await retryVulnerability(vulnId)
      const enriched = res.data.data
      setScan(prev => ({
        ...prev,
        vulnerabilities: prev.vulnerabilities.map(v =>
          v.id === vulnId ? { ...v, ...enriched } : v
        )
      }))
      if (selectedVuln?.id === vulnId) {
        setSelectedVuln(prev => ({ ...prev, ...enriched }))
      }
      addToast('AI enrichment retried successfully', 'success')
    } catch {
      addToast('AI enrichment retry failed', 'error')
    } finally {
      setEnriching(null)
    }
  }

  const handleAskAI = () => {
    navigate('/chat', { state: { scan, reportId: id, fromReport: true } })
  }

  const handleExportJSON = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(scan, null, 2))
    const a = document.createElement('a')
    a.href = dataStr
    a.download = `aegis-report-${id}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    addToast('JSON exported successfully', 'success')
  }

  const handleCopyFindings = () => {
    try {
      const vulns = scan?.vulnerabilities || []
      const text = vulns.map(v => 
        `Severity: ${v.severity}\nIssue: ${v.type}\nFile: ${v.fileName || 'Unknown'}:${v.lineNumber || 'Unknown'}\nRecommendation: ${v.recommendation || 'N/A'}\n---`
      ).join('\n')
      
      const counts = {
        CRITICAL: vulns.filter(v => v.severity === 'CRITICAL').length
      }
      
      const summary = `Security Analysis Report\nTotal Findings: ${vulns.length}\nCritical: ${counts.CRITICAL}\n\n${text}`
      
      navigator.clipboard.writeText(summary)
      addToast('✓ Findings copied successfully.', 'success')
    } catch {
      addToast('Failed to copy findings.', 'error')
    }
  }

  const handleResolve = () => {
    setIsResolved(true)
    addToast('Report marked as resolved', 'success')
  }

  if (loading) return (
    <div className="flex justify-center items-center h-full min-h-[400px]">
      <LoadingSpinner size="lg" />
    </div>
  )

  if (!scan) return null

  const vulns    = scan.vulnerabilities ?? []
  const filtered = filter === 'ALL' ? vulns : vulns.filter(v => v.severity === filter)

  const counts = {
    CRITICAL: vulns.filter(v => v.severity === 'CRITICAL').length,
    HIGH:     vulns.filter(v => v.severity === 'HIGH').length,
    MEDIUM:   vulns.filter(v => v.severity === 'MEDIUM').length,
    LOW:      vulns.filter(v => v.severity === 'LOW').length,
  }

  const fmt = (iso) => iso ? new Date(iso).toLocaleString() : '—'
  const aiConfidence = vulns.length > 0 ? (vulns.reduce((sum, v) => sum + v.confidenceScore, 0) / vulns.length * 100).toFixed(0) : 0

  const maxRisk = counts.CRITICAL > 0 ? 'Critical' : counts.HIGH > 0 ? 'High' : counts.MEDIUM > 0 ? 'Medium' : counts.LOW > 0 ? 'Low' : 'No'

  const parseEvidence = (evidenceStr) => {
    if (!evidenceStr) return null;
    try {
      return JSON.parse(evidenceStr);
    } catch {
      return null;
    }
  }

  const parsedEv = selectedVuln ? parseEvidence(selectedVuln.evidence) : null;

  const isQuickScan = scan.scanType === 'QUICK_SCAN'
  const displayFilename = scan.snippetFilename || (scan.snippetLanguage ? `Snippet.${scan.snippetLanguage}` : 'Snippet')
  const displayLanguage = scan.snippetLanguage || scan.language || 'Unknown'

  return (
    <div className="w-full space-y-lg flex flex-col h-full min-h-screen">
      {/* Header */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <style>{`
          .rd-header { display: flex; flex-direction: column; gap: 14px; }
          @media (min-width: 768px) { .rd-header { flex-direction: row; align-items: flex-end; justify-content: space-between; } }
          .rd-header-actions { display: flex; flex-direction: column; gap: 10px; }
          @media (min-width: 480px) { .rd-header-actions { flex-direction: row; align-items: center; flex-wrap: wrap; } }
          .rd-btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; border-radius: 8px; padding: 9px 16px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; min-height: 40px; }
        `}</style>
        <div className="rd-header">
          <div>
            <div className="flex items-center gap-3 mb-xs">
              <h1 className="text-headline-lg font-headline-lg text-on-surface tracking-tight">Security Analysis Report</h1>
              {isQuickScan && (
                <span style={{ display:'inline-flex', alignItems:'center', gap:5, padding:'4px 10px', borderRadius:99, fontSize:12, fontWeight:700, background:'rgba(79,134,247,0.12)', color:'#4f86f7', border:'1px solid rgba(79,134,247,0.3)', letterSpacing:'0.04em' }}>
                  <span className="material-symbols-outlined" style={{ fontSize:13, fontVariationSettings:"'FILL' 1" }}>shield</span>
                  Quick Scan
                </span>
              )}
            </div>
            <p className="text-body-lg text-on-surface-variant" style={{ maxWidth: '48rem' }}>
              {isQuickScan
                ? 'Snippet analysis — vulnerabilities, secrets, and secure coding violations found in the submitted code.'
                : 'Interactive dashboard detailing detected vulnerabilities, hardcoded secrets, and secure coding violations.'}
            </p>
          </div>
          <div className="rd-header-actions">
            <Link to="/history" className="rd-btn bg-surface border border-outline-variant/50 text-on-surface hover:bg-surface-variant transition-colors shadow-sm" style={{ textDecoration: 'none' }}>
              <FiArrowLeft size={18} />
              Back to History
            </Link>
            <button
              onClick={handleDownload}
              disabled={downloading}
              className="rd-btn bg-primary text-on-primary hover:opacity-90 transition-opacity border-t border-white/20 shadow-sm disabled:opacity-50"
            >
              {downloading ? <LoadingSpinner size="sm" /> : <FiDownload size={18} />}
              Download PDF
            </button>
          </div>
        </div>
      </div>

      {/* Main Layout Grid */}
      <div className="grid grid-cols-1 xl:grid-cols-4 gap-lg">
        {/* Left/Main Workspace (Span 3) */}
        <div className="xl:col-span-3 space-y-lg">
          
          {/* Risk Level Banner */}
          {maxRisk !== 'No' && (
            <div className={`${maxRisk === 'Critical' || maxRisk === 'High' ? 'bg-error-container/30 border-error/20' : 'bg-orange-500/10 border-orange-500/20'} border rounded-xl p-md flex items-start gap-md`}>
              <div className={`p-2 rounded-lg mt-0.5 ${maxRisk === 'Critical' || maxRisk === 'High' ? 'bg-error/10 text-error' : 'bg-orange-500/10 text-orange-500'}`}>
                <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>warning</span>
              </div>
              <div>
                <h3 className="text-title-lg font-title-lg text-on-surface mb-1">
                  {maxRisk} {isQuickScan ? 'Vulnerabilities' : 'Risk'} Detected
                </h3>
                <p className="text-body-md text-on-surface-variant">
                  {isQuickScan
                    ? `This snippet contains ${vulns.length} security ${vulns.length === 1 ? 'issue' : 'issues'}${counts.CRITICAL > 0 ? ', including critical findings' : ''}. Immediate remediation is recommended before integrating this code into your application.`
                    : `Immediate action is recommended for ${vulns.length} vulnerabilities found. Consider applying the recommended patches before deployment.`}
                </p>
              </div>
            </div>
          )}

          {/* Top Grid: Exec Summary & Stats */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-lg">
            {/* Executive Summary */}
            <div className="lg:col-span-2 bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-lg shadow-sm">
              <h2 className="text-title-lg font-title-lg text-on-surface border-b border-outline-variant/50 pb-sm mb-sm font-bold">Executive Summary</h2>
              <div className="flex flex-wrap gap-sm mb-md">
                <div className="bg-surface-variant text-on-surface-variant px-3 py-1.5 rounded-lg text-sm font-label-md border border-outline-variant/50 flex items-center gap-2 font-bold">
                  <span className={`w-2 h-2 rounded-full ${scan.status === 'COMPLETED' ? 'bg-tertiary' : 'bg-error'}`}></span>
                  Status: {scan.status}
                </div>
                <div className="bg-surface-variant text-on-surface-variant px-3 py-1.5 rounded-lg text-sm font-label-md border border-outline-variant/50 flex items-center gap-2 font-bold">
                  <span className="material-symbols-outlined text-[16px]">code</span>
                  Language: {displayLanguage}
                </div>
                <div className="bg-error-container/20 text-on-surface px-3 py-1.5 rounded-lg text-sm font-label-md border border-error/20 flex items-center gap-2 font-bold">
                  <span className="material-symbols-outlined text-[16px] text-error">shield_person</span>
                  Risk: {maxRisk}
                </div>
              </div>
              <p className="text-body-lg text-on-surface-variant leading-relaxed">
                {isQuickScan
                  ? `This code snippet contains ${maxRisk.toLowerCase()} security vulnerabilities. ${vulns.length} ${vulns.length === 1 ? 'issue was' : 'issues were'} detected, including ${counts.CRITICAL} critical ${counts.CRITICAL === 1 ? 'finding' : 'findings'}. Review and remediate them before integrating the code into your application.`
                  : `Your application has a ${maxRisk.toLowerCase()} security risk. ${vulns.length} vulnerabilities were detected, including ${counts.CRITICAL} critical issues. The AI analysis engine has high confidence in these findings based on known CVE patterns.`}
              </p>
            </div>

            {/* Score Cards Grid */}
            <div className="grid grid-cols-2 gap-sm">
              <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-sm shadow-sm flex flex-col justify-between">
                <span className="text-sm font-label-md text-on-surface-variant">Security Score</span>
                <div className="mt-2 flex items-baseline gap-1">
                  <span className="text-headline-lg font-bold text-primary">{(scan.securityScore ?? 100).toFixed(0)}</span>
                  <span className="text-body-md text-on-surface-variant">/100</span>
                </div>
              </div>
              <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-sm shadow-sm flex flex-col justify-between">
                <span className="text-sm font-label-md text-on-surface-variant">Vulnerabilities</span>
                <div className="mt-2 text-headline-lg font-bold text-on-surface">{vulns.length}</div>
              </div>
              <div className={`${counts.CRITICAL > 0 ? 'bg-error-container/20 border-error/30' : 'bg-surface-container-lowest border-outline-variant/50'} border rounded-xl p-sm shadow-sm flex flex-col justify-between`}>
                <span className={`text-sm font-label-md ${counts.CRITICAL > 0 ? 'text-error' : 'text-on-surface-variant'}`}>Critical Issues</span>
                <div className={`mt-2 text-headline-lg font-bold ${counts.CRITICAL > 0 ? 'text-error' : 'text-on-surface'}`}>{counts.CRITICAL}</div>
              </div>
              <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-sm shadow-sm flex flex-col justify-between">
                <span className="text-sm font-label-md text-on-surface-variant flex items-center gap-1">
                  <span className="material-symbols-outlined text-[14px]">verified</span>
                  Detection Confidence
                </span>
                <div className="mt-2 text-headline-lg font-bold text-tertiary">{aiConfidence}%</div>
              </div>
            </div>
          </div>

          {/* Vulnerability Table */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl shadow-sm overflow-hidden">
            <div className="p-md border-b border-outline-variant/50 flex justify-between items-center bg-surface-bright">
              <h2 className="text-title-lg font-title-lg text-on-surface font-bold">Detected Vulnerabilities</h2>
              <div className="flex gap-2">
                <select 
                  value={filter} 
                  onChange={e => setFilter(e.target.value)}
                  className="bg-surface border border-outline-variant/50 text-on-surface rounded-lg py-1.5 px-3 text-sm font-label-md outline-none"
                >
                  <option value="ALL">All Severities</option>
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
            </div>
            
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-outline-variant/50 text-[11px] font-code text-on-surface-variant uppercase tracking-wider bg-surface-container-low">
                    <th className="py-3 px-6 font-medium">Severity</th>
                    <th className="py-3 px-6 font-medium">Vulnerability Name</th>
                    <th className="py-3 px-6 font-medium">Affected Line</th>
                    <th className="py-3 px-6 font-medium text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="text-sm">
                  {filtered.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="p-0">
                        <EmptyState 
                          icon="security"
                          title={filter !== 'ALL' ? "No vulnerabilities match filter" : "No vulnerabilities found"}
                          description={filter !== 'ALL' ? "Try changing the severity filter." : "This scan did not detect any vulnerabilities."}
                        />
                      </td>
                    </tr>
                  ) : filtered.map(v => (
                    <tr 
                      key={v.id} 
                      onClick={() => setSelectedVuln(v)}
                      className={`border-b border-outline-variant/50 hover:bg-primary/5 cursor-pointer transition-colors group ${selectedVuln?.id === v.id ? 'bg-primary/5' : ''}`}
                    >
                      <td className="py-3.5 px-6">
                        <span className={`inline-flex items-center px-2.5 py-1 rounded-md text-[11px] font-bold shadow-sm ${SEV_STYLE[v.severity]}`}>
                          {v.severity}
                        </span>
                      </td>
                      <td className="py-3.5 px-6 font-medium text-on-surface group-hover:text-primary transition-colors">{v.type}</td>
                      <td className="py-3.5 px-6 font-code text-on-surface-variant">
                        {v.lineNumber
                          ? <span className="bg-surface-variant/30 rounded px-2">
                              {isQuickScan
                                ? (displayFilename !== 'Snippet' ? `${displayFilename} : Line ${v.lineNumber}` : `Line ${v.lineNumber}`)
                                : `${v.fileName || ''}:${v.lineNumber}`}
                            </span>
                          : v.fileName ? <span className="bg-surface-variant/30 rounded px-2">{isQuickScan ? displayFilename : v.fileName}</span> : '-'}
                      </td>
                      <td className="py-3.5 px-6 text-right">
                        <button className="text-primary hover:text-primary-container font-label-md text-sm font-bold flex items-center justify-end w-full gap-1">
                          Review <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Code Preview Split Pane */}
          {selectedVuln && selectedVuln.codeSnippet && (
            <div className="bg-[#1e1e1e] rounded-xl border border-outline-variant/50 shadow-sm overflow-hidden flex flex-col">
              <div className="bg-[#2d2d2d] px-4 py-2.5 border-b border-[#404040] flex items-center justify-between">
                <div className="flex gap-2">
                  <div className="w-3 h-3 rounded-full bg-[#ff5f56]"></div>
                  <div className="w-3 h-3 rounded-full bg-[#ffbd2e]"></div>
                  <div className="w-3 h-3 rounded-full bg-[#27c93f]"></div>
                </div>
                <span className="text-[#cccccc] text-xs font-code">
                  {isQuickScan ? displayFilename : selectedVuln.fileName} — {selectedVuln.type}
                </span>
                <button className="text-[#cccccc] hover:text-white" onClick={() => navigator.clipboard.writeText(selectedVuln.codeSnippet)}>
                  <span className="material-symbols-outlined text-[16px]">content_copy</span>
                </button>
              </div>
              <div className="flex flex-col divide-y divide-[#404040]">
                {/* Original Code Snippet — with line numbers */}
                <div className="overflow-x-auto">
                  <div className="text-[#858585] px-5 pt-4 pb-2 uppercase text-[10px] tracking-wider font-bold">Original Code</div>
                  <div className="pb-4">
                    {selectedVuln.codeSnippet.split('\n').map((line, idx) => (
                      <div key={idx} className={`flex gap-0 text-sm font-code leading-relaxed group hover:bg-[#ffffff08] ${
                        selectedVuln.lineNumber && (idx + 1) === selectedVuln.lineNumber ? 'bg-error/10 border-l-2 border-error' : ''
                      }`}>
                        <span className="select-none w-10 shrink-0 text-right pr-4 text-[#444] group-hover:text-[#666] text-xs pt-0.5 pl-2">{idx + 1}</span>
                        <pre className="text-[#cccccc] whitespace-pre overflow-x-auto max-w-full">{line}</pre>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Detection Summary */}
                <div className="p-5 text-sm font-sans text-[#cccccc] leading-relaxed bg-[#252525]">
                  <div className="text-[#858585] mb-3 uppercase text-[10px] tracking-wider font-bold">Detection Summary</div>
                  <p className="text-[#e2e2e2] font-semibold mb-2">{selectedVuln.description}</p>
                  
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
                    <div className="bg-[#1e1e1e] border border-[#404040] rounded-lg p-3">
                      <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">Severity</div>
                      <div className={`font-bold ${selectedVuln.severity === 'CRITICAL' ? 'text-error' : selectedVuln.severity === 'HIGH' ? 'text-[#ffb4a9]' : 'text-secondary'}`}>
                        {selectedVuln.severity}
                      </div>
                    </div>
                    <div className="bg-[#1e1e1e] border border-[#404040] rounded-lg p-3">
                      <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">Detection Confidence</div>
                      <div className="font-bold text-tertiary">{(selectedVuln.confidenceScore * 100).toFixed(0)}%</div>
                    </div>
                    <div className="bg-[#1e1e1e] border border-[#404040] rounded-lg p-3">
                      <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">OWASP</div>
                      <div className="font-bold text-[#e2e2e2] truncate" title={selectedVuln.owaspCategory || 'N/A'}>{selectedVuln.owaspCategory || 'N/A'}</div>
                    </div>
                    <div className="bg-[#1e1e1e] border border-[#404040] rounded-lg p-3">
                      <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">CWE</div>
                      <div className="font-bold text-[#e2e2e2] truncate">{selectedVuln.cweId || 'N/A'}</div>
                    </div>
                  </div>
                </div>

                {/* Detection Evidence (Deterministic) */}
                {parsedEv && (
                  <div className="p-5 text-sm font-sans text-[#cccccc] leading-relaxed bg-[#1e1e1e]">
                    <div className="text-[#858585] mb-4 uppercase text-[10px] tracking-wider font-bold">Detection Evidence</div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                      <div className="space-y-4">
                        <div>
                          <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">Rule ID</div>
                          <div className="font-code text-primary font-bold">{parsedEv.ruleId}</div>
                        </div>
                        <div>
                          <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">Detection Type</div>
                          <div className="inline-flex items-center px-2 py-0.5 rounded bg-surface-variant text-on-surface-variant font-code text-xs">
                            {parsedEv.detectionType}
                          </div>
                        </div>
                      </div>
                      
                      <div className="space-y-4">
                         <div>
                          <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-1">Matched Rules</div>
                          <ul className="space-y-1">
                            {parsedEv.matchedRules?.map((rule, idx) => (
                              <li key={idx} className="flex items-center gap-2 text-green-400 font-code text-xs">
                                <span className="material-symbols-outlined text-[14px]">check</span> {rule}
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    </div>

                    <div className="mb-6">
                      <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mb-3">Detection Flow</div>
                      <div className="bg-[#121212] border border-[#333] p-4 rounded-lg flex flex-col items-center">
                        {/* Source */}
                        <div className="bg-[#2d2d2d] border border-[#444] px-4 py-2 rounded-md w-full max-w-[400px] text-center">
                          <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider">Source</div>
                          <div className="font-code text-primary mt-1">{parsedEv.source}</div>
                        </div>
                        
                        <div className="h-6 w-px bg-[#555]"></div>
                        <span className="material-symbols-outlined text-[#555] text-[16px] -mt-2 -mb-2">keyboard_arrow_down</span>
                        <div className="h-6 w-px bg-[#555]"></div>

                        {/* Propagation */}
                        <div className="bg-[#2d2d2d] border border-[#444] px-4 py-2 rounded-md w-full max-w-[400px] text-center">
                          <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider">Propagation</div>
                          <div className="font-code text-orange-400 mt-1">{parsedEv.propagation}</div>
                        </div>

                        <div className="h-6 w-px bg-[#555]"></div>
                        <span className="material-symbols-outlined text-[#555] text-[16px] -mt-2 -mb-2">keyboard_arrow_down</span>
                        <div className="h-6 w-px bg-[#555]"></div>

                        {/* Sink */}
                        <div className="bg-[#2d2d2d] border border-[#444] px-4 py-2 rounded-md w-full max-w-[400px] text-center">
                          <div className="text-[#858585] text-[10px] uppercase font-bold tracking-wider">Sink</div>
                          <div className="font-code text-error mt-1 break-all">{parsedEv.sink}</div>
                        </div>
                        
                        <div className="h-6 w-px bg-[#555]"></div>
                        <span className="material-symbols-outlined text-[#555] text-[16px] -mt-2 -mb-2">keyboard_arrow_down</span>
                        <div className="h-6 w-px bg-[#555]"></div>

                        <div className="bg-error/10 border border-error/20 px-4 py-2 rounded-md w-full max-w-[400px] text-center">
                          <div className="font-code text-error font-bold">{selectedVuln.type}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Why It Was Detected */}
                {parsedEv && (
                  <div className="p-5 text-sm font-sans text-[#cccccc] leading-relaxed bg-[#252525]">
                    <div className="text-[#858585] mb-2 uppercase text-[10px] tracking-wider font-bold">Why It Was Detected</div>
                    <p className="text-[#e2e2e2] leading-relaxed">{parsedEv.reason}</p>
                    <div className="mt-3 bg-surface-variant/20 p-3 rounded border border-outline-variant/30">
                      <span className="text-[#858585] text-[10px] uppercase font-bold tracking-wider mr-2">Confidence Reason:</span>
                      <span className="text-[#cccccc]">{parsedEv.confidenceReason}</span>
                    </div>
                  </div>
                )}

                {/* AI Enrichment Section */}
                <div className="p-5 text-sm font-sans text-[#cccccc] leading-relaxed bg-[#3a2a2a]/30">
                  <div className="flex items-center justify-between mb-4">
                    <div className="text-tertiary uppercase text-[10px] tracking-wider flex items-center gap-1 font-bold">
                      <span className="material-symbols-outlined text-[14px]">psychology</span> AI Deep Analysis
                    </div>
                    {!selectedVuln.aiExplanation && selectedVuln.aiStatus !== 'FAILED' && (
                      <button onClick={() => handleEnrichOne(selectedVuln.id)} disabled={enriching === selectedVuln.id} className="text-xs bg-tertiary text-on-tertiary px-3 py-1.5 rounded-lg hover:bg-tertiary/90 transition-colors font-bold shadow-sm flex items-center gap-1">
                        {enriching === selectedVuln.id ? <LoadingSpinner size="sm" /> : null}
                        {enriching === selectedVuln.id ? 'Analyzing...' : 'Generate AI Report'}
                      </button>
                    )}
                    {selectedVuln.aiStatus === 'FAILED' && (
                      <button onClick={() => handleRetryOne(selectedVuln.id)} disabled={enriching === selectedVuln.id} className="text-xs bg-error text-on-error px-3 py-1.5 rounded-lg hover:bg-error/90 transition-colors font-bold shadow-sm flex items-center gap-1">
                        {enriching === selectedVuln.id ? <LoadingSpinner size="sm" /> : <span className="material-symbols-outlined text-[14px]">refresh</span>}
                        {enriching === selectedVuln.id ? 'Retrying...' : 'Retry AI Analysis'}
                      </button>
                    )}
                  </div>
                  
                  {selectedVuln.aiExplanation ? (
                    <div className="space-y-6 text-[13px] text-slate-300">
                      {/* Technical Explanation */}
                      <div>
                        <div className="text-slate-400 text-[10px] uppercase font-bold tracking-wider mb-1">Technical Explanation</div>
                        <div className="bg-[#1e1e1e] p-3 rounded-lg border border-[#333] markdown-body text-slate-300">
                          <ReactMarkdown>{cleanAiText(selectedVuln.aiExplanation)}</ReactMarkdown>
                        </div>
                      </div>

                      {/* Root Cause */}
                      {selectedVuln.rootCause && (
                        <div>
                          <div className="text-slate-400 text-[10px] uppercase font-bold tracking-wider mb-1">Root Cause</div>
                          <div className="bg-[#1e1e1e] p-3 rounded-lg border border-[#333] markdown-body text-slate-300">
                            <ReactMarkdown>{cleanAiText(selectedVuln.rootCause)}</ReactMarkdown>
                          </div>
                        </div>
                      )}

                      {/* Business Impact */}
                      {selectedVuln.businessImpact && (
                        <div>
                          <div className="text-slate-400 text-[10px] uppercase font-bold tracking-wider mb-1">Business Impact</div>
                          <div className="bg-error-container/10 p-3 rounded-lg border border-error/20 markdown-body text-[#ffb4a9]">
                            <ReactMarkdown>{cleanAiText(selectedVuln.businessImpact)}</ReactMarkdown>
                          </div>
                        </div>
                      )}

                      {/* Remediation */}
                      {selectedVuln.aiRecommendation && (
                        <div>
                          <div className="text-slate-400 text-[10px] uppercase font-bold tracking-wider mb-1">Remediation</div>
                          <div className="bg-[#1e1e1e] p-3 rounded-lg border border-[#333] markdown-body text-slate-300">
                            <ReactMarkdown>{cleanAiText(selectedVuln.aiRecommendation)}</ReactMarkdown>
                          </div>
                        </div>
                      )}

                      {/* Secure Code */}
                      {selectedVuln.secureCodeExample && (
                        <div>
                          <div className="text-green-400 text-[10px] uppercase font-bold tracking-wider mb-1">Secure Code</div>
                          <pre className="bg-[#121212] p-4 rounded-lg border border-green-500/20 text-green-300 font-code text-[13px] mt-1 overflow-x-auto shadow-inner">
                            <code>{selectedVuln.secureCodeExample}</code>
                          </pre>
                        </div>
                      )}

                      {/* References */}
                      <div>
                        <div className="text-slate-400 text-[10px] uppercase font-bold tracking-wider mb-1">References</div>
                        <div className="flex gap-4 bg-[#1e1e1e] p-3 rounded-lg border border-[#333]">
                          {selectedVuln.owaspCategory && <span className="font-bold text-primary hover:underline cursor-pointer">OWASP {selectedVuln.owaspCategory}</span>}
                          {selectedVuln.cweId && <span className="font-bold text-primary hover:underline cursor-pointer">{selectedVuln.cweId}</span>}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="text-[#858585] italic text-center py-6">AI analysis is pending. Click 'Generate AI Report' to enrich this finding.</div>
                  )}
                </div>
              </div>
            </div>
          )}

        </div>

        {/* Right Sidebar Panel (Span 1) */}
        <div className="space-y-lg">

          {/* Snippet Information Card — Quick Scan only */}
          {isQuickScan && (
            <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl shadow-sm overflow-hidden">
              <div className="p-lg border-b border-outline-variant/50 flex items-center gap-2">
                <span className="material-symbols-outlined text-primary" style={{ fontSize:20, fontVariationSettings:"'FILL' 1" }}>info</span>
                <h2 className="text-title-lg font-title-lg text-on-surface font-bold">Snippet Information</h2>
              </div>
              <div className="p-lg flex flex-col gap-md">
                {[{
                  label: 'Filename',
                  value: displayFilename,
                  icon: 'insert_drive_file'
                }, {
                  label: 'Language',
                  value: displayLanguage.charAt(0).toUpperCase() + displayLanguage.slice(1),
                  icon: 'code'
                }, {
                  label: 'Lines',
                  value: scan.snippetLines ?? '—',
                  icon: 'format_list_numbered'
                }, {
                  label: 'Scanned At',
                  value: fmt(scan.scanStart),
                  icon: 'schedule'
                }, {
                  label: 'Scan Type',
                  value: 'Quick Scan',
                  icon: 'shield'
                }].map(({ label, value, icon }) => (
                  <div key={label} className="flex items-start gap-3">
                    <span className="material-symbols-outlined text-on-surface-variant shrink-0" style={{ fontSize:16, marginTop:2 }}>{icon}</span>
                    <div>
                      <div className="text-[11px] font-bold uppercase tracking-wider text-on-surface-variant mb-0.5">{label}</div>
                      <div className="text-sm font-medium text-on-surface break-all">{value}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Security Action Center */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl shadow-sm overflow-hidden">
            <div className="p-lg border-b border-outline-variant/50">
              <h2 className="text-title-lg font-title-lg text-on-surface font-bold flex items-center gap-2">
                <FiShield className="text-primary" size={24} />
                Security Action Center
              </h2>
              <p className="text-body-md text-on-surface-variant mt-1">Take the next recommended actions after reviewing this security assessment.</p>
            </div>
            
            {/* Status Summary */}
            <div className="p-lg border-b border-outline-variant/50 bg-surface-container-low/30">
              <div className="flex flex-col gap-4">
                <div className="flex gap-4">
                  <div className="bg-surface border border-outline-variant/50 px-4 py-3 rounded-lg flex flex-col flex-1 shadow-sm">
                    <span className="text-[11px] uppercase tracking-wider font-bold text-on-surface-variant mb-1">Risk</span>
                    <span className={`text-lg font-bold ${maxRisk === 'Critical' || maxRisk === 'High' ? 'text-error' : maxRisk === 'Medium' ? 'text-secondary' : 'text-tertiary'}`}>{maxRisk}</span>
                  </div>
                  <div className="bg-surface border border-outline-variant/50 px-4 py-3 rounded-lg flex flex-col flex-1 shadow-sm">
                    <span className="text-[11px] uppercase tracking-wider font-bold text-on-surface-variant mb-1">Findings</span>
                    <span className="text-lg font-bold text-on-surface">{vulns.length}</span>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="bg-surface border border-outline-variant/50 px-4 py-3 rounded-lg flex flex-col flex-1 shadow-sm">
                    <span className="text-[11px] uppercase tracking-wider font-bold text-on-surface-variant mb-1">Score</span>
                    <span className="text-lg font-bold text-primary">{(scan.securityScore ?? 100).toFixed(0)}</span>
                  </div>
                  <div className="bg-surface border border-outline-variant/50 px-4 py-3 rounded-lg flex flex-col flex-1 shadow-sm">
                    <span className="text-[11px] uppercase tracking-wider font-bold text-on-surface-variant mb-1">Critical</span>
                    <span className={`text-lg font-bold ${counts.CRITICAL > 0 ? 'text-error' : 'text-on-surface'}`}>{counts.CRITICAL}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Action Grid */}
            <div className="p-lg grid grid-cols-1 gap-md">
              {/* Download PDF */}
              <div className="group flex flex-col justify-between bg-surface border border-outline-variant/50 p-6 rounded-xl hover:border-primary/40 hover:shadow-md transition-all duration-200">
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                    <FiDownload className="text-primary" size={20} />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-on-surface group-hover:text-primary transition-colors">Download PDF</h3>
                    <p className="text-sm text-on-surface-variant mt-1 leading-relaxed">Download the professional security report.</p>
                  </div>
                </div>
                <button onClick={handleDownload} disabled={downloading} className="w-full bg-primary text-on-primary hover:opacity-90 transition-all duration-200 rounded-lg py-2.5 text-sm font-bold flex justify-center items-center gap-2 shadow-sm disabled:opacity-50">
                  {downloading ? 'Preparing download...' : 'Download'}
                </button>
              </div>

              {/* Ask AI Assistant */}
              <div className="group flex flex-col justify-between bg-surface border border-outline-variant/50 p-6 rounded-xl hover:border-tertiary/40 hover:shadow-md transition-all duration-200">
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-tertiary/10 flex items-center justify-center shrink-0">
                    <FiMessageSquare className="text-tertiary" size={20} />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-on-surface group-hover:text-tertiary transition-colors">Ask AI Assistant</h3>
                    <p className="text-sm text-on-surface-variant mt-1 leading-relaxed">Open the AI Assistant already connected to this report.</p>
                  </div>
                </div>
                <button onClick={handleAskAI} className="w-full bg-surface-container text-on-surface border border-outline-variant hover:bg-surface-container-high transition-all duration-200 rounded-lg py-2.5 text-sm font-bold shadow-sm">
                  Open Assistant
                </button>
              </div>

              {/* Export JSON */}
              <div className="group flex flex-col justify-between bg-surface border border-outline-variant/50 p-6 rounded-xl hover:border-secondary/40 hover:shadow-md transition-all duration-200">
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-secondary/10 flex items-center justify-center shrink-0">
                    <FiCode className="text-secondary" size={20} />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-on-surface group-hover:text-secondary transition-colors">Export JSON</h3>
                    <p className="text-sm text-on-surface-variant mt-1 leading-relaxed">Download the report as structured JSON.</p>
                  </div>
                </div>
                <button onClick={handleExportJSON} className="w-full bg-surface-container text-on-surface border border-outline-variant hover:bg-surface-container-high transition-all duration-200 rounded-lg py-2.5 text-sm font-bold shadow-sm">
                  Export
                </button>
              </div>

              {/* Copy Findings */}
              <div className="group flex flex-col justify-between bg-surface border border-outline-variant/50 p-6 rounded-xl hover:border-orange-500/40 hover:shadow-md transition-all duration-200">
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-orange-500/10 flex items-center justify-center shrink-0">
                    <FiCopy className="text-orange-500" size={20} />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-on-surface group-hover:text-orange-500 transition-colors">Copy Findings</h3>
                    <p className="text-sm text-on-surface-variant mt-1 leading-relaxed">Copy a concise vulnerability summary to clipboard.</p>
                  </div>
                </div>
                <button onClick={handleCopyFindings} className="w-full bg-surface-container text-on-surface border border-outline-variant hover:bg-surface-container-high transition-all duration-200 rounded-lg py-2.5 text-sm font-bold shadow-sm">
                  Copy to Clipboard
                </button>
              </div>

              {/* Mark as Resolved */}
              <div className="group flex flex-col justify-between bg-surface border border-outline-variant/50 p-6 rounded-xl hover:border-green-500/40 hover:shadow-md transition-all duration-200">
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-green-500/10 flex items-center justify-center shrink-0">
                    <FiCheckCircle className="text-green-500" size={20} />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-on-surface group-hover:text-green-600 transition-colors flex items-center gap-2">
                      Mark as Resolved
                      {isResolved && <span className="bg-green-500/20 text-green-700 text-[11px] uppercase tracking-wider px-2 py-0.5 rounded-md font-bold">Resolved</span>}
                    </h3>
                    <p className="text-sm text-on-surface-variant mt-1 leading-relaxed">Mark this report as resolved.</p>
                  </div>
                </div>
                <button onClick={handleResolve} disabled={isResolved} className={`w-full transition-all duration-200 rounded-lg py-2.5 text-sm font-bold shadow-sm ${isResolved ? 'bg-green-500/10 text-green-700 border border-green-500/20 cursor-not-allowed' : 'bg-surface-container text-on-surface border border-outline-variant hover:bg-surface-container-high'}`}>
                  {isResolved ? 'Resolved' : 'Mark as Resolved'}
                </button>
              </div>
            </div>
          </div>
          
          {/* Severity Distribution */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl shadow-sm p-lg flex flex-col">
            <h2 className="text-title-lg font-title-lg text-on-surface border-b border-outline-variant/50 pb-sm mb-md font-bold">Severity Distribution</h2>
            <div className="flex-1 flex flex-col justify-center gap-lg">
              
              <div className="flex items-center justify-between gap-md">
                <div className="w-16 h-16 rounded-full border-[6px] border-surface-variant relative shrink-0">
                  <div className="absolute inset-0 rounded-full border-[6px] border-error" style={{clipPath: 'polygon(50% 50%, 50% 0, 100% 0, 100% 50%)'}}></div>
                </div>
                <div className="flex-1 space-y-2">
                  <div className="flex justify-between text-sm"><span className="text-error font-bold">Critical</span><span className="font-bold text-on-surface">{counts.CRITICAL}</span></div>
                  <div className="w-full bg-surface-variant h-1.5 rounded-full overflow-hidden"><div className="bg-error h-full" style={{width: vulns.length ? `${(counts.CRITICAL/vulns.length)*100}%` : '0%'}}></div></div>
                </div>
              </div>
              
              <div className="flex items-center justify-between gap-md">
                <div className="flex-1 space-y-2">
                  <div className="flex justify-between text-sm"><span className="text-[#ffb4a9] font-bold">High</span><span className="font-bold text-on-surface">{counts.HIGH}</span></div>
                  <div className="w-full bg-surface-variant h-1.5 rounded-full overflow-hidden"><div className="bg-[#ffb4a9] h-full" style={{width: vulns.length ? `${(counts.HIGH/vulns.length)*100}%` : '0%'}}></div></div>
                </div>
              </div>
              
              <div className="flex items-center justify-between gap-md">
                <div className="flex-1 space-y-2">
                  <div className="flex justify-between text-sm"><span className="text-secondary font-bold">Medium</span><span className="font-bold text-on-surface">{counts.MEDIUM}</span></div>
                  <div className="w-full bg-surface-variant h-1.5 rounded-full overflow-hidden"><div className="bg-secondary h-full" style={{width: vulns.length ? `${(counts.MEDIUM/vulns.length)*100}%` : '0%'}}></div></div>
                </div>
              </div>

              <div className="flex items-center justify-between gap-md">
                <div className="flex-1 space-y-2">
                  <div className="flex justify-between text-sm"><span className="text-on-surface-variant font-bold">Low</span><span className="font-bold text-on-surface">{counts.LOW}</span></div>
                  <div className="w-full bg-surface-variant h-1.5 rounded-full overflow-hidden"><div className="bg-outline-variant h-full" style={{width: vulns.length ? `${(counts.LOW/vulns.length)*100}%` : '0%'}}></div></div>
                </div>
              </div>

            </div>
          </div>

          {/* Timeline */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl shadow-sm p-lg">
            <h2 className="text-title-lg font-title-lg text-on-surface border-b border-outline-variant/50 pb-sm mb-lg font-bold">Analysis Timeline</h2>
            <div className="relative pl-6 space-y-lg">
              <div className="absolute left-[11px] top-2 bottom-2 w-px bg-outline-variant/50"></div>
              
              <div className="relative flex items-start gap-4">
                <div className="absolute -left-6 w-5 h-5 rounded-full bg-surface border-2 border-primary flex items-center justify-center z-10 mt-0.5">
                  <div className="w-2 h-2 rounded-full bg-primary"></div>
                </div>
                <div>
                  <h4 className="text-[13px] font-bold text-on-surface">Analysis Started</h4>
                  <p className="text-[11px] text-on-surface-variant mt-0.5">{fmt(scan.scanStart)}</p>
                </div>
              </div>

              <div className="relative flex items-start gap-4">
                <div className="absolute -left-6 w-5 h-5 rounded-full bg-surface border-2 border-primary flex items-center justify-center z-10 mt-0.5">
                  <div className="w-2 h-2 rounded-full bg-primary"></div>
                </div>
                <div>
                  <h4 className="text-[13px] font-bold text-on-surface">AI Processing</h4>
                  <p className="text-[11px] text-on-surface-variant mt-0.5">{vulns.length} findings parsed</p>
                </div>
              </div>

              <div className="relative flex items-start gap-4">
                <div className="absolute -left-6 w-5 h-5 rounded-full bg-primary border-2 border-primary flex items-center justify-center z-10 mt-0.5 shadow-[0_0_8px_rgba(0,88,190,0.5)]">
                  <span className="material-symbols-outlined text-[12px] text-white font-bold">check</span>
                </div>
                <div>
                  <h4 className="text-[13px] font-bold text-primary">Report Generated</h4>
                  <p className="text-[11px] text-on-surface-variant mt-0.5">Took {typeof scan.durationSeconds === 'number' ? scan.durationSeconds.toFixed(2) : (scan.durationSeconds ?? 0)}s</p>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  )
}
