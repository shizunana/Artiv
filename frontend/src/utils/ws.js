import { getToken } from './auth'

let socket = null
let reconnectTimer = null
let retryDelay = 1000
let listeners = new Set()
let connectedBefore = false
let manuallyStopped = false

function buildUrl() {
  const raw = import.meta.env.VITE_WS_URL || '/ws'
  const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const base = raw.startsWith('ws') ? raw : `${scheme}//${window.location.host}${raw}`
  return `${base}${base.includes('?') ? '&' : '?'}token=${encodeURIComponent(getToken() || '')}`
}

function connect() {
  if (!getToken() || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return
  manuallyStopped = false
  const currentSocket = new WebSocket(buildUrl())
  socket = currentSocket
  currentSocket.onopen = () => {
    retryDelay = 1000
    const eventType = connectedBefore ? 'WS_RECONNECTED' : 'WS_CONNECTED'
    connectedBefore = true
    listeners.forEach((listener) => listener({ type: eventType }))
  }
  currentSocket.onmessage = (event) => {
    try { listeners.forEach((listener) => listener(JSON.parse(event.data))) } catch (_) { /* 忽略非 JSON 心跳消息 */ }
  }
  currentSocket.onclose = () => {
    if (socket === currentSocket) socket = null
    if (!manuallyStopped) scheduleReconnect()
  }
  currentSocket.onerror = () => currentSocket.close()
}

function scheduleReconnect() {
  if (!listeners.size || !getToken()) return
  clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(connect, retryDelay)
  retryDelay = Math.min(retryDelay * 2, 15000)
}

export function startMessageSocket(listener) {
  listeners.add(listener)
  connect()
  return () => listeners.delete(listener)
}

export function stopMessageSocket() {
  manuallyStopped = true
  clearTimeout(reconnectTimer)
  socket?.close()
  socket = null
  listeners = new Set()
  connectedBefore = false
  retryDelay = 1000
}
