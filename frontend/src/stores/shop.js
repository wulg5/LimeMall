import { defineStore } from 'pinia'
import { portalLogin, getMe } from '@/api/portal'
import { buildLoginPayload } from '@/utils/crypto'

/**
 * 商城前台会员登录态 Store。
 * 与后台管理员令牌隔离：独立 localStorage key，互不影响。
 */
export const SHOP_TOKEN_KEY = 'lime_shop_token'

export const useShopStore = defineStore('shop', {
  state: () => ({
    token: localStorage.getItem(SHOP_TOKEN_KEY) || '',
    info: null
  }),

  actions: {
    async login(form) {
      const payload = await buildLoginPayload(form)
      const data = await portalLogin(payload)
      this.token = data.token
      localStorage.setItem(SHOP_TOKEN_KEY, data.token)
    },

    async fetchMe() {
      this.info = await getMe()
    },

    refreshSessionState() {
      if (!this.token || localStorage.getItem(SHOP_TOKEN_KEY) === this.token) return false
      this.clearSession()
      return true
    },

    clearSession() {
      this.token = ''
      this.info = null
    },

    logout() {
      if (this.token && localStorage.getItem(SHOP_TOKEN_KEY) === this.token) {
        localStorage.removeItem(SHOP_TOKEN_KEY)
      }
      this.clearSession()
    }
  }
})
