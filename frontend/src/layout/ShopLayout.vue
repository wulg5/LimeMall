<template>
  <div class="shop-layout">
    <!-- 顶部导航 -->
    <header class="shop-header">
      <div class="header-inner">
        <router-link to="/shop" class="shop-logo">
          <img src="/img/logo.svg" alt="LimeMall" />
          <span>青柠商城</span>
        </router-link>

        <nav class="shop-nav">
          <router-link to="/shop" exact-active-class="active">首页</router-link>
          <router-link to="/shop/products" active-class="active">全部商品</router-link>
          <router-link to="/shop/coupons" active-class="active">领券中心</router-link>
          <router-link to="/shop/points" active-class="active">积分商城</router-link>
          <router-link to="/shop/orders" active-class="active">我的订单</router-link>
          <router-link v-if="shopStore.token" to="/shop/member" active-class="active">会员中心</router-link>
        </nav>

        <div class="shop-search">
          <el-input v-model="keyword" placeholder="搜索商品" clearable @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

        <div class="shop-actions">
          <router-link to="/shop/cart" class="cart-link">
            <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0" :max="99">
              <el-icon :size="22"><ShoppingCart /></el-icon>
            </el-badge>
            <span>购物车</span>
          </router-link>

          <el-dropdown v-if="shopStore.token" trigger="click" @command="handleCommand">
            <span class="user-chip">
              <el-avatar :size="30" :src="shopStore.info?.avatar || '/img/avatar.svg'" />
              <span>{{ shopStore.info?.nickname || '会员' }}</span>
              <level-badge :level="shopStore.info?.level || 1" />
              <el-icon><ArrowDown /></el-icon>
            </span>
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
            <span class="divider">|</span>
            <router-link to="/shop/login?mode=register">注册</router-link>
          </div>
        </div>
      </div>
    </header>

    <!-- 页面内容 -->
    <main class="shop-main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="shop-footer">
      青柠商城 LimeMall · 软件工程课程设计作品 · 后台管理入口
      <router-link to="/admin/login" class="admin-link">管理端</router-link>
    </footer>

    <!-- 悬浮客服 -->
    <shop-chat-widget />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Search, ShoppingCart } from '@element-plus/icons-vue'
import { useShopStore } from '@/stores/shop'
import { useCartStore } from '@/stores/cart'
import { MEMBER_LEVEL } from '@/utils/format'
import ShopChatWidget from '@/components/ShopChatWidget.vue'
import LevelBadge from '@/components/LevelBadge.vue'

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()
const cartStore = useCartStore()

const keyword = ref('')

const levelText = computed(() => MEMBER_LEVEL[shopStore.info?.level]?.text || '普通会员')

onMounted(async () => {
  if (shopStore.token && !shopStore.info) {
    try {
      await shopStore.fetchMe()
    } catch (e) {
      shopStore.logout()
    }
  }
})

function handleSearch() {
  router.push({ path: '/shop/products', query: { name: keyword.value || undefined } })
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
.shop-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f8f6;
}

.shop-header {
  background: #ffffff;
  border-bottom: 1px solid #e8efeb;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 16px;
}

.shop-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #065f46;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
  flex-shrink: 0;
}

.shop-logo img {
  width: 38px;
  height: 38px;
  border-radius: 10px;
}

.shop-nav {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.shop-nav a {
  padding: 8px 14px;
  border-radius: 10px;
  color: #374151;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}

.shop-nav a:hover {
  color: var(--el-color-primary);
  background: #f0faf5;
}

.shop-nav a.active {
  color: var(--el-color-primary);
  font-weight: 600;
  background: #e7f8f2;
}

.shop-search {
  flex: 1;
  max-width: 320px;
}

.shop-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
}

.cart-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #374151;
  text-decoration: none;
  font-size: 14px;
}

.cart-link:hover {
  color: var(--el-color-primary);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  font-size: 14px;
  color: #374151;
}

.user-chip:hover {
  background: #f0faf5;
}

.auth-links {
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.auth-links a {
  color: var(--el-color-primary);
  text-decoration: none;
}

.auth-links .divider {
  color: #d1d5db;
}

.shop-main {
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.shop-footer {
  text-align: center;
  padding: 24px;
  color: #9ca3af;
  font-size: 13px;
  border-top: 1px solid #e8efeb;
  background: #ffffff;
}

.admin-link {
  color: #9ca3af;
  margin-left: 8px;
}
</style>
