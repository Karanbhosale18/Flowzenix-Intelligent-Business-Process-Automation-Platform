import api from './api'

const AiAssistantService = {
  parseRequest: (prompt) => api.post('/ai/parse-request', { prompt }),
}

export default AiAssistantService
