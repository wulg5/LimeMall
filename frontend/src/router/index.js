import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useShopStore } from '@/stores/shop'

/**
 * 路由表。
 * 管理端：侧边栏菜单由后端下发（RBAC），路由表与之对应；
 * 商城前台：/shop 独立布局与会员登录态。
 * 全局守卫：未登录跳对应登录页，未加载用户信息时先拉取。
 */
const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
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
      { path: 'login', component: () => import('@/views/shop/Login.vue'), meta: { title: '会员登录' } }
    ]
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据看板' } },
      { path: 'product/list', component: () => import('@/views/product/list.vue'), meta: { title: '商品列表' } },
      { path: 'product/edit', component: () => import('@/views/product/edit.vue'), meta: { title: '商品编辑' } },
      { path: 'product/brand', component: () => import('@/views/product/brand.vue'), meta: { title: '品牌管理' } },
      { path: 'product/category', component: () => import('@/views/product/category.vue'), meta: { title: '分类管理' } },
      { path: 'order/list', component: () => import('@/views/order/list.vue'), meta: { title: '订单列表' } },
      { path: 'order/return', component: () => import('@/views/order/return.vue'), meta: { title: '退款审核' } },
      { path: 'member/list', component: () => import('@/views/member/list.vue'), meta: { title: '会员管理' } },
      { path: 'marketing/coupon', component: () => import('@/views/marketing/coupon.vue'), meta: { title: '优惠券管理' } },
      { path: 'system/admin', component: () => import('@/views/system/admin.vue'), meta: { title: '管理员管理' } },
      { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { title: '角色管理' } },
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
  if (to.path === '/login') {
    return true
  }
  // 商城前台路由
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
  // 管理后台路由
  const userStore = useUserStore()
  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!userStore.info) {
    try {
      await userStore.fetchInfo()
    } catch (e) {
      userStore.logout()
      return { path: '/login' }
    }
  }
  return true
})

export default router
