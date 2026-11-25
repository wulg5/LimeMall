<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在或已被移除</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #f4f7f6;
}

h1 {
  font-size: 88px;
  margin: 0;
  color: var(--el-color-primary);
}

p {
  color: #6b7280;
  margin: 0 0 12px;
}
</style>
