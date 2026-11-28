<template>
  <div class="page-container">
    <nav class="order-queues" aria-label="订单待办">
      <button type="button" class="queue queue-delivery" :aria-pressed="query.status === 1" @click="showShipping">
        <span class="queue-icon"><el-icon><Van /></el-icon></span>
        <span><strong>待发货</strong><small>查看已付款的订单</small></span>
        <el-icon class="queue-arrow"><ArrowRight /></el-icon>
      </button>
      <router-link v-if="canSeeReturns" class="queue queue-review" :to="{ path: '/admin/order/return', query: { status: 0 } }">
        <span class="queue-icon"><el-icon><DocumentChecked /></el-icon></span>
        <span><strong>待审核退款</strong><small>查看会员退款申请</small></span>
        <el-icon class="queue-arrow"><ArrowRight /></el-icon>
      </router-link>
      <router-link v-if="canSeeReturns" class="queue queue-stock" :to="{ path: '/admin/order/return', query: { status: 5 } }">
        <span class="queue-icon"><el-icon><Box /></el-icon></span>
        <span><strong>待确认退货入库</strong><small>查看待验收的退货</small></span>
        <el-icon class="queue-arrow"><ArrowRight /></el-icon>
      </router-link>
    </nav>

    <section class="page-card order-register" aria-labelledby="order-register-heading">
      <div class="table-toolbar"><h2 id="order-register-heading" class="title">订单记录</h2><span v-if="!listError" class="total-label">共 {{ total }} 笔订单</span></div>
      <el-form inline class="order-filters" label-position="top" @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input v-model="query.orderSn" placeholder="输入订单号" clearable @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable>
            <el-option v-for="(item, key) in ORDER_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单日期" class="date-filter">
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" :icon="Search" native-type="submit" :loading="loading">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div v-if="listError" class="order-error" role="alert">
        <span>订单加载失败，请重试。</span><el-button :loading="loading" @click="load">重新加载</el-button>
      </div>
      <el-table v-else v-loading="loading" :data="list" :empty-text="loading ? '正在加载订单…' : '没有符合条件的订单'" aria-label="订单记录">
        <el-table-column label="订单 / 下单时间" min-width="200">
          <template #default="{ row }">
            <div class="row-stack">
              <el-button link type="primary" class="order-number" @click="openDetail(row)">{{ row.orderSn }}</el-button>
              <time>{{ formatOrderTime(row.createTime) }}</time>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="195">
          <template #default="{ row }">
            <div class="cell-product">
              <product-image class="table-pic" :src="row.firstItemPic" alt="" />
              <div class="row-stack"><span class="item-name">{{ row.firstItemName }}</span><small>共 {{ row.itemCount }} 件</small></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="会员 / 收货人" min-width="132">
          <template #default="{ row }">
            <div class="row-stack"><span>{{ row.memberNickname || '—' }}</span><small>{{ row.receiverName }} {{ row.receiverPhone }}</small></div>
          </template>
        </el-table-column>
        <el-table-column label="实付金额" width="115" align="right">
          <template #default="{ row }"><span class="amount">{{ money(row.payAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态 / 售后" min-width="138">
          <template #default="{ row }">
            <div class="row-stack">
              <span class="status-chip" :class="'status-' + row.status">{{ ORDER_STATUS[row.status]?.text || '未知状态' }}</span>
              <span v-if="RETURN_STATUS[row.refundStatus]" class="refund-label" :class="'refund-' + row.refundStatus">
                {{ row.refundStatus === 0 ? '退款待审核' : RETURN_STATUS[row.refundStatus].text }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="158" fixed="right">
          <template #default="{ row }">
            <div class="order-actions">
              <el-button v-if="row.status === 1 && !isRefundActive(row.refundStatus) && userStore.hasPerm('oms:order:deliver')"
                         link type="primary" @click="openDeliver(row)">发货</el-button>
              <router-link v-else-if="canSeeReturns && isRefundActive(row.refundStatus)" class="return-link"
                           :to="{ path: '/admin/order/return', query: { status: row.refundStatus, orderSn: row.orderSn } }">
                {{ row.refundStatus === 0 ? '退款审核' : row.refundStatus === 5 ? '退货入库' : '查看退款' }}
              </router-link>
              <el-button v-else-if="row.status === 0 && userStore.hasPerm('oms:order:close')"
                         link type="danger" @click="handleClose(row)">关闭</el-button>
              <el-button link @click="openDetail(row)" :aria-label="'订单详情 ' + row.orderSn">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listError && total > 0" class="pagination-wrapper">
        <el-pagination background layout="prev, pager, next, sizes" :total="total"
                       v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
      </div>
    </section>


    <!-- 订单详情抽屉 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="600px">
      <div v-loading="detailLoading" class="detail-body">
        <div v-if="detailError" role="alert" class="order-error">订单详情加载失败。<el-button @click="openDetail(detailRow)">重试</el-button></div>
        <template v-if="detail">
        <el-steps :active="stepActive" simple finish-status="success" class="steps">
          <el-step title="待付款" />
          <el-step title="待发货" />
          <el-step title="已发货" />
          <el-step title="已完成" />
        </el-steps>

        <div class="detail-section">
          <div class="section-title">订单信息</div>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="订单号" :span="2">{{ detail.orderSn }}</el-descriptions-item>
            <el-descriptions-item label="会员">{{ detail.memberNickname }}（{{ detail.memberUsername }}）</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="ORDER_STATUS[detail.status]?.type">{{ ORDER_STATUS[detail.status]?.text }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="商品总额">{{ money(detail.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="优惠抵扣">-{{ money(detail.couponAmount) }}</el-descriptions-item>
            <el-descriptions-item label="运费">{{ money(detail.freightAmount) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额"><b class="amount">{{ money(detail.payAmount) }}</b></el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.deliveryTime" label="发货时间">{{ detail.deliveryTime }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.logisticsCompany" label="物流">
              {{ detail.logisticsCompany }} {{ detail.logisticsSn }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.receiveTime" label="收货时间">{{ detail.receiveTime }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.closeTime" label="关闭时间">{{ detail.closeTime }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ detail.receiverName }} {{ detail.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.note" label="买家留言" :span="2">{{ detail.note }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="section-title">商品明细</div>
          <el-table :data="detail.items" size="small">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="cell-product">
                  <product-image class="table-pic" :src="row.productPic" alt="" />
                  <span class="item-name">{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="规格" width="130">
              <template #default="{ row }">{{ specsToText(row.skuSpecs) }}</template>
            </el-table-column>
            <el-table-column label="单价" width="90">
              <template #default="{ row }">{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="70" />
            <el-table-column label="小计" width="100">
              <template #default="{ row }">{{ money(row.itemAmount) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="detail.returnApply" class="detail-section">
          <div class="section-title">退款申请</div>
          <el-alert :closable="false"
                    :type="RETURN_STATUS[detail.returnApply.status]?.type === 'danger' ? 'error' : RETURN_STATUS[detail.returnApply.status]?.type"
                    :title="`${RETURN_STATUS[detail.returnApply.status]?.text} · 退款金额 ${money(detail.returnApply.amount)}`">
            <p class="return-reason">原因：{{ detail.returnApply.reason }}</p>
            <p v-if="detail.returnApply.description" class="return-reason">说明：{{ detail.returnApply.description }}</p>
            <p v-if="detail.returnApply.handleNote" class="return-reason">审核备注：{{ detail.returnApply.handleNote }}</p>
          </el-alert>
        </div>

        <div v-if="detail.status === 1 && !isRefundActive(detail.returnApply?.status) && userStore.hasPerm('oms:order:deliver')" class="detail-footer">
          <el-button type="primary" @click="openDeliver(detail)">去发货</el-button>
        </div>
        </template>
      </div>
    </el-drawer>

    <!-- 发货弹窗 -->
    <el-dialog v-model="deliverVisible" title="订单发货" width="440px" destroy-on-close>
      <el-form scroll-to-error ref="deliverFormRef" :model="deliverForm" :rules="deliverRules" label-width="88px">
        <el-form-item label="物流公司" prop="logisticsCompany">
          <el-select v-model="deliverForm.logisticsCompany" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="京东物流" value="京东物流" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" prop="logisticsSn">
          <el-input v-model="deliverForm.logisticsSn" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverVisible = false">取消</el-button>
        <el-button type="primary" :loading="deliverSaving" @click="handleDeliver">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Box, DocumentChecked, Refresh, Search, Van } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { orderPage, orderDetail, orderDeliver, orderClose } from '@/api/order'
import { isRefundActive, money, ORDER_STATUS, RETURN_STATUS, specsToText } from '@/utils/format'

import dayjs from 'dayjs'
import ProductImage from '@/components/ProductImage.vue'

const route = useRoute()
const userStore = useUserStore()

const canSeeReturns = computed(() => userStore.hasPerm('oms:return:list'))
const loading = ref(true)
const listError = ref(false)
const detailError = ref(false)
const detailRow = ref(null)
let loadVersion = 0
let detailVersion = 0
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderSn: '',
  status: route.query.status ? Number(route.query.status) : null,
  dateRange: []
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const deliverVisible = ref(false)
const deliverSaving = ref(false)
const deliverFormRef = ref()
const deliverForm = reactive({ id: null, logisticsCompany: '', logisticsSn: '' })
const deliverRules = {
  logisticsCompany: [{ required: true, message: '请选择物流公司', trigger: 'change' }],
  logisticsSn: [{ required: true, message: '请输入物流单号', trigger: 'blur' }]
}

/** 状态步骤条：0待付款→1待发货→2已发货→3已完成；已关闭不在流程内 */
const stepActive = computed(() => {
  if (!detail.value) return 0
  return detail.value.status === 4 ? 0 : detail.value.status + 1
})

onMounted(load)

async function load() {
  const version = ++loadVersion
  loading.value = true
  listError.value = false
  try {
    const data = await orderPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      orderSn: query.orderSn || undefined,
      status: query.status ?? undefined,
      beginDate: query.dateRange?.[0] || undefined,
      endDate: query.dateRange?.[1] || undefined
    })
    if (version !== loadVersion) return
    list.value = data.records
    total.value = data.total
  } catch {
    if (version === loadVersion) listError.value = true
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function formatOrderTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '—'
}

function showShipping() {
  query.status = query.status === 1 ? null : 1
  handleSearch()
}

function handleSearch() {
  query.pageNum = 1
  load()
}

function handleReset() {
  query.orderSn = ''
  query.status = null
  query.dateRange = []
  query.pageNum = 1
  load()
}

async function openDetail(row) {
  const version = ++detailVersion
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = false
  detailRow.value = row
  detail.value = null
  try {
    const data = await orderDetail(row.id)
    if (version === detailVersion) detail.value = data
  } catch {
    if (version === detailVersion) detailError.value = true
  } finally {
    if (version === detailVersion) detailLoading.value = false
  }
}
function openDeliver(row) {
  deliverForm.id = row.id
  deliverForm.logisticsCompany = ''
  deliverForm.logisticsSn = ''
  deliverVisible.value = true
}

async function handleDeliver() {
  if (!await deliverFormRef.value.validate().catch(() => false)) return
  deliverSaving.value = true
  try {
    await orderDeliver(deliverForm.id, {
      logisticsCompany: deliverForm.logisticsCompany,
      logisticsSn: deliverForm.logisticsSn
    })
    ElMessage.success('发货成功')
    deliverVisible.value = false
    detailVisible.value = false
    load()
  } finally {
    deliverSaving.value = false
  }
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(
      `确定关闭订单「${row.orderSn}」吗？关闭后将回补库存并恢复优惠券。`,
      '关闭订单',
      { type: 'warning' }
    )
    await orderClose(row.id)
    ElMessage.success('订单已关闭')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>

<style scoped>
.order-queues { display: flex; gap: 12px; flex-wrap: wrap; }
.queue { flex: 1 1 225px; min-width: 0; display: grid; grid-template-columns: 42px minmax(0, 1fr) auto; align-items: center; gap: 11px; min-height: 82px; padding: 13px; font: inherit; text-align: left; text-decoration: none; color: var(--ink); background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-panel); cursor: pointer; }
.queue:hover, .queue[aria-pressed='true'] { border-color: var(--queue-ink); }
.queue[aria-pressed='true'] { background: var(--queue-bg); }
.queue-delivery { --queue-ink: #286247; --queue-bg: #e5f1e5; }
.queue-review { --queue-ink: #87500f; --queue-bg: #f8ecda; }
.queue-stock { --queue-ink: #395b77; --queue-bg: #e8eff5; }
.queue-icon { display: grid; place-items: center; width: 40px; height: 40px; color: var(--queue-ink); background: var(--queue-bg); border-radius: 8px; font-size: 20px; }
.queue strong { display: block; font-size: 15px; }
.queue small, .total-label { display: block; color: var(--muted); margin-top: 3px; font-size: 12px; }
.queue-arrow { color: var(--queue-ink); }
.table-toolbar .title { margin: 0; }
.order-filters { display: flex; flex-wrap: wrap; align-items: end; gap: 8px 13px; padding: 12px; margin-bottom: 15px; background: #f4f6f0; border-radius: 8px; }
.order-filters :deep(.el-form-item) { flex: 1 1 142px; margin: 0; min-width: 0; }
.order-filters .date-filter { flex: 1.7 1 240px; }
.order-filters :deep(.el-date-editor) { width: 100%; }
.order-filters .filter-actions { flex: 0 0 auto; }
.row-stack { display: grid; justify-items: start; gap: 3px; line-height: 1.4; }
.row-stack small, .row-stack time { color: var(--muted); font-size: 12px; font-variant-numeric: tabular-nums; }
.row-stack .order-number { padding: 0; min-height: 25px; font-weight: 700; }
.item-name { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; overflow-wrap: anywhere; }
.amount { font-weight: 750; color: var(--pine); white-space: nowrap; }
.refund-label { color: var(--muted); font-size: 12px; font-weight: 650; }
.refund-0 { color: #87500f; }
.refund-5 { color: #395b77; }
.order-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 5px 10px; }
.order-actions .el-button { margin: 0; }
.return-link { font-weight: 650; }
.order-error { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; padding: 16px; color: var(--price); background: #fbefea; border-radius: 8px; }
.detail-body { display: grid; gap: 18px; min-height: 65px; min-width: 0; }
.detail-section { min-width: 0; }
.section-title { font-weight: 700; margin-bottom: 8px; }
.detail-footer { display: flex; justify-content: flex-end; }
.return-reason { margin: 4px 0; }
@media (max-width: 600px) { .detail-body :deep(.el-steps) { display: none; } }

.status-chip { display: inline-flex; align-items: center; min-height: 22px; padding: 2px 6px; background: #edf1ee; color: #52665b; border-radius: 5px; white-space: nowrap; font-size: 12px; font-weight: 700; }
.status-1 { background: #f8ecda; color: #87500f; }
.status-2 { background: #e8eff5; color: #395b77; }
.status-3 { background: #e5f1e5; color: #286247; }
.status-4 { background: #f8ebe5; color: var(--price); }
</style>
