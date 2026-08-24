import request from '@/utils/request'

export function getInvitations(params) {
  return request.get('/invitations', { params })
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
