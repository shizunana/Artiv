<template>
  <router-view />
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { startMessageSocket, stopMessageSocket } from '@/utils/ws'

const userStore = useUserStore()
const messageStore = useMessageStore()
let stopListener = null

async function handleSocketEvent(event) {
  try {
    if (event?.type === 'WS_CONNECTED' || event?.type === 'WS_RECONNECTED') {
      await Promise.all([
        messageStore.fetchUnreadCount(),
        messageStore.fetchConversations({ page: 1, size: 30 }),
        messageStore.fetchNotifications({ page: 1, size: 30 }),
      ])
    } else {
      await messageStore.fetchUnreadCount()
    }
    if (event?.type === 'NEW_MESSAGE' && event.data?.subtype === 'ORDER_COMPLETED') {
      await userStore.fetchMe()
    }
  } catch (_) {
    // HTTP refresh will be retried when the account menu is opened.
  }
}

watch(() => userStore.token, (token) => {
  stopListener?.()
  stopListener = null
  stopMessageSocket()
  if (token) {
    stopListener = startMessageSocket(handleSocketEvent)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  stopListener?.()
  stopMessageSocket()
})
</script>
