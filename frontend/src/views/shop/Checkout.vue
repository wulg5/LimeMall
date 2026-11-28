<template>
  <div class="checkout-page">
    <h1 class="page-title">确认订单</h1>

    <!-- 收货信息 -->
    <div class="section-card">
      <h2>收货信息</h2>
      <el-form scroll-to-error ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 560px">
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
        <product-image :src="item.pic" alt="" />
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
        <button type="button" class="coupon-option" :aria-pressed="couponMemberId === null" :class="{ active: couponMemberId === null }"
             @click="selectCoupon(null)">
          不使用优惠券
        </button>
        <button type="button" v-for="coupon in usableCoupons" :key="coupon.id" class="coupon-option"
             :class="{ active: couponMemberId === coupon.id }" :aria-pressed="couponMemberId === coupon.id" @click="selectCoupon(coupon.id)">
          <span>{{ coupon.name }}</span>
          <b>{{ coupon.type === 1 ? `减 ${Number(coupon.amount)} 元` : `${Number(coupon.amount)} 折` }}</b>
        </button>
      </div>
      <p v-else class="no-coupon">
        暂无可用优惠券
        <router-link to="/shop/coupons">去领券中心 ></router-link>
      </p>
    </div>

    <!-- 金额汇总 -->
    <load-state v-if="quoting && !quote" :loading="quoting" />
    <div v-else-if="!quote" class="section-card" role="status">
      <p>{{ pendingConfirmation ? '上次提交的结果尚待确认。' : '暂时无法显示结算金额。' }}</p>
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
import LoadState from '@/components/LoadState.vue'
import ProductImage from '@/components/ProductImage.vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOrder, getCheckoutResult, getMyCoupons, getOrderQuote } from '@/api/portal'
import { legacyCartLineId, newCartRevision, useCartStore } from '@/stores/cart'
import { useShopStore } from '@/stores/shop'
import { money } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const shopStore = useShopStore()

const formRef = ref()
const submitting = ref(false)
const checkoutPath = route.fullPath
let mounted = true
onBeforeUnmount(() => { mounted = false })

cartStore.syncFromStorage()
const buyingNow = route.query.buyNow === '1'
const buyNowItem = buyingNow ? cartStore.buyNowItem : null
const selectedItems = buyingNow ? (buyNowItem ? [buyNowItem] : []) : cartStore.checkedItems
const checkoutRevision = buyingNow ? buyNowItem?.lineId : cartStore.revision
const items = selectedItems.map(item => ({ ...item }))
const orderItems = items.map(({ skuId, quantity }) => ({ skuId, quantity }))
const checkoutLines = items.map(({ skuId, lineId }) => ({ skuId, lineId }))
const legacyKeyPrefix = `lime_checkout:${shopStore.info?.id}:${buyingNow ? 'buy-now:' : ''}`
const retryKey = `lime_pending_checkout:${shopStore.info?.id}:${buyingNow ? 'buy-now' : 'cart'}`
const cartRetryKey = `lime_shared_checkout:${shopStore.info?.id}`
const buyNowRetryKey = `lime_shared_buy_now:${shopStore.info?.id}`
const sharedRetryKey = buyingNow ? buyNowRetryKey : cartRetryKey
const alternateRetryKey = buyingNow ? cartRetryKey : buyNowRetryKey
let localAttempt = null
const legacyRequests = new Map()
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
    for (let index = 0; index < sessionStorage.length; index++) {
      const key = sessionStorage.key(index)
      if (!key?.startsWith(legacyKeyPrefix)) continue
      if (!/^\d+:\d+(,\d+:\d+)*$/.test(key.slice(legacyKeyPrefix.length))) continue
      const legacyId = sessionStorage.getItem(key)
      if (!/^[0-9a-f]{32}$/.test(legacyId || '')) continue
      legacyRequests.set(legacyId, key)
    }
    return /^[0-9a-f]{32}$/.test(storedRequestId || '')
      ? storedRequestId : legacyRequests.keys().next().value ?? null
  } catch {
    return null
  }
}

function readSharedCheckout(key = sharedRetryKey) {
  try {
    const attempt = JSON.parse(localStorage.getItem(key) || 'null')
    return /^[0-9a-f]{32}$/.test(attempt?.id || '') ? attempt : null
  } catch {
    return null
  }
}

