import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { ADMIN_TOKEN_KEY, useUserStore } from '@/stores/user'
import { SHOP_TOKEN_KEY, useShopStore } from '@/stores/shop'
import 'element-plus/dist/index.css'
import '@/assets/styles/index.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局注册 Element Plus 图标组件，菜单图标可按名称动态渲染
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

window.addEventListener('storage', (event) => {
  const route = router.currentRoute.value
  if ((event.key === SHOP_TOKEN_KEY || event.key === null) && useShopStore().refreshSessionState()
      && route.path.startsWith('/shop') && route.meta.auth) {
    router.replace({ path: '/shop/login', query: { redirect: route.fullPath } })
  }
  if ((event.key === ADMIN_TOKEN_KEY || event.key === null) && useUserStore().refreshSessionState()
      && route.path.startsWith('/admin') && route.path !== '/admin/login') {
    router.replace('/admin/login')
  }
})

app.mount('#app')
