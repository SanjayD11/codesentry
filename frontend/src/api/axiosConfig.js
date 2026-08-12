import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Inject JWT on every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isRefreshing = false;

// Handle 401 → redirect to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      if (!isRefreshing) {
        isRefreshing = true;
        // Dispatch a custom event so the React app can show a single toast
        window.dispatchEvent(new Event('session-expired'));
        
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        
        // Wait briefly for the toast to be seen before redirecting
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
      }
    }
    return Promise.reject(err)
  }
)

export default api
