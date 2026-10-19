import api from './api'

const AdminUserService = {
  getUsers: (params = {}) => api.get('/admin/users', { params }),
  getManagers: () => api.get('/admin/users/managers'),
  createUser: (userData) => api.post('/admin/users', userData),
  updateUser: (userId, userData) => api.put(`/admin/users/${userId}`, userData),
  getDirectReports: (userId) => api.get(`/admin/users/${userId}/direct-reports`),
}

export default AdminUserService
