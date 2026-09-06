import request from '@/utils/request'

export const getAdminOverview = () => request.get('/admin/overview')
export const getAdminStats = (params) => request.get('/admin/stats', { params })
export const getAdminDemandStats = (params) => request.get('/admin/stats/demand', { params })
export const getAdminPriceDistribution = (params) => request.get('/admin/stats/price-distribution', { params })

export const getAdminUsers = (params) => request.get('/admin/users', { params })
export const getAdminUser = (id) => request.get(`/admin/users/${id}`)
export const changeAdminUserStatus = (id, data) => request.put(`/admin/users/${id}/status`, data)
export const creditAdminUserBalance = (id, data) => request.post(`/admin/users/${id}/balance/credit`, data)
export const adjustAdminUserBalance = (id, data) => request.post(`/admin/users/${id}/balance/adjust`, data)

export const getAdminCertifications = (params) => request.get('/admin/certifications', { params })
export const getAdminCertification = (id) => request.get(`/admin/certifications/${id}`)
export const approveCertification = (id, reason) => request.post(`/admin/certifications/${id}/approve`, { reason })
export const rejectCertification = (id, reason) => request.post(`/admin/certifications/${id}/reject`, { reason })

export const getAdminProjects = (params) => request.get('/admin/projects', { params })
export const getAdminProject = (id) => request.get(`/admin/projects/${id}`)
export const moderateProject = (id, data) => request.put(`/admin/projects/${id}/moderation`, data)

export const getAdminDisputes = (params) => request.get('/admin/disputes', { params })
export const getAdminDispute = (id) => request.get(`/admin/disputes/${id}`)
export const arbitrateDispute = (id, data) => request.post(`/admin/disputes/${id}/arbitrate`, data)

export const getAdminAuditLogs = (params) => request.get('/admin/audit-logs', { params })

export const getAdminReviewAppeals = (params) => request.get('/admin/review-appeals', { params })
export const getAdminReviewAppeal = (id) => request.get(`/admin/review-appeals/${id}`)
export const approveReviewAppeal = (id, data) => request.put(`/admin/review-appeals/${id}/approve`, data)
export const rejectReviewAppeal = (id, data) => request.put(`/admin/review-appeals/${id}/reject`, data)

export const getAdminReports = (params) => request.get('/admin/reports', { params })
export const getAdminReport = (id) => request.get(`/admin/reports/${id}`)
export const resolveAdminReport = (id, handled, data) => request.put(`/admin/reports/${id}/resolve`, data, { params: { handled } })
