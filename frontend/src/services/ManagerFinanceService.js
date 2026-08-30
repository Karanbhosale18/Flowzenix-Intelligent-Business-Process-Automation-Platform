import api from './api'

const ManagerFinanceService = {
  async getAssignment() {
    const response = await api.get('/manager/finance-manager')
    return response.data
  },

  async updateAssignment(financeManagerId) {
    const response = await api.put('/manager/finance-manager', { financeManagerId })
    return response.data
  },
}

export default ManagerFinanceService
