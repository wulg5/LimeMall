<template>
  <span v-if="level >= 2" class="level-badge" :class="['lv-' + level, 'size-' + size]">
    <i class="mark"></i>{{ text }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 会员等级徽章（银卡 / 金卡 / 钻石）。
 * 普通会员不展示徽章，保持界面克制。
 */
const props = defineProps({
  level: { type: Number, default: 1 },
  /** 尺寸：sm 小号 / md 中号 */
  size: { type: String, default: 'sm' }
})

const text = computed(() => ({ 2: '银卡', 3: '金卡', 4: '钻石' }[props.level] || ''))
</script>

<style scoped>
.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 18px;
  padding: 0 7px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
  vertical-align: 1px;
  white-space: nowrap;
}

.mark {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.85;
}

.lv-2 {
  background: #e8eff5;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.lv-3 {
  background: #faecd4;
  color: #78350f;
  border: 1px solid #f59e0b;
}

.lv-4 {
  background: #354f66;
  color: #ffffff;
  border: 1px solid #354f66;
}
.size-md { height: 22px; padding-inline: 8px; }
</style>
