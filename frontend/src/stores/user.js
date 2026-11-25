import { defineStore } from 'pinia'
import { login as loginApi, getInfo } from '@/api/auth'

/**
 * 用户登录态 Store。
 * 保存 JWT 令牌与管理员信息（角色/权限标识/菜单树），令牌持久化到 localStorage。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('lime_token') || '',
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
      const data = await loginApi(form)
      this.token = data.token
      localStorage.setItem('lime_token', data.token)
    },

    async fetchInfo() {
      const data = await getInfo()
      this.info = data
      this.roles = data.roles || []
      this.perms = data.perms || []
      this.menus = data.menus || []
    },

    logout() {
      this.token = ''
      this.info = null
      this.roles = []
      this.perms = []
      this.menus = []
      localStorage.removeItem('lime_token')
    }
  }
})
