import request from '@/utils/request'

export function getInvitations(params) {
  return request.get('/invitations', { params })
}

export function createInvitation(data) {
  return request.post('/invitations', data)
}

export function respondInvitation(id, status) {
  return request.put(`/invitations/${id}/respond`, null, { params: { status } })
}
