import { getToken } from './auth'

let socket = null
let reconnectTimer = null
let retryDelay = 1000
let listeners = new Set()

function buildUrl() {
  const raw = import.meta.env.VITE_WS_URL || '/ws'
  const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const base = raw.startsWith('ws') ? raw : `${scheme}//${window.location.host}${raw}`
  return `${base}${base.includes('?') ? '&' : '?'}token=${encodeURIComponent(getToken() || '')}`
}

function connect() {
  if (!getToken() || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return
  socket = new WebSocket(buildUrl())
  socket.onopen = () => { retryDelay = 1000 }
  socket.onmessage = (event) => {
    try { listeners.forEach((listener) => listener(JSON.parse(event.data))) } catch (_) { /* 忽略非 JSON 心跳消息 */ }
  }
  socket.onclose = () => scheduleReconnect()
  socket.onerror = () => socket?.close()
}

function scheduleReconnect() {
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
  clearTimeout(reconnectTimer)
  socket?.close()
  socket = null
  listeners = new Set()
}
