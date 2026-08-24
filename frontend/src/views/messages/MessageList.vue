<template>
  <div class="message-page">
    <div class="page-container message-page__body">
      <aside class="message-nav" aria-label="消息中心导航">
        <div class="message-nav__title">
          <el-icon><ChatDotRound /></el-icon>
          <strong>消息中心</strong>
        </div>
        <nav class="message-nav__items">
          <button type="button" :class="{ active: activeTab === 'conversations' }" @click="selectMessageSection('conversations')">
            <el-icon><ChatDotRound /></el-icon>
            <span>我的消息</span>
            <span
              v-if="messageStore.conversationUnreadCount"
              class="message-nav__unread"
              :aria-label="`${messageStore.conversationUnreadCount} 条会话未读`"
            >{{ displayUnreadCount(messageStore.conversationUnreadCount) }}</span>
          </button>
          <button type="button" :class="{ active: activeTab === 'notifications' }" @click="selectMessageSection('notifications')">
            <el-icon><Bell /></el-icon>
            <span>系统消息</span>
            <span
              v-if="messageStore.notificationUnreadCount"
              class="message-nav__unread"
              :aria-label="`${messageStore.notificationUnreadCount} 条系统消息未读`"
            >{{ displayUnreadCount(messageStore.notificationUnreadCount) }}</span>
          </button>
        </nav>
        <div class="message-nav__divider" />
        <button type="button" class="message-nav__setting" disabled>
          <el-icon><Setting /></el-icon>
          <span>消息设置</span>
          <small>暂未开放</small>
        </button>
      </aside>

      <section class="message-center">
        <div v-if="activeTab === 'conversations'" class="message-section">
          <header class="message-section__header">
            <h2>我的消息</h2>
          </header>
        <div class="message-layout">
          <aside class="conversation-list" v-loading="loadingConversations && !conversations.length">
            <div class="panel-title"><h3>会话</h3><el-button link :loading="loadingConversations" @click="loadConversations">刷新</el-button></div>
            <div class="conversation-scroll">
              <el-empty v-if="!conversations.length" description="暂无沟通会话" :image-size="72" />
              <button
                v-for="item in conversations"
                :key="conversationId(item)"
                class="conversation-item"
                :class="{ active: sameConversation(active, item) }"
                @click="openConversation(item)"
              >
                <el-avatar :size="38" :src="item.withUserAvatar">{{ item.withUserNickname?.slice(0, 1) }}</el-avatar>
                <span class="conversation-copy">
                  <b>{{ item.withUserNickname || `用户 #${item.withUserId}` }}</b>
                  <small>{{ item.lastContent }}</small>
                </span>
                <el-badge v-if="item.unreadCount" :value="item.unreadCount" :max="99" />
              </button>
            </div>
          </aside>

          <section class="chat-panel">
            <template v-if="active">
              <header><b>{{ active.withUserNickname || `用户 #${active.withUserId}` }}</b><small>沟通会话</small></header>
              <div class="chat-history">
                <div v-if="loadingMessages && !messages.length" class="chat-loading">正在读取会话</div>
                <el-empty v-else-if="!messages.length" description="开始沟通吧" :image-size="72" />
                <template v-for="(msg, index) in messages" :key="msg.id">
                  <time v-if="shouldShowTimeDivider(index)" class="chat-time-divider">{{ formatChatTime(msg.createdAt) }}</time>
                  <div v-if="isBusinessSystemMessage(msg)" class="business-system-message-row">
                    <article class="business-system-message">
                      <strong class="business-system-message__title">{{ msg.subtype === 'PRIVATE_ORDER_CARD' ? '订单状态' : '系统提示' }}</strong>
                      <p>{{ msg.content }}</p>
                      <button v-if="hasMessageDetail(msg)" type="button" class="business-system-message__action" @click="viewMessageDetail(msg)">
                        <span>查看详情</span>
                        <el-icon><ArrowRight /></el-icon>
                      </button>
                    </article>
                  </div>
                  <div v-else class="bubble-row" :class="{ mine: isMineMessage(msg) }">
                    <el-avatar v-if="!isMineMessage(msg)" :size="34" :src="active.withUserAvatar" class="bubble-avatar">
                      {{ active.withUserNickname?.slice(0, 1) }}
                    </el-avatar>
                    <div class="bubble" :class="{ 'bubble--images': isImageMessage(msg) }">
                      <template v-if="isImageMessage(msg)">
                        <div class="message-images">
                          <el-image
                            v-for="image in imageUrls(msg)"
                            :key="image"
                            :src="image"
                            :preview-src-list="imageUrls(msg)"
                            :initial-index="imageUrls(msg).indexOf(image)"
                            preview-teleported
                            fit="cover"
                          />
                        </div>
                      </template>
                      <template v-else>{{ msg.content }}</template>
                    </div>
                    <el-avatar v-if="isMineMessage(msg)" :size="34" :src="userStore.user?.avatar" class="bubble-avatar">
                      {{ userStore.user?.nickname?.slice(0, 1) }}
                    </el-avatar>
                  </div>
                </template>
              </div>
              <footer class="chat-composer">
                <div class="chat-extension-bar" aria-label="消息扩展功能">
                  <el-button text class="chat-extension-bar__item" :loading="sendingImages" @click="openImagePicker">
                    <el-icon class="chat-extension-bar__image-icon"><Picture /></el-icon>
                    <span>发送图片</span>
                  </el-button>
                  <el-button text class="chat-extension-bar__item" @click="sendOrderCard">
                    <el-icon class="chat-extension-bar__image-icon"><Document /></el-icon>
                    <span>发送订单状态</span>
                  </el-button>
                  <span class="chat-extension-bar__hint">单次最多 9 张，每张不超过 5 MB</span>
                </div>
                <div v-if="pendingImages.length" class="image-queue">
                  <div v-for="item in pendingImages" :key="item.url" class="image-queue__item">
                    <img :src="item.url" :alt="item.name" />
                    <button type="button" aria-label="移除图片" @click="removePendingImage(item.url)">×</button>
                  </div>
                  <el-button link type="danger" @click="clearPendingImages">清空</el-button>
                  <el-button type="primary" size="small" :loading="sendingImages" @click="sendImages">发送 {{ pendingImages.length }} 张图片</el-button>
                </div>
                <div class="chat-composer__input-row">
                  <el-input v-model="draft" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="输入消息，Enter 发送，Shift + Enter 换行" @keydown.enter.exact.prevent="send" />
                  <el-button type="primary" :loading="sending" @click="send">发送</el-button>
                </div>
                <input ref="imageInput" class="visually-hidden" type="file" accept="image/png,image/jpeg,image/gif,image/webp" multiple @change="queueImages" />
              </footer>
            </template>
            <el-empty v-else description="从左侧选择会话，或从应征、橱窗下单页面发起沟通" />
          </section>
        </div>
        </div>

        <div v-else class="message-section">
          <header class="message-section__header">
            <h2>系统消息</h2>
            <el-button link @click="readAll">全部已读</el-button>
          </header>
        <div class="notification-panel" v-loading="loadingNotifications">
          <el-empty v-if="!notifications.length" description="暂无系统消息" />
          <article v-for="notice in notifications" :key="notice.id" class="notification-card" :class="{ unread: !notice.readAt }" @click="openNotification(notice)">
            <div class="notice-icon">系</div>
            <div class="notice-copy"><b>{{ notificationTitle(notice) }}</b><p>{{ notice.content }}</p><small>{{ formatDateTime(notice.createdAt) }}</small></div>
            <el-button link type="primary">{{ notificationAction(notice) }}</el-button>
          </article>
        </div>
        </div>
        <el-dialog v-model="orderPickerVisible" title="选择要发送的订单" width="620px">
          <p class="order-picker__hint">当前会话双方共有 {{ relatedOrders.length }} 笔订单，请选择其中一笔发送状态卡片。</p>
          <div class="order-picker">
            <button v-for="order in relatedOrders" :key="order.id" type="button" class="order-picker__item" @click="sendSelectedOrder(order)">
              <span><b>{{ order.stallTitle || `订单 #${order.id}` }}</b><small>订单 #{{ order.id }} · {{ formatDateTime(order.createdAt) }}</small></span>
              <span><el-tag size="small">{{ orderStatusText(order.status) }}</el-tag><strong>¥{{ Number(order.amount || 0).toFixed(2) }}</strong></span>
            </button>
          </div>
        </el-dialog>
      </section>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Bell, ChatDotRound, Document, Picture, Setting } from '@element-plus/icons-vue'
