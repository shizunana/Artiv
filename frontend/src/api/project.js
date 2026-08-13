import request from '@/utils/request'

export function getProjects(params) {
  return request.get('/projects', { params })
}

export function getMyProjects(params) {
  return request.get('/projects/mine', { params })
}

export function getProject(id) {
  return request.get(`/projects/${id}`)
}

export function getProjectEmployer(id) {
  return request.get(`/users/${id}`)
}

export function createProject(data) {
  return request.post('/projects', data)
}

export function updateProject(id, data) {
  return request.put(`/projects/${id}`, data)
}

export function deleteProject(id) {
  return request.delete(`/projects/${id}`)
}

export function getApplications(projectId, params) {
  return request.get(`/projects/${projectId}/applications`, { params })
}

export function createProjectOrderIntent(projectId, painterId) {
  return request.post(`/projects/${projectId}/order-intents/${painterId}`)
}

export function getMyApplications(params) {
  return request.get('/applications/mine', { params })
}

export function applyProject(projectId, data) {
  return request.post(`/projects/${projectId}/applications`, data)
}

export function deleteApplication(id) {
  return request.delete(`/applications/${id}`)
}

export function updateApplicationQuote(id, quote) {
  return request.put(`/applications/${id}/quote`, { quote })
}

export function selectApplication(id) {
  return request.put(`/applications/${id}/select`)
}
