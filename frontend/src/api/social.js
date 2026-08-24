import request from '@/utils/request'

export function getPainterFollowStatus(painterId) {
  return request.get(`/follows/painters/${painterId}/status`)
}

export function followPainter(painterId) {
  return request.put(`/follows/painters/${painterId}`)
}

export function unfollowPainter(painterId) {
  return request.delete(`/follows/painters/${painterId}`)
}

export function getFollowedPainters(params) {
  return request.get('/follows/painters', { params })
}

export function getWorkFavoriteStatus(workId) {
  return request.get(`/favorites/works/${workId}/status`)
}

export function favoriteWork(workId) {
  return request.put(`/favorites/works/${workId}`)
}

export function unfavoriteWork(workId) {
  return request.delete(`/favorites/works/${workId}`)
}

export function getFavoriteWorks(params) {
  return request.get('/favorites/works', { params })
}

export function getStallFavoriteStatus(stallId) {
  return request.get(`/favorites/stalls/${stallId}/status`)
}

export function favoriteStall(stallId) {
  return request.put(`/favorites/stalls/${stallId}`)
}

export function unfavoriteStall(stallId) {
  return request.delete(`/favorites/stalls/${stallId}`)
}

export function getFavoriteStalls(params) {
  return request.get('/favorites/stalls', { params })
}
