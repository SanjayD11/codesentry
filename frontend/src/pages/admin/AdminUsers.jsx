import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import {
  getAllUsers, createUser, updateUser, toggleUserStatus,
  changeUserRole, resetUserPassword, deleteUser
} from '../../api/adminApi';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

function formatDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}
function formatRelativeTime(d) {
  if (!d) return '—';
  const diff = Date.now() - new Date(d);
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'Just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}
function useDebounce(val, delay) {
  const [dv, setDv] = useState(val);
  useEffect(() => { const t = setTimeout(() => setDv(val), delay); return () => clearTimeout(t); }, [val, delay]);
  return dv;
}

const COLORS = {
  primary: '#0058be', surface: '#ffffff', surfaceLow: '#f0f3ff',
  outline: '#c2c6d6', onSurface: '#111c2d', onVariant: '#424754',
  error: '#ba1a1a', errorContainer: '#ffdad6', tertiary: '#006947',
  tertiaryBg: 'rgba(0,105,71,0.1)', primaryBg: 'rgba(0,88,190,0.08)',
};

const pill = (label, color, bg) => (
  <span style={{ display:'inline-flex', alignItems:'center', gap:5, padding:'3px 10px',
    borderRadius:99, fontSize:12, fontWeight:700, background:bg, color }}>
    {label}
  </span>
);

export default function AdminUsers() {
  const { user } = useAuth();
  const addToast = useToast();
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(15);
  const [search, setSearch] = useState('');
  const [filterRole, setFilterRole] = useState('');
  const [filterActive, setFilterActive] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const debouncedSearch = useDebounce(search, 350);

  // Modals
  const [selectedUser, setSelectedUser] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(null);
  const [showResetModal, setShowResetModal] = useState(null);
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Forms
  const [createForm, setCreateForm] = useState({ firstName:'', lastName:'', email:'', password:'', role:'USER' });
  const [editForm, setEditForm] = useState({ firstName:'', lastName:'', email:'' });
  const [resetPassword, setResetPassword] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size, sortBy, direction: sortDir };
      if (debouncedSearch) params.search = debouncedSearch;
      if (filterRole) params.role = filterRole;
      if (filterActive !== '') params.active = filterActive;
      const data = await getAllUsers(params);
      setUsers(data.content || []);
      setTotal(data.totalElements || 0);
    } catch {
      addToast('Failed to load users', 'error');
    } finally {
      setLoading(false);
    }
  }, [page, size, sortBy, sortDir, debouncedSearch, filterRole, filterActive, addToast]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { setPage(0); }, [debouncedSearch, filterRole, filterActive]);

  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;

  const totalPages = Math.ceil(total / size);

  const handleCreate = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      await createUser(createForm);
      addToast('User created successfully', 'success');
      setShowCreateModal(false);
      setCreateForm({ firstName:'', lastName:'', email:'', password:'', role:'USER' });
      load();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to create user', 'error');
    } finally { setActionLoading(false); }
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    try {
      await updateUser(showEditModal.id, editForm);
      addToast('User updated successfully', 'success');
      setShowEditModal(null);
      load();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to update user', 'error');
    } finally { setActionLoading(false); }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (resetPassword.length < 8) { addToast('Password must be at least 8 characters', 'error'); return; }
    setActionLoading(true);
    try {
      await resetUserPassword(showResetModal.id, resetPassword);
      addToast('Password reset successfully', 'success');
      setShowResetModal(null);
      setResetPassword('');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to reset password', 'error');
    } finally { setActionLoading(false); }
  };

  const confirm = (title, message, action) => setConfirmDialog({ title, message, action });
  const runConfirm = async () => {
    if (!confirmDialog) return;
    setActionLoading(true);
    try { await confirmDialog.action(); } finally {
      setActionLoading(false);
      setConfirmDialog(null);
    }
  };

  const handleToggle = (u) => confirm(
    u.active ? 'Disable User' : 'Enable User',
    `Are you sure you want to ${u.active ? 'disable' : 'enable'} ${u.email}?`,
    async () => {
      await toggleUserStatus(u.id, !u.active);
      addToast(`User ${u.active ? 'disabled' : 'enabled'} successfully`, 'success');
      load();
    }
  );

  const handleRole = (u) => {
    const newRole = u.role === 'ADMIN' ? 'USER' : 'ADMIN';
    confirm(
      `Change Role to ${newRole}`,
      `Change ${u.email}'s role from ${u.role} to ${newRole}?`,
      async () => {
        await changeUserRole(u.id, newRole);
        addToast('Role updated successfully', 'success');
        load();
      }
    );
  };

  const handleDelete = (u) => confirm(
    'Delete User',
    `Permanently delete ${u.email}? This cannot be undone.`,
    async () => {
      await deleteUser(u.id);
      addToast('User deleted', 'success');
      load();
    }
  );

  const openEdit = (u) => {
    setEditForm({ firstName: u.firstName, lastName: u.lastName, email: u.email });
    setShowEditModal(u);
  };

  const inputStyle = {
    width:'100%', padding:'8px 12px', border:`1px solid ${COLORS.outline}`,
    borderRadius:8, outline:'none', fontSize:13.5, fontFamily:'inherit',
    boxSizing:'border-box', background:'var(--snt-surface)'
  };
  const labelStyle = { display:'block', fontSize:12, fontWeight:600, color:COLORS.onVariant, marginBottom:4 };
  const btnPrimary = {
    display:'inline-flex', alignItems:'center', gap:6, padding:'8px 18px',
    background:COLORS.primary, color:'#fff', border:'none', borderRadius:8,
    fontWeight:600, fontSize:13, cursor:'pointer', fontFamily:'inherit'
  };
  const btnOutline = {
    ...btnPrimary, background:'#ffffff', color:'#0f172a',
    border:'1px solid #cbd5e1', boxShadow:'0 1px 2px rgba(15,23,42,0.04)'
  };
  const btnDanger = { ...btnOutline, color:COLORS.error, borderColor:COLORS.error };

  const modalOverlay = {
    position:'fixed', inset:0, zIndex:9999, display:'flex', alignItems:'center',
    justifyContent:'center', padding:12, background:'rgba(15,23,42,0.45)'
  };
  const modalBox = (maxW = 448) => ({
    background:'#ffffff', borderRadius:16, boxShadow:'0 20px 40px rgba(15,23,42,0.15)',
    border:'1px solid #cbd5e1', width:'100%', maxWidth:maxW,
    padding:20, fontFamily:"'Manrope',sans-serif", maxHeight:'90vh', overflowY:'auto'
  });

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", color:COLORS.onSurface, padding:0 }}>
      <style>{`
        .au-row:hover { background: #f5f8ff !important; }
        .au-row td { padding:12px 16px; border-bottom:1px solid #f0f3ff; font-size:13.5px; vertical-align:middle; }
        .au-th { padding:10px 16px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:${COLORS.onVariant}; border-bottom:2px solid #e8eef8; text-align:left; }
        .au-btn-sm { display:inline-flex; align-items:center; justify-content:center; gap:4px; padding:6px 10px; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer; border:1px solid transparent; font-family:inherit; transition:all 0.15s; min-height:32px; }

        .au-table-wrapper { display: block; }
        .au-cards-wrapper { display: none; }

        @media (max-width: 767px) {
          .au-table-wrapper { display: none !important; }
          .au-cards-wrapper { display: flex !important; flex-direction: column; gap: 12px; }
          .au-filter-bar {
            display: grid !important;
            grid-template-columns: 1fr 1fr !important;
            gap: 8px !important;
          }
          .au-search-box {
            grid-column: 1 / -1 !important;
            max-width: 100% !important;
          }
          .au-filter-select {
            width: 100% !important;
            min-width: 0 !important;
          }
        }
      `}</style>

      {/* Header */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:12, marginBottom:20 }}>
        <div>
          <h1 style={{ margin:0, fontSize:22, fontWeight:800, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>User Management</h1>
          <p style={{ margin:'4px 0 0', fontSize:13.5, color:COLORS.onVariant }}>
            {total.toLocaleString()} users in platform
          </p>
        </div>
        <button style={btnPrimary} onClick={() => setShowCreateModal(true)}>
          <span className="material-symbols-outlined" style={{ fontSize:16 }}>person_add</span>
          Add User
        </button>
      </div>

      {/* Filters */}
      <div className="au-filter-bar" style={{ display:'flex', gap:10, flexWrap:'wrap', marginBottom:16 }}>
        <div className="au-search-box" style={{ display:'flex', alignItems:'center', gap:6, background:'#ffffff',
          border:'1px solid #cbd5e1', borderRadius:10, padding:'6px 14px', flex:'1', minWidth:180, maxWidth:320, boxShadow:'0 1px 2px rgba(15,23,42,0.04)' }}>
          <span className="material-symbols-outlined" style={{ fontSize:16, color:'#64748b' }}>search</span>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search users..."
            style={{ border:'none', background:'transparent', outline:'none', fontSize:13.5, width:'100%', color:'#0f172a' }} />
        </div>
        <select className="au-filter-select" value={filterRole} onChange={e => setFilterRole(e.target.value)}
          style={{ ...inputStyle, width:'auto', minWidth:120, borderRadius:10, padding:'6px 12px', border:'1px solid #cbd5e1', background:'#ffffff' }}>
          <option value="">All Roles</option>
          <option value="USER">Members</option>
          <option value="ADMIN">Admins</option>
        </select>
        <select className="au-filter-select" value={filterActive} onChange={e => setFilterActive(e.target.value)}
          style={{ ...inputStyle, width:'auto', minWidth:120, borderRadius:10, padding:'6px 12px', border:'1px solid #cbd5e1', background:'#ffffff' }}>
          <option value="">All Status</option>
          <option value="true">Active</option>
          <option value="false">Disabled</option>
        </select>
      </div>

      {/* Mobile Card List View (< 768px) */}
      <div className="au-cards-wrapper">
        {loading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:48 }}><LoadingSpinner size="lg" /></div>
        ) : users.length === 0 ? (
          <div style={{ textAlign:'center', padding:32, background:'#fff', borderRadius:14, border:'1px solid #e2e8f0', color:COLORS.onVariant }}>
            No users found
          </div>
        ) : (
          users.map(u => (
            <div key={u.id} style={{
              background:'#ffffff', borderRadius:14, border:'1px solid #e2e8f0', padding:16,
              boxShadow:'0 1px 3px rgba(15,23,42,0.04)', display:'flex', flexDirection:'column', gap:12
            }}>
              <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', gap:8 }}>
                <div style={{ display:'flex', alignItems:'center', gap:10, minWidth:0 }} onClick={() => setSelectedUser(u)}>
                  <div style={{ width:38, height:38, borderRadius:'50%', background:COLORS.primaryBg,
                    color:COLORS.primary, display:'flex', alignItems:'center', justifyContent:'center',
                    fontSize:13, fontWeight:700, flexShrink:0 }}>
                    {(u.firstName?.[0]||'') + (u.lastName?.[0]||'')}
                  </div>
                  <div style={{ minWidth:0 }}>
                    <div style={{ fontWeight:700, fontSize:14.5, color:COLORS.onSurface, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                      {u.firstName} {u.lastName}
                    </div>
                    <div style={{ fontSize:12.5, color:COLORS.onVariant, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                      {u.email}
                    </div>
                  </div>
                </div>
                <div style={{ display:'flex', gap:6, flexShrink:0 }}>
                  {u.role === 'ADMIN'
                    ? pill('Admin', COLORS.primary, COLORS.primaryBg)
                    : pill('Member', COLORS.onVariant, '#f0f3ff')}
                  {u.active
                    ? pill('Active', COLORS.tertiary, COLORS.tertiaryBg)
                    : pill('Disabled', COLORS.error, COLORS.errorContainer)}
                </div>
              </div>

              <div style={{ display:'flex', justifyContent:'space-between', fontSize:12, color:COLORS.onVariant, padding:'8px 10px', background:'#f8fafc', borderRadius:8 }}>
                <span>Projects: <strong>{u.projectCount ?? 0}</strong></span>
                <span>Joined: <strong>{formatDate(u.createdAt)}</strong></span>
                <span>Login: <strong>{formatRelativeTime(u.lastLogin)}</strong></span>
              </div>

              <div style={{ display:'grid', gridTemplateColumns:'repeat(5, 1fr)', gap:6, paddingTop:4, borderTop:'1px solid #f1f5f9' }}>
                <button className="au-btn-sm" style={{ background:'#f0f3ff', color:COLORS.primary }}
                  onClick={() => openEdit(u)} title="Edit user">
                  <span className="material-symbols-outlined" style={{ fontSize:15 }}>edit</span>
                </button>
                <button className="au-btn-sm" style={{ background:'#f0f3ff', color:COLORS.onVariant }}
                  onClick={() => { setShowResetModal(u); setResetPassword(''); }} title="Reset password">
                  <span className="material-symbols-outlined" style={{ fontSize:15 }}>lock_reset</span>
                </button>
                <button className="au-btn-sm"
                  style={{ background: u.active ? COLORS.errorContainer : COLORS.tertiaryBg,
                    color: u.active ? COLORS.error : COLORS.tertiary }}
                  onClick={() => handleToggle(u)} title={u.active ? 'Disable' : 'Enable'}
                  disabled={u.email === user?.email}>
                  <span className="material-symbols-outlined" style={{ fontSize:15 }}>
                    {u.active ? 'person_off' : 'person'}
                  </span>
                </button>
                <button className="au-btn-sm"
                  style={{ background:'#f0f3ff', color:COLORS.onVariant }}
                  onClick={() => handleRole(u)} title="Change Role"
                  disabled={u.email === user?.email}>
                  <span className="material-symbols-outlined" style={{ fontSize:15 }}>
                    {u.role === 'ADMIN' ? 'arrow_downward' : 'arrow_upward'}
                  </span>
                </button>
                <button className="au-btn-sm"
                  style={{ background:COLORS.errorContainer, color:COLORS.error }}
                  onClick={() => handleDelete(u)} title="Delete user"
                  disabled={u.email === user?.email}>
                  <span className="material-symbols-outlined" style={{ fontSize:15 }}>delete</span>
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Desktop/Tablet Table View (>= 768px) */}
      <div className="au-table-wrapper" style={{ background:'#ffffff', borderRadius:16, border:'1px solid #e2e8f0', boxShadow:'0 1px 3px rgba(15,23,42,0.04)', overflow:'hidden', transition: 'border-color 0.2s ease, box-shadow 0.2s ease' }}
        onMouseEnter={e => { e.currentTarget.style.borderColor = '#93c5fd'; e.currentTarget.style.boxShadow = '0 4px 18px rgba(37,99,235,0.1), 0 1px 4px rgba(37,99,235,0.04)' }}
        onMouseLeave={e => { e.currentTarget.style.borderColor = '#e2e8f0'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(15,23,42,0.04)' }}
      >
        {loading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:48 }}><LoadingSpinner size="lg" /></div>
        ) : (
          <div style={{ overflowX:'auto' }}>
            <table style={{ width:'100%', borderCollapse:'collapse', minWidth:720 }}>
              <thead style={{ background:'#f8fafc', borderBottom:'1px solid #cbd5e1' }}>
                <tr>
                  <th className="au-th">User</th>
                  <th className="au-th">Email</th>
                  <th className="au-th">Role</th>
                  <th className="au-th">Status</th>
                  <th className="au-th">Projects</th>
                  <th className="au-th">Registered</th>
                  <th className="au-th">Last Login</th>
                  <th className="au-th" style={{ textAlign:'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 ? (
                  <tr><td colSpan={8} style={{ textAlign:'center', padding:40, color:COLORS.onVariant }}>
                    No users found
                  </td></tr>
                ) : users.map(u => (
                  <tr key={u.id} className="au-row" style={{ cursor:'pointer' }} onClick={() => setSelectedUser(u)}>
                    <td>
                      <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                        <div style={{ width:34, height:34, borderRadius:'50%', background:COLORS.primaryBg,
                          color:COLORS.primary, display:'flex', alignItems:'center', justifyContent:'center',
                          fontSize:12, fontWeight:700, flexShrink:0 }}>
                          {(u.firstName?.[0]||'') + (u.lastName?.[0]||'')}
                        </div>
                        <span style={{ fontWeight:600 }}>{u.firstName} {u.lastName}</span>
                      </div>
                    </td>
                    <td style={{ color:COLORS.onVariant }}>{u.email}</td>
                    <td>{u.role === 'ADMIN'
                      ? pill('Admin', COLORS.primary, COLORS.primaryBg)
                      : pill('Member', COLORS.onVariant, '#f0f3ff')}</td>
                    <td>{u.active
                      ? pill('Active', COLORS.tertiary, COLORS.tertiaryBg)
                      : pill('Disabled', COLORS.error, COLORS.errorContainer)}</td>
                    <td style={{ fontWeight:600 }}>{u.projectCount ?? 0}</td>
                    <td style={{ color:COLORS.onVariant }}>{formatDate(u.createdAt)}</td>
                    <td style={{ color:COLORS.onVariant }}>{formatRelativeTime(u.lastLogin)}</td>
                    <td onClick={e => e.stopPropagation()}>
                      <div style={{ display:'flex', gap:4, justifyContent:'flex-end', flexWrap:'wrap' }}>
                        <button className="au-btn-sm" style={{ background:'#f0f3ff', color:COLORS.primary }}
                          onClick={() => openEdit(u)} title="Edit user">
                          <span className="material-symbols-outlined" style={{ fontSize:13 }}>edit</span>
                        </button>
                        <button className="au-btn-sm" style={{ background:'#f0f3ff', color:COLORS.onVariant }}
                          onClick={() => { setShowResetModal(u); setResetPassword(''); }} title="Reset password">
                          <span className="material-symbols-outlined" style={{ fontSize:13 }}>lock_reset</span>
                        </button>
                        <button className="au-btn-sm"
                          style={{ background: u.active ? COLORS.errorContainer : COLORS.tertiaryBg,
                            color: u.active ? COLORS.error : COLORS.tertiary }}
                          onClick={() => handleToggle(u)} title={u.active ? 'Disable' : 'Enable'}
                          disabled={u.email === user?.email}>
                          <span className="material-symbols-outlined" style={{ fontSize:13 }}>
                            {u.active ? 'person_off' : 'person'}
                          </span>
                        </button>
                        <button className="au-btn-sm"
                          style={{ background:'#f0f3ff', color:COLORS.onVariant }}
                          onClick={() => handleRole(u)} title="Change Role"
                          disabled={u.email === user?.email}>
                          <span className="material-symbols-outlined" style={{ fontSize:13 }}>
                            {u.role === 'ADMIN' ? 'arrow_downward' : 'arrow_upward'}
                          </span>
                        </button>
                        <button className="au-btn-sm"
                          style={{ background:COLORS.errorContainer, color:COLORS.error }}
                          onClick={() => handleDelete(u)} title="Delete user"
                          disabled={u.email === user?.email}>
                          <span className="material-symbols-outlined" style={{ fontSize:13 }}>delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center',
            padding:'12px 16px', borderTop:`1px solid #f0f3ff`, background:'var(--snt-surface-2)', flexWrap:'wrap', gap:8 }}>
            <span style={{ fontSize:13, color:COLORS.onVariant }}>
              Showing {page * size + 1}–{Math.min((page + 1) * size, total)} of {total}
            </span>
            <div style={{ display:'flex', gap:4 }}>
              <button style={btnOutline} disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
              <span style={{ padding:'8px 14px', fontSize:13, fontWeight:600 }}>{page + 1} / {totalPages}</span>
              <button style={btnOutline} disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
            </div>
          </div>
        )}
      </div>

      {/* Mobile Pagination for cards (< 768px) */}
      {totalPages > 1 && (
        <div className="au-cards-wrapper" style={{ marginTop: 12 }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center',
            padding:'12px 16px', background:'#ffffff', borderRadius:12, border:'1px solid #e2e8f0', flexWrap:'wrap', gap:8 }}>
            <span style={{ fontSize:12.5, color:COLORS.onVariant }}>
              {page + 1} of {totalPages} pages ({total} users)
            </span>
            <div style={{ display:'flex', gap:6 }}>
              <button style={btnOutline} disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
              <button style={btnOutline} disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
            </div>
          </div>
        </div>
      )}

      {/* User Detail Modal */}
      {selectedUser && (
        <div style={modalOverlay} onClick={() => setSelectedUser(null)}>
          <div style={modalBox(480)} onClick={e => e.stopPropagation()}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
              <h3 style={{ margin:0, fontSize:18, fontWeight:700, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>User Details</h3>
              <button onClick={() => setSelectedUser(null)} style={{ background:'none', border:'none', cursor:'pointer', padding:4 }}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div style={{ display:'flex', gap:12, alignItems:'center', marginBottom:20 }}>
              <div style={{ width:52, height:52, borderRadius:'50%', background:COLORS.primaryBg, color:COLORS.primary,
                display:'flex', alignItems:'center', justifyContent:'center', fontSize:20, fontWeight:700 }}>
                {(selectedUser.firstName?.[0]||'') + (selectedUser.lastName?.[0]||'')}
              </div>
              <div>
                <p style={{ margin:0, fontWeight:700, fontSize:16 }}>{selectedUser.firstName} {selectedUser.lastName}</p>
                <p style={{ margin:'2px 0 0', color:COLORS.onVariant, fontSize:13.5 }}>{selectedUser.email}</p>
              </div>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:10, marginBottom:20 }}>
              {[
                ['Role', selectedUser.role],
                ['Status', selectedUser.active ? 'Active' : 'Disabled'],
                ['Projects', selectedUser.projectCount ?? 0],
                ['Email Verified', selectedUser.emailVerified ? 'Yes' : 'No'],
                ['Registered', formatDate(selectedUser.createdAt)],
                ['Last Login', formatRelativeTime(selectedUser.lastLogin)],
              ].map(([k, v]) => (
                <div key={k} style={{ padding:12, background:'#f5f8ff', borderRadius:8 }}>
                  <p style={{ margin:0, fontSize:11, fontWeight:600, color:COLORS.onVariant, textTransform:'uppercase' }}>{k}</p>
                  <p style={{ margin:'4px 0 0', fontWeight:700, fontSize:14 }}>{String(v)}</p>
                </div>
              ))}
            </div>
            <div style={{ display:'flex', justifyContent:'flex-end', gap:8 }}>
              <button style={btnOutline} onClick={() => setSelectedUser(null)}>Close</button>
              <button style={btnPrimary} onClick={() => { openEdit(selectedUser); setSelectedUser(null); }}>
                Edit User
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div style={modalOverlay}>
          <div style={modalBox()}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
              <h3 style={{ margin:0, fontSize:18, fontWeight:700, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Create User</h3>
              <button onClick={() => setShowCreateModal(false)} style={{ background:'none', border:'none', cursor:'pointer' }}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <form onSubmit={handleCreate} style={{ display:'flex', flexDirection:'column', gap:14 }}>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
                <div><label style={labelStyle}>First Name</label>
                  <input required style={inputStyle} value={createForm.firstName}
                    onChange={e => setCreateForm(f => ({ ...f, firstName: e.target.value }))} /></div>
                <div><label style={labelStyle}>Last Name</label>
                  <input required style={inputStyle} value={createForm.lastName}
                    onChange={e => setCreateForm(f => ({ ...f, lastName: e.target.value }))} /></div>
              </div>
              <div><label style={labelStyle}>Email</label>
                <input required type="email" style={inputStyle} value={createForm.email}
                  onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))} /></div>
              <div><label style={labelStyle}>Password</label>
                <input required type="password" minLength={8} style={inputStyle} value={createForm.password}
                  onChange={e => setCreateForm(f => ({ ...f, password: e.target.value }))} /></div>
              <div><label style={labelStyle}>Role</label>
                <select style={inputStyle} value={createForm.role}
                  onChange={e => setCreateForm(f => ({ ...f, role: e.target.value }))}>
                  <option value="USER">Member (USER)</option>
                  <option value="ADMIN">Administrator (ADMIN)</option>
                </select></div>
              <div style={{ display:'flex', justifyContent:'flex-end', gap:8, marginTop:4 }}>
                <button type="button" style={btnOutline} onClick={() => setShowCreateModal(false)}>Cancel</button>
                <button type="submit" style={btnPrimary} disabled={actionLoading}>
                  {actionLoading ? 'Creating...' : 'Create User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {showEditModal && (
        <div style={modalOverlay}>
          <div style={modalBox()}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
              <h3 style={{ margin:0, fontSize:18, fontWeight:700, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Edit User</h3>
              <button onClick={() => setShowEditModal(null)} style={{ background:'none', border:'none', cursor:'pointer' }}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <form onSubmit={handleEdit} style={{ display:'flex', flexDirection:'column', gap:14 }}>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
                <div><label style={labelStyle}>First Name</label>
                  <input required style={inputStyle} value={editForm.firstName}
                    onChange={e => setEditForm(f => ({ ...f, firstName: e.target.value }))} /></div>
                <div><label style={labelStyle}>Last Name</label>
                  <input required style={inputStyle} value={editForm.lastName}
                    onChange={e => setEditForm(f => ({ ...f, lastName: e.target.value }))} /></div>
              </div>
              <div><label style={labelStyle}>Email</label>
                <input required type="email" style={inputStyle} value={editForm.email}
                  onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} /></div>
              <div style={{ display:'flex', justifyContent:'flex-end', gap:8 }}>
                <button type="button" style={btnOutline} onClick={() => setShowEditModal(null)}>Cancel</button>
                <button type="submit" style={btnPrimary} disabled={actionLoading}>
                  {actionLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showResetModal && (
        <div style={modalOverlay}>
          <div style={modalBox(400)}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 }}>
              <h3 style={{ margin:0, fontSize:18, fontWeight:700, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Reset Password</h3>
              <button onClick={() => setShowResetModal(null)} style={{ background:'none', border:'none', cursor:'pointer' }}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <p style={{ margin:'0 0 16px', fontSize:13.5, color:COLORS.onVariant }}>
              Set a new password for <strong>{showResetModal.email}</strong>
            </p>
            <form onSubmit={handleResetPassword} style={{ display:'flex', flexDirection:'column', gap:14 }}>
              <div><label style={labelStyle}>New Password (min 8 chars)</label>
                <input required type="password" minLength={8} style={inputStyle} value={resetPassword}
                  onChange={e => setResetPassword(e.target.value)} /></div>
              <div style={{ display:'flex', justifyContent:'flex-end', gap:8 }}>
                <button type="button" style={btnOutline} onClick={() => setShowResetModal(null)}>Cancel</button>
                <button type="submit" style={btnPrimary} disabled={actionLoading}>
                  {actionLoading ? 'Resetting...' : 'Reset Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirm Dialog */}
      {confirmDialog && (
        <div style={modalOverlay}>
          <div style={modalBox(380)}>
            <h3 style={{ margin:'0 0 10px', fontSize:17, fontWeight:700, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>
              {confirmDialog.title}
            </h3>
            <p style={{ margin:'0 0 20px', fontSize:13.5, color:COLORS.onVariant }}>{confirmDialog.message}</p>
            <div style={{ display:'flex', justifyContent:'flex-end', gap:8 }}>
              <button style={btnOutline} onClick={() => setConfirmDialog(null)} disabled={actionLoading}>Cancel</button>
              <button style={btnDanger} onClick={runConfirm} disabled={actionLoading}>
                {actionLoading ? 'Processing...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
