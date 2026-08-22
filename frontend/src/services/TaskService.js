import api from './api'

const TaskService = {
  async listMine() {
    const response = await api.get('/tasks/my')
    return response.data
  },

  async approve(taskId, comment) {
    const response = await api.post(`/tasks/${taskId}/approve`, { comment })
    return response.data
  },

  async reject(taskId, comment) {
    const response = await api.post(`/tasks/${taskId}/reject`, { comment })
    return response.data
  },

  async requestInformation(taskId, comment) {
    const response = await api.post(`/tasks/${taskId}/request-information`, { comment })
    return response.data
  },
}

export default TaskService
