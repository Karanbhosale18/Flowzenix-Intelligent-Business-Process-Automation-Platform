import api from './api'

const TaskService = {
  // `statusOrStatuses` is optional: omit for the pending inbox (unchanged
  // default), pass a single status ('APPROVED'), an array (['APPROVED',
  // 'REJECTED']), or 'ALL' for the full decision history.
  async listMine(statusOrStatuses) {
    const params = {}
    if (statusOrStatuses) {
      params.status = Array.isArray(statusOrStatuses) ? statusOrStatuses.join(',') : statusOrStatuses
    }
    const response = await api.get('/tasks/my', { params })
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