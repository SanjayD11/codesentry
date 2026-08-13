import api from './axiosConfig'

export const sendChat = (data, file) => {
  if (file) {
    const formData = new FormData()
    formData.append('message', data.message || '')
    if (data.conversationId) formData.append('conversationId', data.conversationId)
    if (data.scanId) formData.append('scanId', data.scanId)
    formData.append('file', file)
    return api.postForm('/chat', formData)
  }
  return api.post('/chat', data)
}
export const getConversation = (conversationId) =>
  api.get(`/chat/conversation/${conversationId}`)
export const listConversations = () => api.get('/chat/conversations')
export const deleteConversation = (conversationId) =>
  api.delete(`/chat/conversation/${conversationId}`)
