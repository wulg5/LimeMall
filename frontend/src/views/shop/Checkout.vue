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
        <span class="subtotal">{{ quoteItems.has(item.skuId) ? money(quoteItems.get(item.skuId).itemAmount) : '—' }}</span>
      </div>
    </div>

    <!-- 优惠券 -->
    <div class="section-card">
      <h2>优惠券</h2>
      <div v-if="usableCoupons.length" class="coupon-list">
        <div class="coupon-option" :class="{ active: couponMemberId === null }"
             @click="selectCoupon(null)">
          不使用优惠券
        </div>
        <div v-for="coupon in usableCoupons" :key="coupon.id" class="coupon-option"
             :class="{ active: couponMemberId === coupon.id }" @click="selectCoupon(coupon.id)">
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
    <div v-if="!quote && !quoting" class="section-card">
      <el-button @click="refreshCheckout">{{ pendingConfirmation ? '查询下单结果' : '重新获取结算金额' }}</el-button>
    </div>
    <div v-if="quote" class="summary-bar" v-loading="quoting">
      <div class="amounts">
        <span>商品总额：{{ money(quote.totalAmount) }}</span>
        <span>运费：{{ money(quote.freightAmount) }}<i v-if="(shopStore.info?.level ?? 1) >= 3">（金卡会员免运费）</i><i v-else-if="quote.freightAmount > 0">（满 99 包邮）</i></span>
        <span>优惠：-{{ money(quote.couponAmount) }}</span>
      </div>
      <div class="pay-row">
        <span>应付金额：<b class="pay-amount">{{ money(quote.payAmount) }}</b></span>
        <el-button type="primary" size="large" class="submit-btn" :loading="submitting" :disabled="quoting" @click="handleSubmit">
          {{ pendingConfirmation ? '确认或重试下单' : '提交订单' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOrder, getCheckoutResult, getMyCoupons, getOrderQuote } from '@/api/portal'
import { useCartStore } from '@/stores/cart'
import { useShopStore } from '@/stores/shop'
import { money } from '@/utils/format'

const router = useRouter()
const cartStore = useCartStore()
const shopStore = useShopStore()

const formRef = ref()
const submitting = ref(false)
const items = cartStore.checkedItems.map(item => ({ ...item }))
const orderItems = items.map(({ skuId, quantity }) => ({ skuId, quantity }))
const cartSignature = orderItems.map(({ skuId, quantity }) => `${skuId}:${quantity}`).sort().join(',')
const retryKey = `lime_checkout:${shopStore.info?.id}:${cartSignature}`
let checkoutRequestId = readCheckoutRequestId()
const pendingConfirmation = ref(Boolean(checkoutRequestId))
const quote = ref(null)
const quoting = ref(true)
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

const quoteItems = computed(() => new Map(quote.value?.items.map(item => [item.skuId, item])))
const usableCoupons = computed(() => quote.value ? coupons.value.filter(coupon =>
  coupon.status === 0 && Number(quote.value.totalAmount) >= Number(coupon.minAmount)
) : [])

async function loadQuote(selectedId) {
  quoting.value = true
  try {
    quote.value = await getOrderQuote({ items: orderItems, couponMemberId: selectedId })
    couponMemberId.value = selectedId
    return true
  } catch {
    return false
  } finally {
    quoting.value = false
  }
}

async function selectCoupon(selectedId) {
  if (quoting.value || submitting.value || selectedId === couponMemberId.value) return
  if (!await loadQuote(selectedId)) {
    quote.value = null
    await Promise.all([loadQuote(null), loadCoupons()])
  }
}

async function loadCoupons() {
  try {
    coupons.value = await getMyCoupons()
  } catch {
    coupons.value = []
  }
}
function readCheckoutRequestId() {
  try {
    const storedRequestId = sessionStorage.getItem(retryKey)
    return /^[0-9a-f]{32}$/.test(storedRequestId || '') ? storedRequestId : null
  } catch {
    return null
  }
}

function rememberCheckoutRequest() {
  checkoutRequestId ||= Array.from(crypto.getRandomValues(new Uint8Array(16)),
    (byte) => byte.toString(16).padStart(2, '0')).join('')
  pendingConfirmation.value = true
  try {
    sessionStorage.setItem(retryKey, checkoutRequestId)
  } catch {
    // 当前页面仍保留请求号，可继续重试。
  }
}

async function showPlacedOrder(order) {
  checkoutRequestId = null
  pendingConfirmation.value = false
  try {
    sessionStorage.removeItem(retryKey)
  } catch {
    // 浏览器禁用存储时，请求号仅保存在当前页面。
  }
  cartStore.removeChecked()
  const requiresPayment = order.status === 0
  const orderNumber = `订单号 ${order.orderSn}`
  const message = requiresPayment
    ? `${orderNumber}，应付 ${money(order.payAmount)}，请尽快完成支付。`
    : order.status === 4
      ? `${orderNumber}，该订单已关闭。`
      : `${orderNumber}，请在订单列表查看当前状态。`
  await ElMessageBox.alert(message, '订单已生成', {
    type: 'success',
    confirmButtonText: requiresPayment ? '查看订单并支付' : '查看订单'
  }).catch(() => {})
  router.push('/shop/orders')
}

async function recoverOrder() {
  if (!pendingConfirmation.value) return false
  const order = await getCheckoutResult(checkoutRequestId)
  if (!order) {
    pendingConfirmation.value = false
    return false
  }
  await showPlacedOrder(order)
  return true
}

async function refreshCheckout() {
  quoting.value = true
  try {
    if (!await recoverOrder()) {
      await Promise.all([loadQuote(null), loadCoupons()])
    }
  } catch {
    // 保留请求号，恢复连接后可以继续查询。
  } finally {
    quoting.value = false
  }
}

onMounted(async () => {
  if (!items.length) {
    ElMessage.warning('请先在购物车勾选要结算的商品')
    router.replace('/shop/cart')
    return
  }
  await refreshCheckout()
})

async function handleSubmit() {
  if (!quote.value || quoting.value || submitting.value) return
  submitting.value = true
  try {
    if (await recoverOrder()) return
    await formRef.value.validate()
    rememberCheckoutRequest()
    const order = await createOrder({
      checkoutRequestId,
      items: orderItems.map(item => ({
        ...item,
        expectedUnitPrice: quoteItems.value.get(item.skuId).price
      })),
      expectedPayAmount: quote.value.payAmount,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      note: form.note || undefined,
      couponMemberId: couponMemberId.value || undefined
    })
    await showPlacedOrder(order)
  } catch {
    if (!pendingConfirmation.value) return
    try {
      if (!await recoverOrder()) {
        const selectedId = couponMemberId.value
        quote.value = null
        const [refreshed] = await Promise.all([loadQuote(selectedId), loadCoupons()])
        if (!refreshed && selectedId !== null) await loadQuote(null)
      }
    } catch {
      // 查询失败时继续使用同一个请求号，避免另建订单。
    }
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
