import request from '@/utils/request'
import { consumeGuideStream } from '@/utils/guideStream'

export const getGuideCapabilities = (signal) => request.get('/admin/guide/capabilities', {
  signal,
  silentError: true,
})

const base = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')

export async function guideJson(path, { token, signal, method = 'GET', body } = {}) {
  const timeout = AbortSignal.timeout(15000)
  const response = await fetch(`${base}/admin/guide${path}`, {
    method, signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/json', ...(body && !(body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}) },
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  })
  const result = await response.json()
  if (!response.ok || result.code !== 200) throw guideError(result.message || '向导请求失败', result.code || response.status)
  return result.data
}

export async function guideImage(path, { token, signal }) {
  const response = await fetch(`${base}/admin/guide${path}`, { signal, headers: { Authorization: `Bearer ${token}` }, cache: 'no-store' })
  if (!response.ok || !response.headers.get('content-type')?.startsWith('image/jpeg')) throw guideError('参考图已过期或没有访问权限', 404)
  return response.blob()
}

export function guideError(message, code) { return Object.assign(new Error(message), { code }) }

export async function sendGuideStream(sessionId, payload, { token, signal, onEvent }) {
  const response = await fetch(`${base}/admin/guide/sessions/${encodeURIComponent(sessionId)}/messages/stream`, {
    method: 'POST', signal: AbortSignal.any([signal, AbortSignal.timeout(320000)]),
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', Accept: 'text/event-stream, application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream')) {
    const result = await response.json().catch(() => ({}))
    throw guideError(result.message || '向导服务没有返回事件流', result.code || response.status)
  }
  await consumeGuideStream(response, onEvent)
}