function writeSharedCheckout(key, attempt) {
  try {
    localStorage.setItem(key, JSON.stringify(attempt))
  } catch {
    // 当前标签页仍使用自己的结算请求号。
  }
}

function samePurchase(left, right) {
  return left.length === right.length
    && left.every(item => right.some(other => other.skuId === item.skuId && other.quantity === item.quantity))
}

function alternateCheckout() {
  const attempt = readSharedCheckout(alternateRetryKey)
  if (!attempt || attempt.completed || !orderItems.length) return null
  if (!attempt.items) return attempt
  const pendingItems = [attempt, ...(attempt.retries || [])].flatMap(entry => entry.items)
  const overlaps = orderItems.some(item => pendingItems.some(other => other.skuId === item.skuId))
  return overlaps ? attempt : null
}

function checkoutOrigin(requestId) {
  for (const key of [sharedRetryKey, alternateRetryKey]) {
    const attempt = readSharedCheckout(key)
    if (attempt?.id === requestId) return { key, attempt }
  }
  return { key: sharedRetryKey, attempt: localAttempt?.id === requestId ? localAttempt : null }
}

function syncSharedCheckout() {
  const shared = readSharedCheckout()
  if (shared && (!shared.completed || shared.revision === checkoutRevision || shared.id === checkoutRequestId)) {
    checkoutRequestId = shared.id
    pendingConfirmation.value = true
  }
}

function rememberCheckoutRequest() {
  checkoutRequestId ||= (/^[0-9a-f]{32}$/.test(checkoutRevision || '') ? checkoutRevision : newCartRevision())
  const shared = readSharedCheckout()
  const selection = { items: orderItems, lines: checkoutLines }
  if (shared?.id === checkoutRequestId && shared.lines) {
    const retries = shared.retries || []
    localAttempt = [shared, ...retries].some(entry => samePurchase(entry.items, orderItems))
      ? shared : { ...shared, retries: [...retries, selection] }
  } else {
    localAttempt = { id: checkoutRequestId, revision: checkoutRevision, completed: false, ...selection }
  }
  writeSharedCheckout(sharedRetryKey, localAttempt)
  pendingConfirmation.value = true
  try {
    sessionStorage.setItem(retryKey, checkoutRequestId)
    const legacyRetryKey = legacyRequests.get(checkoutRequestId)
    if (legacyRetryKey) sessionStorage.removeItem(legacyRetryKey)
  } catch {
    // 当前页面仍保留请求号，可继续重试。
  }
}

async function showPlacedOrder(order, recovered = false) {
  const requestId = checkoutRequestId
  if (!requestId) return
  checkoutRequestId = null
  pendingConfirmation.value = false
  const { key, attempt } = checkoutOrigin(requestId)
  const purchased = attempt?.items && [attempt, ...(attempt.retries || [])].find(entry => samePurchase(entry.items, order.items))
  const lines = purchased?.lines ?? order.items.map(({ skuId }) => ({ skuId, lineId: legacyCartLineId(skuId) }))
  if (key === buyNowRetryKey) {
    const item = cartStore.buyNowItem
    if (item && lines.some(line => line.skuId === item.skuId && line.lineId === item.lineId)) {
      cartStore.clearBuyNow(item)
    }
  } else {
    await cartStore.removePurchased(requestId, order.items, lines)
  }
  const shared = readSharedCheckout(key)
  if (shared?.id === requestId && !shared.completed) {
    writeSharedCheckout(key, { ...shared, completed: true })
  }
  try {
    if (sessionStorage.getItem(retryKey) === requestId) sessionStorage.removeItem(retryKey)
    const legacyRetryKey = legacyRequests.get(requestId)
    if (legacyRetryKey && sessionStorage.getItem(legacyRetryKey) === requestId) {
      sessionStorage.removeItem(legacyRetryKey)
    }
  } catch {
    // 浏览器禁用存储时，请求号仅保存在当前页面。
  }
  if (!mounted || route.fullPath !== checkoutPath) return
  const requiresPayment = order.status === 0
  const orderNumber = `订单号 ${order.orderSn}`
  const message = requiresPayment
    ? `${orderNumber}，应付 ${money(order.payAmount)}，请尽快完成支付。`
    : order.status === 4
      ? `${orderNumber}，该订单已关闭。`
      : `${orderNumber}，请在订单列表查看当前状态。`
  ElMessageBox.alert(message, recovered ? '已找回原订单' : '订单已生成', {
    type: 'success',
    confirmButtonText: requiresPayment ? '查看订单并支付' : '查看订单'
  }).catch(() => {}).then(() => {
    if (mounted && route.fullPath === checkoutPath) router.push('/shop/orders')
  })
}

