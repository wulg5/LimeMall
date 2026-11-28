<template>
  <div class="shop-layout">
    <a class="skip-link" href="#shop-content">跳到商城内容</a>
    <!-- 顶部导航 -->
    <header class="shop-header">
      <div class="header-inner">
        <router-link to="/shop" class="brand-lockup"><span class="brand-mark" aria-hidden="true"><el-icon><Citrus /></el-icon></span><span><strong>青柠商城</strong><small>LimeMall</small></span></router-link>

        <form class="shop-search" role="search" @submit.prevent="handleSearch">
          <el-autocomplete v-model="keyword" :fetch-suggestions="fetchSuggestions" :trigger-on-focus="false" :debounce="300" select-when-unmatched
                           placeholder="搜索商品、品牌或关键词" aria-label="搜索商品" clearable
                           @select="handleSelect">
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #default="{ item }">
              <span class="suggest-item">{{ item.value }}</span>
            </template>
          </el-autocomplete>
          <button type="submit" class="search-submit">搜索</button>
        </form>

        <div class="shop-actions">
          <router-link to="/shop/cart" class="cart-link" aria-label="购物车">
            <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0" :max="99">
              <el-icon :size="22"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="cart-label">购物车</span>
          </router-link>

          <el-dropdown v-if="shopStore.token" trigger="click" @command="handleCommand">
            <button class="user-chip" type="button" aria-label="会员菜单">
              <el-avatar :size="30" :src="shopStore.info?.avatar || '/img/avatar.svg'" />
              <span class="member-name">{{ shopStore.info?.nickname || '会员' }}</span>
              <level-badge :level="shopStore.info?.level || 1" />
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ levelText }}
                </el-dropdown-item>
                <el-dropdown-item command="member" divided>会员中心</el-dropdown-item>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="coupons">我的优惠券</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <div v-else class="auth-links">
            <router-link to="/shop/login">登录</router-link>
            <router-link to="/shop/login?mode=register">注册</router-link>
          </div>
        </div>
      </div>
    <nav class="shop-nav nav-band" aria-label="商城导航">
        <div class="nav-inner">
          <router-link to="/shop" exact-active-class="active">首页</router-link>
          <router-link to="/shop/products" active-class="active">全部商品</router-link>
          <router-link to="/shop/coupons" active-class="active">领券中心</router-link>
          <router-link to="/shop/points" active-class="active">积分商城</router-link>
          <router-link to="/shop/orders" active-class="active">我的订单</router-link>
          <router-link v-if="shopStore.token" to="/shop/member" active-class="active">会员中心</router-link>
        </div>
      </nav>
    </header>

    <!-- 页面内容 -->
    <main id="shop-content" class="shop-main" tabindex="-1">
      <router-view v-slot="{ Component }">
        <component :is="Component" :key="checkoutViewKey" />
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="shop-footer">
      <div class="footer-inner">
        <span><strong>青柠商城 LimeMall</strong>　软件工程课程设计作品</span>
        <span><a href="/img/demo-products/SOURCES.html">商品图片来源</a><router-link to="/admin/login">管理端</router-link></span>
      </div>
    </footer>

    <!-- 悬浮客服 -->
    <shop-chat-widget />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Search, ShoppingCart, Orange as Citrus } from '@element-plus/icons-vue'
import { useShopStore } from '@/stores/shop'
import { CART_STORAGE_KEY, useCartStore } from '@/stores/cart'
import { searchSuggest } from '@/api/portal'
import { MEMBER_LEVEL } from '@/utils/format'
import ShopChatWidget from '@/components/ShopChatWidget.vue'
import LevelBadge from '@/components/LevelBadge.vue'

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()
const cartStore = useCartStore()

const checkoutViewKey = computed(() => route.path === '/shop/checkout' ? route.fullPath : undefined)
const keyword = ref('')
watch(() => route.query.name, name => { keyword.value = typeof name === "string" ? name : "" }, { immediate: true })

const levelText = computed(() => MEMBER_LEVEL[shopStore.info?.level]?.text || '普通会员')

function syncCart(event) {
  if (event.key === CART_STORAGE_KEY || event.key === null) cartStore.syncFromStorage()
}

onUnmounted(() => window.removeEventListener('storage', syncCart))

onMounted(async () => {
  window.addEventListener('storage', syncCart)
  cartStore.syncFromStorage()
  if (shopStore.token && !shopStore.info) {
    try {
      await shopStore.fetchMe()
    } catch (e) {
      shopStore.logout()
    }
  }
})

function handleSearch() {
  router.push({ path: '/shop/products', query: { name: keyword.value.trim() || undefined } })
}

