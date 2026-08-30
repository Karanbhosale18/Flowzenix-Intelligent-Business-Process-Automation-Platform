import api from './api'

const AuthService = {
  async login(username, password) {
    const response = await api.post('/auth/login', { username, password })
    if (response.data.token) {
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

  async signup({ username, email, password, role, department, managerId, adminId, adminReferenceId, managerReferenceId }) {
    // Backend SignupRequest accepts an optional Set<String> `role`, plus
    // `department` and `managerId`. We only include the optional fields when
    // they're actually provided, so the backend's "default to EMPLOYEE"
    // behaviour still kicks in for a plain signup.
    const payload = { username, email, password }
    if (role) payload.role = [role]
    if (department && department.trim()) payload.department = department.trim()
    if (managerId !== undefined && managerId !== null && `${managerId}`.trim() !== '') {
      payload.managerId = Number(managerId)
    }
    if (adminId !== undefined && adminId !== null && `${adminId}`.trim() !== '') {
      payload.adminId = Number(adminId)
    }
    if (adminReferenceId !== undefined && adminReferenceId !== null && `${adminReferenceId}`.trim() !== '') {
      payload.adminReferenceId = Number(adminReferenceId)
    }
    if (managerReferenceId !== undefined && managerReferenceId !== null && `${managerReferenceId}`.trim() !== '') {
      payload.managerReferenceId = Number(managerReferenceId)
    }
    const response = await api.post('/auth/signup', payload)
    return response.data
  },

  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  },

  getCurrentUser() {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  },

  isAuthenticated() {
    return !!localStorage.getItem('token')
  },
}

export default AuthService
