<template>
  <div class="admin-layout">
    <a href="#admin-content" class="skip-link">跳到工作区</a>
    <nav class="admin-sidebar" aria-label="管理后台导航">
      <router-link to="/admin/dashboard" class="brand-lockup">
        <span class="brand-mark" aria-hidden="true"><el-icon><Orange /></el-icon></span>
        <span><strong>青柠商城</strong><small>管理后台</small></span>
      </router-link>
      <el-scrollbar class="menu-scroll">
        <el-menu :key="activeMenu" :default-active="activeMenu" :default-openeds="activeParents" router class="side-menu">
          <sidebar-item v-for="menu in userStore.menus" :key="menu.id" :menu="menu" />
        </el-menu>
      </el-scrollbar>
      <router-link class="store-link" to="/shop"><el-icon><Shop /></el-icon> 访问商城 <el-icon><ArrowRight /></el-icon></router-link>
    </nav>
    <div class="admin-workspace">
      <header class="admin-bar">
        <div class="admin-context">
          <el-button class="menu-toggle" :icon="Menu" aria-label="打开后台导航" @click="mobileMenu = true" />
          <span>工作台</span><el-icon><ArrowRight /></el-icon><strong>{{ route.meta.title }}</strong>
        </div>
        <el-dropdown trigger="click" @command="handleCommand">
          <button type="button" class="user-chip" aria-label="管理员菜单">
            <el-avatar :size="30" :src="userStore.info?.avatar || '/img/avatar.svg'" />
            <span>{{ userStore.info?.nickname || '管理员' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="userStore.roles.length" disabled>{{ userStore.roles.join(' / ') }}</el-dropdown-item>
              <el-dropdown-item command="profile" divided>个人中心</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main id="admin-content" class="admin-main" tabindex="-1">
        <header class="admin-page-head"><h1>{{ route.meta.title }}</h1></header>
        <router-view />
      </main>
    </div>
    <el-drawer v-model="mobileMenu" title="管理后台导航" direction="ltr" size="min(300px, 90vw)" class="mobile-admin-menu">
      <el-menu :default-active="activeMenu" :default-openeds="activeParents" router @select="mobileMenu = false">
        <sidebar-item v-for="menu in userStore.menus" :key="menu.id" :menu="menu" />
      </el-menu>
      <router-link class="drawer-store-link" to="/shop">访问商城</router-link>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowRight, Menu, Orange, Shop } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mobileMenu = ref(false)
const activeMenu = computed(() =>
  route.path.startsWith('/admin/product/edit') ? '/admin/product/list' : route.path)
const activeParents = computed(() => findMenuParents(userStore.menus, activeMenu.value) || [])

watch(() => route.path, () => { mobileMenu.value = false })

function findMenuParents(menus, path) {
  for (const menu of menus) {
    if (menu.path === path) return []
    if (menu.children?.length) {
      const parents = findMenuParents(menu.children, path)
      if (parents) return [String(menu.id), ...parents]
    }
  }
  return null
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/admin/system/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '退出后台', { type: 'warning' })
      userStore.logout()
      router.push('/admin/login')
    } catch {
      // 管理员取消退出。
    }
  }
}
</script>

<style scoped>
.admin-layout { display: grid; grid-template-columns: 213px minmax(0, 1fr); min-height: 100dvh; align-items: start; }
.admin-sidebar { position: sticky; top: 0; height: 100dvh; min-height: 0; display: flex; flex-direction: column; background: var(--pine); color: var(--paper); }
.brand-lockup { margin: 17px 13px 14px; padding: 8px 5px; }
.brand-lockup strong { font-size: 19px; }
.brand-lockup small { color: #d4e4d4; font-size: 12px; }
.menu-scroll { flex: 1; min-height: 0; }
.side-menu { --el-menu-bg-color: transparent; --el-menu-hover-bg-color: #ffffff13; --el-menu-text-color: #e3ebe4; --el-menu-active-color: var(--pine); --el-menu-item-height: 44px; --el-menu-sub-item-height: 39px; padding: 0 9px 12px; border: 0; }
.side-menu :deep(.el-menu-item), .side-menu :deep(.el-sub-menu__title) { min-width: 0; border-radius: 7px; margin-bottom: 3px; padding-right: 8px; }
.side-menu :deep(.el-menu-item.is-active) { background: var(--lime); font-weight: 750; }
.side-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) { color: var(--lime); }
.side-menu :deep(.el-menu-item:focus-visible), .store-link:focus-visible { outline-color: var(--lime); }
.side-menu :deep(.el-menu-item.is-active:focus-visible) { outline-color: var(--pine); }
.store-link { display: flex; align-items: center; gap: 9px; min-height: 49px; border-top: 1px solid #ffffff30; padding: 9px 16px; color: #e3ebe4; text-decoration: none; }
.store-link .el-icon:last-child { margin-left: auto; }
.store-link:hover { background: #ffffff13; }
.admin-workspace { min-width: 0; }
.admin-bar { min-height: 60px; display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 6px var(--space-page); background: var(--paper); border-bottom: 1px solid var(--line); }
.admin-context, .user-chip { display: flex; align-items: center; gap: 8px; }
.admin-context { color: var(--muted); min-width: 0; font-size: 13px; }
.admin-context strong { color: var(--pine); overflow-wrap: anywhere; }
.user-chip { min-height: 43px; border: 0; background: transparent; border-radius: var(--radius-control); padding: 3px 7px; color: var(--pine); font: inherit; cursor: pointer; }
.user-chip:hover, .user-chip[aria-expanded='true'] { background: var(--soft-green); }
.admin-main { padding: 23px var(--space-page) 34px; min-width: 0; }
.admin-page-head { margin-bottom: 17px; }
.admin-page-head h1 { margin: 0; color: var(--pine); font-size: clamp(23px, 2.6vw, 36px); letter-spacing: -.04em; line-height: 1.25; }
.menu-toggle { display: none; }
.mobile-admin-menu .el-menu { border: 0; }
.drawer-store-link { display: inline-block; margin: 14px 19px; }
@media (max-width: 900px) {
  .admin-layout { display: block; }
  .admin-sidebar { display: none; }
  .menu-toggle { display: inline-flex; }
}
@media (max-width: 440px) {
  .admin-context > span, .admin-context > .el-icon, .user-chip > span:not(.el-avatar) { display: none; }
}
</style>
