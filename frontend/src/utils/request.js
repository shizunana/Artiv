import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
})

// 请求拦截：携带 token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理 code 与 401
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      clearAuth()
      router.push({ name: 'Login' })
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }
    if (error.response?.status === 401) {
      clearAuth()
      router.push({ name: 'Login' })
    }
    const responseData = error.response?.data
    const message = responseData?.message || error.message || '网络错误'
    ElMessage.error(message)
    const normalizedError = new Error(message)
    normalizedError.code = responseData?.code
    normalizedError.status = error.response?.status
    normalizedError.cause = error
    return Promise.reject(normalizedError)
  },
)

export default request
