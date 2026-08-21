import React, { useState, useEffect, useCallback } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getAllAuditLogs, exportAuditLogsCsv } from '../../api/adminApi';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

function formatDateTime(d) {
  if (!d) return '—';
  return new Date(d).toLocaleString('en-US', {
    year:'numeric', month:'short', day:'numeric',
    hour:'2-digit', minute:'2-digit', second:'2-digit'
  });
}
function useDebounce(val, delay) {
  const [dv, setDv] = useState(val);
  useEffect(() => { const t = setTimeout(() => setDv(val), delay); return () => clearTimeout(t); }, [val, delay]);
  return dv;
}

const C = {
  primary:'#0058be', outline:'#c2c6d6', onSurface:'#111c2d', onVariant:'#424754',
  error:'#ba1a1a', errorContainer:'#ffdad6', tertiary:'#006947', tertiaryBg:'rgba(0,105,71,0.1)',
  primaryBg:'rgba(0,88,190,0.08)',
};

const ACTION_COLORS = {
  USER_CREATED: { color: C.tertiary, bg: C.tertiaryBg },
  USER_UPDATED: { color: C.primary, bg: C.primaryBg },
  USER_DELETED: { color: C.error, bg: C.errorContainer },
  USER_ENABLED: { color: C.tertiary, bg: C.tertiaryBg },
  USER_DISABLED: { color: '#8a5d00', bg: 'rgba(138,93,0,0.1)' },
  ROLE_CHANGED: { color: C.primary, bg: C.primaryBg },
  PASSWORD_RESET: { color: '#7b4fc1', bg: 'rgba(123,79,193,0.1)' },
  PROJECT_ARCHIVED: { color: '#8a5d00', bg: 'rgba(138,93,0,0.1)' },
  PROJECT_RESTORED: { color: C.tertiary, bg: C.tertiaryBg },
  PROJECT_DELETED: { color: C.error, bg: C.errorContainer },
  PROJECT_TRANSFERRED: { color: C.primary, bg: C.primaryBg },
  USER_STATUS_CHANGED: { color: C.primary, bg: C.primaryBg },
};

