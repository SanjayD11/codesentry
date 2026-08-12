import api from './axiosConfig'
import axios from 'axios'

/**
 * Triggers an async scan for a project.
 * @param {number} projectId - The project to scan
 * @param {object|null} config - Optional scan configuration (ScanConfigurationDto shape).
 *   If null/undefined, the backend applies defaults.
 */
export const triggerScan = (projectId, config = null) =>
  api.post(`/scan/${projectId}`, config ? { configuration: config } : undefined)

export const getScan = (scanId) => api.get(`/scan/${scanId}`)
export const getProjectScans = (projectId) => api.get(`/scan/project/${projectId}`)
export const getAllUserScans = () => api.get('/scan/my')
export const directScan = (data) => api.post('/scan/direct', data)
export const directScanFile = (formData) => {
  const token = localStorage.getItem('token')
  return axios.post('/api/v1/scan/direct/file', formData, {
    headers: { Authorization: `Bearer ${token}` }
  })
}
export const deleteScan = (scanId) => api.delete(`/scan/${scanId}`)
