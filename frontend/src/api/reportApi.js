import api from './axiosConfig'

export const generateReport = (scanId) => api.post(`/reports/generate/${scanId}`)
export const listMyReports = () => api.get('/reports/my')
export const getProjectReports = (projectId) => api.get(`/reports/project/${projectId}`)
export const deleteReport = (reportId) => api.delete(`/reports/${reportId}`)
export const downloadReport = (reportId) =>
  api.get(`/reports/${reportId}/download`, { responseType: 'blob' })
