import request from '@/utils/request'

export function getStalls(params) {
  return request.get('/stalls', { params })
}

export function getMyStalls(params) {
  return request.get('/stalls/mine', { params })
}

export function getStall(id) {
  return request.get(`/stalls/${id}`)
}

export function createStall(data) {
  return request.post('/stalls', data)
}

export function updateStall(id, data) {
  return request.put(`/stalls/${id}`, data)
}

export function changeStallStatus(id, status) {
  return request.put(`/stalls/${id}/status`, null, { params: { status } })
}

export function deleteStall(id) {
  return request.delete(`/stalls/${id}`)
}

export function purchaseStall(id) {
  return request.post(`/stalls/${id}/purchase`)
}
