<template>
  <div class="page-container">
    <!-- 搜索区 -->
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="query.orderSn" placeholder="输入订单号" clearable style="width: 200px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="(item, key) in ORDER_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单日期">
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表区 -->
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">订单列表</span>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column label="订单号" width="190">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ row.orderSn }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="memberNickname" label="会员" width="110" />
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="cell-product">
              <img class="table-pic" :src="row.firstItemPic || '/img/logo.svg'" alt="" />
              <span class="item-name">{{ row.firstItemName }}</span>
              <el-tag size="small" type="info">共 {{ row.itemCount }} 件</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="实付金额" width="120">
          <template #default="{ row }">
            <span class="amount">{{ money(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.status]?.type">{{ ORDER_STATUS[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="150">
          <template #default="{ row }">{{ row.receiverName }} {{ row.receiverPhone }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1 && userStore.hasPerm('oms:order:deliver')"
                       link type="success" @click="openDeliver(row)">发货</el-button>
            <el-button v-if="(row.status === 0 || row.status === 1) && userStore.hasPerm('oms:order:close')"
                       link type="danger" @click="handleClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
                       v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                       @current-change="load" @size-change="load" />
      </div>
    </div>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="600px">
      <div v-if="detail" v-loading="detailLoading" class="detail-body">
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
                  <img class="table-pic" :src="row.productPic || '/img/logo.svg'" alt="" />
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
                    :type="detail.returnApply.status === 0 ? 'warning' : detail.returnApply.status === 1 ? 'success' : 'error'"
                    :title="`${RETURN_STATUS[detail.returnApply.status]?.text} · 退款金额 ${money(detail.returnApply.amount)}`">
            <p class="return-reason">原因：{{ detail.returnApply.reason }}</p>
            <p v-if="detail.returnApply.description" class="return-reason">说明：{{ detail.returnApply.description }}</p>
            <p v-if="detail.returnApply.handleNote" class="return-reason">审核备注：{{ detail.returnApply.handleNote }}</p>
          </el-alert>
        </div>

        <div v-if="detail.status === 1 && userStore.hasPerm('oms:order:deliver')" class="detail-footer">
          <el-button type="primary" @click="openDeliver(detail)">去发货</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 发货弹窗 -->
    <el-dialog v-model="deliverVisible" title="订单发货" width="440px" destroy-on-close>
      <el-form ref="deliverFormRef" :model="deliverForm" :rules="deliverRules" label-width="88px">
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
import { Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { orderPage, orderDetail, orderDeliver, orderClose } from '@/api/order'
import { money, ORDER_STATUS, RETURN_STATUS, specsToText } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
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
  loading.value = true
  try {
    const data = await orderPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      orderSn: query.orderSn || undefined,
      status: query.status ?? undefined,
      beginDate: query.dateRange?.[0] || undefined,
      endDate: query.dateRange?.[1] || undefined
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
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
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await orderDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

function openDeliver(row) {
  deliverForm.id = row.id
  deliverForm.logisticsCompany = ''
  deliverForm.logisticsSn = ''
  deliverVisible.value = true
}

async function handleDeliver() {
  await deliverFormRef.value.validate()
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
.item-name {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount {
  color: #dc2626;
  font-weight: 600;
}

.detail-body {
  padding: 0 4px 20px;
}

.steps {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 22px;
}

.section-title {
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 14px;
  background: var(--el-color-primary);
  border-radius: 2px;
  margin-right: 8px;
  vertical-align: -1px;
}

.return-reason {
  margin: 4px 0 0;
  font-size: 13px;
}

.detail-footer {
  text-align: right;
}
</style>
