import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useShopStore } from '@/stores/shop'

/**
 * Axios 实例与统一拦截器。
 * 1. 请求拦截：按接口前缀自动携带对应端令牌
 *    （/portal/** 用会员令牌，其余用管理员令牌）；
 * 2. 响应拦截：解包后端统一 Result 结构；401 清空对应登录态并跳转对应登录页；
 *    业务错误统一弹出 ElMessage 提示。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

function isPortalRequest(config) {
  return (config.url || '').startsWith('/portal')
}

request.interceptors.request.use((config) => {
  const store = isPortalRequest(config) ? useShopStore() : useUserStore()
  store.refreshSessionState()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      const portal = isPortalRequest(response.config)
      const store = portal ? useShopStore() : useUserStore()
      const sent = response.config.headers.Authorization
      if (!store.token || sent === `Bearer ${store.token}`) {
        store.logout()
        ElMessage.error(res.message || '登录已过期，请重新登录')
        window.location.href = portal ? '/shop/login' : '/admin/login'
      }
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '操作失败')
    const error = new Error(res.message || '操作失败')
    error.businessRejected = res.code >= 400 && res.code < 500
    return Promise.reject(error)
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
