import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { guideJson, sendGuideStream } from '@/api/guide'

export const useGuideStore = defineStore('guide', () => {
  const sessionId = ref('')
  const messages = ref([])
  const draft = ref('')
  const busy = ref(false)
  const stopping = ref(false)
  const error = ref('')
  let identity = null
  let epoch = 0
  let controller = null
  let clientMessageId = ''
  const storageKey = owner => `artiv_guide_session_${owner}`
  const saveId = () => { if (identity && sessionId.value) sessionStorage.setItem(storageKey(identity.owner), sessionId.value) }
  const same = version => version === epoch && identity
  const errorMessage = e => e instanceof TypeError ? '连接向导服务失败，请检查网络或服务状态；不会自动重发' : e.message

  function reset() {
    const old = identity
    const oldId = sessionId.value
    epoch++
    controller?.abort()
    controller = null
    identity = null
    clientMessageId = ''
    sessionId.value = ''
    messages.value = []
    draft.value = ''
    error.value = ''
    busy.value = false
    stopping.value = false
    if (old) sessionStorage.removeItem(storageKey(old.owner))
    // 使用原账号捕获的凭据清理原会话，不借用新账号 Token；失败由后端 TTL 回收。
    if (old && oldId) void guideJson(`/sessions/${oldId}`, { token: old.token, method: 'DELETE' }).catch(() => {})
  }

  async function activate(owner, token) {
    identity = { owner, token }
    const version = ++epoch
    const remembered = sessionStorage.getItem(storageKey(owner))
    if (!remembered) return
    busy.value = true
    controller = new AbortController()
    try {
      let snapshot = await guideJson(`/sessions/${remembered}`, { token, signal: controller.signal })
      if (!same(version)) return
      sessionId.value = remembered
      if (snapshot.activeMessageId) {
        await guideJson(`/sessions/${remembered}/messages/${snapshot.activeMessageId}/cancel`, { token, method: 'POST', signal: controller.signal })
        snapshot = await guideJson(`/sessions/${remembered}`, { token, signal: controller.signal })
      }
      if (same(version)) messages.value = snapshot.messages
    } catch (e) {
      if (!same(version)) return
      if (e.code === 404) sessionStorage.removeItem(storageKey(owner))
      else error.value = errorMessage(e)
    } finally { if (same(version)) { busy.value = false; controller = null } }
  }

  async function send() {
    if (!identity || busy.value || stopping.value || !draft.value.trim()) return
    const text = draft.value.trim()
    const version = epoch
    const token = identity.token
    const requestController = new AbortController()
    controller = requestController
    busy.value = true
    error.value = ''
    let assistant
    try {
      if (!sessionId.value) {
        const snapshot = await guideJson('/sessions', { token, method: 'POST', signal: controller.signal })
        if (!same(version)) return
        sessionId.value = snapshot.id
        saveId()
      }
      clientMessageId = crypto.randomUUID()
      assistant = reactive({ id: `pending-${clientMessageId}`, role: 'assistant', text: '', status: 'generating', error: null, usage: null, references: [], toolStatus: '' })
      messages.value.push({ id: clientMessageId, role: 'user', text, status: 'complete' }, assistant)
      draft.value = ''
      await sendGuideStream(sessionId.value, { clientMessageId, text }, {
        token, signal: requestController.signal,
        onEvent(name, data) {
          if (!same(version)) return
          if (name === 'meta') assistant.id = data.messageId
          if (name === 'delta') assistant.text += data.text
          if (name === 'usage') assistant.usage = data
          if (name === 'references') assistant.references = data
          if (name === 'tool_status') assistant.toolStatus = data.message
          if (name === 'done') assistant.status = data.status
          if (name === 'error') { assistant.status = data.status || 'error'; assistant.error = data.message; error.value = data.message }
        },
      })
    } catch (e) {
      if (!same(version)) return
      if (assistant) { assistant.status = requestController.signal.aborted ? 'cancelled' : 'error'; assistant.error = requestController.signal.aborted ? null : errorMessage(e) }
      if (!requestController.signal.aborted) error.value = errorMessage(e)
      // 过期会话只提示并重置 ID，不自动重放可能已经收费的消息。
      if (e.code === 404) { sessionId.value = ''; sessionStorage.removeItem(storageKey(identity.owner)) }
    } finally {
      if (same(version)) { busy.value = false; controller = null; clientMessageId = '' }
    }
  }

  async function stop() {
    if (!busy.value || !identity || stopping.value) return
    const version = epoch, token = identity.token, id = sessionId.value, messageId = clientMessageId
    stopping.value = true
    controller?.abort()
    try {
      if (id && messageId) await guideJson(`/sessions/${id}/messages/${messageId}/cancel`, { token, method: 'POST' })
    } catch (e) { if (same(version)) error.value = errorMessage(e) }
    finally { if (same(version)) stopping.value = false }
  }

  async function clear() {
    if (!identity || stopping.value) return
    const version = ++epoch, token = identity.token, id = sessionId.value
    controller?.abort()
    controller = null
    busy.value = true
    try {
      if (id) await guideJson(`/sessions/${id}`, { token, method: 'DELETE' })
      if (!same(version)) return
      sessionId.value = ''; messages.value = []; draft.value = ''; error.value = ''
      sessionStorage.removeItem(storageKey(identity.owner))
    } catch (e) {
      if (same(version)) {
        error.value = errorMessage(e)
        messages.value.forEach(message => { if (message.status === 'generating') message.status = 'cancelled' })
      }
    }
    finally { if (same(version)) { busy.value = false; clientMessageId = '' } }
  }
  async function resolveReference(reference) {
    if (!identity || !sessionId.value) return null
    const version = epoch
    try {
      const result = await guideJson(`/sessions/${encodeURIComponent(sessionId.value)}/references/${encodeURIComponent(reference.kind)}/${encodeURIComponent(reference.id)}`, { token: identity.token })
      return same(version) ? result : null
    } catch (e) { if (same(version)) error.value = errorMessage(e); return null }
  }
  return { sessionId, messages, draft, busy, stopping, error, activate, reset, send, stop, clear, resolveReference }
})
