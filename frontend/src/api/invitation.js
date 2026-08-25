import request from '@/utils/request'

export function getInvitations(params, config = {}) {
  return request.get('/invitations', { ...config, params })
}

export function getInvitationByProject(projectId) {
  return request.get(`/invitations/project/${projectId}`)
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