let searchVersion = 0
async function fetchSuggestions(query, callback) {
  const version = ++searchVersion
  const name = query.trim()
  if (!name) {
    callback([])
    return
  }
  try {
    const list = await searchSuggest(name)
    if (version === searchVersion) callback((list || []).map(item => ({ value: item.name, id: item.id })))
  } catch {
    if (version === searchVersion) callback([])
  }
}

function handleSelect(item) {
  if (item.id == null) {
    handleSearch()
    return
  }
  router.push('/shop/product/' + item.id)
}

async function handleCommand(command) {
  if (command === 'member') {
    router.push('/shop/member')
  } else if (command === 'orders') {
    router.push('/shop/orders')
  } else if (command === 'coupons') {
    router.push('/shop/coupons?tab=mine')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      shopStore.logout()
      if (route.meta.auth) {
        router.push('/shop')
      }
    } catch (e) {
      /* 取消 */
    }
  }
}
</script>

<style scoped>
.shop-layout { min-height: 100dvh; display: flex; flex-direction: column; }
.shop-header { background: var(--pine); color: var(--paper); }
.header-inner, .nav-inner, .footer-inner { max-width: var(--shop-width); margin: 0 auto; padding-inline: var(--space-page); }
.header-inner { display: grid; grid-template-columns: minmax(158px, .85fr) minmax(250px, 2fr) minmax(200px, .85fr); align-items: center; gap: clamp(9px, 2.5vw, 38px); padding-block: 17px; }
.brand-lockup small { color: var(--lime); }
.shop-search { display: flex; min-width: 0; }
.shop-search :deep(.el-autocomplete) { flex: 1; min-width: 0; }
.shop-search :deep(.el-input__wrapper) { border-radius: 8px 0 0 8px; box-shadow: none; min-height: 43px; }
.shop-search :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px var(--focus) inset; }
.search-submit { border: 0; border-radius: 0 8px 8px 0; min-width: 67px; background: var(--lime); color: var(--pine); font: inherit; font-weight: 750; cursor: pointer; }
.search-submit:hover { background: #eaf5b8; }
.shop-actions, .cart-link, .auth-links, .user-chip { display: flex; align-items: center; }
.shop-actions { gap: clamp(8px, 1.4vw, 20px); justify-content: flex-end; }
.cart-link, .auth-links a, .user-chip { min-height: 44px; color: var(--paper); text-decoration: none; white-space: nowrap; }
.cart-link { gap: 7px; }
.auth-links { gap: 9px; }
.auth-links a { display: inline-flex; align-items: center; }
.auth-links a:hover, .cart-link:hover { color: var(--lime); text-decoration: underline; }
.user-chip { gap: 7px; padding: 0 6px; background: transparent; border: 0; cursor: pointer; }
.user-chip:hover, .user-chip[aria-expanded='true'] { background: #ffffff1a; }
.user-chip .member-name { max-width: 90px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.nav-band { background: var(--paper); border-bottom: 1px solid var(--line); color: var(--ink); }
.nav-inner { display: flex; gap: clamp(10px, 2.4vw, 30px); align-items: center; overflow-x: auto; }
.shop-nav a { display: flex; align-items: center; min-height: 45px; border-bottom: 3px solid transparent; padding: 3px 3px 0; text-decoration: none; color: var(--muted); font-weight: 650; white-space: nowrap; }
.shop-nav a:hover, .shop-nav a.active { color: var(--action); }
.shop-nav a.active { border-bottom-color: var(--action); }
.shop-main { flex: 1; width: 100%; max-width: var(--shop-width); min-width: 0; margin: 0 auto; padding: 21px var(--space-page) 58px; }
.shop-footer { padding: 24px 0 32px; background: var(--paper); border-top: 1px solid var(--line); color: var(--muted); }
.footer-inner { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 12px; }
.footer-inner strong { color: var(--pine); }
.footer-inner a { margin-left: 13px; }
@media (max-width: 920px) {
  .user-chip :deep(.level-badge), .user-chip .member-name { display: none; }
  .header-inner { grid-template-columns: max-content minmax(170px, 1fr) max-content; }
}
@media (max-width: 650px) {
  .header-inner { grid-template-columns: max-content minmax(0, 1fr); padding-block: 10px; gap: 8px; }
  .shop-search { grid-row: 2; grid-column: 1 / -1; }
  .shop-actions { grid-row: 1; grid-column: 2; }
  .shop-actions :deep(.el-badge__content) { transform: translate(50%, -20%); }
  .shop-main { padding-top: 13px; padding-bottom: 34px; }
  .nav-inner { gap: 16px; }
  .shop-header .brand-lockup:focus-visible, .shop-actions :focus-visible { outline-color: var(--lime); }
  .shop-nav a { font-size: 13px; }
}
@media (max-width: 360px) { .cart-label { display: none; } .brand-lockup strong { font-size: 19px; } }
</style>
