import { defineStore } from 'pinia'
import { getMessages, markAllRead } from '@/api/message'

export const useMessageStore = defineStore('message', {
  state: () => ({
    list: [],
    unreadCount: 0,
    wsConnected: false,
  }),
  actions: {
    async fetchMessages(params) {
      const data = await getMessages(params)
      this.list = data.records || []
      this.unreadCount = data.unreadCount ?? 0
      return data
    },
    async readAll() {
      await markAllRead()
      this.list.forEach((item) => (item.read = true))
      this.unreadCount = 0
    },
    /** WebSocket 收到新消息时调用 */
    onNewMessage(message) {
      this.list.unshift(message)
      if (!message.read) {
        this.unreadCount += 1
      }
    },
  },
})
