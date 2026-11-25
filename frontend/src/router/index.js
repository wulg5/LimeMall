import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useShopStore } from '@/stores/shop'

/**
 * 路由表。
 * 架构约定：
 * - 商城前台是主站（/ 与 /shop/**），无需登录即可浏览；
 * - 管理后台是独立入口（/admin/login 登录，/admin/** 业务页），与学生端完全隔离；
 * - 侧边栏菜单由后端 RBAC 下发，path 与 /admin/** 路由一一对应。
 */
const routes = [
  // 根路径进入商城前台
  { path: '/', redirect: '/shop' },
  // 旧地址兼容（管理端路由已统一迁移到 /admin 前缀下）
  { path: '/login', redirect: '/shop' },
  { path: '/dashboard', redirect: '/admin/dashboard' },
  { path: '/product/:pathMatch(.*)*', redirect: (to) => '/admin' + to.fullPath },
  { path: '/order/:pathMatch(.*)*', redirect: (to) => '/admin' + to.fullPath },
  { path: '/member/:pathMatch(.*)*', redirect: (to) => '/admin' + to.fullPath },
  { path: '/marketing/:pathMatch(.*)*', redirect: (to) => '/admin' + to.fullPath },
  { path: '/system/:pathMatch(.*)*', redirect: (to) => '/admin' + to.fullPath },

  // ============ 商城前台（C端） ============
  {
    path: '/shop',
    component: () => import('@/layout/ShopLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/shop/Home.vue'), meta: { title: '首页' } },
      { path: 'products', component: () => import('@/views/shop/Products.vue'), meta: { title: '全部商品' } },
      { path: 'product/:id', component: () => import('@/views/shop/ProductDetail.vue'), meta: { title: '商品详情' } },
      { path: 'cart', component: () => import('@/views/shop/Cart.vue'), meta: { title: '购物车' } },
      { path: 'checkout', component: () => import('@/views/shop/Checkout.vue'), meta: { title: '确认订单', auth: true } },
      { path: 'orders', component: () => import('@/views/shop/Orders.vue'), meta: { title: '我的订单', auth: true } },
      { path: 'coupons', component: () => import('@/views/shop/Coupons.vue'), meta: { title: '领券中心' } },
      { path: 'points', component: () => import('@/views/shop/Points.vue'), meta: { title: '积分商城' } },
      { path: 'member', component: () => import('@/views/shop/Member.vue'), meta: { title: '会员中心', auth: true } },
      { path: 'login', component: () => import('@/views/shop/Login.vue'), meta: { title: '会员登录' } }
    ]
  },

  // ============ 管理后台（B端，独立入口） ============
  {
    path: '/admin/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: () => import('@/layout/index.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据看板' } },
      { path: 'product/list', component: () => import('@/views/product/list.vue'), meta: { title: '商品列表' } },
      { path: 'product/edit', component: () => import('@/views/product/edit.vue'), meta: { title: '商品编辑' } },
      { path: 'product/brand', component: () => import('@/views/product/brand.vue'), meta: { title: '品牌管理' } },
      { path: 'product/category', component: () => import('@/views/product/category.vue'), meta: { title: '分类管理' } },
      { path: 'product/comment', component: () => import('@/views/product/comment.vue'), meta: { title: '评价管理' } },
      { path: 'order/list', component: () => import('@/views/order/list.vue'), meta: { title: '订单列表' } },
      { path: 'order/return', component: () => import('@/views/order/return.vue'), meta: { title: '退款审核' } },
      { path: 'chat', component: () => import('@/views/chat/ChatWorkbench.vue'), meta: { title: '客服聊天' } },
      { path: 'member/list', component: () => import('@/views/member/list.vue'), meta: { title: '会员管理' } },
      { path: 'marketing/coupon', component: () => import('@/views/marketing/coupon.vue'), meta: { title: '优惠券管理' } },
      { path: 'marketing/content', component: () => import('@/views/marketing/content.vue'), meta: { title: '内容管理' } },
      { path: 'marketing/flash', component: () => import('@/views/marketing/flash.vue'), meta: { title: '秒杀管理' } },
      { path: 'system/admin', component: () => import('@/views/system/admin.vue'), meta: { title: '管理员管理' } },
      { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { title: '角色管理' } },
      { path: 'system/log', component: () => import('@/views/system/log.vue'), meta: { title: '操作日志' } },
      { path: 'system/profile', component: () => import('@/views/system/profile.vue'), meta: { title: '个人中心' } }
    ]
  },

  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  document.title = (to.meta.title ? `${to.meta.title} · ` : '') + '青柠商城管理系统'

  // 商城前台：公开浏览，auth 页面要求会员登录
  if (to.path.startsWith('/shop')) {
    const shopStore = useShopStore()
    if (shopStore.token && !shopStore.info) {
      try {
        await shopStore.fetchMe()
      } catch (e) {
        shopStore.logout()
      }
    }
    if (to.meta.auth && !shopStore.token) {
      return { path: '/shop/login', query: { redirect: to.fullPath } }
    }
    return true
  }

  // 管理后台登录页
  if (to.path === '/admin/login') {
    return true
  }

  // 管理后台业务页：要求管理员登录
  if (to.path.startsWith('/admin')) {
    const userStore = useUserStore()
    if (!userStore.token) {
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    if (!userStore.info) {
      try {
        await userStore.fetchInfo()
      } catch (e) {
        userStore.logout()
        return { path: '/admin/login' }
      }
    }
    return true
  }
  return true
})

export default router
