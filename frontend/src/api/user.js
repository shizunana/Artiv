import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getMe() {
  return request.get('/auth/me')
}

export function updateProfile(id, data) {
  return request.put(`/users/${id}`, data)
}

export function updateMyPainterProfile(data) {
  return request.put('/users/me/painter-profile', data)
}

export function updateMyAccountProfile(data) {
  return request.put('/users/me/account', data)
}

export function updateMyWorkbenchRole(role) {
  return request.put('/users/me/workbench-role', { role })
}

export function updateMyPassword(data) {
  return request.put('/users/me/password', data)
}

export function updateMyAvatar(data) {
  return request.put('/users/me/avatar', data)
}

export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

export function submitCertification(data) {
  return request.post('/users/me/certifications', data)
}

export function getLatestCertification() {
  return request.get('/users/me/certifications/latest')
}

export function getCertificationStatus() {
  return request.get('/users/me/certifications/status')
}

export function submitCertificationChange(data) {
  return request.post('/users/me/certifications/changes', data)
}

export function getArtists(params) {
  return request.get('/users/artists', { params })
}

export function getPublicProfile(id, role) {
  return request.get(`/users/${id}/public-profile`, { params: { role } })
}

export function getPublicProfileWorks(id, params) {
  return request.get(`/users/${id}/works`, { params })
}

export function getPublicProfileStalls(id, params) {
  return request.get(`/users/${id}/stalls`, { params })
}

export function getPublicProfileProjects(id, params) {
  return request.get(`/users/${id}/projects`, { params })
}
