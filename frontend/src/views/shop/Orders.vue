<template>
  <div class="orders-page">
    <h1 class="page-title">我的订单</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="已发货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已关闭" name="4" />
    </el-tabs>

    <div v-if="list.length && !listError" class="order-list">
      <div v-for="order in list" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-sn">{{ order.orderSn }}</span>
          <span class="order-time">{{ order.createTime }}</span>
          <span class="order-status" :class="'order-status-' + order.status">{{ ORDER_STATUS[order.status]?.text || '未知状态' }}</span>
        </div>

        <div class="order-items">
          <div class="order-item">
            <product-image :src="order.firstItemPic" alt="" />
            <div class="item-name">{{ order.firstItemName }}</div>
            <span class="item-count">共 {{ order.itemCount }} 件</span>
          </div>
        </div>

        <div class="order-footer">
          <div class="amount-info">
            实付金额：<b>{{ money(order.payAmount) }}</b>
            <span v-if="order.status === 0" class="pay-tip">{{ remainText(order) }}</span>
          </div>
          <div class="order-actions">
            <el-tag v-if="order.refundStatus != null" :type="RETURN_STATUS[order.refundStatus]?.type">
              {{ RETURN_STATUS[order.refundStatus]?.text }}
            </el-tag>
            <el-button size="small" @click="openDetail(order)">订单详情</el-button>
            <template v-if="order.status === 0">
              <el-button size="small" type="primary" @click="handlePay(order)">{{ Number(order.payAmount) > 0 ? '去支付' : '确认免支付订单' }}</el-button>
              <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
            </template>
            <template v-if="canApplyRefund(order)">
              <el-button size="small" type="warning" plain @click="openRefund(order)">申请退款</el-button>
            </template>
            <template v-if="order.status === 2 && !isRefundActive(order.refundStatus)">
              <el-button size="small" type="primary" @click="handleReceive(order)">确认收货</el-button>
            </template>
            <template v-if="order.status === 3 && !order.commented">
              <el-button size="small" type="success" plain @click="openReview(order)">评价商品</el-button>
            </template>
            <span v-else-if="order.status === 3 && order.commented" class="commented-tip">已评价</span>
          </div>
        </div>
      </div>

      <div class="pager">
        <el-pagination background layout="prev, pager, next" :total="total"
                       v-model:current-page="pageNum" :page-size="pageSize" @current-change="load" />
      </div>
    </div>
    <load-state v-else-if="loadingOrders || listError" :loading="loadingOrders" :error="listError" @retry="load" />
    <el-empty v-else description="暂无相关订单" />

    <el-dialog v-model="detailVisible" title="订单详情" width="min(680px, 92vw)">
      <el-skeleton v-if="detailLoading" :rows="5" animated />
      <template v-else-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ detail.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ ORDER_STATUS[detail.status]?.text }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.receiverName }} {{ detail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ detail.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.logisticsCompany" label="物流公司">{{ detail.logisticsCompany }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.logisticsSn" label="运单号">{{ detail.logisticsSn }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.deliveryTime" label="发货时间">{{ detail.deliveryTime }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">{{ money(detail.payAmount) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" size="small" class="detail-items">
          <el-table-column prop="productName" label="商品" min-width="125" />
          <el-table-column label="规格" min-width="100">
            <template #default="{ row }">{{ specsToText(row.skuSpecs) || '—' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="65" />
          <el-table-column label="小计" width="92">
            <template #default="{ row }">{{ money(row.itemAmount) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="评价商品" width="560px" destroy-on-close>
      <div class="review-list">
        <div v-for="item in reviewItems" :key="item.productId" class="review-item">
          <div class="review-goods">
            <product-image :src="item.pic" alt="" />
            <span class="review-name">{{ item.productName }}</span>
            <el-rate v-model="item.rating" />
          </div>
          <el-input v-model="item.content" type="textarea" :rows="2" maxlength="500" show-word-limit
                    placeholder="说说这件商品怎么样吧（必填）" />
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSaving" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import ProductImage from '@/components/ProductImage.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelOrder, getMyOrderDetail, getMyOrders, payOrder, receiveOrder, refundOrder, submitComments
} from '@/api/portal'
import { isRefundActive, money, ORDER_STATUS, RETURN_STATUS, specsToText } from '@/utils/format'


const activeTab = ref('all')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(5)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
let latestDetailRequest = 0

const nowTs = ref(Date.now())
let tickTimer = null
let lastPendingRefresh = Date.now()
const loadingOrders = ref(true)
const listError = ref(false)
let latestListRequest = 0

onMounted(() => {
  load()
  tickTimer = setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer)
})

function tick() {
  nowTs.value = Date.now()
  if (document.hidden || loadingOrders.value || nowTs.value - lastPendingRefresh < 5_000) return
  if (list.value.some(order => order.status === 0)) {
    lastPendingRefresh = nowTs.value
    load().catch(() => {})
  }
}

/** 待付款订单剩余支付时间文案 */
function remainText(order) {
  if (!order.paymentDeadline) return ''
  const remain = dayjs(order.paymentDeadline).valueOf() - nowTs.value
  if (remain <= 0) {
    return '已超时，系统即将自动关闭订单'
  }
  const minutes = Math.floor(remain / 60000)
  const seconds = Math.floor((remain % 60000) / 1000)
  return `剩余支付时间 ${minutes}:${String(seconds).padStart(2, '0')}，超时自动取消`
}

function canApplyRefund(order) {
  if (isRefundActive(order.refundStatus)) return false
  if (order.status === 1 || order.status === 2) return true
  return order.status === 3 && order.returnDeadline
    && dayjs(order.returnDeadline).valueOf() > nowTs.value
}

async function load() {
  const request = ++latestListRequest
  loadingOrders.value = true
  listError.value = false
  try {
    const data = await getMyOrders({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeTab.value === 'all' ? undefined : Number(activeTab.value)
    })
    if (request !== latestListRequest) return
    list.value = data.records
    total.value = data.total
  } catch {
    if (request === latestListRequest) listError.value = true
  } finally {
    if (request === latestListRequest) loadingOrders.value = false
  }
}

function handleTabChange() {
  pageNum.value = 1
  load()
}

async function openDetail(order) {
  const request = ++latestDetailRequest
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getMyOrderDetail(order.id)
    if (request === latestDetailRequest) detail.value = result
  } catch {
    if (request === latestDetailRequest) detailVisible.value = false
  } finally {
    if (request === latestDetailRequest) detailLoading.value = false
  }
}

async function handlePay(order) {
  try {
    const prompt = Number(order.payAmount) === 0
      ? '本单无需支付，确认后进入待发货。'
      : `确认支付 ${money(order.payAmount)} 吗？`
    await ElMessageBox.confirm(prompt, '订单支付', { type: 'info' })
  } catch (e) {
    return // 用户取消
  }
  // 失败（如"支付功能尚未配置"）由全局拦截器提示；成功则跳转支付宝收银台
  try {
    const result = await payOrder(order.id)
    if (result?.paid) {
      ElMessage.success('订单已确认，请等待发货')
      await load()
    } else if (result?.payUrl) {
      ElMessage.success('正在跳转支付宝收银台...')
      window.location.href = result.payUrl
    }
  } catch (e) {
    /* 错误提示已由全局拦截器给出 */
  }
}

async function handleCancel(order) {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？库存与优惠券将自动回补。', '取消订单', { type: 'warning' })
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    /* 取消 */
  }
}

async function handleReceive(order) {
  try {
    await ElMessageBox.confirm('请确认已收到商品，确认收货后订单完成。', '确认收货', { type: 'info' })
    await receiveOrder(order.id)
    ElMessage.success('确认收货成功')
    load()
  } catch (e) {
    /* 取消 */
  }
}

async function openRefund(order) {
  let reason = ''
  try {
    const prompt = order.status === 3
      ? '请填写退款原因。审核通过后，商家确认退货入库再提交退款。'
      : '请填写退款原因（如：商品与描述不符 / 尺码不合适）'
    const { value } = await ElMessageBox.prompt(prompt, '申请退款', {
      inputPlaceholder: '请输入退款原因',
      inputValidator: (val) => (val && val.trim().length >= 2 ? true : '退款原因至少 2 个字'),
      confirmButtonText: '提交申请',
      cancelButtonText: '取消'
    })
    reason = value
  } catch (e) {
    return
  }
  await refundOrder(order.id, { reason })
  ElMessage.success('退款申请已提交，等待商家审核')
  load()
}

/* ---------------- 评价 ---------------- */
const reviewVisible = ref(false)
const reviewSaving = ref(false)
const reviewOrder = ref(null)
const reviewItems = ref([])

async function openReview(order) {
  const detail = await getMyOrderDetail(order.id)
  const reviewed = new Set(detail.reviewedProductIds || [])
  const products = new Map((detail.items || []).map(item => [item.productId, item]))
  const remaining = [...products.values()].filter(item => !reviewed.has(item.productId))
  if (!remaining.length) {
    await load()
    return
  }
  reviewOrder.value = order
  reviewItems.value = remaining.map((item) => ({
    productId: item.productId,
    productName: item.productName,
    pic: item.productPic,
    rating: 5,
    content: ''
  }))
  reviewVisible.value = true
}

async function submitReview() {
  if (reviewItems.value.some((item) => !item.content.trim())) {
    ElMessage.warning('请填写每件商品的评价内容')
    return
  }
  reviewSaving.value = true
  try {
    await submitComments({
      orderId: reviewOrder.value.id,
      comments: reviewItems.value.map((item) => ({
        productId: item.productId,
        rating: item.rating,
        content: item.content.trim()
      }))
    })
    ElMessage.success('评价成功，感谢您的反馈')
    reviewVisible.value = false
    load()
  } finally {
    reviewSaving.value = false
  }
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin: 0 0 6px;
  color: #111827;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.order-header {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 20px;
  background: #f8faf9;
  border-bottom: 1px solid #f0f3f1;
  font-size: 13px;
}

.order-sn {
  font-weight: 600;
  color: #111827;
}

.order-time {
  color: #9ca3af;
}

.order-status {
  margin-left: auto;
}

.order-items {
  padding: 16px 20px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.order-item img {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
}

.item-name {
  flex: 1;
  color: #374151;
}

.item-count {
  color: #9ca3af;
  font-size: 13px;
}

.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-top: 1px solid #f8faf9;
}

.amount-info {
  color: #6b7280;
  font-size: 14px;
}

.amount-info b {
  color: #dc2626;
  font-size: 17px;
}

.pay-tip {
  margin-left: 14px;
  color: #d97706;
  font-size: 12.5px;
  background: #fffbeb;
  padding: 3px 10px;
  border-radius: 8px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.commented-tip {
  font-size: 12px;
  color: #9ca3af;
  padding: 0 4px;
}

.detail-items {
  margin-top: 16px;
}

.review-item {
  border: 1px solid #f1f5f3;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
}

.review-goods {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.review-goods img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
}

.review-name {
  flex: 1;
  font-size: 13px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-header, .order-footer, .order-actions { flex-wrap: wrap; }
.order-actions .el-button { margin-left: 0; }
.order-item { min-width: 0; }
.item-name { overflow-wrap: anywhere; }
@media (max-width: 620px) {
  .order-card { padding: 12px; }
  .order-header, .order-footer { padding-inline: 0; gap: 8px; }
  .order-footer { align-items: flex-start; }
  .order-actions { justify-content: flex-start; width: 100%; gap: 6px; }
}

.order-status { align-self: start; border-radius: 5px; padding: 3px 7px; color: var(--muted); background: #edf1ee; font-size: 12px; font-weight: 700; white-space: nowrap; }
.order-status-1 { background: #f8ecda; color: #87500f; }
.order-status-2 { background: #e8eff5; color: #395b77; }
.order-status-3 { background: #e5f1e5; color: #286247; }
.order-status-4 { background: #f8ebe5; color: var(--price); }
</style>
