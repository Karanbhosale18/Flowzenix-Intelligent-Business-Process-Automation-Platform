import axios from 'axios'

// Support VITE_API_BASE_URL (standard) with fallback to VITE_API_URL and localhost dev default.
const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080'
const sanitizedBase = rawBaseUrl.replace(/\/+$/, '')
const API_BASE_URL = sanitizedBase.endsWith('/api') ? sanitizedBase : `${sanitizedBase}/api`

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Attach the JWT to every outgoing request, if we have a valid non-empty token.
// Strictly guards against sending "Bearer null", "Bearer undefined", or empty tokens.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token && token !== 'null' && token !== 'undefined' && token.trim() !== '') {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    delete config.headers.Authorization
  }
  return config
})

// If the backend responds 401 (token missing/expired/invalid),
// clear local auth state and bounce back to /login.
// 403 (forbidden/insufficient permission) is preserved without clearing state.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
