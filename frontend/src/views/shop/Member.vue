<template>
  <div v-if="info" class="member-page">
    <!-- 会员卡片 -->
    <div class="member-card">
      <div class="member-top">
        <el-avatar :size="72" :src="info.avatar || '/img/avatar.svg'" />
        <div class="member-base">
          <h2>
            {{ info.nickname }}
            <el-tag :type="levelInfo.tag" effect="light">{{ levelInfo.text }}</el-tag>
          </h2>
          <p>账号 {{ info.username }} · 当前积分 <b>{{ info.points }}</b></p>
        </div>
        <div class="member-actions">
          <el-button @click="$router.push('/shop/orders')">我的订单</el-button>
          <el-button @click="$router.push('/shop/coupons?tab=mine')">我的优惠券</el-button>
          <el-button type="primary" @click="$router.push('/shop')">继续购物</el-button>
        </div>
      </div>

      <div class="progress-block">
        <div class="progress-label">
          <template v-if="info.nextLevelPoints">
            距离 {{ nextLevelText }} 还需 <b>{{ info.nextLevelPoints - info.points }}</b> 积分
          </template>
          <template v-else>已是最高等级，尊享钻石会员全部权益</template>
        </div>
        <el-progress :percentage="progressPercent" :show-text="false" :stroke-width="10" color="#10b981" />
      </div>
    </div>

    <!-- 等级权益 -->
    <div class="benefit-block">
      <h3>等级权益</h3>
      <p class="benefit-tip">消费 1 元累积 1 积分（金卡 2 倍、钻石 3 倍），积分达标自动升级</p>
      <div class="benefit-grid">
        <div v-for="lv in LEVELS" :key="lv.level" class="benefit-card"
             :class="{ current: info.level === lv.level }">
          <div class="lv-name">{{ lv.text }}</div>
          <div class="lv-points">{{ lv.points === 0 ? '注册即享' : `${lv.points} 积分起` }}</div>
          <ul>
            <li v-for="perk in lv.perks" :key="perk">{{ perk }}</li>
          </ul>
          <el-tag v-if="info.level === lv.level" type="success" size="small">当前等级</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useShopStore } from '@/stores/shop'
import { MEMBER_LEVEL } from '@/utils/format'

const shopStore = useShopStore()

const LEVELS = [
  { level: 1, points: 0, text: '普通会员', perks: ['注册即享购物权益', '参与领券中心活动', '7 天无理由退款'] },
  { level: 2, points: 500, text: '银卡会员', perks: ['专属客服优先响应', '生日礼券一张', '积分正常累积'], tag: 'info' },
  { level: 3, points: 2000, text: '金卡会员', perks: ['消费积分 2 倍累积', '专属优惠活动', '专属客服优先响应'] },
  { level: 4, points: 5000, text: '钻石会员', perks: ['消费积分 3 倍累积', '新品优先购买', '专属客服一对一服务'] }
]

const LEVEL_BASE = { 1: 0, 2: 500, 3: 2000, 4: 5000 }

const info = computed(() => shopStore.info)

const levelInfo = computed(() =>
  MEMBER_LEVEL[info.value?.level] || MEMBER_LEVEL[1]
)

const nextLevelText = computed(() => {
  if (!info.value?.nextLevelPoints) return ''
  const nextLevel = { 500: '银卡会员', 2000: '金卡会员', 5000: '钻石会员' }
  return nextLevel[info.value.nextLevelPoints] || ''
})

const progressPercent = computed(() => {
  if (!info.value) return 0
  if (!info.value.nextLevelPoints) return 100
  const base = LEVEL_BASE[info.value.level] ?? 0
  const span = info.value.nextLevelPoints - base
  const done = info.value.points - base
  return Math.max(0, Math.min(100, Math.round((done / span) * 100)))
})

onMounted(async () => {
  // 重新拉取，保证积分/等级为最新
  try {
    await shopStore.fetchMe()
  } catch (e) {
    /* 拦截器处理 */
  }
})
</script>

<style scoped>
.member-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.member-card {
  background: linear-gradient(130deg, #065f46, #10b981);
  border-radius: 18px;
  padding: 28px 30px;
  color: #ffffff;
}

.member-top {
  display: flex;
  align-items: center;
  gap: 18px;
}

.member-base h2 {
  margin: 0 0 8px;
  font-size: 22px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-base p {
  margin: 0;
  font-size: 13px;
  opacity: 0.9;
}

.member-base b {
  font-size: 18px;
}

.member-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.progress-block {
  margin-top: 22px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  padding: 14px 16px;
}

.progress-label {
  font-size: 13px;
  margin-bottom: 10px;
  opacity: 0.95;
}

.progress-block :deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.25);
}

.benefit-block {
  background: #ffffff;
  border-radius: 18px;
  padding: 24px 26px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.benefit-block h3 {
  margin: 0 0 6px;
  color: #111827;
}

.benefit-tip {
  color: #9ca3af;
  font-size: 13px;
  margin: 0 0 18px;
}

.benefit-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.benefit-card {
  border: 1.5px solid #eef2f0;
  border-radius: 14px;
  padding: 18px;
  position: relative;
  transition: all 0.2s;
}

.benefit-card.current {
  border-color: var(--el-color-primary);
  background: #f2fbf7;
}

.lv-name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.lv-points {
  font-size: 12px;
  color: #9ca3af;
  margin: 6px 0 12px;
}

.benefit-card ul {
  padding-left: 16px;
  margin: 0 0 10px;
}

.benefit-card li {
  font-size: 12.5px;
  color: #4b5563;
  line-height: 2;
}

@media (max-width: 900px) {
  .benefit-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