import { getConversation, getConversations, getNotifications, markRead, sendPrivateMessage } from '@/api/message'
import { formatDateTime } from '@/utils/date'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { startMessageSocket } from '@/utils/ws'
import { uploadFile } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const activeTab = ref(route.query.tab === 'notifications' ? 'notifications' : 'conversations')
const conversations = ref([])
const notifications = ref([])
const messages = ref([])
const active = ref(null)
const orderPickerVisible = ref(false)
const relatedOrders = ref([])
const draft = ref('')
const loadingConversations = ref(false)
const loadingNotifications = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const sendingImages = ref(false)
const imageInput = ref(null)
const pendingImages = ref([])
const maxImageCount = 9
const messageCache = new Map()
const historyRequestTokens = new Map()
let historyRequestSequence = 0
let stopSocket

const conversationId = (item) => String(item.withUserId)
const sameConversation = (first, second) => first && conversationId(first) === conversationId(second)
const displayUnreadCount = (value) => Number(value) > 99 ? '99+' : String(Number(value) || 0)

async function loadConversations({ silent = false } = {}) {
  if (!silent) loadingConversations.value = true
  try {
    const data = await getConversations({ page: 1, size: 30 })
    conversations.value = data.records || []
  } finally {
    if (!silent) loadingConversations.value = false
  }
}

