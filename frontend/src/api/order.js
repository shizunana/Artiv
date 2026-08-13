import request from '@/utils/request'

export function getOrders(params) {
  return request.get('/orders', { params })
}

export function getOrder(id) {
  return request.get(`/orders/${id}`)
}

export function createOrder(data) {
  return request.post('/orders', data)
}

export function acceptOrder(id) {
  return request.post(`/orders/${id}/accept`)
}

export function rejectOrder(id, reason) {
  return request.post(`/orders/${id}/reject`, { reason })
}

export function cancelOrder(id, reason) {
  return request.post(`/orders/${id}/cancel`, { reason })
}

export function requestOrderAfterSale(id, reason) {
  return request.post(`/orders/${id}/after-sale`, { reason })
}

export function submitOrderStage(id, data) {
  return request.post(`/orders/${id}/stages`, data)
}

export function reviewOrderStage(id, data) {
  return request.put(`/orders/${id}/stages/current`, data)
}
