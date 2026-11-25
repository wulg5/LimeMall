<template>
  <div class="checkout-page">
    <h1 class="page-title">确认订单</h1>

    <!-- 收货信息 -->
    <div class="section-card">
      <h2>收货信息</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 560px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input v-model="form.receiverAddress" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
        </el-form-item>
        <el-form-item label="买家留言">
          <el-input v-model="form.note" placeholder="选填" maxlength="200" />
        </el-form-item>
      </el-form>
    </div>

    <!-- 商品清单 -->
    <div class="section-card">
      <h2>商品清单</h2>
      <div v-for="item in items" :key="item.skuId" class="goods-row">
        <img :src="item.pic || '/img/logo.svg'" alt="" />
        <div class="goods-info">
          <div class="name">{{ item.name }}</div>
          <div class="specs">{{ item.specs }}</div>
        </div>
        <span class="qty">× {{ item.quantity }}</span>
        <span class="subtotal">{{ money(item.price * item.quantity) }}</span>
      </div>
    </div>

    <!-- 优惠券 -->
    <div class="section-card">
      <h2>优惠券</h2>
      <div v-if="usableCoupons.length" class="coupon-list">
        <div class="coupon-option" :class="{ active: couponMemberId === null }"
             @click="couponMemberId = null">
          不使用优惠券
        </div>
        <div v-for="coupon in usableCoupons" :key="coupon.id" class="coupon-option"
             :class="{ active: couponMemberId === coupon.id }" @click="couponMemberId = coupon.id">
          <span>{{ coupon.name }}</span>
          <b>{{ coupon.type === 1 ? `减 ${Number(coupon.amount)} 元` : `${Number(coupon.amount)} 折` }}</b>
        </div>
      </div>
      <p v-else class="no-coupon">
        暂无可用优惠券
        <router-link to="/shop/coupons">去领券中心 ></router-link>
      </p>
    </div>

    <!-- 金额汇总 -->
    <div class="summary-bar">
      <div class="amounts">
        <span>商品总额：{{ money(totalAmount) }}</span>
        <span>运费：{{ money(freightAmount) }}<i v-if="(shopStore.info?.level ?? 1) >= 3">（金卡会员免运费）</i><i v-else-if="freightAmount > 0">（满 99 包邮）</i></span>
        <span>优惠：-{{ money(couponAmount) }}</span>
      </div>
      <div class="pay-row">
        <span>应付金额：<b class="pay-amount">{{ money(payAmount) }}</b></span>
        <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOrder, getMyCoupons } from '@/api/portal'
import { useCartStore } from '@/stores/cart'
import { useShopStore } from '@/stores/shop'
import { money } from '@/utils/format'

const router = useRouter()
const cartStore = useCartStore()
const shopStore = useShopStore()

const formRef = ref()
const submitting = ref(false)
const items = ref([...cartStore.checkedItems])
const coupons = ref([])
const couponMemberId = ref(null)

const form = reactive({
  receiverName: '',
  receiverPhone: shopStore.info?.phone || '',
  receiverAddress: '',
  note: ''
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

const totalAmount = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)

const usableCoupons = computed(() =>
  coupons.value.filter((coupon) => coupon.status === 0 && totalAmount.value >= Number(coupon.minAmount))
)

const selectedCoupon = computed(() =>
  coupons.value.find((coupon) => coupon.id === couponMemberId.value) || null
)

const couponAmount = computed(() => {
  if (!selectedCoupon.value) return 0
  const coupon = selectedCoupon.value
  const discount = coupon.type === 1
    ? Number(coupon.amount)
    : totalAmount.value * (10 - Number(coupon.amount)) / 10
  return Math.min(discount, totalAmount.value)
})

const freightAmount = computed(() => {
  // 金卡及以上会员全场免运费；其余满 99 包邮
  if ((shopStore.info?.level ?? 1) >= 3) {
    return 0
  }
  return totalAmount.value < 99 ? 10 : 0
})

const payAmount = computed(() =>
  Math.max(0, totalAmount.value - couponAmount.value + freightAmount.value)
)

onMounted(async () => {
  if (!items.value.length) {
    ElMessage.warning('请先在购物车勾选要结算的商品')
    router.replace('/shop/cart')
    return
  }
  try {
    coupons.value = await getMyCoupons()
  } catch (e) {
    coupons.value = []
  }
})

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const order = await createOrder({
      items: items.value.map((item) => ({ skuId: item.skuId, quantity: item.quantity })),
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      note: form.note || undefined,
      couponMemberId: couponMemberId.value || undefined
    })
    cartStore.removeChecked()
    await ElMessageBox.alert(
      `订单提交成功！订单号 ${order.orderSn}，应付 ${money(order.payAmount)}，请尽快完成支付。`,
      '下单成功',
      { type: 'success', confirmButtonText: '去支付' }
    )
    router.push('/shop/orders')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin: 0 0 18px;
  color: #111827;
}

.section-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.section-card h2 {
  font-size: 16px;
  margin: 0 0 16px;
  color: #111827;
}

.section-card h2::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 15px;
  background: var(--el-color-primary);
  border-radius: 2px;
  margin-right: 8px;
  vertical-align: -2px;
}

.goods-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f8faf9;
}

.goods-row:last-child {
  border-bottom: none;
}

.goods-row img {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-info .name {
  font-size: 14px;
  color: #111827;
}

.goods-info .specs {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.qty {
  color: #6b7280;
}

.subtotal {
  color: #dc2626;
  font-weight: 600;
  width: 100px;
  text-align: right;
}

.coupon-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.coupon-option {
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 16px;
  cursor: pointer;
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 13px;
  color: #4b5563;
  transition: all 0.2s;
}

.coupon-option b {
  color: #dc2626;
}

.coupon-option.active {
  border-color: var(--el-color-primary);
  background: #e7f8f2;
}

.no-coupon {
  color: #9ca3af;
  font-size: 13px;
  margin: 0;
}

.no-coupon a {
  color: var(--el-color-primary);
  margin-left: 8px;
}

.summary-bar {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  bottom: 16px;
  box-shadow: 0 6px 20px rgba(15, 46, 36, 0.08);
}

.amounts {
  display: flex;
  gap: 24px;
  color: #6b7280;
  font-size: 14px;
}

.amounts i {
  color: #9ca3af;
  font-style: normal;
  font-size: 12px;
}

.pay-row {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #4b5563;
}

.pay-amount {
  color: #dc2626;
  font-size: 24px;
}

.submit-btn {
  min-width: 140px;
  border-radius: 12px;
}
</style>