async function recoverOrder() {
  const alternate = alternateCheckout()
  if (!pendingConfirmation.value && !alternate) return false
  const candidates = new Set([checkoutRequestId, ...legacyRequests.keys(), alternate?.id])
  for (const requestId of candidates) {
    if (!requestId) continue
    const order = await getCheckoutResult(requestId)
    if (!order) continue
    checkoutRequestId = requestId
    await showPlacedOrder(order, true)
    return true
  }
  pendingConfirmation.value = false
  return false
}

function discardRejectedCheckout() {
  const rejectedId = checkoutRequestId
  checkoutRequestId = null
  localAttempt = null
  try {
    const shared = readSharedCheckout()
    if (shared?.id === rejectedId && !shared.completed) localStorage.removeItem(sharedRetryKey)
    if (sessionStorage.getItem(retryKey) === rejectedId) sessionStorage.removeItem(retryKey)
  } catch {
    // 浏览器禁用存储时，当前页仍可下一次提交。
  }
}

async function refreshCheckout() {
  quoting.value = true
  try {
    syncSharedCheckout()
    if (!await recoverOrder() && items.length) {
      await Promise.all([loadQuote(null), loadCoupons()])
    }
  } catch {
    // 保留请求号，恢复连接后可以继续查询。
  } finally {
    quoting.value = false
  }
}

onMounted(async () => {
  syncSharedCheckout()
  if (!items.length && !pendingConfirmation.value) {
    ElMessage.warning(buyingNow ? '请从商品详情重新发起立即购买' : '请先在购物车勾选要结算的商品')
    router.replace('/shop/cart')
    return
  }
  await refreshCheckout()
  if (!items.length && !pendingConfirmation.value && checkoutRequestId) router.replace('/shop/cart')
})

async function handleSubmit() {
  if (!quote.value || quoting.value || submitting.value) return
  submitting.value = true
  try {
    const locks = globalThis.navigator?.locks
    if (locks) {
      await locks.request(cartRetryKey, submitCheckout)
    } else {
      await submitCheckout()
    }
  } finally {
    submitting.value = false
  }
}

async function submitCheckout() {
  let posted = false
  try {
    if (!mounted || route.fullPath !== checkoutPath || !shopStore.token) return
    syncSharedCheckout()
    if (await recoverOrder()) return
    if (alternateCheckout()) {
      ElMessage.warning('同一商品的上次下单尚未确认，请回原结算入口重试')
      return
    }
    if (!buyingNow) {
      cartStore.syncFromStorage()
      if (cartStore.revision !== checkoutRevision) {
        ElMessage.warning('购物车已更新，请重新结算')
        router.replace('/shop/cart')
        return
      }
    }
    await formRef.value.validate()
    rememberCheckoutRequest()
    posted = true
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
  } catch (error) {
    if (!pendingConfirmation.value) return
    try {
      if (!await recoverOrder()) {
        if (posted && error.businessRejected) discardRejectedCheckout()
        const selectedId = couponMemberId.value
        quote.value = null
        const [refreshed] = await Promise.all([loadQuote(selectedId), loadCoupons()])
        if (!refreshed && selectedId !== null) await loadQuote(null)
      }
    } catch {
      // 查询失败时继续使用同一个请求号，避免另建订单。
    }
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

.section-card h2::before { display: none; }
.section-card h2 { padding-bottom: 9px; border-bottom: 1px solid var(--line); color: var(--pine); }
.coupon-option { font: inherit; text-align: left; color: var(--ink); }
.goods-row, .amounts, .pay-row { min-width: 0; }
.amounts, .pay-row { flex-wrap: wrap; row-gap: 8px; }
@media (max-width: 620px) {
  .section-card, .summary-bar { padding: 12px; }
  .section-card :deep(.el-form-item__label) { width: min(76px, 26vw) !important; padding-right: 8px; }
  .goods-row { display: grid; grid-template-columns: 48px minmax(0, 1fr) auto; gap: 8px; }
  .goods-row img { width: 48px; height: 48px; }
  .goods-row .subtotal { grid-column: 2 / -1; width: auto; justify-self: end; }
  .pay-row .submit-btn { width: 100%; }
}
</style>
