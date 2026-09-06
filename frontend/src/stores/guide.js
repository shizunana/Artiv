import { defineStore } from 'pinia'
import { reactive, ref, shallowRef } from 'vue'
import { guideJson, guideImage, sendGuideStream } from '@/api/guide'

export const useGuideStore = defineStore('guide', () => {
  const sessionId = ref('')
  const messages = ref([])
  const draft = ref('')
  const busy = ref(false)
  const stopping = ref(false)
  const error = ref('')
  const pendingImages = shallowRef([])
  let identity = null
  let epoch = 0
  let controller = null
  let clientMessageId = ''
  const storageKey = owner => `artiv_guide_session_${owner}`
  const saveId = () => { if (identity && sessionId.value) sessionStorage.setItem(storageKey(identity.owner), sessionId.value) }
  const same = version => version === epoch && identity
  const errorMessage = e => e instanceof TypeError ? '连接向导服务失败，请检查网络或服务状态；不会自动重发' : e.message

  function releasePreview() {
    pendingImages.value.forEach(image => URL.revokeObjectURL(image.url))
    pendingImages.value = []
  }
  // 选择文件只生成本地预览。只有明确点击“发送并分析”才上传并调用模型。
  function selectImages(files) {
    const selected = Array.from(files || [])
    if (busy.value || stopping.value || !selected.length) return false
    if (pendingImages.value.length + selected.length > 4) {
      error.value = '每条消息最多 4 张参考图，请减少选择或先移除已有图片'; return false
    }
    if (selected.some(file => !['image/png', 'image/jpeg'].includes(file.type) || !/\.(png|jpe?g)$/i.test(file.name) || !file.size || file.size > 5 * 1024 * 1024)) {
      error.value = '请选择 5 MB 以内的 PNG/JPG 图片'; return false
    }
    pendingImages.value = [...pendingImages.value, ...selected.map(file => ({ key: crypto.randomUUID(), file, url: URL.createObjectURL(file), uploadedId: null }))]
    error.value = ''
    return true
  }
  function removeImage(key) {
    if (busy.value || stopping.value) return
    const old = pendingImages.value.find(image => image.key === key)
    if (!old) return
    pendingImages.value = pendingImages.value.filter(image => image !== old)
    URL.revokeObjectURL(old.url)
    if (old.uploadedId && !pendingImages.value.some(image => image.uploadedId === old.uploadedId) && identity && sessionId.value) {
      // 仅清理未被消息引用的附件；已接受的图片随会话清空，不单独破坏历史依据。
      void guideJson(`/sessions/${sessionId.value}/images/${old.uploadedId}`, { token: identity.token, method: 'DELETE' }).catch(() => {})
    }
  }

  function reset() {
    const old = identity
    const oldId = sessionId.value
    epoch++
    controller?.abort()
    releasePreview()
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
    if (!identity || busy.value || stopping.value || (!draft.value.trim() && !pendingImages.value.length)) return
    const text = draft.value.trim()
    const originalDraft = draft.value
    const attachments = pendingImages.value
    const version = epoch
    const token = identity.token
    const requestController = new AbortController()
    controller = requestController
    busy.value = true
    error.value = ''
    let assistant
    let accepted = false
    let sentId = ''
    try {
      if (!sessionId.value) {
        const snapshot = await guideJson('/sessions', { token, method: 'POST', signal: controller.signal })
        if (!same(version)) return
        sessionId.value = snapshot.id
        saveId()
      }
      // 顺序上传以保持编号，并兼容后端串行图片解码；部分失败时保留已上传 ID 供用户手动重试。
      for (const attachment of attachments) {
        if (requestController.signal.aborted || !same(version)) return
        if (attachment.uploadedId) continue
        const form = new FormData(); form.append('file', attachment.file)
        const uploaded = await guideJson(`/sessions/${sessionId.value}/images`, { token, method: 'POST', body: form, signal: requestController.signal })
        if (!same(version)) return
        attachment.uploadedId = uploaded.id
      }
      if (requestController.signal.aborted) return
      const imageIds = attachments.map(image => image.uploadedId)
      if (new Set(imageIds).size !== imageIds.length) throw new Error('同一条消息中有重复图片，请移除重复项后再发送')
      clientMessageId = crypto.randomUUID()
      sentId = clientMessageId
      assistant = reactive({ id: `pending-${clientMessageId}`, role: 'assistant', text: '', status: 'generating', error: null, usage: null, references: [], toolStatus: '' })
      messages.value.push({ id: clientMessageId, role: 'user', text, imageIds, status: 'complete' }, assistant)
      await sendGuideStream(sessionId.value, { clientMessageId, text, ...(imageIds.length ? { imageIds, imageConsent: true } : {}) }, {
        token, signal: requestController.signal,
        onEvent(name, data) {
          if (!same(version)) return
          if (name === 'meta') {
            accepted = true; assistant.id = data.messageId
            if (draft.value === originalDraft) draft.value = ''
            if (pendingImages.value === attachments) releasePreview()
          }
          if (name === 'delta') assistant.text += data.text
          if (name === 'usage') assistant.usage = data
          if (name === 'references') assistant.references = data
          if (name === 'tool_status' || name === 'status') assistant.toolStatus = data.message
          if (name === 'done') assistant.status = data.status
          if (name === 'error') { assistant.status = data.status || 'error'; assistant.error = data.message; error.value = data.message }
        },
      })
    } catch (e) {
      if (!same(version)) return
      if (!accepted && sentId) messages.value = messages.value.filter(m => m.id !== sentId && m !== assistant)
      if (assistant) { assistant.status = requestController.signal.aborted ? 'cancelled' : 'error'; assistant.error = requestController.signal.aborted ? null : errorMessage(e) }
      if (!requestController.signal.aborted) error.value = errorMessage(e)
      // 过期会话只提示并重置 ID，不自动重放可能已经收费的消息。
      if (e.code === 404) {
        sessionId.value = ''; messages.value = []; sessionStorage.removeItem(storageKey(identity.owner))
        attachments.forEach(image => { image.uploadedId = null })
      }
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
      releasePreview()
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
  async function referenceBlob(id, signal) {
    if (!identity || !sessionId.value) return null
    const version = epoch
    const blob = await guideImage(`/sessions/${sessionId.value}/images/${encodeURIComponent(id)}`, { token: identity.token, signal })
    return same(version) ? blob : null
  }
  return { sessionId, messages, draft, busy, stopping, error, activate, reset, send, stop, clear, resolveReference,
    pendingImages, selectImages, removeImage, referenceBlob }
})
