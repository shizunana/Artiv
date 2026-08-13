import request from '@/utils/request'

export function getWorks(params) {
  return request.get('/works', { params })
}

export function getMyWorks(params) {
  return request.get('/works/mine', { params })
}

export function createWork(data) {
  return request.post('/works', data)
}

export function changeWorkVisibility(id, visibility) {
  return request.put(`/works/${id}/visibility`, null, { params: { visibility } })
}

export function deleteWork(id) {
  return request.delete(`/works/${id}`)
}
