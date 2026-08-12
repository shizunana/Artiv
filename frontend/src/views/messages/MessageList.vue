<template>
  <div class="message-center wb-card">
    <el-tabs v-model="activeTab" class="message-tabs" @tab-change="onTabChange">
      <el-tab-pane label="我的消息" name="conversations">
        <div class="message-layout">
          <aside class="conversation-list" v-loading="loadingConversations">
            <div class="panel-title"><h3>会话</h3><el-button link @click="loadConversations">刷新</el-button></div>
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
              <div class="chat-history" v-loading="loadingMessages">
                <el-empty v-if="!messages.length" description="开始沟通吧" :image-size="72" />
                <template v-for="msg in messages" :key="msg.id">
                  <article v-if="isBusinessSystemMessage(msg)" class="business-system-message">
                    <span class="business-system-message__badge">系统提示</span>
                    <p>{{ msg.content }}</p>
                    <div class="business-system-message__footer">
                      <button type="button" @click="viewMessageDetail(msg)">查看详情</button>
                      <time>{{ formatDateTime(msg.createdAt) }}</time>
                    </div>
                  </article>
                  <div v-else class="bubble-row" :class="{ mine: msg.fromId === userStore.user?.id }">
                    <div class="bubble">{{ msg.content }}<time>{{ formatDateTime(msg.createdAt) }}</time></div>
                  </div>
                </template>
              </div>
              <footer>
                <el-input v-model="draft" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="输入消息，Enter 发送，Shift + Enter 换行" @keydown.enter.exact.prevent="send" />
                <el-button type="primary" :loading="sending" @click="send">发送</el-button>
              </footer>
            </template>
            <el-empty v-else description="从左侧选择会话，或从应征、橱窗下单页面发起沟通" />
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统通知" name="notifications">
        <div class="notification-panel" v-loading="loadingNotifications">
          <div class="panel-title"><h3>系统通知</h3><el-button link @click="readAll">全部已读</el-button></div>
          <el-empty v-if="!notifications.length" description="暂无系统通知" />
          <article v-for="notice in notifications" :key="notice.id" class="notification-card" :class="{ unread: !notice.readAt }" @click="openNotification(notice)">
            <div class="notice-icon">系</div>
            <div class="notice-copy"><b>{{ notificationTitle(notice) }}</b><p>{{ notice.content }}</p><small>{{ formatDateTime(notice.createdAt) }}</small></div>
            <el-button link type="primary">{{ notificationAction(notice) }}</el-button>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConversation, getConversations, getNotifications, markRead, sendPrivateMessage } from '@/api/message'
import { formatDateTime } from '@/utils/date'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { startMessageSocket } from '@/utils/ws'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const activeTab = ref('conversations')
const conversations = ref([])
const notifications = ref([])
const messages = ref([])
const active = ref(null)
const draft = ref('')
const loadingConversations = ref(false)
const loadingNotifications = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
let stopSocket

const conversationId = (item) => String(item.withUserId)
const sameConversation = (first, second) => first && conversationId(first) === conversationId(second)

async function loadConversations() {
  loadingConversations.value = true
  try {
    const data = await getConversations({ page: 1, size: 30 })
    conversations.value = data.records || []
  } finally {
    loadingConversations.value = false
  }
}

async function loadNotifications() {
  loadingNotifications.value = true
  try {
    const data = await getNotifications({ page: 1, size: 30 })
    notifications.value = data.records || []
  } finally {
    loadingNotifications.value = false
  }
}

