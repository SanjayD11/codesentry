import axios from 'axios'
import { isTokenExpired, clearAuthStorage } from '../utils/tokenUtils'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Inject JWT on every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    if (isTokenExpired(token)) {
      clearAuthStorage()
    } else {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

let redirectingToLogin = false;

// Handle 401 → redirect to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      // Only trigger once per session expiry cycle.
      const hadToken = !!localStorage.getItem('token')
      clearAuthStorage()

      if (!redirectingToLogin) {
        redirectingToLogin = true

        if (hadToken) {
          // User was genuinely logged in — show session-expired message
          window.dispatchEvent(new Event('session-expired'))
          setTimeout(() => {
            redirectingToLogin = false
            window.location.href = '/login'
          }, 1500)
        } else {
          // No token at all — silent redirect, no toast spam
          redirectingToLogin = false
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

export default api

