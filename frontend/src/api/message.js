import request from '@/utils/request'

export function getMessages(params) {
  return request.get('/messages', { params })
}

export function markRead(id) {
  return request.put(`/messages/${id}/read`)
}

export function markAllRead() {
  return request.put('/messages/read-all')
}
