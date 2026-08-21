import React, { useState, useEffect, useCallback } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getAllProjects, archiveProject, restoreProject, deleteProject } from '../../api/adminApi';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

function formatDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-US', { year:'numeric', month:'short', day:'numeric' });
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

const pill = (label, color, bg) => (
  <span style={{ display:'inline-flex', padding:'3px 10px', borderRadius:99,
    fontSize:12, fontWeight:700, background:bg, color }}>{label}</span>
);

function riskLevel(score) {
  if (score >= 80) return { label:'Low', color:C.tertiary, bg:C.tertiaryBg };
  if (score >= 50) return { label:'Medium', color:'#8a5d00', bg:'rgba(138,93,0,0.1)' };
  return { label:'High', color:C.error, bg:C.errorContainer };
}

export default function AdminProjects() {
  const { user } = useAuth();
  const addToast = useToast();

  const [projects, setProjects] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(15);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterActive, setFilterActive] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const debouncedSearch = useDebounce(search, 350);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size, sortBy, direction: sortDir };
      if (debouncedSearch) params.search = debouncedSearch;
      if (filterStatus) params.status = filterStatus;
      if (filterActive !== '') params.active = filterActive;
      const data = await getAllProjects(params);
      setProjects(data.content || []);
      setTotal(data.totalElements || 0);
    } catch {
      addToast('Failed to load projects', 'error');
    } finally { setLoading(false); }
  }, [page, size, sortBy, sortDir, debouncedSearch, filterStatus, filterActive, addToast]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { setPage(0); }, [debouncedSearch, filterStatus, filterActive]);

  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;

  const totalPages = Math.ceil(total / size);

  const confirm = (title, msg, action) => setConfirmDialog({ title, msg, action });
  const runConfirm = async () => {
    setActionLoading(true);
    try { await confirmDialog.action(); }
    finally { setActionLoading(false); setConfirmDialog(null); }
  };

  const handleArchive = (p) => confirm(
    'Archive Project',
    `Archive "${p.name}"? Users will not be able to use it until restored.`,
    async () => { await archiveProject(p.id); addToast('Project archived', 'success'); load(); }
  );
  const handleRestore = (p) => confirm(
    'Restore Project',
    `Restore "${p.name}" back to active status?`,
    async () => { await restoreProject(p.id); addToast('Project restored', 'success'); load(); }
  );
  const handleDelete = (p) => confirm(
    'Delete Project',
    `Permanently delete "${p.name}"? All associated scans and files will be removed.`,
    async () => { await deleteProject(p.id); addToast('Project deleted', 'success'); load(); }
  );

  const inputStyle = {
    padding:'6px 12px', border:`1px solid ${C.outline}`, borderRadius:99,
    outline:'none', fontSize:13.5, fontFamily:'inherit', background:'var(--snt-surface)'
  };
  const btnOutline = {
    display:'inline-flex', alignItems:'center', gap:5, padding:'5px 12px',
    background:'#ffffff', border:'1px solid #cbd5e1', borderRadius:7,
    fontWeight:600, fontSize:12, cursor:'pointer', fontFamily:'inherit', color:'#0f172a',
    boxShadow:'0 1px 2px rgba(15,23,42,0.04)'
  };

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", color:C.onSurface, padding:0 }}>
      <style>{`
        .ap-row:hover { background:#f5f8ff !important; }
        .ap-row td { padding:12px 16px; border-bottom:1px solid #f0f3ff; font-size:13.5px; vertical-align:middle; }
        .ap-th { padding:10px 16px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:${C.onVariant}; border-bottom:2px solid #e8eef8; text-align:left; }

        .ap-table-wrapper { display: block; }
        .ap-cards-wrapper { display: none; }

        @media (max-width: 767px) {
          .ap-table-wrapper { display: none !important; }
          .ap-cards-wrapper { display: flex !important; flex-direction: column; gap: 12px; }
          .ap-filter-bar {
            display: grid !important;
            grid-template-columns: 1fr 1fr !important;
            gap: 8px !important;
          }
          .ap-search-box {
            grid-column: 1 / -1 !important;
            max-width: 100% !important;
          }
        }
      `}</style>

      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:12, marginBottom:20 }}>
        <div>
          <h1 style={{ margin:0, fontSize:22, fontWeight:800, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Project Management</h1>
          <p style={{ margin:'4px 0 0', fontSize:13.5, color:C.onVariant }}>
            {total.toLocaleString()} projects · Lifecycle management
          </p>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="ap-filter-bar" style={{ display:'flex', gap:10, flexWrap:'wrap', marginBottom:16 }}>
        <div className="ap-search-box" style={{ display:'flex', alignItems:'center', gap:6, background:'#ffffff',
          border:'1px solid #cbd5e1', borderRadius:10, padding:'6px 14px', flex:'1', minWidth:200, maxWidth:360, boxShadow:'0 1px 2px rgba(15,23,42,0.04)' }}>
          <span className="material-symbols-outlined" style={{ fontSize:16, color:'#64748b' }}>search</span>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search projects or owner..."
            style={{ border:'none', background:'transparent', outline:'none', fontSize:13.5, width:'100%', color:'#0f172a' }} />
        </div>
        <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} style={{ ...inputStyle, border:'1px solid #cbd5e1', borderRadius:10, background:'#ffffff', minWidth:0, width:'100%' }}>
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="ARCHIVED">Archived</option>
        </select>
        <select value={sortBy} onChange={e => setSortBy(e.target.value)} style={{ ...inputStyle, border:'1px solid #cbd5e1', borderRadius:10, background:'#ffffff', minWidth:0, width:'100%' }}>
          <option value="createdAt">Created</option>
          <option value="updatedAt">Updated</option>
          <option value="name">Name</option>
          <option value="securityScore">Risk Score</option>
        </select>
      </div>

      {/* Mobile Card List View (< 768px) */}
      <div className="ap-cards-wrapper">
        {loading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:48 }}><LoadingSpinner size="lg" /></div>
        ) : projects.length === 0 ? (
          <div style={{ textAlign:'center', padding:32, background:'#fff', borderRadius:14, border:'1px solid #e2e8f0', color:C.onVariant }}>
            No projects found
          </div>
        ) : (
          projects.map(p => {
            const risk = riskLevel(p.securityScore ?? 100);
            return (
              <div key={p.id} style={{
                background:'#ffffff', borderRadius:14, border:'1px solid #e2e8f0', padding:16,
                boxShadow:'0 1px 3px rgba(15,23,42,0.04)', display:'flex', flexDirection:'column', gap:10
              }}>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', gap:8 }}>
                  <div>
                    <div style={{ fontWeight:700, fontSize:15, color:C.onSurface }}>{p.name}</div>
                    {p.description && <div style={{ fontSize:12, color:C.onVariant, marginTop:2 }}>{p.description}</div>}
                  </div>
                  <div style={{ display:'flex', gap:6, flexShrink:0 }}>
                    {p.active ? pill('Active', C.tertiary, C.tertiaryBg) : pill('Archived', C.onVariant, '#f0f3ff')}
                    {pill(risk.label, risk.color, risk.bg)}
                  </div>
                </div>

                <div style={{ display:'flex', justifyContent:'space-between', fontSize:12, color:C.onVariant, background:'#f8fafc', padding:'8px 10px', borderRadius:8, flexWrap:'wrap', gap:6 }}>
                  <span>Owner: <strong>{p.user ? `${p.user.firstName} ${p.user.lastName}` : '—'}</strong></span>
                  <span>Type: <strong>{p.projectType || 'General'}</strong></span>
                  <span>Updated: <strong>{formatDate(p.updatedAt)}</strong></span>
                </div>

                <div style={{ display:'flex', justifyContent:'flex-end', gap:8, paddingTop:4, borderTop:'1px solid #f1f5f9' }}>
                  {p.active ? (
                    <button style={{ ...btnOutline, color:'#8a5d00', borderColor:'#d4a017', padding:'6px 12px', minHeight:32 }}
                      onClick={() => handleArchive(p)} title="Archive">
                      <span className="material-symbols-outlined" style={{ fontSize:14 }}>archive</span>
                      Archive
                    </button>
                  ) : (
                    <button style={{ ...btnOutline, color:C.tertiary, borderColor:C.tertiary, padding:'6px 12px', minHeight:32 }}
                      onClick={() => handleRestore(p)} title="Restore">
                      <span className="material-symbols-outlined" style={{ fontSize:14 }}>unarchive</span>
                      Restore
                    </button>
                  )}
                  <button style={{ ...btnOutline, color:C.error, borderColor:C.error, padding:'6px 12px', minHeight:32 }}
                    onClick={() => handleDelete(p)} title="Delete">
                    <span className="material-symbols-outlined" style={{ fontSize:14 }}>delete</span>
                    Delete
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Desktop/Tablet Table View (>= 768px) */}
      <div className="ap-table-wrapper" style={{ background:'#ffffff', borderRadius:16, border:'1px solid #e2e8f0',
        boxShadow:'0 1px 3px rgba(15,23,42,0.04)', overflow:'hidden', transition: 'border-color 0.2s ease, box-shadow 0.2s ease' }}
        onMouseEnter={e => { e.currentTarget.style.borderColor = '#93c5fd'; e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)' }}
        onMouseLeave={e => { e.currentTarget.style.borderColor = '#e2e8f0'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)' }}
      >
        {loading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:48 }}><LoadingSpinner size="lg" /></div>
        ) : (
          <div style={{ overflowX:'auto' }}>
            <table style={{ width:'100%', borderCollapse:'collapse', minWidth:800 }}>
              <thead style={{ background:'#f8fafc', borderBottom:'1px solid #cbd5e1' }}>
                <tr>
                  <th className="ap-th">Project Name</th>
                  <th className="ap-th">Owner</th>
                  <th className="ap-th">Language</th>
                  <th className="ap-th">Status</th>
                  <th className="ap-th">Risk Level</th>
                  <th className="ap-th">Created</th>
                  <th className="ap-th">Updated</th>
                  <th className="ap-th" style={{ textAlign:'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {projects.length === 0 ? (
                  <tr><td colSpan={8} style={{ textAlign:'center', padding:40, color:C.onVariant }}>
                    No projects found
                  </td></tr>
                ) : projects.map(p => {
                  const risk = riskLevel(p.securityScore ?? 100);
                  return (
                    <tr key={p.id} className="ap-row">
                      <td>
                        <div style={{ fontWeight:700, fontSize:14 }}>{p.name}</div>
                        {p.description && <div style={{ fontSize:12, color:C.onVariant, marginTop:2 }}>
                          {p.description.substring(0, 60)}{p.description.length > 60 ? '...' : ''}
                        </div>}
                      </td>
                      <td style={{ color:C.onVariant }}>
                        <div>{p.user?.firstName} {p.user?.lastName}</div>
                        <div style={{ fontSize:12 }}>{p.user?.email}</div>
                      </td>
                      <td>{p.projectType
                        ? pill(p.projectType, C.primary, C.primaryBg)
                        : <span style={{ color:C.onVariant }}>—</span>}</td>
                      <td>{p.active
                        ? pill('Active', C.tertiary, C.tertiaryBg)
                        : pill('Archived', C.onVariant, '#f0f3ff')}</td>
                      <td>{pill(risk.label, risk.color, risk.bg)}</td>
                      <td style={{ color:C.onVariant, fontSize:13 }}>{formatDate(p.createdAt)}</td>
                      <td style={{ color:C.onVariant, fontSize:13 }}>{formatDate(p.updatedAt)}</td>
                      <td>
                        <div style={{ display:'flex', gap:4, justifyContent:'flex-end' }}>
                          {p.active ? (
                            <button style={{ ...btnOutline, color:'#8a5d00', borderColor:'#d4a017' }}
                              onClick={() => handleArchive(p)} title="Archive">
                              <span className="material-symbols-outlined" style={{ fontSize:13 }}>archive</span>
                              Archive
                            </button>
                          ) : (
                            <button style={{ ...btnOutline, color:C.tertiary, borderColor:C.tertiary }}
                              onClick={() => handleRestore(p)} title="Restore">
                              <span className="material-symbols-outlined" style={{ fontSize:13 }}>unarchive</span>
                              Restore
                            </button>
                          )}
                          <button style={{ ...btnOutline, color:C.error, borderColor:C.error }}
                            onClick={() => handleDelete(p)} title="Delete">
                            <span className="material-symbols-outlined" style={{ fontSize:13 }}>delete</span>
                          </button>
                        </div>
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
              Showing {page*size+1}–{Math.min((page+1)*size,total)} of {total}
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
        <div className="ap-cards-wrapper" style={{ marginTop: 12 }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center',
            padding:'12px 16px', background:'#ffffff', borderRadius:12, border:'1px solid #e2e8f0', flexWrap:'wrap', gap:8 }}>
            <span style={{ fontSize:12.5, color:C.onVariant }}>
              {page + 1} of {totalPages} pages ({total} projects)
            </span>
            <div style={{ display:'flex', gap:6 }}>
              <button style={btnOutline} disabled={page===0} onClick={()=>setPage(p=>p-1)}>Prev</button>
              <button style={btnOutline} disabled={page>=totalPages-1} onClick={()=>setPage(p=>p+1)}>Next</button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm Dialog */}
      {confirmDialog && (
        <div style={{ position:'fixed', inset:0, zIndex:9999, display:'flex', alignItems:'center', justifyContent:'center',
          padding:12, background:'rgba(15,23,42,0.45)' }}>
          <div style={{ background:'#ffffff', borderRadius:16, boxShadow:'0 20px 40px rgba(15,23,42,0.15)',
            border:'1px solid #cbd5e1', width:'100%', maxWidth:380, padding:20, fontFamily:"'Manrope',sans-serif" }}>
            <h3 style={{ margin:'0 0 10px', fontSize:17, fontWeight:700, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>
              {confirmDialog.title}
            </h3>
            <p style={{ margin:'0 0 20px', fontSize:13.5, color:C.onVariant }}>{confirmDialog.msg}</p>
            <div style={{ display:'flex', justifyContent:'flex-end', gap:8 }}>
              <button style={btnOutline} disabled={actionLoading} onClick={() => setConfirmDialog(null)}>Cancel</button>
              <button onClick={runConfirm} disabled={actionLoading}
                style={{ ...btnOutline, background:C.error, color:'#fff', borderColor:C.error }}>
                {actionLoading ? 'Processing...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
