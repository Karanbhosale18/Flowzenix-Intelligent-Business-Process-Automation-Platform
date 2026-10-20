import api from './api'

const AuthService = {
  async login(username, password) {
    const response = await api.post('/auth/login', { username, password })
    if (response.data && response.data.token) {
      localStorage.setItem('token', response.data.token)
      localStorage.setItem(
        'user',
        JSON.stringify({
          id: response.data.id,
          username: response.data.username,
          email: response.data.email,
          roles: response.data.roles,
        })
      )
    }
    return response.data
  },

  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  },

  getCurrentUser() {
    try {
      const raw = localStorage.getItem('user')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  },

  isAuthenticated() {
    const token = localStorage.getItem('token')
    return !!token && token !== 'null' && token !== 'undefined' && token.trim() !== ''
  },
}

export default AuthService
