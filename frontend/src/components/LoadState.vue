<template>
  <div v-if="loading || error" class="load-state" :role="error ? 'alert' : 'status'" :aria-busy="loading">
    <template v-if="error">
      <span>{{ message }}</span>
      <el-button size="small" @click="$emit('retry')">重新加载</el-button>
    </template>
    <template v-else><el-icon class="is-loading" aria-hidden="true"><Loading /></el-icon> 正在加载…</template>
  </div>
</template>

<script setup>
import { Loading } from '@element-plus/icons-vue'

defineProps({
  loading: Boolean,
  error: Boolean,
  message: { type: String, default: '加载失败，请重试。' }
})
defineEmits(['retry'])
</script>

<style scoped>
.load-state { min-height: 66px; display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; padding: 14px; border: 1px solid var(--line); border-radius: var(--radius-control); background: var(--paper); color: var(--muted); }
.load-state[role='alert'] { justify-content: flex-start; background: #fbefea; color: var(--price); }
</style>
