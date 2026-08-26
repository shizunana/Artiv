import request from '@/utils/request'

export function createPublicInvitation(data) {
  return request.post('/invitations', data)
}

export function getInvitations(params, config = {}) {
  return request.get('/invitations', { ...config, params })
}

export function getInvitationByProject(projectId) {
  return request.get(`/invitations/project/${projectId}`, { silentError: true })
}

export function acceptInvitation(id, data) {
  return request.post(`/invitations/${id}/accept`, data)
}

export function rejectInvitation(id, data) {
  return request.post(`/invitations/${id}/reject`, data)
}

export function hideInvitation(id) {
  return request.delete(`/invitations/${id}`)
}
