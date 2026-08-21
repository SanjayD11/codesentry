import React, { useState, useEffect, useCallback } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getAdminStats, getSystemHealth } from '../../api/adminApi';
import { exportPlatformData } from '../../api/adminApi';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

// Simple debounce hook
function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  return debouncedValue;
}

// React-friendly animated number counter matching the HTML's behavior
function AnimatedNumber({ value }) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    let start = 0;
    const end = parseInt(value, 10) || 0;
    if (end === 0) {
      setCount(0);
      return;
    }
    const duration = 1000;
    const startTime = performance.now();
    let animationFrame;
    
    const animate = (timestamp) => {
      const elapsed = timestamp - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // easeOutCubic
      setCount(Math.floor(end * eased));
      if (progress < 1) {
        animationFrame = requestAnimationFrame(animate);
      }
    };
    
    animationFrame = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationFrame);
  }, [value]);
  
  return <span>{count.toLocaleString()}</span>;
}

export default function AdminDashboard() {
  const { user } = useAuth();
  const addToast = useToast();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  
  const [stats, setStats] = useState({
    totalUsers: 0, activeUsers: 0, disabledUsers: 0, usersRegisteredToday: 0,
    totalProjects: 0, activeProjects: 0, archivedProjects: 0, projectsCreatedToday: 0,
    totalScans: 0, completedScans: 0, failedScans: 0, scansToday: 0,
    criticalVulnerabilities: 0, highVulnerabilities: 0, mediumVulnerabilities: 0, lowVulnerabilities: 0,
    averageScanScore: 100, averageFindings: 0, averageScanDuration: 0,
    totalReports: 0, aiReportsGenerated: 0, aiRequests: 0
  });
  
  const [health, setHealth] = useState({
    backendStatus: 'Unknown',
    databaseStatus: 'Unknown',
    aiProviderStatus: 'Unknown',
    storageUsage: 'Unknown'
  });
  
  // Export Modal State
  const [showExportModal, setShowExportModal] = useState(false);
  const [exportForm, setExportForm] = useState({
    datasets: {
      users: true,
      projects: true,
      scans: false,
      reports: false,
      vulnerabilities: false,
      auditLogs: false,
      summary: false
    },
    format: 'csv'
  });
  const [isExporting, setIsExporting] = useState(false);

  const loadAllData = useCallback(async () => {
    try {
      const [statsData, healthData] = await Promise.all([
        getAdminStats(),
        getSystemHealth()
      ]);
      setStats(statsData);
      setHealth(healthData);
    } catch (err) {
      addToast('Failed to load admin dashboard data', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    if (user?.role !== 'ADMIN') return;
    loadAllData();
    const intervalId = setInterval(loadAllData, 30000);
    return () => clearInterval(intervalId);
  }, [user, loadAllData]);

  const handleExportSubmit = async () => {
    try {
      setIsExporting(true);
      const activeDatasets = Object.entries(exportForm.datasets)
        .filter(([_, isActive]) => isActive)
        .map(([key, _]) => key);
        
      if (activeDatasets.length === 0) {
        addToast('Please select at least one dataset to export.', 'error');
        return;
      }
      
      addToast('Export started. Your download will begin shortly.', 'info');
      
      const response = await exportPlatformData(activeDatasets, exportForm.format);
      
      const contentDisposition = response.headers.get('Content-Disposition');
      let filename = `codesentry-export.${exportForm.format === 'excel' ? 'xlsx' : exportForm.format}`;
      if (contentDisposition && contentDisposition.indexOf('attachment') !== -1) {
        const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
        const matches = filenameRegex.exec(contentDisposition);
        if (matches != null && matches[1]) {
          filename = matches[1].replace(/['"]/g, '');
        }
      }
      
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      
      addToast('Export completed successfully.', 'success');
      setShowExportModal(false);
    } catch (err) {
      addToast('Failed to export platform data. Please try again.', 'error');
    } finally {
      setIsExporting(false);
    }
  };

  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;
  if (loading) return <div className="h-full flex items-center justify-center min-h-[400px]"><LoadingSpinner size="lg" /></div>;

  return (
    <div className="admin-dashboard-root fade-in">
      <style>{`
        .admin-dashboard-root {
          --surface-container-low: #f0f3ff;
          --outline: #727785;
          --primary-container: #2170e4;
          --inverse-on-surface: #ecf1ff;
          --surface-container-highest: #d8e3fb;
          --background: #f9f9ff;
          --secondary-container: #d0e1fb;
          --primary: #0058be;
          --primary-fixed: #d8e2ff;
          --surface-container-lowest: #ffffff;
          --on-primary-container: #fefcff;
          --tertiary-fixed-dim: #4edea3;
          --inverse-surface: #263143;
          --surface-container-high: #dee8ff;
          --tertiary-container: #00855b;
          --tertiary: #006947;
          --on-surface-variant: #424754;
          --on-surface: #111c2d;
          --secondary: #505f76;
          --on-secondary-container: #54647a;
          --error: #ba1a1a;
          --outline-variant: #c2c6d6;
          --error-container: #ffdad6;
          --on-primary: #ffffff;
          --on-tertiary-container: #f5fff6;
          --on-error-container: #93000a;
          --surface-container: #e7eeff;
          --secondary-fixed: #d3e4fe;
          --primary-fixed-dim: #adc6ff;
          --on-primary-fixed-variant: #004395;

          --radius: .25rem; --radius-lg: .6rem; --radius-xl: 1rem; --radius-full: 9999px;
          --xs: 4px; --sm: 8px; --md: 16px; --lg: 24px; --xl: 32px;

          --font-display: 'Plus Jakarta Sans', sans-serif;
          --font-body: 'Manrope', sans-serif;
          --font-code: 'Geist', monospace;

          --shadow-sm: 0 4px 12px rgba(17,28,45,.02), 0 1px 3px rgba(17,28,45,.02);
        }

        .admin-dashboard-root {
          display: flex;
          flex-direction: column;
          gap: 20px;
          width: 100%;
          font-family: var(--font-body);
          color: var(--on-surface);
        }

        .page-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          flex-wrap: wrap;
          gap: 16px;
        }

        .page-title-group h1 {
          font-family: var(--font-display);
          font-weight: 800;
          font-size: 26px;
          margin: 0;
          color: var(--on-surface);
          letter-spacing: -0.02em;
        }

        .page-title-group p {
          color: var(--on-surface-variant);
          margin: 4px 0 0 0;
          font-size: 14px;
        }

        .header-actions {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          width: auto;
        }

        .btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          padding: 8px 16px;
          border-radius: var(--radius-full);
          font-family: var(--font-display);
          font-weight: 700;
          font-size: 13.5px;
          cursor: pointer;
          transition: all 0.2s ease;
          border: none;
          white-space: nowrap;
        }

        .btn-outline {
          background: var(--surface-container-lowest);
          border: 1px solid var(--outline-variant);
          color: var(--on-surface);
        }

        .btn-outline:hover {
          background: var(--surface-container-low);
        }

        .btn-primary {
          background: var(--primary);
          color: var(--on-primary);
        }

        .btn-primary:hover {
          background: var(--on-primary-fixed-variant);
          box-shadow: 0 4px 12px rgba(0, 88, 190, 0.2);
        }

        .panel {
          background: #ffffff;
          border-radius: 16px;
          border: 1px solid #e2e8f0;
          padding: 20px;
          box-shadow: 0 1px 3px rgba(15,23,42,0.04);
          transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }

        .panel:hover {
          border-color: #93c5fd;
          box-shadow: 0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04);
        }

        .panel-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: var(--lg);
        }
        
        .panel-header .titles .eyebrow {
          font-size: 11px;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          font-weight: 700;
          color: var(--on-surface-variant);
          margin-bottom: 2px;
          display: block;
        }

        .panel-title {
          font-family: var(--font-display);
          font-size: 18px;
          font-weight: 700;
          margin: 0;
        }

        .stats-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
          gap: 14px;
        }

        .stats-grid-compact {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(min(100%, 160px), 1fr));
          gap: 12px;
        }

        .stat-card {
          display: flex;
          flex-direction: column;
          gap: 14px;
          background: #ffffff;
          border: 1px solid #e2e8f0;
          border-radius: 14px;
          padding: 18px;
          box-shadow: 0 1px 3px rgba(15,23,42,0.04);
          transition: border-color 0.2s ease, box-shadow 0.2s ease;
          min-width: 0;
        }

        .stat-card:hover {
          background: #ffffff;
          border-color: #93c5fd;
          box-shadow: 0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04);
        }

        .stat-top {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .stat-icon {
          width: 40px;
          height: 40px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        
        .stat-icon span { font-size: 20px; font-variation-settings: 'FILL' 1; }
        .stat-icon.primary { background: var(--primary-fixed); color: var(--primary); }
        .stat-icon.secondary { background: var(--secondary-fixed); color: var(--secondary); }
        .stat-icon.tertiary { background: var(--tertiary-fixed-dim); color: var(--tertiary); }
        .stat-icon.error { background: var(--error-container); color: var(--error); }

        .stat-value {
          font-family: var(--font-display);
          font-weight: 800;
          font-size: 28px;
          color: #111c2d;
          margin: 0;
          line-height: 1.1;
          letter-spacing: -0.01em;
          word-break: break-word;
        }

        .stat-label {
          color: var(--on-surface-variant);
          font-size: 13px;
          font-weight: 600;
          margin: 4px 0 0 0;
        }
        
        .stat-subtext {
          font-size: 12px;
          margin-top: 8px;
          font-weight: 600;
          display: flex;
          gap: 10px;
          flex-wrap: wrap;
        }

        .sys-health-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 16px;
          padding: 18px 16px;
          border: 1px solid #f0f3ff;
          border-radius: 12px;
          margin-top: 16px;
        }

        .sys-health-item {
          display: flex;
          align-items: center;
          gap: 10px;
          min-width: 0;
        }

        .sys-health-item .dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          flex-shrink: 0;
        }

        .sys-health-item .dot.up { background: var(--tertiary); box-shadow: 0 0 8px var(--tertiary); }
        .sys-health-item .dot.down { background: var(--error); box-shadow: 0 0 8px var(--error); }
        
        .sys-health-item .health-label {
          font-size: 11px;
          text-transform: uppercase;
          color: var(--on-surface-variant);
          font-weight: 700;
        }
        
        .sys-health-item .health-val {
          font-size: 13px;
          font-weight: 600;
          color: var(--on-surface);
        }

        /* Modals */
        .modal-overlay {
          position: fixed;
          top: 0; left: 0; right: 0; bottom: 0;
          background: rgba(15, 23, 42, 0.45);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
          padding: 12px;
        }

        .modal-content {
          background: #ffffff;
          border-radius: 16px;
          box-shadow: 0 20px 40px rgba(0,0,0,0.15);
          border: 1px solid var(--outline-variant);
          width: 100%;
          max-width: 448px;
          max-height: 90vh;
          overflow-y: auto;
          font-family: 'Manrope', sans-serif;
        }
        
        .modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px 20px;
          border-bottom: 1px solid #f0f3ff;
        }
        
        .modal-header h3 { margin: 0; font-size: 17px; font-weight: 700; font-family: 'Plus Jakarta Sans', 'Manrope', sans-serif; }
        .modal-body { padding: 20px; }
        .modal-footer { padding: 14px 20px; border-top: 1px solid #f0f3ff; display: flex; justify-content: flex-end; gap: 8px; background: #fafbff; border-bottom-left-radius: 16px; border-bottom-right-radius: 16px; flex-wrap: wrap; }

        @media (max-width: 1024px) {
          .sys-health-grid { grid-template-columns: repeat(2, 1fr) !important; }
        }

        @media (max-width: 640px) {
          .page-header {
            flex-direction: column;
            align-items: stretch;
            gap: 12px;
          }
          .page-title-group h1 { font-size: 22px; }
          .header-actions {
            width: 100%;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(min(100%, 120px), 1fr));
            gap: 8px;
          }
          .header-actions .btn {
            padding: 8px 10px;
            font-size: 12.5px;
          }
          .stats-grid {
            grid-template-columns: 1fr !important;
          }
          .stats-grid-compact {
            grid-template-columns: repeat(2, 1fr) !important;
            gap: 10px;
          }
          .sys-health-grid { grid-template-columns: 1fr !important; gap: 12px; padding: 14px; }
          .panel { padding: 16px; border-radius: 14px; }
          .stat-card { padding: 14px; }
          .stat-value { font-size: 24px; }
        }

        @media (max-width: 360px) {
          .stats-grid-compact {
            grid-template-columns: 1fr !important;
          }
          .header-actions {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>

      {/* Header Area */}
      <section className="page-header">
        <div className="page-title-group">
          <h1>Admin Dashboard</h1>
          <p>Monitor platform activity and manage your workspace efficiently.</p>
        </div>
        <div className="header-actions">
          <button className="btn btn-outline" onClick={() => setShowExportModal(true)}>
            <span className="material-symbols-outlined">download</span>Export
          </button>
          <button className="btn btn-outline" onClick={() => navigate('/admin/settings')}>
            <span className="material-symbols-outlined">settings</span>Settings
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/admin/users')}>
            <span className="material-symbols-outlined">group</span>Users
          </button>
        </div>
      </section>



      {/* Core KPIs */}
      <section className="stats-grid">
        <div className="panel stat-card" onClick={() => navigate('/admin/users')} style={{ cursor: 'pointer' }}>
          <div className="stat-top">
            <div className="stat-icon primary"><span className="material-symbols-outlined">group</span></div>
          </div>
          <div>
            <p className="stat-value"><AnimatedNumber value={stats.totalUsers} /></p>
            <p className="stat-label">Total Users</p>
            <div className="stat-subtext">
              <span style={{ color: 'var(--tertiary)' }}>{stats.activeUsers} Active</span>
              <span style={{ color: 'var(--error)' }}>{stats.disabledUsers} Disabled</span>
            </div>
          </div>
        </div>

        <div className="panel stat-card" onClick={() => navigate('/admin/projects')} style={{ cursor: 'pointer' }}>
          <div className="stat-top">
            <div className="stat-icon secondary"><span className="material-symbols-outlined">folder</span></div>
          </div>
          <div>
            <p className="stat-value"><AnimatedNumber value={stats.totalProjects} /></p>
            <p className="stat-label">Total Projects</p>
            <div className="stat-subtext">
              <span style={{ color: 'var(--tertiary)' }}>{stats.activeProjects} Active</span>
              <span style={{ color: 'var(--on-surface-variant)' }}>{stats.archivedProjects} Archived</span>
            </div>
          </div>
        </div>

        <div className="panel stat-card">
          <div className="stat-top">
            <div className="stat-icon tertiary"><span className="material-symbols-outlined">shield</span></div>
          </div>
          <div>
            <p className="stat-value"><AnimatedNumber value={stats.totalScans} /></p>
            <p className="stat-label">Total Scans</p>
            <div className="stat-subtext">
              <span style={{ color: 'var(--tertiary)' }}>{stats.completedScans} Completed</span>
              <span style={{ color: 'var(--error)' }}>{stats.failedScans} Failed</span>
            </div>
          </div>
        </div>

        <div className="panel stat-card">
          <div className="stat-top">
            <div className="stat-icon error"><span className="material-symbols-outlined">bug_report</span></div>
          </div>
          <div>
            <p className="stat-value"><AnimatedNumber value={stats.criticalVulnerabilities} /></p>
            <p className="stat-label">Critical Findings</p>
            <div className="stat-subtext">
              <span style={{ color: '#d4a017' }}>{stats.highVulnerabilities} High</span>
              <span style={{ color: 'var(--on-surface-variant)' }}>{stats.mediumVulnerabilities} Med</span>
            </div>
          </div>
        </div>
      </section>

      {/* Analytics KPIs */}
      <h2 style={{ margin: '8px 0 0', fontSize: 17, fontWeight: 700 }}>Analytics Engine</h2>
      <section className="stats-grid-compact">
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>Avg Risk Score</p>
          <p className="stat-value" style={{ fontSize: '22px' }}>{stats.averageScanScore?.toFixed(1) || '100.0'}</p>
        </div>
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>Avg Findings/Scan</p>
          <p className="stat-value" style={{ fontSize: '22px' }}>{stats.averageFindings?.toFixed(1) || '0.0'}</p>
        </div>
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>Avg Scan Time</p>
          <p className="stat-value" style={{ fontSize: '22px' }}>{stats.averageScanDuration?.toFixed(1) || '0'}s</p>
        </div>
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>AI Reports</p>
          <p className="stat-value" style={{ fontSize: '22px' }}><AnimatedNumber value={stats.totalReports} /></p>
        </div>
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>AI Chat Requests</p>
          <p className="stat-value" style={{ fontSize: '22px' }}><AnimatedNumber value={stats.aiRequests} /></p>
        </div>
      </section>

      {/* Daily Activity KPIs */}
      <h2 style={{ margin: '8px 0 0', fontSize: 17, fontWeight: 700 }}>Today's Activity</h2>
      <section className="stats-grid-compact">
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>New Users Today</p>
          <p className="stat-value" style={{ fontSize: '22px', color: 'var(--primary)' }}><AnimatedNumber value={stats.usersRegisteredToday} /></p>
        </div>
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>Projects Created Today</p>
          <p className="stat-value" style={{ fontSize: '22px', color: 'var(--primary)' }}><AnimatedNumber value={stats.projectsCreatedToday} /></p>
        </div>
        <div className="panel stat-card" style={{ padding: '14px' }}>
          <p className="stat-label" style={{ fontSize: '11px', textTransform: 'uppercase' }}>Scans Run Today</p>
          <p className="stat-value" style={{ fontSize: '22px', color: 'var(--primary)' }}><AnimatedNumber value={stats.scansToday} /></p>
        </div>
      </section>

      {/* Export Modal */}
      {showExportModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '448px' }}>
            <div className="modal-header">
              <h3>Export Platform Data</h3>
              <button onClick={() => setShowExportModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            
            <div className="modal-body">
              <p style={{ margin: '0 0 20px', fontSize: 13.5, color: 'var(--on-surface-variant)' }}>
                Select datasets to export. Data will be formatted based on your selection.
              </p>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 24 }}>
                {[
                  { id: 'users', label: 'User Directory', desc: 'User profiles and roles' },
                  { id: 'projects', label: 'Project Metadata', desc: 'Project names, statuses, risk scores' },
                  { id: 'scans', label: 'Scan History', desc: 'All executed security scans' },
                  { id: 'auditLogs', label: 'Audit Logs', desc: 'Security trail and actions' },
                  { id: 'reports', label: 'AI Reports', desc: 'Generated PDF report metadata' }
                ].map(item => (
                  <label key={item.id} style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer' }}>
                    <input 
                      type="checkbox" 
                      checked={exportForm.datasets[item.id]}
                      onChange={(e) => setExportForm(prev => ({
                        ...prev, 
                        datasets: { ...prev.datasets, [item.id]: e.target.checked }
                      }))}
                      style={{ width: 18, height: 18, accentColor: 'var(--primary)' }}
                    />
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 14 }}>{item.label}</div>
                      <div style={{ fontSize: 12, color: 'var(--on-surface-variant)' }}>{item.desc}</div>
                    </div>
                  </label>
                ))}
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: 'var(--on-surface-variant)', marginBottom: 6, textTransform: 'uppercase' }}>
                  Export Format
                </label>
                <select 
                  value={exportForm.format}
                  onChange={(e) => setExportForm(prev => ({ ...prev, format: e.target.value }))}
                  style={{ width: '100%', padding: '10px 12px', border: '1px solid var(--outline-variant)', borderRadius: 8, fontSize: 14, fontFamily: 'inherit' }}
                >
                  <option value="csv">CSV (Spreadsheet)</option>
                  <option value="excel">Excel (.xlsx)</option>
                  <option value="pdf">PDF Report</option>
                </select>
              </div>
            </div>
            
            <div className="modal-footer">
              <button className="btn btn-outline" onClick={() => setShowExportModal(false)} disabled={isExporting}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleExportSubmit} disabled={isExporting}>
                {isExporting ? 'Exporting...' : 'Export Data'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
