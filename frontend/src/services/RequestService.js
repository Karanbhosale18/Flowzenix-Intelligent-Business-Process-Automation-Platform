import api from './api'

const RequestService = {
  async create(payload) {
    // payload: { requestType, title, description, priority, metadata }
    const response = await api.post('/requests', payload)
    return response.data
  },

  async listMine() {
    const response = await api.get('/requests')
    return response.data
  },

  async getById(id) {
    const response = await api.get(`/requests/${id}`)
    return response.data
  },

  async cancel(id) {
    const response = await api.post(`/requests/${id}/cancel`)
    return response.data
  },

  async provideInformation(id, data) {
    const response = await api.post(`/requests/${id}/provide-information`, data)
    return response.data
  },
}

export default RequestService
