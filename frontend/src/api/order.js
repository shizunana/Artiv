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

export function updateOrderStatus(id, status) {
  return request.put(`/orders/${id}/status`, { status })
}
