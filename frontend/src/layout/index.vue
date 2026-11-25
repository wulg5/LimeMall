<template>
  <el-container class="layout">
    <el-aside width="232px" class="aside">
      <div class="logo">
        <img src="/img/logo.svg" alt="logo" />
        <div class="logo-text">
          <div class="name">青柠商城</div>
          <div class="sub">LimeMall Admin</div>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu :key="activeMenu" :default-active="activeMenu" router unique-opened class="side-menu">
          <sidebar-item v-for="menu in userStore.menus" :key="menu.id" :menu="menu" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header" height="60px">
        <div class="header-left">
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-chip">
              <el-avatar :size="30" :src="userStore.info?.avatar || '/img/avatar.svg'" />
              <span class="nickname">{{ userStore.info?.nickname || '管理员' }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ userStore.roles.join(' / ') }}
                </el-dropdown-item>
                <el-dropdown-item command="profile" divided>个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前高亮菜单：商品编辑页归属商品列表 */
const activeMenu = computed(() =>
  route.path.startsWith('/product/edit') ? '/product/list' : route.path
)

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/system/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      userStore.logout()
      router.push('/login')
    } catch (e) {
      /* 取消退出 */
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #ffffff;
  border-right: 1px solid #ecf1ee;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid #f1f5f3;
}

.logo img {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

.logo .name {
  font-size: 17px;
  font-weight: 700;
  color: #065f46;
  letter-spacing: 1px;
}

.logo .sub {
  font-size: 11px;
  color: #9ca3af;
  letter-spacing: 0.5px;
}

.menu-scroll {
  flex: 1;
}

.side-menu {
  border-right: none;
  padding: 10px;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  border-radius: 10px;
  margin: 2px 0;
  color: #374151;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: #f0faf5;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #10b981, #34d399);
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.28);
}

.side-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #ffffff;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  height: 40px;
  line-height: 40px;
  min-width: 0;
}

.header {
  background: #ffffff;
  border-bottom: 1px solid #ecf1ee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  transition: background 0.2s;
}

.user-chip:hover {
  background: #f0faf5;
}

.user-chip .nickname {
  font-size: 14px;
  color: #374151;
}

.user-chip .arrow {
  font-size: 12px;
  color: #9ca3af;
}

.main {
  padding: 0;
  background: #f4f7f6;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
