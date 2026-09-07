import request from '@/utils/request'

export function getProjects(params) {
  // 重复键 projectCategories=头像&projectCategories=插画，避免 Axios 默认 [] 后缀。
  return request.get('/projects', { params, paramsSerializer: { indexes: null } })
}

export function getMyProjects(params) {
  return request.get('/projects/mine', { params })
}

export function getProject(id) {
  return request.get(`/projects/${id}`)
}

export function getProjectPanel(id) {
  return request.get(`/projects/${id}/panel`)
}

export function getProjectEmployer(id) {
  return request.get(`/users/${id}`)
}

export function createProject(data) {
  return request.post('/projects', data)
}

export function updateProjectContent(id, data) {
  return request.put(`/projects/${id}/content`, data)
}

export function updateProjectDeadline(id, deadline) {
  return request.put(`/projects/${id}/deadline`, null, { params: { deadline } })
}

export function deleteProject(id) {
  return request.delete(`/projects/${id}`)
}

export function changeProjectStatus(id, status, deadline) {
  return request.put(`/projects/${id}/status`, null, { params: { status, ...(deadline ? { deadline } : {}) } })
}

export function publishProjectPublic(id, deadline) {
  return request.put(`/projects/${id}/publish-public`, null, { params: { deadline } })
}

export function getApplications(projectId, params) {
  return request.get(`/projects/${projectId}/applications`, { params })
}

export function createProjectOrder(projectId, applicationId) {
  return request.post(`/projects/${projectId}/applications/${applicationId}/order`)
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
