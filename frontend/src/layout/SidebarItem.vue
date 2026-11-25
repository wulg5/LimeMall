<template>
  <el-sub-menu v-if="menu.children && menu.children.length" :index="String(menu.id)">
    <template #title>
      <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
      <span>{{ menu.name }}</span>
    </template>
    <sidebar-item v-for="child in menu.children" :key="child.id" :menu="child" />
  </el-sub-menu>

  <el-menu-item v-else :index="menu.path">
    <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
    <span>{{ menu.name }}</span>
  </el-menu-item>
</template>

<script setup>
/**
 * 侧边栏菜单递归渲染组件。
 * 菜单数据来自后端 RBAC（/admin/auth/info），目录含子菜单时渲染为可展开分组。
 */
defineProps({
  menu: { type: Object, required: true }
})
</script>
