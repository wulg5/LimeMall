import { defineStore } from 'pinia'
import { login as loginApi, getInfo } from '@/api/auth'
import { buildLoginPayload } from '@/utils/crypto'

/**
 * 用户登录态 Store。
 * 保存 JWT 令牌与管理员信息（角色/权限标识/菜单树），令牌持久化到 localStorage。
 */
export const ADMIN_TOKEN_KEY = 'lime_token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    info: null,
    roles: [],
    perms: [],
    menus: []
  }),

  getters: {
    /** 判断当前用户是否拥有某权限标识 */
    hasPerm: (state) => (perm) => state.perms.includes(perm)
  },

  actions: {
    async login(form) {
      const payload = await buildLoginPayload(form)
      const data = await loginApi(payload)
      this.token = data.token
      localStorage.setItem(ADMIN_TOKEN_KEY, data.token)
    },

    async fetchInfo() {
      const data = await getInfo()
      this.info = data
      this.roles = data.roles || []
      this.perms = data.perms || []
      this.menus = data.menus || []
    },

    refreshSessionState() {
      if (!this.token || localStorage.getItem(ADMIN_TOKEN_KEY) === this.token) return false
      this.clearSession()
      return true
    },

    clearSession() {
      this.token = ''
      this.info = null
      this.roles = []
      this.perms = []
      this.menus = []
    },

    logout() {
      if (this.token && localStorage.getItem(ADMIN_TOKEN_KEY) === this.token) {
        localStorage.removeItem(ADMIN_TOKEN_KEY)
      }
      this.clearSession()
    }
  }
})
