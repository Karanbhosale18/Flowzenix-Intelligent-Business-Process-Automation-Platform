import api from './api'

const WorkflowService = {
  list(includeInactive = true) {
    return api.get('/admin/workflows', { params: { includeInactive } }).then((r) => r.data)
  },
  get(id) {
    return api.get(`/admin/workflows/${id}`).then((r) => r.data)
  },
  listActive() {
    return api.get('/workflows/active').then((r) => r.data)
  },
  create(payload) {
    return api.post('/admin/workflows', payload).then((r) => r.data)
  },
  update(id, payload) {
    return api.put(`/admin/workflows/${id}`, payload).then((r) => r.data)
  },
  addStep(workflowId, payload) {
    return api.post(`/admin/workflows/${workflowId}/steps`, payload).then((r) => r.data)
  },
  updateStep(workflowId, stepId, payload) {
    return api.put(`/admin/workflows/${workflowId}/steps/${stepId}`, payload).then((r) => r.data)
  },
  deleteStep(workflowId, stepId) {
    return api.delete(`/admin/workflows/${workflowId}/steps/${stepId}`)
  },
  reorder(workflowId, stepIds) {
    return api.put(`/admin/workflows/${workflowId}/steps/reorder`, { stepIds }).then((r) => r.data)
  },
  activate(id) {
    return api.post(`/admin/workflows/${id}/activate`).then((r) => r.data)
  },
  deactivate(id) {
    return api.post(`/admin/workflows/${id}/deactivate`).then((r) => r.data)
  },
}

export default WorkflowService
