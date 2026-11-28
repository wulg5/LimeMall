<template>
  <div class="coupons-page">
    <h1 class="page-title">领券中心</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="可领取优惠券" name="available" />
      <el-tab-pane label="我的优惠券" name="mine" />
    </el-tabs>

    <!-- 领券中心 -->
    <template v-if="activeTab === 'available'">
      <load-state v-if="loading || couponError" :loading="loading" :error="couponError" @retry="loadCoupons('available')" />
      <div v-else-if="available.length" class="coupon-grid">
        <div v-for="coupon in available" :key="coupon.id" class="coupon-card" :class="{ disabled: coupon.received || coupon.remaining <= 0 }">
          <div class="coupon-value">
            <template v-if="coupon.type === 1">
              <span class="symbol">¥</span><span class="num">{{ Number(coupon.amount) }}</span>
            </template>
            <template v-else>
              <span class="num">{{ Number(coupon.amount) }}</span><span class="symbol">折</span>
            </template>
            <div class="threshold">{{ Number(coupon.minAmount) > 0 ? `满 ${Number(coupon.minAmount)} 元可用` : '无门槛' }}</div>
          </div>
          <div class="coupon-info">
            <div class="coupon-name">{{ coupon.name }}</div>
            <div class="coupon-time">有效期至 {{ coupon.endTime }}</div>
            <div class="coupon-remain">剩余 {{ coupon.remaining }} 张</div>
          </div>
          <el-button class="receive-btn" :disabled="coupon.received || coupon.remaining <= 0"
                     @click="handleReceive(coupon)">
            {{ coupon.received ? '已领取' : coupon.remaining <= 0 ? '已领完' : '立即领取' }}
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂无可领取的优惠券" />
    </template>

    <!-- 我的优惠券 -->
    <template v-else>
      <div v-if="!shopStore.token" class="login-hint">
        <el-empty description="登录后查看我的优惠券">
          <el-button type="primary" @click="$router.push({ path: '/shop/login', query: { redirect: '/shop/coupons?tab=mine' } })">
            去登录
          </el-button>
        </el-empty>
      </div>
      <template v-else>
        <load-state v-if="loading || couponError" :loading="loading" :error="couponError" @retry="loadCoupons('mine')" />
        <div v-else-if="mine.length" class="coupon-grid">
          <div v-for="coupon in mine" :key="coupon.id" class="coupon-card" :class="{ disabled: coupon.status !== 0 }">
            <div class="coupon-value">
              <template v-if="coupon.type === 1">
                <span class="symbol">¥</span><span class="num">{{ Number(coupon.amount) }}</span>
              </template>
              <template v-else>
                <span class="num">{{ Number(coupon.amount) }}</span><span class="symbol">折</span>
              </template>
              <div class="threshold">{{ Number(coupon.minAmount) > 0 ? `满 ${Number(coupon.minAmount)} 元可用` : '无门槛' }}</div>
            </div>
            <div class="coupon-info">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-time">有效期至 {{ coupon.endTime }}</div>
            </div>
            <el-tag :type="COUPON_MEMBER_STATUS[coupon.status]?.type" class="status-tag">
              {{ COUPON_MEMBER_STATUS[coupon.status]?.text }}
            </el-tag>
          </div>
        </div>
        <el-empty v-else description="还没有领取过优惠券">
          <el-button type="primary" @click="activeTab = 'available'">去领券</el-button>
        </el-empty>
      </template>
    </template>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAvailableCoupons, getMyCoupons, receiveCoupon } from '@/api/portal'
import { useShopStore } from '@/stores/shop'
import { COUPON_MEMBER_STATUS } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()

const activeTab = ref(route.query.tab === 'mine' ? 'mine' : 'available')
const available = ref([])
const mine = ref([])
const loading = ref(false)
const couponError = ref(false)
let version = 0

onMounted(() => {
  if (shopStore.token) loadCoupons(activeTab.value)
})

async function loadCoupons(tab) {
  const request = ++version
  loading.value = true
  couponError.value = false
  try {
    const getCoupons = tab === 'mine' ? getMyCoupons : getAvailableCoupons
    const coupons = await getCoupons()
    if (request !== version) return
    const target = tab === 'mine' ? mine : available
    target.value = coupons
  } catch {
    if (request === version) couponError.value = true
  } finally {
    if (request === version) loading.value = false
  }
}

async function handleTabChange(tab) {
  if (!shopStore.token) {
    router.push({ path: '/shop/login', query: { redirect: '/shop/coupons?tab=' + tab } })
    return
  }
  await loadCoupons(tab)
}

async function handleReceive(coupon) {
  if (!shopStore.token) {
    router.push({ path: '/shop/login', query: { redirect: '/shop/coupons' } })
    return
  }
  await receiveCoupon(coupon.id)
  ElMessage.success('领取成功，可在「我的优惠券」中查看')
  await loadCoupons('available')
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin: 0 0 6px;
  color: #111827;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.coupon-card {
  background: #ffffff;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
  position: relative;
  overflow: hidden;
}

.coupon-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background: linear-gradient(180deg, #34d399, #059669);
}

.coupon-card.disabled {
  opacity: 0.6;
}

.coupon-value {
  text-align: center;
  flex-shrink: 0;
  width: 110px;
  color: #dc2626;
}

.coupon-value .symbol {
  font-size: 15px;
  font-weight: 600;
}

.coupon-value .num {
  font-size: 38px;
  font-weight: 700;
}

.coupon-value .threshold {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.coupon-info {
  flex: 1;
  min-width: 0;
}

.coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-time {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}

.coupon-remain {
  font-size: 12px;
  color: var(--el-color-primary);
  margin-top: 4px;
}

.receive-btn {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  border-radius: 10px;
  flex-shrink: 0;
}

.status-tag {
  flex-shrink: 0;
}

.login-hint {
  background: #ffffff;
  border-radius: 16px;
  padding: 32px;
}

@media (max-width: 900px) {
  .coupon-grid {
    grid-template-columns: 1fr;
  }
}

.coupon-grid { grid-template-columns: repeat(auto-fit, minmax(min(100%, 315px), 1fr)); }
.coupon-card { border: 1px solid var(--line); box-shadow: none; }
.coupon-card::before { background: var(--action); }
.coupon-value { color: var(--price); }
.coupon-info { overflow-wrap: anywhere; }
@media (max-width: 390px) { .coupon-card { gap: 8px; } .coupon-value { width: 79px; } }
</style>
