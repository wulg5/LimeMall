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

    <div v-if="list.length" class="order-list">
      <div v-for="order in list" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-sn">{{ order.orderSn }}</span>
          <span class="order-time">{{ order.createTime }}</span>
          <el-tag :type="ORDER_STATUS[order.status]?.type" class="order-status">
            {{ ORDER_STATUS[order.status]?.text }}
          </el-tag>
        </div>

        <div class="order-items">
          <div class="order-item">
            <img :src="order.firstItemPic || '/img/logo.svg'" alt="" />
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
              <el-button size="small" type="primary" @click="handlePay(order)">去支付</el-button>
              <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
            </template>
            <template v-if="(order.status === 1 || order.status === 2) && !isRefundActive(order.refundStatus)">
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
    <el-empty v-else description="暂无相关订单" />

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="评价商品" width="560px" destroy-on-close>
      <div class="review-list">
        <div v-for="item in reviewItems" :key="item.productId" class="review-item">
          <div class="review-goods">
            <img :src="item.pic || '/img/logo.svg'" alt="" />
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
import { onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelOrder, getMyOrderDetail, getMyOrders, payOrder, receiveOrder, refundOrder, submitComments
} from '@/api/portal'
import { isRefundActive, money, ORDER_STATUS, RETURN_STATUS } from '@/utils/format'

/** 与后端 limemall.order.pay-timeout-minutes 保持一致（默认 30 分钟） */
const PAY_TIMEOUT_MINUTES = 30

const activeTab = ref('all')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(5)

const nowTs = ref(Date.now())
let tickTimer = null
let expireRefreshScheduled = false

onMounted(() => {
  load()
  tickTimer = setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer)
})

function tick() {
  nowTs.value = Date.now()
  // 出现超时的待付款订单：等后端定时任务关单后自动刷新一次列表
  if (!expireRefreshScheduled) {
    const expired = list.value.some((order) => order.status === 0
      && dayjs(order.createTime).add(PAY_TIMEOUT_MINUTES, 'minute').valueOf() <= nowTs.value)
    if (expired) {
      expireRefreshScheduled = true
      setTimeout(() => load(), 65_000)
    }
  }
}

/** 待付款订单剩余支付时间文案 */
function remainText(order) {
  const deadline = dayjs(order.createTime).add(PAY_TIMEOUT_MINUTES, 'minute')
  const remain = deadline.valueOf() - nowTs.value
  if (remain <= 0) {
    return '已超时，系统即将自动关闭订单'
  }
  const minutes = Math.floor(remain / 60000)
  const seconds = Math.floor((remain % 60000) / 1000)
  return `剩余支付时间 ${minutes}:${String(seconds).padStart(2, '0')}，超时自动取消`
}

async function load() {
  const data = await getMyOrders({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    status: activeTab.value === 'all' ? undefined : Number(activeTab.value)
  })
  list.value = data.records
  total.value = data.total
}

function handleTabChange() {
  pageNum.value = 1
  load()
}

function openDetail(order) {
  ElMessageBox.alert(
    `<div style="line-height:1.9">
      订单号：${order.orderSn}<br/>
      下单时间：${order.createTime}<br/>
      收货人：${order.receiverName} ${order.receiverPhone}
    </div>`,
    '订单信息',
    { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
  )
}

async function handlePay(order) {
  try {
    await ElMessageBox.confirm(`确认支付 ${money(order.payAmount)} 吗？`, '订单支付', { type: 'info' })
  } catch (e) {
    return // 用户取消
  }
  // 失败（如"支付功能尚未配置"）由全局拦截器提示；成功则跳转支付宝收银台
  try {
    const result = await payOrder(order.id)
    if (result && result.payUrl) {
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
    const { value } = await ElMessageBox.prompt('请填写退款原因（如：商品与描述不符 / 尺码不合适）', '申请退款', {
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
  reviewOrder.value = order
  reviewItems.value = (detail.items || []).map((item) => ({
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
</style>
