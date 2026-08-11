import { defineStore } from 'pinia'
import { getToken, setToken, setUser, getUser, clearAuth } from '@/utils/auth'
import { login as loginApi, getMe } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    user: getUser() || null,
    /** 当前演示身份（企划方/画师工作台切换用），null 表示跟随注册角色 */
    activeRole: null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 3,
    isPainter: (state) => (state.activeRole ?? state.user?.role) === 2,
    isEmployer: (state) => (state.activeRole ?? state.user?.role) === 1,
    certStatus: (state) => state.user?.certStatus ?? 0,
    /** 当前工作台身份（用于路由守卫与侧边栏） */
    workbenchRole: (state) => state.activeRole ?? state.user?.role ?? 1,
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
      this.user = user
      setUser(user)
      return user
    },
    logout() {
      this.token = ''
      this.user = null
      this.activeRole = null
      clearAuth()
    },
    /** 演示环境：切换企划方/画师工作台身份（不改变登录账号） */
    switchRole(role) {
      this.activeRole = role
    },
  },
})
