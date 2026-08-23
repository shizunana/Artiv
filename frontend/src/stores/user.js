import { defineStore } from 'pinia'
import { getToken, setToken, setUser, getUser, clearAuth } from '@/utils/auth'
import { login as loginApi, getMe, updateMyWorkbenchRole } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    user: getUser() || null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 3,
    isPainter: (state) => state.user?.role === 2,
    isEmployer: (state) => state.user?.role === 1,
    certStatus: (state) => state.user?.certStatus ?? 0,
    /** 当前工作台身份（用于路由守卫与侧边栏） */
    workbenchRole: (state) => state.user?.role ?? 1,
  },
  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      this.user = data.user
      setToken(data.token)
      setUser(data.user)
      return data
    },
    async fetchMe() {
      const user = await getMe()
      this.updateCurrentUser(user)
      return user
    },
    updateCurrentUser(user) {
      this.user = user
      setUser(user)
    },
    logout() {
      this.token = ''
      this.user = null
      clearAuth()
    },
    /** 保存默认工作台入口；画师交易权限仍只由 certStatus 等后端规则决定。 */
    async switchRole(role) {
      const user = await updateMyWorkbenchRole(role)
      this.updateCurrentUser(user)
      return user
    },
  },
})
