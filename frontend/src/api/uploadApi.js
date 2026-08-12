import api from './axiosConfig'
import axios from 'axios'

export const uploadFiles = (projectId, formData, overrideDuplicate = false) => {
  const token = localStorage.getItem('token')
  return axios.post(`/api/v1/uploads/${projectId}?overrideDuplicate=${overrideDuplicate}`, formData, {
    headers: { Authorization: `Bearer ${token}` }
  })
}

export const getProjectFiles = (projectId, params) =>
  api.get(`/uploads/project/${projectId}`, { params })

export const getFileMetadata = (fileId) => api.get(`/uploads/${fileId}`)
export const deleteFile = (fileId) => api.delete(`/uploads/${fileId}`)
export const restoreFile = (fileId) => api.put(`/uploads/restore/${fileId}`)
export const searchFiles = (params) => api.get('/uploads/search', { params })
export const getUploadStats = () => api.get('/uploads/statistics')

export const downloadFile = (fileId) =>
  api.get(`/uploads/download/${fileId}`, { responseType: 'blob' })
