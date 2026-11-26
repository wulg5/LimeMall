<template>
  <div v-if="info" class="member-page">
    <!-- 会员卡片 -->
    <div class="member-card">
      <div class="member-top">
        <el-avatar :size="72" :src="info.avatar || '/img/avatar.svg'" />
        <div class="member-base">
          <h2>
            {{ info.nickname }}
            <level-badge :level="info.level" class="big-badge" />
          </h2>
          <p>账号 {{ info.username }}<span v-if="info.userNo" class="user-no"> · 编号 {{ info.userNo }}</span></p>
        </div>
        <div class="stat-chips">
          <div class="stat-chip">
            <div class="chip-label">成长值</div>
            <div class="chip-value">{{ info.growth ?? 0 }}</div>
          </div>
          <div class="stat-chip">
            <div class="chip-label">积分余额</div>
            <div class="chip-value">{{ info.points ?? 0 }}</div>
          </div>
        </div>
        <div class="member-actions">
          <el-button @click="$router.push('/shop/points')">积分商城</el-button>
          <el-button @click="$router.push('/shop/orders')">我的订单</el-button>
          <el-button type="primary" @click="$router.push('/shop/coupons?tab=mine')">我的优惠券</el-button>
        </div>
      </div>

      <div v-if="info.expiringPoints > 0" class="expire-tip">
        <span class="dot"></span>
        有 {{ info.expiringPoints }} 积分将于 {{ info.expiringDate || '近期' }} 过期，快去积分商城兑换吧
        <el-button link type="warning" @click="$router.push('/shop/points')">去兑换 &gt;</el-button>
      </div>
      <div class="progress-block">
        <div class="progress-label">
          <template v-if="info.nextLevelGrowth">
            距离 {{ nextLevelText }} 还需 <b>{{ info.nextLevelGrowth - (info.growth ?? 0) }}</b> 成长值（消费 1 元 = 1 成长值，只增不减）
          </template>
          <template v-else>已是最高等级，尊享钻石会员全部权益</template>
        </div>
        <el-progress :percentage="progressPercent" :show-text="false" :stroke-width="10" color="#10b981" />
      </div>
    </div>

    <!-- 等级权益 -->
    <div class="benefit-block">
      <h3>等级权益</h3>
      <p class="benefit-tip">
        成长值由消费累计（1 元 = 1 成长值），决定等级且只升不降；
        积分按等级倍率获取（金卡 1.2 倍 / 钻石 1.5 倍），可在积分商城以 100 积分 = 1 元兑换优惠券。
      </p>
      <div class="benefit-grid">
        <div v-for="lv in LEVELS" :key="lv.level" class="benefit-card"
             :class="{ current: info.level === lv.level }">
          <div class="lv-head">
            <span class="lv-name">{{ lv.text }}</span>
            <level-badge v-if="lv.level > 1" :level="lv.level" />
          </div>
          <div class="lv-points">{{ lv.growth === 0 ? '注册即享' : `${lv.growth} 成长值起` }}</div>
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
import LevelBadge from '@/components/LevelBadge.vue'

const shopStore = useShopStore()

const LEVELS = [
  { level: 1, growth: 0, text: '普通会员', perks: ['购物累计成长值', '积分按 1 倍获取', '参与领券中心活动'] },
  { level: 2, growth: 1000, text: '银卡会员', perks: ['积分按 1 倍获取', '专属客服优先响应', '会员日活动提醒'] },
  { level: 3, growth: 5000, text: '金卡会员', perks: ['积分按 1.2 倍获取', '全场免运费', '专属客服优先响应'] },
  { level: 4, growth: 15000, text: '钻石会员', perks: ['积分按 1.5 倍获取', '全场免运费', '新品优先购买', '专属客服一对一'] }
]

const LEVEL_BASE = { 1: 0, 2: 1000, 3: 5000, 4: 15000 }

const info = computed(() => shopStore.info)

const nextLevelText = computed(() => {
  if (!info.value?.nextLevelGrowth) return ''
  const names = { 1000: '银卡会员', 5000: '金卡会员', 15000: '钻石会员' }
  return names[info.value.nextLevelGrowth] || ''
})

const progressPercent = computed(() => {
  if (!info.value) return 0
  const growth = info.value.growth ?? 0
  if (!info.value.nextLevelGrowth) return 100
  const base = LEVEL_BASE[info.value.level] ?? 0
  const span = info.value.nextLevelGrowth - base
  const done = growth - base
  return Math.max(0, Math.min(100, Math.round((done / span) * 100)))
})

onMounted(async () => {
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
  background: linear-gradient(130deg, #064e3b, #10b981);
  border-radius: 16px;
  padding: 28px 30px;
  color: #ffffff;
}

.member-top {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
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
  opacity: 0.85;
}

.stat-chips {
  display: flex;
  gap: 12px;
  margin-left: 18px;
}

.stat-chip {
  background: rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  padding: 10px 18px;
  text-align: center;
  min-width: 92px;
}

.chip-label {
  font-size: 12px;
  opacity: 0.85;
}

.chip-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 4px;
}

.member-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.expire-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  margin-bottom: 4px;
  padding: 10px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  font-size: 12.5px;
  color: #b45309;
}

.expire-tip .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  flex-shrink: 0;
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
  border-radius: 16px;
  padding: 24px 26px;
  border: 1px solid #eef2f0;
}

.benefit-block h3 {
  margin: 0 0 6px;
  color: #111827;
}

.benefit-tip {
  color: #9ca3af;
  font-size: 13px;
  margin: 0 0 18px;
  line-height: 1.8;
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

.lv-head {
  display: flex;
  align-items: center;
  gap: 8px;
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

.big-badge :deep(span) {
  font-size: 12px;
  height: 22px;
  padding: 0 9px;
}

@media (max-width: 900px) {
  .benefit-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