async function loadNotifications({ silent = false } = {}) {
  if (!silent) loadingNotifications.value = true
  try {
    const data = await getNotifications({ page: 1, size: 30 })
    notifications.value = data.records || []
  } finally {
    if (!silent) loadingNotifications.value = false
  }
}

async function openConversation(item, { force = false } = {}) {
  const key = conversationId(item)
  const alreadyActive = sameConversation(active.value, item)
  const cached = messageCache.get(key)
  const needsRefresh = force || !cached || Number(item.unreadCount || 0) > 0
  if (alreadyActive && !needsRefresh) return

  active.value = item
  if (cached) messages.value = cached
  else messages.value = []
  loadingMessages.value = !cached && needsRefresh
  await scrollHistoryToBottom()
  if (!needsRefresh) return

  const requestToken = ++historyRequestSequence
  historyRequestTokens.set(key, requestToken)
  if (!cached) loadingMessages.value = true
  try {
    const data = await getConversation({ withUserId: item.withUserId, page: 1, size: 100 })
    const records = data.records || []
    if (historyRequestTokens.get(key) !== requestToken) return
    messageCache.set(key, records)
    if (!sameConversation(active.value, item)) return
    messages.value = records
    await markActiveConversationRead()
    const currentItem = conversations.value.find((conversation) => conversationId(conversation) === key)
    if (currentItem) currentItem.unreadCount = 0
    await messageStore.fetchUnreadCount()
    await scrollHistoryToBottom()
  } finally {
    if (historyRequestTokens.get(key) === requestToken && sameConversation(active.value, item)) {
      loadingMessages.value = false
    }
  }
}

async function markActiveConversationRead(extraMessage = null) {
  if (!active.value) return
  const currentUserId = String(userStore.user?.id)
  const visibleOrderIds = new Set(
    messages.value.map((message) => message.orderId).filter(Boolean).map(String),
  )
  if (extraMessage?.orderId && extraMessage.type === 3) {
    visibleOrderIds.add(String(extraMessage.orderId))
  }
  const candidates = [
    ...messages.value.filter((message) => String(message.toId) === currentUserId && !message.readAt),
    ...notifications.value.filter((notice) => !notice.readAt
      && notice.orderId
      && visibleOrderIds.has(String(notice.orderId))),
  ]
  if (extraMessage && !extraMessage.readAt
      && (String(extraMessage.toId) === currentUserId
        || (extraMessage.orderId && visibleOrderIds.has(String(extraMessage.orderId))))) {
    candidates.push(extraMessage)
  }
  const unique = [...new Map(candidates.map((message) => [message.id, message])).values()]
  if (!unique.length) return
  await Promise.all(unique.map((message) => markRead(message.id)))
  const readAt = new Date().toISOString()
  unique.forEach((message) => { message.readAt = readAt })
}

