<template>
  <div class="message-list">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>消息中心</h3>
        <el-button size="small" round @click="readAll">全部已读</el-button>
      </div>

      <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
      <div v-else v-loading="loading" class="msg-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="msg-item"
          :class="{ 'is-unread': !msg.readAt }"
          @click="markRead(msg)"
        >
          <div class="msg-dot" :class="{ 'is-read': msg.readAt }"></div>
          <div class="msg-content">
            <p>{{ msg.content }}</p>
            <span class="msg-time">{{ formatDateTime(msg.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMessages, markRead as markReadApi, markAllRead } from '@/api/message'
import { formatDateTime } from '@/utils/date'

const messages = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getMessages({ page: 1, size: 30 })
    messages.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function markRead(msg) {
  if (msg.readAt) return
  await markReadApi(msg.id)
  msg.readAt = new Date().toISOString()
}

async function readAll() {
  await markAllRead()
  messages.value.forEach((m) => (m.readAt = new Date().toISOString()))
}

onMounted(load)
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.wb-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.wb-card__head h3 {
  margin: 0;
  font-size: 17px;
}

.msg-list {
  display: flex;
  flex-direction: column;
}

.msg-item {
  display: flex;
  gap: 12px;
  padding: 14px 8px;
  border-bottom: 1px solid var(--huiyue-border);
  cursor: pointer;
}

.msg-item:last-child {
  border-bottom: none;
}

.msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--huiyue-accent);
  margin-top: 7px;
  flex-shrink: 0;
}

.msg-dot.is-read {
  background: #dcdfe6;
}

.msg-content {
  flex: 1;
}

.msg-content p {
  margin: 0 0 6px;
  color: var(--huiyue-text);
  line-height: 1.6;
}

.msg-time {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}
</style>
