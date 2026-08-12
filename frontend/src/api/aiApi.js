import api from './axiosConfig'

export const enrichVulnerability = (vulnId) =>
  api.post(`/ai/enrich/vulnerability/${vulnId}`)

export const enrichScan = (scanId) =>
  api.post(`/ai/enrich/scan/${scanId}`)

export const getEnrichment = (vulnId) =>
  api.get(`/ai/enrich/vulnerability/${vulnId}`)

export const retryVulnerability = (vulnId) =>
  api.post(`/ai/enrich/retry/${vulnId}`)
