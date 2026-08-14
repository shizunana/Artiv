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
    await messageStore.fetchUnreadCount()
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
