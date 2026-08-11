import request from '@/utils/request'

export function getProjects(params) {
  return request.get('/projects', { params })
}

export function getProject(id) {
  return request.get(`/projects/${id}`)
}

export function createProject(data) {
  return request.post('/projects', data)
}

export function applyProject(id, data) {
  return request.post(`/projects/${id}/applications`, data)
}