async function openConversation(item) {
  active.value = item
  loadingMessages.value = true
  try {
    const data = await getConversation({ withUserId: item.withUserId, page: 1, size: 100 })
    messages.value = data.records || []
    await Promise.all(messages.value.filter((msg) => msg.toId === userStore.user?.id && !msg.readAt).map((msg) => markRead(msg.id)))
    await messageStore.fetchUnreadCount()
    await loadConversations()
  } finally {
    loadingMessages.value = false
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content || !active.value) return
  sending.value = true
  try {
    const message = await sendPrivateMessage({ toId: active.value.withUserId, content })
    messages.value.push(message)
    draft.value = ''
    await loadConversations()
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

function isBusinessSystemMessage(message) {
  return ['APPLICATION_CONVERSATION_STARTED', 'STALL_PURCHASE_CONVERSATION_STARTED'].includes(message.subtype)
}

function viewMessageDetail(message) {
  // TODO: 后续订单、企划详情页确定后，按 message.orderId / message.projectId 接入跳转。
  void message
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
  return '系统通知'
}

function notificationAction(notice) {
  return notice.subtype?.startsWith('APPLICATION') ? '查看应征' : notice.subtype?.startsWith('STALL') ? '查看消息' : '查看'
}

async function openNotification(notice) {
  if (!notice.readAt) await markRead(notice.id)
  await messageStore.fetchUnreadCount()
  if (notice.subtype === 'APPLICATION_CREATED' && notice.projectId) {
    router.push({ name: 'ProjectApplications', params: { id: notice.projectId } })
  } else if (notice.subtype?.startsWith('APPLICATION') || notice.subtype?.startsWith('STALL')) {
    activeTab.value = 'conversations'
    await loadConversations()
  }
}

async function openFromQuery() {
  const withUserId = Number(route.query.withUserId)
  if (!withUserId) return
  activeTab.value = 'conversations'
  await openConversation({ withUserId, withUserNickname: route.query.nickname || `用户 #${withUserId}` })
}

async function onTabChange(name) {
  if (name === 'notifications') await loadNotifications()
}

onMounted(async () => {
  await Promise.all([loadConversations(), loadNotifications(), messageStore.fetchUnreadCount()])
  await openFromQuery()
  stopSocket = startMessageSocket(async (event) => {
    await Promise.all([messageStore.fetchUnreadCount(), loadConversations(), loadNotifications()])
    if (event.type === 'NEW_MESSAGE' && active.value
      && event.data.fromId === active.value.withUserId) {
      messages.value.push(event.data)
      await markRead(event.data.id)
      await messageStore.fetchUnreadCount()
    }
  })
})

watch(() => route.fullPath, openFromQuery)
onUnmounted(() => stopSocket?.())
</script>

<style scoped>
.message-center { height: calc(100vh - 112px); min-height: 520px; padding: 0; overflow: hidden; }
.message-tabs { display: flex; height: 100%; flex-direction: column; }
.message-tabs :deep(.el-tabs__header) { flex: 0 0 auto; margin: 0; padding: 0 22px; border-bottom: 1px solid var(--huiyue-border); }
.message-tabs :deep(.el-tabs__content) { flex: 1; min-height: 0; padding: 0; }
.message-tabs :deep(.el-tab-pane) { height: 100%; }
.message-layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); height: 100%; min-height: 0; }
.conversation-list { display: flex; min-height: 0; flex-direction: column; border-right: 1px solid var(--huiyue-border); padding: 16px; }
.conversation-scroll, .chat-history { scrollbar-width: none; -ms-overflow-style: none; }
.conversation-scroll::-webkit-scrollbar, .chat-history::-webkit-scrollbar { width: 0; height: 0; }
.conversation-scroll { min-height: 0; flex: 1; overflow-y: auto; }
.panel-title, .chat-panel header, .chat-panel footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.panel-title h3 { margin: 0; font-size: 17px; }
.conversation-item { display: flex; width: 100%; gap: 10px; align-items: center; border: 0; background: transparent; padding: 13px 8px; text-align: left; cursor: pointer; border-radius: 6px; }
.conversation-item.active, .conversation-item:hover { background: var(--huiyue-primary-light); }
.conversation-copy { min-width: 0; flex: 1; display: grid; gap: 4px; }
.conversation-copy b, .conversation-copy small { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.conversation-copy small { color: var(--huiyue-text-secondary); }
.chat-panel { min-width: 0; min-height: 0; display: grid; grid-template-rows: auto minmax(0, 1fr) auto; }
.chat-panel header { padding: 18px 24px; border-bottom: 1px solid var(--huiyue-border); }
.chat-panel header small { color: var(--huiyue-text-secondary); }
.chat-history { min-height: 0; padding: 22px 24px; overflow-y: auto; }
.bubble-row { display: flex; margin: 10px 0; }
.bubble-row.mine { justify-content: flex-end; }
.bubble { max-width: 68%; padding: 10px 13px; border-radius: 8px; background: #f2f4f7; line-height: 1.6; white-space: pre-wrap; }
.mine .bubble { background: var(--huiyue-primary); color: #fff; }
.bubble time { display: block; margin-top: 5px; font-size: 11px; opacity: .7; }
.business-system-message { width: min(460px, 78%); margin: 14px 0; padding: 12px 14px 10px; border: 1px solid #c8d8ea; border-radius: 8px; background: #f6faff; color: #46566b; }
.business-system-message__badge { display: inline-block; margin-bottom: 6px; padding: 2px 7px; border-radius: 10px; background: #e4f3ff; color: var(--huiyue-primary); font-size: 11px; }
.business-system-message p { margin: 0; line-height: 1.65; }
.business-system-message__footer { display: flex; align-items: center; justify-content: space-between; margin-top: 9px; }
.business-system-message__footer button { padding: 0; border: 0; background: transparent; color: var(--huiyue-primary); cursor: pointer; font-size: 12px; }
.business-system-message__footer time { color: var(--huiyue-text-secondary); font-size: 11px; }
.chat-panel footer { align-items: flex-end; padding: 16px 24px; border-top: 1px solid var(--huiyue-border); }
.chat-panel footer :deep(.el-textarea) { flex: 1; }
.notification-panel { min-height: 535px; padding: 20px 28px; }
.notification-card { display: flex; align-items: flex-start; gap: 14px; padding: 18px; margin-top: 12px; border: 1px solid var(--huiyue-border); border-radius: 10px; cursor: pointer; }
.notification-card:hover { border-color: var(--huiyue-primary); }
.notification-card.unread { background: #f5fbff; }
.notice-icon { flex: 0 0 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: #fff; background: var(--huiyue-primary); font-weight: 700; }
.notice-copy { flex: 1; min-width: 0; }
.notice-copy b { color: var(--huiyue-text); }
.notice-copy p { margin: 8px 0; color: var(--huiyue-text-secondary); line-height: 1.6; }
.notice-copy small { color: var(--huiyue-text-secondary); }
@media (max-width: 760px) { .message-center { height: auto; min-height: 0; }.message-tabs { min-height: 0; }.message-layout { grid-template-columns: 1fr; height: auto; }.conversation-list { border-right: 0; border-bottom: 1px solid var(--huiyue-border); max-height: 220px; }.conversation-scroll { max-height: 172px; }.chat-panel { min-height: 440px; }.notification-panel { padding: 16px; }.business-system-message { width: auto; max-width: 100%; } }
</style>
