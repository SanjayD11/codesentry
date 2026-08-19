import api from './axiosConfig'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1'

// ── Dashboard ─────────────────────────────────────────────────────────────────
export const getAdminStats = () => api.get('/admin/stats').then(r => r.data.data)
export const getSystemHealth = () => api.get('/admin/health').then(r => r.data.data)

// ── Users ──────────────────────────────────────────────────────────────────────
export const getAllUsers = (params) => api.get('/admin/users', { params }).then(r => r.data.data)
export const getUserById = (userId) => api.get(`/admin/users/${userId}`).then(r => r.data.data)
export const createUser = (data) => api.post('/admin/users', data).then(r => r.data.data)
export const updateUser = (userId, data) => api.put(`/admin/users/${userId}`, data).then(r => r.data.data)
export const toggleUserStatus = (userId, active) => api.patch(`/admin/users/${userId}/status`, { active }).then(r => r.data)
export const changeUserRole = (userId, role) => api.patch(`/admin/users/${userId}/role`, { role }).then(r => r.data)
export const resetUserPassword = (userId, newPassword) => api.patch(`/admin/users/${userId}/reset-password`, { newPassword }).then(r => r.data)
export const deleteUser = (userId) => api.delete(`/admin/users/${userId}`).then(r => r.data)

// ── Projects ───────────────────────────────────────────────────────────────────
export const getAllProjects = (params) => api.get('/admin/projects', { params }).then(r => r.data.data)
export const archiveProject = (projectId) => api.patch(`/admin/projects/${projectId}/archive`).then(r => r.data)
export const restoreProject = (projectId) => api.patch(`/admin/projects/${projectId}/restore`).then(r => r.data)
export const deleteProject = (projectId) => api.delete(`/admin/projects/${projectId}`).then(r => r.data)
export const transferProjectOwnership = (projectId, newOwnerId) => api.patch(`/admin/projects/${projectId}/transfer`, { newOwnerId }).then(r => r.data)

// ── Scans ──────────────────────────────────────────────────────────────────────
export const getAllScans = (params) => api.get('/admin/scans', { params }).then(r => r.data.data)

// ── Audit Logs ─────────────────────────────────────────────────────────────────
export const getAllAuditLogs = (params) => api.get('/admin/audit-logs', { params }).then(r => r.data.data)
export const exportAuditLogsCsv = async (params) => {
  const token = localStorage.getItem('token')
  const queryString = new URLSearchParams(Object.fromEntries(
    Object.entries(params || {}).filter(([, v]) => v != null && v !== '')
  )).toString()
  const response = await fetch(`${API_BASE}/admin/audit-logs/export${queryString ? '?' + queryString : ''}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!response.ok) throw new Error('Export failed')
  return response
}

// ── Settings ───────────────────────────────────────────────────────────────────
export const getAllSettings = () => api.get('/admin/settings').then(r => r.data.data)
export const updateSetting = (settingKey, value) => api.put(`/admin/settings/${settingKey}`, { value }).then(r => r.data.data)

// ── Data Export ────────────────────────────────────────────────────────────────
export const exportPlatformData = async (datasets, format) => {
  const token = localStorage.getItem('token')
  const response = await fetch(`${API_BASE}/admin/export`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ datasets, format })
  })
  if (!response.ok) throw new Error('Export failed')
  return response
}

