import api from './api'

const ProfileService = {
  async get() {
    const response = await api.get('/profile')
    return response.data
  },
  async update(payload) {
    const response = await api.put('/profile', payload)
    return response.data
  },
}

export default ProfileService
