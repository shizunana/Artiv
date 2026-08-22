import request from '@/utils/request'

export function getReviews(params) {
  const { userId, ...query } = params
  return request.get(`/users/${userId}/reviews`, { params: query })
}

export function createReview(orderId, data) {
  return request.post(`/orders/${orderId}/reviews`, data)
}

export function submitReviewAppeal(reviewId, data) {
  return request.post(`/reviews/${reviewId}/appeals`, data)
}

export function getReviewAppeal(reviewId) {
  return request.get(`/reviews/${reviewId}/appeal`)
}
