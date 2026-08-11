import request from '@/utils/request'

export function getReviews(params) {
  return request.get('/reviews', { params })
}

export function createReview(orderId, data) {
  return request.post(`/orders/${orderId}/review`, data)
}
