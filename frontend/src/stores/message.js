import { defineStore } from 'pinia'
import { getConversations, getNotifications, getUnreadSummary, markAllNotificationsRead } from '@/api/message'

export const useMessageStore = defineStore('message', {
  state: () => ({
    list: [],
    conversations: [],
    notifications: [],
    unreadCount: 0,
    conversationUnreadCount: 0,
    notificationUnreadCount: 0,
    wsConnected: false,
  }),
  actions: {
    async fetchConversations(params) {
      const data = await getConversations(params)
      this.conversations = data.records || []
      return data
    },
    async fetchUnreadCount() {
      const summary = await getUnreadSummary()
      this.unreadCount = Number(summary?.totalUnreadCount || 0)
      this.conversationUnreadCount = Number(summary?.conversationUnreadCount || 0)
      this.notificationUnreadCount = Number(summary?.notificationUnreadCount || 0)
      return this.unreadCount
    },
    async fetchNotifications(params) {
      const data = await getNotifications(params)
      this.notifications = data.records || []
      return data
    },
    async readAll() {
      await markAllNotificationsRead()
      this.notifications.forEach((item) => { item.readAt = new Date().toISOString() })
      await this.fetchUnreadCount()
    },
    /** WebSocket 收到新消息时调用 */
    onNewMessage(message) {
      if (!message.readAt) this.fetchUnreadCount()
      this.fetchConversations({ page: 1, size: 30 })
      this.fetchNotifications({ page: 1, size: 30 })
    },
  },
})
