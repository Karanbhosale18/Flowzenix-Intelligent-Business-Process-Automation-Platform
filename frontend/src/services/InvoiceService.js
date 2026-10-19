import api from './api'

const InvoiceService = {
  extract: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/invoices/extract', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  upload: (formData) => {
    return api.post('/invoices/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  getById: (id) => api.get(`/invoices/${id}`),

  checkDuplicate: (invoiceNumber) =>
    api.get('/invoices/check-duplicate', { params: { invoiceNumber } }),
}

export default InvoiceService