async function send() {
  const content = draft.value.trim()
  if (!content || !active.value) return
  sending.value = true
  try {
    const message = await sendPrivateMessage({ toId: active.value.withUserId, content })
    messages.value.push(message)
    syncActiveMessageCache()
    draft.value = ''
    await loadConversations({ silent: true })
    await scrollHistoryToBottom()
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function openImagePicker() {
  imageInput.value?.click()
}

function queueImages(event) {
  const selected = Array.from(event.target.files || [])
  const available = maxImageCount - pendingImages.value.length
  const accepted = selected.slice(0, Math.max(0, available))
  if (accepted.length < selected.length) {
    ElMessage.warning(`单次最多发送 ${maxImageCount} 张图片`)
  }
  const existing = new Set(pendingImages.value.map((item) => `${item.name}_${item.size}_${item.lastModified}`))
  accepted.forEach((file) => {
    const identity = `${file.name}_${file.size}_${file.lastModified}`
    if (!existing.has(identity)) {
      pendingImages.value.push({ file, name: file.name, url: URL.createObjectURL(file) })
      existing.add(identity)
    }
  })
  event.target.value = ''
}

function removePendingImage(url) {
  const target = pendingImages.value.find((item) => item.url === url)
  if (target) URL.revokeObjectURL(target.url)
  pendingImages.value = pendingImages.value.filter((item) => item.url !== url)
}

function clearPendingImages() {
  pendingImages.value.forEach((item) => URL.revokeObjectURL(item.url))
  pendingImages.value = []
}

async function sendImages() {
  if (!active.value || !pendingImages.value.length) return
  sendingImages.value = true
  try {
    const uploaded = await Promise.all(pendingImages.value.map((item) => uploadFile(item.file)))
    const imageUrls = uploaded.map((result) => typeof result === 'string' ? result : result?.url).filter(Boolean)
    const message = await sendPrivateMessage({ toId: active.value.withUserId, images: imageUrls })
    messages.value.push(message)
    syncActiveMessageCache()
    clearPendingImages()
    await loadConversations({ silent: true })
    await scrollHistoryToBottom()
  } catch (error) {
    ElMessage.error(error.message || '图片发送失败')
  } finally {
    sendingImages.value = false
  }
}

function isImageMessage(message) {
  return message.subtype === 'PRIVATE_IMAGE'
}

function isMineMessage(message) {
  return String(message.fromId) === String(userStore.user?.id)
}

function syncActiveMessageCache() {
  if (!active.value) return
  messageCache.set(conversationId(active.value), [...messages.value])
}

function imageUrls(message) {
  if (!isImageMessage(message)) return []
  try {
    const payload = JSON.parse(message.content || '{}')
    return Array.isArray(payload.images) ? payload.images : []
  } catch {
    return []
  }
}

async function scrollHistoryToBottom() {
  await nextTick()
  const history = document.querySelector('.chat-history')
  if (history) history.scrollTop = history.scrollHeight
}

function isBusinessSystemMessage(message) {
  return message.type === 3
    && message.subtype !== 'PRIVATE_CHAT'
    && message.subtype !== 'PRIVATE_IMAGE'
}

function hasMessageDetail(message) {
  return Boolean(message.orderId || message.projectId)
}

function shouldShowTimeDivider(index) {
  const current = new Date(messages.value[index]?.createdAt).getTime()
  if (!Number.isFinite(current)) return false
  if (index === 0) return true
  const previous = new Date(messages.value[index - 1]?.createdAt).getTime()
  return !Number.isFinite(previous) || current - previous >= 5 * 60 * 1000
}

function formatChatTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const sameDay = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  const clock = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (sameDay) return `今天 ${clock}`
  const dateText = `${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日`
  return date.getFullYear() === now.getFullYear()
    ? `${dateText} ${clock}`
    : `${date.getFullYear()}年${dateText} ${clock}`
}

function viewMessageDetail(message) {
  if (message.orderId) {
    router.push({ name: userStore.workbenchRole === 2 ? 'PainterOrderDetail' : 'DashboardOrderDetail', params: { id: message.orderId } })
  } else if (message.projectId) {
    router.push({ name: 'ProjectDetail', params: { id: message.projectId } })
  }
}

async function sendOrderCard() {
  if (!active.value) return
  try {
    const { getOrders } = await import('@/api/order')
    const data = await getOrders({ page: 1, size: 50 })
    relatedOrders.value = (data.records || []).filter(order => [order.employerId, order.painterId].includes(active.value.withUserId))
    if (!relatedOrders.value.length) { ElMessage.info('与该用户暂无可发送的订单'); return }
    orderPickerVisible.value = true
  } catch (error) { ElMessage.error(error.message || '订单列表加载失败') }
}

function orderStatusText(status) {
  return ['待画师接单','进行中','待确认','已完成','已取消','退款/售后'][status] || '状态更新'
}

async function sendSelectedOrder(order) {
  try {
    const message = await sendPrivateMessage({
      toId: active.value.withUserId,
      orderId: order.id,
      content: `订单 #${order.id}｜${order.stallTitle || order.projectTitle || '订单'}｜${orderStatusText(order.status)}｜¥${Number(order.amount || 0).toFixed(2)}`,
    })
    messages.value.push(message)
    syncActiveMessageCache()
    orderPickerVisible.value = false
    await scrollHistoryToBottom()
  } catch (error) { ElMessage.error(error.message || '订单状态发送失败') }
}

async function readAll() {
  await messageStore.readAll()
  await loadNotifications()
}

function notificationTitle(notice) {
  if (notice.subtype === 'APPLICATION_CREATED') return '收到新的企划应征'
  if (notice.subtype === 'APPLICATION_SELECTED') return '应征已被选定'
  if (notice.subtype === 'STALL_PURCHASED') return '已购买橱窗'
  if (notice.subtype === 'STALL_PURCHASE_RECEIVED') return '收到橱窗订单'
  if (notice.subtype === 'PROJECT_ORDER_CREATED') return '企划订单已发送'
  if (notice.subtype === 'PROJECT_ORDER_RECEIVED') return '收到企划订单'
  if (notice.subtype?.startsWith('ORDER_')) return '订单状态更新'
  if (notice.subtype?.startsWith('PROJECT_MODERATION_')) return '企划治理通知'
  if (notice.subtype?.startsWith('CERTIFICATION_')) return '画师认证通知'
  if (notice.subtype?.startsWith('REVIEW_')) return '合作评价通知'
  if (notice.subtype?.startsWith('ACCOUNT_')) return '账号状态通知'
  return '系统通知'
}

function notificationAction(notice) {
  if (notice.orderId) return '查看订单'
  if (notice.subtype === 'APPLICATION_CREATED') return '查看企划'
  if (notice.subtype === 'APPLICATION_SELECTED' || notice.subtype?.startsWith('PROJECT_MODERATION_')) return '查看企划'
  if (notice.subtype?.startsWith('CERTIFICATION_')) return '查看认证'
  if (notice.subtype?.startsWith('REVIEW_')) return '查看评价'
  if (notice.subtype?.startsWith('ACCOUNT_')) return '查看账户'
  return notice.projectId ? '查看企划' : '查看工作台'
}

async function openNotification(notice) {
  if (!notice.readAt) await markRead(notice.id)
  await messageStore.fetchUnreadCount()
  if (notice.orderId) {
    router.push({ name: userStore.workbenchRole === 2 ? 'PainterOrderDetail' : 'DashboardOrderDetail', params: { id: notice.orderId } })
  } else if (notice.projectId) {
    router.push({ name: 'ProjectDetail', params: { id: notice.projectId } })
  } else if (notice.subtype?.startsWith('CERTIFICATION_')) {
    router.push({ name: 'AccountSettings', query: { section: 'identity' } })
  } else if (notice.subtype?.startsWith('REVIEW_')) {
    router.push({ name: 'ArtistProfile', params: { id: userStore.user.id }, query: { role: 'painter' } })
  } else if (notice.subtype?.startsWith('ACCOUNT_')) {
    router.push({ name: 'AccountSettings' })
  } else if (notice.subtype?.startsWith('APPLICATION') || notice.subtype?.startsWith('STALL')) {
    await selectMessageSection('conversations')
    await loadConversations()
  } else {
    router.push(userStore.workbenchRole === 2 ? '/painter' : '/dashboard')
  }
}

async function openFromQuery() {
  const withUserId = Number(route.query.withUserId)
  if (!withUserId) return
  activeTab.value = 'conversations'
  const existing = conversations.value.find((item) => Number(item.withUserId) === withUserId)
  await openConversation(existing || { withUserId, withUserNickname: route.query.nickname || `用户 #${withUserId}` })
}

async function selectMessageSection(name) {
  const section = name === 'notifications' ? 'notifications' : 'conversations'
  activeTab.value = section
  await router.replace({
    name: 'Messages',
    query: section === 'notifications' ? { tab: 'notifications' } : {},
  })
}

onMounted(async () => {
  await Promise.all([loadConversations(), loadNotifications(), messageStore.fetchUnreadCount()])
  await openFromQuery()
  stopSocket = startMessageSocket(async (event) => {
    if (event.type === 'WS_CONNECTED' || event.type === 'WS_RECONNECTED') {
      await Promise.all([messageStore.fetchUnreadCount(), loadConversations({ silent: true }), loadNotifications({ silent: true })])
      if (event.type === 'WS_RECONNECTED' && active.value) {
        await openConversation(active.value, { force: true })
      }
      return
    }
    await Promise.all([messageStore.fetchUnreadCount(), loadConversations({ silent: true }), loadNotifications({ silent: true })])
    if (event.type === 'NEW_MESSAGE' && active.value
      && String(event.data.fromId) === String(active.value.withUserId)) {
      if (!messages.value.some((message) => message.id === event.data.id)) {
        messages.value.push(event.data)
        syncActiveMessageCache()
      }
      await markActiveConversationRead(event.data)
      await Promise.all([messageStore.fetchUnreadCount(), loadConversations({ silent: true }), loadNotifications({ silent: true })])
      await scrollHistoryToBottom()
    }
  })
})

watch(() => route.query.tab, async (tab) => {
  activeTab.value = tab === 'notifications' ? 'notifications' : 'conversations'
  if (activeTab.value === 'notifications') await loadNotifications()
})
watch(() => route.fullPath, openFromQuery)
onUnmounted(() => {
  stopSocket?.()
  clearPendingImages()
})
</script>

<style scoped>
.message-page { min-height: calc(100vh - var(--artiv-header-height)); padding: 28px 0 44px; background: #f4f7f9; }
.message-page__body { display: grid; grid-template-columns: 188px minmax(0, 1fr); align-items: start; gap: 16px; }
.message-nav { position: sticky; top: calc(var(--artiv-header-height) + 24px); overflow: hidden; border: 1px solid #e4ebf0; border-radius: 10px; background: #fff; box-shadow: 0 8px 28px rgb(42 67 84 / 5%); }
.message-nav__title { display: flex; align-items: center; gap: 9px; padding: 18px 20px 15px; border-bottom: 1px solid #edf1f4; color: var(--artiv-text); font-size: 16px; }
.message-nav__title .el-icon { color: var(--artiv-primary); font-size: 18px; }
.message-nav__items { display: grid; gap: 4px; padding: 10px; }
.message-nav__items button, .message-nav__setting { display: flex; width: 100%; min-height: 42px; align-items: center; gap: 10px; padding: 0 12px; border: 0; border-radius: 7px; background: transparent; color: var(--artiv-text-secondary); cursor: pointer; font: inherit; text-align: left; transition: color .2s, background .2s; }
.message-nav__items button:hover { color: var(--artiv-primary); background: #f4fbff; }
.message-nav__items button.active { color: var(--artiv-primary); background: var(--artiv-primary-light); font-weight: 600; }
.message-nav__items .el-icon, .message-nav__setting .el-icon { font-size: 17px; }
.message-nav__unread { display: inline-flex; min-width: 18px; height: 18px; align-items: center; justify-content: center; margin-left: auto; padding: 0 5px; border-radius: 999px; background: #f56c6c; color: #fff; font-size: 11px; font-weight: 700; line-height: 18px; }
.message-nav__divider { height: 1px; margin: 2px 14px 0; background: #edf1f4; }
.message-nav__setting { margin: 8px 10px 10px; width: calc(100% - 20px); color: #a6afb9; cursor: not-allowed; }
.message-nav__setting small { margin-left: auto; color: #b6bec6; font-size: 10px; }
.message-center { height: calc(100vh - var(--artiv-header-height) - 56px); min-height: 620px; overflow: hidden; border: 1px solid #e4ebf0; border-radius: 10px; background: #fff; box-shadow: 0 8px 28px rgb(42 67 84 / 5%); }
.message-section { display: flex; height: 100%; min-height: 0; flex-direction: column; }
.message-section__header { display: flex; min-height: 54px; flex: 0 0 auto; align-items: center; justify-content: space-between; padding: 0 22px; border-bottom: 1px solid var(--artiv-border); }
.message-section__header h2 { margin: 0; font-size: 16px; }
.message-layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); min-height: 0; flex: 1; }
.conversation-list { display: flex; min-height: 0; flex-direction: column; border-right: 1px solid var(--artiv-border); padding: 16px; }
.conversation-scroll, .chat-history { scrollbar-width: none; -ms-overflow-style: none; }
.conversation-scroll::-webkit-scrollbar, .chat-history::-webkit-scrollbar { width: 0; height: 0; }
.conversation-scroll { min-height: 0; flex: 1; overflow-y: auto; }
.panel-title, .chat-panel header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.panel-title h3 { margin: 0; font-size: 17px; }
.conversation-item { display: flex; width: 100%; gap: 10px; align-items: center; border: 0; background: transparent; padding: 13px 8px; text-align: left; cursor: pointer; border-radius: 6px; }
.conversation-item.active, .conversation-item:hover { background: var(--artiv-primary-light); }
.conversation-copy { min-width: 0; flex: 1; display: grid; gap: 4px; }
.conversation-copy b, .conversation-copy small { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.conversation-copy small { color: var(--artiv-text-secondary); }
.chat-panel { min-width: 0; min-height: 0; display: grid; grid-template-rows: auto minmax(0, 1fr) auto; }
.chat-panel header { padding: 18px 24px; border-bottom: 1px solid var(--artiv-border); }
.chat-panel header small { color: var(--artiv-text-secondary); }
.chat-history { min-height: 0; padding: 20px 28px 26px; overflow-y: auto; background: #f5f7fa; }
.chat-loading { display: grid; min-height: 120px; place-items: center; color: #9aa3af; font-size: 12px; }
.chat-time-divider { display: block; margin: 18px auto 12px; color: #9aa3af; font-size: 12px; text-align: center; }
.bubble-row { display: flex; align-items: flex-start; gap: 8px; margin: 10px 0; }
.bubble-row.mine { justify-content: flex-end; }
.bubble-avatar { flex: 0 0 auto; margin-top: 1px; border: 1px solid rgba(31, 42, 55, .06); background: #d9dee6; }
.bubble { position: relative; max-width: calc(68% - 42px); padding: 10px 13px; border: 1px solid #e5e9ef; border-radius: 8px; background: #fff; line-height: 1.6; white-space: pre-wrap; box-shadow: 0 1px 2px rgba(31, 42, 55, .04); }
.bubble-row:not(.mine) .bubble::before { position: absolute; top: 11px; left: -6px; width: 10px; height: 10px; border-left: 1px solid #e5e9ef; border-bottom: 1px solid #e5e9ef; background: #fff; content: ''; transform: rotate(45deg); }
.mine .bubble { border-color: var(--artiv-primary); background: var(--artiv-primary); color: #fff; box-shadow: none; }
.mine .bubble::after { position: absolute; top: 11px; right: -5px; width: 10px; height: 10px; background: var(--artiv-primary); content: ''; transform: rotate(45deg); }
.bubble--images { width: min(480px, 78%); max-width: 78%; padding: 6px; }
.message-images { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; }
.message-images :deep(.el-image) { width: 100%; aspect-ratio: 1 / 1; border-radius: 5px; overflow: hidden; cursor: zoom-in; }
.message-images :deep(.el-image:only-child) { grid-column: span 2; max-height: 380px; aspect-ratio: auto; }
.business-system-message-row { display: flex; justify-content: center; margin: 12px 0 18px; }
.business-system-message { width: min(520px, 74%); overflow: hidden; border: 1px solid #e2e7ed; border-radius: 8px; background: #fff; color: #46505c; box-shadow: 0 2px 8px rgba(31, 42, 55, .05); }
.business-system-message__title { display: block; padding: 14px 16px 0; color: var(--artiv-text); font-size: 14px; }
.business-system-message p { margin: 0; padding: 9px 16px 14px; color: #5f6975; line-height: 1.65; white-space: pre-wrap; }
.business-system-message__action { display: flex; width: 100%; align-items: center; justify-content: space-between; padding: 10px 16px; border: 0; border-top: 1px solid #edf0f4; background: #fff; color: var(--artiv-text); cursor: pointer; font-size: 12px; text-align: left; }
.business-system-message__action:hover { color: var(--artiv-primary); background: #f9fcfe; }
.chat-composer { display: grid; gap: 10px; padding: 10px 24px 16px; border-top: 1px solid var(--artiv-border); background: #fff; }
.chat-extension-bar { display: flex; min-height: 30px; align-items: center; gap: 4px; border-bottom: 1px solid #edf0f4; }
.chat-extension-bar :deep(.el-button) { margin: 0; padding-inline: 7px; }
.chat-extension-bar__item { color: var(--artiv-text); }
.chat-extension-bar__item:hover { color: var(--artiv-primary); }
.chat-extension-bar__image-icon { color: var(--artiv-text); font-size: 16px; }
.chat-extension-bar__hint { margin-left: auto; color: var(--artiv-text-secondary); font-size: 12px; }
.image-queue { display: flex; max-height: 90px; align-items: center; gap: 8px; overflow-x: auto; }
.image-queue__item { position: relative; flex: 0 0 auto; width: 64px; height: 64px; }
.image-queue__item img { width: 100%; height: 100%; border: 1px solid var(--artiv-border); border-radius: 5px; object-fit: cover; }
.image-queue__item button { position: absolute; top: -6px; right: -6px; width: 18px; height: 18px; border: 0; border-radius: 50%; background: #4c5563; color: #fff; cursor: pointer; line-height: 16px; }
.chat-composer__input-row { display: flex; align-items: flex-end; gap: 12px; }
.chat-composer__input-row :deep(.el-textarea) { flex: 1; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; clip-path: inset(50%); }
.notification-panel { min-height: 0; flex: 1; padding: 8px 28px 24px; overflow-y: auto; }
.notification-card { display: flex; align-items: flex-start; gap: 14px; padding: 18px; margin-top: 12px; border: 1px solid var(--artiv-border); border-radius: 10px; cursor: pointer; }
.notification-card:hover { border-color: var(--artiv-primary); }
.notification-card.unread { background: #f5fbff; }
.order-picker__hint { margin: 0 0 14px; color: var(--artiv-text-secondary); }.order-picker { display: grid; gap: 10px; max-height: 430px; overflow-y: auto; }.order-picker__item { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 18px; padding: 14px 16px; border: 1px solid var(--artiv-border); border-radius: 8px; background: #fff; text-align: left; cursor: pointer; }.order-picker__item:hover { border-color: var(--artiv-primary); background: #f7fcff; }.order-picker__item>span { display: grid; gap: 6px; }.order-picker__item>span:last-child { justify-items: end; }.order-picker__item small { color: var(--artiv-text-secondary); }.order-picker__item strong { color: #f59a23; }
.notice-icon { flex: 0 0 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: #fff; background: var(--artiv-primary); font-weight: 700; }
.notice-copy { flex: 1; min-width: 0; }
.notice-copy b { color: var(--artiv-text); }
.notice-copy p { margin: 8px 0; color: var(--artiv-text-secondary); line-height: 1.6; }
.notice-copy small { color: var(--artiv-text-secondary); }
@media (max-width: 900px) { .message-page__body { grid-template-columns: 158px minmax(0, 1fr); }.message-layout { grid-template-columns: 260px minmax(0, 1fr); }.message-nav__setting small { display: none; } }
@media (max-width: 760px) { .message-page { padding: 16px 0 28px; }.message-page__body { grid-template-columns: 1fr; }.message-nav { position: static; display: flex; align-items: center; padding: 8px; }.message-nav__title { padding: 0 10px; border: 0; }.message-nav__items { display: flex; flex: 1; padding: 0 8px; }.message-nav__items button { width: auto; }.message-nav__divider, .message-nav__setting { display: none; }.message-center { height: auto; min-height: 0; }.message-layout { grid-template-columns: 1fr; height: auto; }.conversation-list { border-right: 0; border-bottom: 1px solid var(--artiv-border); max-height: 220px; }.conversation-scroll { max-height: 172px; }.chat-panel { min-height: 440px; }.chat-history { padding-inline: 16px; }.notification-panel { padding: 16px; }.business-system-message { width: 100%; }.chat-extension-bar__hint { display: none; }.chat-composer { padding-inline: 16px; }.bubble--images { width: min(100%, 360px); max-width: 88%; } }
</style>