export default function AdminAuditLogs() {
  const { user } = useAuth();
  const addToast = useToast();

  const [logs, setLogs] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [searchAction, setSearchAction] = useState('');
  const [searchEmail, setSearchEmail] = useState('');
  const [searchResource, setSearchResource] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [exporting, setExporting] = useState(false);
  const debouncedAction = useDebounce(searchAction, 350);
  const debouncedEmail = useDebounce(searchEmail, 350);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size, sortBy:'createdAt', direction:'desc' };
      if (debouncedAction) params.action = debouncedAction;
      if (debouncedEmail) params.userEmail = debouncedEmail;
      if (searchResource) params.resource = searchResource;
      if (fromDate) params.fromDate = new Date(fromDate).toISOString();
      if (toDate) {
        const to = new Date(toDate);
        to.setHours(23, 59, 59, 999);
        params.toDate = to.toISOString();
      }
      const data = await getAllAuditLogs(params);
      setLogs(data.content || []);
      setTotal(data.totalElements || 0);
    } catch {
      addToast('Failed to load audit logs', 'error');
    } finally { setLoading(false); }
  }, [page, size, debouncedAction, debouncedEmail, searchResource, fromDate, toDate, addToast]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { setPage(0); }, [debouncedAction, debouncedEmail, searchResource, fromDate, toDate]);

  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;

  const totalPages = Math.ceil(total / size);

  const handleExport = async () => {
    setExporting(true);
    try {
      const params = {};
      if (searchAction) params.action = searchAction;
      if (searchEmail) params.userEmail = searchEmail;
      if (fromDate) params.fromDate = new Date(fromDate).toISOString();
      if (toDate) { const to = new Date(toDate); to.setHours(23,59,59,999); params.toDate = to.toISOString(); }
      const response = await exportAuditLogsCsv(params);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'audit-logs.csv'; a.click();
      URL.revokeObjectURL(url);
      addToast('Audit log CSV exported successfully', 'success');
    } catch { addToast('Export failed', 'error'); }
    finally { setExporting(false); }
  };

  const inputStyle = {
    padding:'6px 12px', border:`1px solid ${C.outline}`, borderRadius:99,
    outline:'none', fontSize:13, fontFamily:'inherit', background:'var(--snt-surface)',
    color:C.onSurface
  };
  const btnOutline = {
    display:'inline-flex', alignItems:'center', gap:6, padding:'7px 14px',
    background:'#ffffff', border:'1px solid #cbd5e1', borderRadius:8,
    fontWeight:600, fontSize:13, cursor:'pointer', fontFamily:'inherit', color:'#0f172a',
    boxShadow:'0 1px 2px rgba(15,23,42,0.04)'
  };

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", color:C.onSurface, padding:0 }}>
      <style>{`
        .al-row:hover { background:#f5f8ff !important; }
        .al-row td { padding:11px 14px; border-bottom:1px solid #f0f3ff; font-size:13px; vertical-align:top; }
        .al-th { padding:10px 14px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:${C.onVariant}; border-bottom:2px solid #e8eef8; text-align:left; background:#fafbff; }

        .al-table-wrapper { display: block; }
        .al-cards-wrapper { display: none; }

        @media (max-width: 767px) {
          .al-table-wrapper { display: none !important; }
          .al-cards-wrapper { display: flex !important; flex-direction: column; gap: 10px; }
          .al-filters-grid {
            display: grid !important;
            grid-template-columns: 1fr 1fr !important;
            gap: 8px !important;
          }
          .al-filters-full {
            grid-column: 1 / -1 !important;
          }
        }
      `}</style>

      {/* Header */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:12, marginBottom:20 }}>
        <div>
          <h1 style={{ margin:0, fontSize:22, fontWeight:800, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Audit Logs</h1>
          <p style={{ margin:'4px 0 0', fontSize:13.5, color:C.onVariant }}>
            {total.toLocaleString()} events · Read-only security trail
          </p>
        </div>
        <button style={{ ...btnOutline, background:C.primary, color:'#fff', borderColor:C.primary }}
          onClick={handleExport} disabled={exporting}>
          <span className="material-symbols-outlined" style={{ fontSize:15 }}>download</span>
          {exporting ? 'Exporting...' : 'Export CSV'}
        </button>
      </div>

      {/* Filters */}
      <div className="al-filters-grid" style={{ display:'flex', gap:10, flexWrap:'wrap', marginBottom:16, alignItems:'center' }}>
        <div className="al-filters-full" style={{ display:'flex', alignItems:'center', gap:6, background:'#ffffff',
          border:`1px solid ${C.outline}`, borderRadius:10, padding:'6px 14px', flex:1, minWidth:160 }}>
          <span className="material-symbols-outlined" style={{ fontSize:15, color:C.onVariant }}>search</span>
          <input value={searchAction} onChange={e => setSearchAction(e.target.value)}
            placeholder="Filter action..." style={{ border:'none', background:'transparent', outline:'none', fontSize:13, width:'100%' }} />
        </div>
        <div className="al-filters-full" style={{ display:'flex', alignItems:'center', gap:6, background:'#ffffff',
          border:`1px solid ${C.outline}`, borderRadius:10, padding:'6px 14px', flex:1, minWidth:160 }}>
          <span className="material-symbols-outlined" style={{ fontSize:15, color:C.onVariant }}>person_search</span>
          <input value={searchEmail} onChange={e => setSearchEmail(e.target.value)}
            placeholder="Filter user email..." style={{ border:'none', background:'transparent', outline:'none', fontSize:13, width:'100%' }} />
        </div>
        <select value={searchResource} onChange={e => setSearchResource(e.target.value)} style={{ ...inputStyle, borderRadius:10, background:'#ffffff' }}>
          <option value="">All Resources</option>
          <option value="User">User</option>
          <option value="Project">Project</option>
        </select>
        <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} style={{ ...inputStyle, borderRadius:10, background:'#ffffff' }} title="From date" />
        <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} style={{ ...inputStyle, borderRadius:10, background:'#ffffff' }} title="To date" />
        {(searchAction || searchEmail || searchResource || fromDate || toDate) && (
          <button style={{ ...btnOutline, borderRadius:10 }} onClick={() => {
            setSearchAction(''); setSearchEmail(''); setSearchResource(''); setFromDate(''); setToDate('');
          }}>
            <span className="material-symbols-outlined" style={{ fontSize:14 }}>clear</span>
            Clear
          </button>
        )}
      </div>

      {/* Mobile Cards View (< 768px) */}
      <div className="al-cards-wrapper">
        {loading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:48 }}><LoadingSpinner size="lg" /></div>
        ) : logs.length === 0 ? (
          <div style={{ textAlign:'center', padding:32, background:'#fff', borderRadius:14, border:'1px solid #e2e8f0', color:C.onVariant }}>
            No audit logs found
          </div>
        ) : (
          logs.map(log => {
            const ac = ACTION_COLORS[log.action] || { color:C.onVariant, bg:'#f0f3ff' };
            return (
              <div key={log.id} style={{
                background:'#ffffff', borderRadius:14, border:'1px solid #e2e8f0', padding:14,
                boxShadow:'0 1px 3px rgba(15,23,42,0.04)', display:'flex', flexDirection:'column', gap:8
              }}>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', gap:6, flexWrap:'wrap' }}>
                  <span style={{ display:'inline-flex', padding:'3px 8px', borderRadius:99,
                    fontSize:11.5, fontWeight:700, background:ac.bg, color:ac.color }}>
                    {log.action}
                  </span>
                  <span style={{ fontSize:11.5, color:C.onVariant }}>
                    {formatDateTime(log.createdAt)}
                  </span>
                </div>

                <div style={{ fontSize:13, display:'flex', alignItems:'center', justifyContent:'space-between', gap:8 }}>
                  <div>
                    <span style={{ fontWeight:600 }}>{log.user ? `${log.user.firstName} ${log.user.lastName}` : 'System'}</span>
                    {log.user?.email && <span style={{ fontSize:12, color:C.onVariant, display:'block' }}>{log.user.email}</span>}
                  </div>
                  {log.status === 'SUCCESS'
                    ? <span style={{ display:'inline-flex', padding:'2px 8px', borderRadius:99,
                        fontSize:11, fontWeight:700, background:C.tertiaryBg, color:C.tertiary }}>SUCCESS</span>
                    : <span style={{ display:'inline-flex', padding:'2px 8px', borderRadius:99,
                        fontSize:11, fontWeight:700, background:C.errorContainer, color:C.error }}>
                        {log.status || 'UNKNOWN'}
                      </span>}
                </div>

                {(log.resource || log.ipAddress || log.details) && (
                  <div style={{ fontSize:12, color:C.onVariant, background:'#f8fafc', padding:'8px 10px', borderRadius:8, display:'flex', flexDirection:'column', gap:4 }}>
                    {log.resource && <div>Resource: <strong style={{ color:C.onSurface }}>{log.resource}</strong></div>}
                    {log.ipAddress && <div>IP: <code style={{ fontFamily:'monospace' }}>{log.ipAddress}</code></div>}
                    {log.details && <div>Details: {log.details}</div>}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Desktop/Tablet Table View (>= 768px) */}
      <div className="al-table-wrapper" style={{ background:'#ffffff', borderRadius:16, border:'1px solid #e2e8f0',
        boxShadow:'0 1px 3px rgba(15,23,42,0.04)', overflow:'hidden', transition: 'border-color 0.2s ease, box-shadow 0.2s ease' }}
        onMouseEnter={e => { e.currentTarget.style.borderColor = '#93c5fd'; e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)' }}
        onMouseLeave={e => { e.currentTarget.style.borderColor = '#e2e8f0'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)' }}
      >
        {loading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:48 }}><LoadingSpinner size="lg" /></div>
        ) : (
          <div style={{ overflowX:'auto' }}>
            <table style={{ width:'100%', borderCollapse:'collapse', minWidth:900 }}>
              <thead>
                <tr>
                  <th className="al-th">Timestamp</th>
                  <th className="al-th">User</th>
                  <th className="al-th">Action</th>
                  <th className="al-th">Resource</th>
                  <th className="al-th">Status</th>
                  <th className="al-th">IP Address</th>
                  <th className="al-th">Details</th>
                </tr>
              </thead>
              <tbody>
                {logs.length === 0 ? (
                  <tr><td colSpan={7} style={{ textAlign:'center', padding:40, color:C.onVariant }}>
                    No audit logs found
                  </td></tr>
                ) : logs.map(log => {
                  const ac = ACTION_COLORS[log.action] || { color:C.onVariant, bg:'#f0f3ff' };
                  return (
                    <tr key={log.id} className="al-row">
                      <td style={{ whiteSpace:'nowrap', color:C.onVariant, fontSize:12 }}>
                        {formatDateTime(log.createdAt)}
                      </td>
                      <td>
                        {log.user ? (
                          <div>
                            <div style={{ fontWeight:600, fontSize:13 }}>
                              {log.user.firstName} {log.user.lastName}
                            </div>
                            <div style={{ fontSize:11.5, color:C.onVariant }}>{log.user.email}</div>
                          </div>
                        ) : <span style={{ color:C.onVariant }}>System</span>}
                      </td>
                      <td>
                        <span style={{ display:'inline-flex', padding:'3px 10px', borderRadius:99,
                          fontSize:11.5, fontWeight:700, background:ac.bg, color:ac.color,
                          whiteSpace:'nowrap' }}>
                          {log.action}
                        </span>
                      </td>
                      <td style={{ color:C.onVariant, fontSize:13 }}>{log.resource || '—'}</td>
                      <td>
                        {log.status === 'SUCCESS'
                          ? <span style={{ display:'inline-flex', padding:'2px 8px', borderRadius:99,
                              fontSize:11.5, fontWeight:700, background:C.tertiaryBg, color:C.tertiary }}>SUCCESS</span>
                          : <span style={{ display:'inline-flex', padding:'2px 8px', borderRadius:99,
                              fontSize:11.5, fontWeight:700, background:C.errorContainer, color:C.error }}>
                              {log.status || 'UNKNOWN'}
                            </span>}
                      </td>
                      <td style={{ color:C.onVariant, fontSize:12, fontFamily:'monospace' }}>
                        {log.ipAddress || '—'}
                      </td>
                      <td style={{ maxWidth:300, color:C.onVariant, fontSize:12.5 }}>
                        {log.details || '—'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center',
            padding:'12px 16px', borderTop:`1px solid #f0f3ff`, background:'var(--snt-surface-2)', flexWrap:'wrap', gap:8 }}>
            <span style={{ fontSize:13, color:C.onVariant }}>
              Showing {page*size+1}–{Math.min((page+1)*size, total)} of {total}
            </span>
            <div style={{ display:'flex', gap:4 }}>
              <button style={btnOutline} disabled={page===0} onClick={()=>setPage(p=>p-1)}>Prev</button>
              <span style={{ padding:'6px 12px', fontSize:13, fontWeight:600 }}>{page+1}/{totalPages}</span>
              <button style={btnOutline} disabled={page>=totalPages-1} onClick={()=>setPage(p=>p+1)}>Next</button>
            </div>
          </div>
        )}
      </div>

      {/* Mobile Pagination for cards (< 768px) */}
      {totalPages > 1 && (
        <div className="al-cards-wrapper" style={{ marginTop: 12 }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center',
            padding:'12px 16px', background:'#ffffff', borderRadius:12, border:'1px solid #e2e8f0', flexWrap:'wrap', gap:8 }}>
            <span style={{ fontSize:12.5, color:C.onVariant }}>
              {page + 1} of {totalPages} pages ({total} events)
            </span>
            <div style={{ display:'flex', gap:6 }}>
              <button style={btnOutline} disabled={page===0} onClick={()=>setPage(p=>p-1)}>Prev</button>
              <button style={btnOutline} disabled={page>=totalPages-1} onClick={()=>setPage(p=>p+1)}>Next</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
