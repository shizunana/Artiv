import request from '@/utils/request'

export function getBlockedUsers(params) {
  return request.get('/blocks/users', { params })
}

export function getUserBlockStatus(userId) {
  return request.get(`/blocks/users/${userId}/status`)
}

export function blockUser(userId) {
  return request.put(`/blocks/users/${userId}`)
}

export function unblockUser(userId) {
  return request.delete(`/blocks/users/${userId}`)
}

export function submitUserReport(data) {
  return request.post('/reports', data)
}
