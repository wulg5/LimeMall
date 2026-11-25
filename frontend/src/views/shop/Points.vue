<template>
  <div class="points-page">
    <!-- 页头 -->
    <div class="points-header">
      <div class="ph-copy">
        <h1>积分商城</h1>
        <p>购物得积分：100 积分 = 1 元 · 金卡 1.2 倍 · 钻石 1.5 倍</p>
        <p class="ph-note">积分消费不影响会员等级（等级由成长值决定，只升不降）</p>
      </div>
      <div v-if="shopStore.token" class="balance-card">
        <div class="b-label">我的积分</div>
        <div class="b-value">{{ shopStore.info?.points ?? 0 }}</div>
        <level-badge :level="shopStore.info?.level || 1" />
      </div>
      <el-button v-else type="primary"
                 @click="$router.push({ path: '/shop/login', query: { redirect: '/shop/points' } })">
        登录后兑换
      </el-button>
    </div>

    <!-- 兑换列表 -->
    <div class="coupon-grid">
      <div v-for="coupon in coupons" :key="coupon.id" class="ex-card">
        <div class="ex-top">
          <div class="ex-amount"><i>¥</i>{{ Number(coupon.amount) }}</div>
          <span class="ex-type">{{ coupon.type === 1 ? '满减券' : '折扣券' }}</span>
        </div>
        <div class="ex-name">{{ coupon.name }}</div>
        <div class="ex-cond">
          {{ Number(coupon.minAmount) > 0 ? `满 ${Number(coupon.minAmount)} 元可用` : '无门槛使用' }}
        </div>
        <div class="ex-expire">有效期至 {{ coupon.endTime }}</div>
        <div class="ex-footer">
          <div class="ex-points">{{ coupon.pointsPrice }}<span> 积分</span></div>
          <el-button type="warning" size="small"
                     :disabled="!shopStore.token || (shopStore.info?.points ?? 0) < coupon.pointsPrice"
                     :loading="exchangingId === coupon.id"
                     @click="handleExchange(coupon)">
            {{ !shopStore.token ? '请先登录'
              : (shopStore.info?.points ?? 0) < coupon.pointsPrice ? '积分不足' : '立即兑换' }}
          </el-button>
        </div>
      </div>
      <el-empty v-if="!coupons.length" description="暂无可兑换的优惠券" />
    </div>

    <div class="rule-card">
      <h3>兑换规则</h3>
      <ul>
        <li>积分通过购物获取：实付 1 元 = 1 积分（金卡 1.2 倍、钻石 1.5 倍，向下取整）。</li>
        <li>兑换比例固定为 100 积分 = 1 元面额，仅可兑换标注「积分兑」的优惠券。</li>
        <li>兑换成功后优惠券自动发放到「我的优惠券」，使用规则与普通领券一致。</li>
        <li>积分消费不影响会员等级；等级由成长值（消费金额累计）决定，只升不降。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exchangeCoupon, getExchangeCoupons } from '@/api/portal'
import { useShopStore } from '@/stores/shop'
import LevelBadge from '@/components/LevelBadge.vue'

const shopStore = useShopStore()

const coupons = ref([])
const exchangingId = ref(null)

onMounted(async () => {
  if (shopStore.token && !shopStore.info) {
    try {
      await shopStore.fetchMe()
    } catch (e) {
      /* 拦截器处理 */
    }
  }
  coupons.value = await getExchangeCoupons()
})

async function handleExchange(coupon) {
  try {
    await ElMessageBox.confirm(
      `将消耗 ${coupon.pointsPrice} 积分兑换「${coupon.name}」，兑换后发放至我的优惠券。确认兑换？`,
      '积分兑换',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  exchangingId.value = coupon.id
  try {
    await exchangeCoupon(coupon.id)
    ElMessage.success('兑换成功，优惠券已发放到「我的优惠券」')
    await shopStore.fetchMe()
    coupons.value = await getExchangeCoupons()
  } finally {
    exchangingId.value = null
  }
}
</script>

<style scoped>
.points-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.points-header {
  background: linear-gradient(115deg, #7c2d12, #b45309 60%, #d97706);
  border-radius: 16px;
  padding: 30px 34px;
  color: #ffffff;
  display: flex;
  align-items: center;
  gap: 24px;
}

.ph-copy {
  flex: 1;
}

.ph-copy h1 {
  margin: 0 0 10px;
  font-size: 26px;
  letter-spacing: 1px;
}

.ph-copy p {
  margin: 0 0 6px;
  font-size: 13.5px;
  opacity: 0.9;
}

.ph-note {
  font-size: 12px !important;
  opacity: 0.75 !important;
}

.balance-card {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  padding: 14px 24px;
  text-align: center;
}

.b-label {
  font-size: 12px;
  opacity: 0.85;
}

.b-value {
  font-size: 30px;
  font-weight: 700;
  margin: 4px 0;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.ex-card {
  background: #ffffff;
  border: 1px solid #f1e9dd;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: all 0.2s;
}

.ex-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(180, 83, 9, 0.14);
}

.ex-top {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.ex-amount {
  font-size: 32px;
  font-weight: 700;
  color: #dc2626;
}

.ex-amount i {
  font-size: 15px;
  font-style: normal;
}

.ex-type {
  font-size: 11px;
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
  padding: 2px 8px;
  border-radius: 999px;
}

.ex-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ex-cond {
  font-size: 12px;
  color: #6b7280;
}

.ex-expire {
  font-size: 11.5px;
  color: #b6c0bb;
}

.ex-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed #f1e9dd;
}

.ex-points {
  font-size: 17px;
  font-weight: 700;
  color: #d97706;
}

.ex-points span {
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
}

.rule-card {
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 14px;
  padding: 20px 24px;
}

.rule-card h3 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 15px;
}

.rule-card ul {
  margin: 0;
  padding-left: 18px;
}

.rule-card li {
  font-size: 13px;
  color: #6b7280;
  line-height: 2;
}

@media (max-width: 1000px) {
  .coupon-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
