import { defineStore } from 'pinia'
import { portalLogin, getMe } from '@/api/portal'

/**
 * 商城前台会员登录态 Store。
 * 与后台管理员令牌隔离：独立 localStorage key，互不影响。
 */
export const useShopStore = defineStore('shop', {
  state: () => ({
    token: localStorage.getItem('lime_shop_token') || '',
    info: null
  }),

  actions: {
    async login(form) {
      const data = await portalLogin(form)
      this.token = data.token
      localStorage.setItem('lime_shop_token', data.token)
    },

    async fetchMe() {
      this.info = await getMe()
    },

    logout() {
      this.token = ''
      this.info = null
      localStorage.removeItem('lime_shop_token')
    }
  }
})
