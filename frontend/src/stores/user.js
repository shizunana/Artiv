import { defineStore } from 'pinia'
import { getToken, setToken, setUser, getUser, clearAuth } from '@/utils/auth'
import { login as loginApi, getMe } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    user: getUser() || null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 3,
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
      clearAuth()
    },
  },
})
