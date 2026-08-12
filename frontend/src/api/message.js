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

export function markAllNotificationsRead() {
  return request.put('/messages/notifications/read-all')
}

export function sendPrivateMessage(data) {
  return request.post('/messages/private', data)
}

export function getConversation(params) {
  return request.get('/messages/conversation', { params })
}

export function getConversations(params) {
  return request.get('/messages/conversations', { params })
}

export function getUnreadCount() {
  return request.get('/messages/unread-count')
}

export function getNotifications(params) {
  return request.get('/messages/notifications', { params })
}
