import { defineStore } from 'pinia'
import { getConversations, getNotifications, getUnreadCount, markAllRead } from '@/api/message'

export const useMessageStore = defineStore('message', {
  state: () => ({
    list: [],
    conversations: [],
    notifications: [],
    unreadCount: 0,
    wsConnected: false,
  }),
  actions: {
    async fetchConversations(params) {
      const data = await getConversations(params)
      this.conversations = data.records || []
      return data
    },
    async fetchUnreadCount() {
      this.unreadCount = await getUnreadCount()
      return this.unreadCount
    },
    async fetchNotifications(params) {
      const data = await getNotifications(params)
      this.notifications = data.records || []
      return data
    },
    async readAll() {
      await markAllRead()
      this.notifications.forEach((item) => { item.readAt = new Date().toISOString() })
      this.unreadCount = 0
    },
    /** WebSocket 收到新消息时调用 */
    onNewMessage(message) {
      if (!message.readAt) {
        this.unreadCount += 1
      }
      this.fetchConversations({ page: 1, size: 30 })
      this.fetchNotifications({ page: 1, size: 30 })
    },
  },
})
