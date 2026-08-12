import api from './axiosConfig'

export const getProjects = (params) => api.get('/projects', { params })
export const getProject = (id) => api.get(`/projects/${id}`)
export const createProject = (data) => api.post('/projects', data)
export const updateProject = (id, data) => api.put(`/projects/${id}`, data)
export const deleteProject = (id) => api.delete(`/projects/${id}`)
export const getProjectStats = (id) => api.get(`/projects/${id}/statistics`)
export const searchProjects = (params) => api.get('/projects/search', { params })
