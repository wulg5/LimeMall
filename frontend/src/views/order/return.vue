<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input v-model="query.orderSn" placeholder="输入订单号" clearable style="width: 200px"
                    @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="(item, key) in RETURN_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" native-type="submit" :loading="loading">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="table-toolbar">
        <h2 class="title">退款申请</h2>
      </div>

      <load-state v-if="listError" :error="listError" @retry="load" />
      <el-table v-else v-loading="loading" :empty-text="loading ? '正在加载…' : '暂无符合条件的记录'" :data="list">
        <el-table-column prop="orderSn" label="订单号" width="190" />
        <el-table-column prop="memberNickname" label="会员" width="120" />
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">
            <span class="amount">{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="问题描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="售后状态" width="136">
          <template #default="{ row }">
            <el-tag :type="row.status === 5 ? 'primary' : RETURN_STATUS[row.status]?.type">{{ RETURN_STATUS[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 && userStore.hasPerm('oms:return:handle')"
                       link type="primary" @click="openHandle(row)">审核退款</el-button>
            <el-button v-else-if="row.status === 5 && userStore.hasPerm('oms:return:handle')"
                       link type="primary" :loading="stockConfirmingId === row.id"
                       @click="handleReturnedStock(row)">确认退货入库</el-button>
            <span v-else class="handled-text">
              {{ row.handleNote || (row.status === 3 ? '等待退款确认' : (row.status === 5 ? '等待退货入库' : '已处理')) }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listError" class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
                       v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                       @current-change="load" @size-change="load" />
      </div>
    </div>

    <!-- 审核弹窗 -->
    <el-dialog v-model="handleVisible" title="退款审核" width="480px" destroy-on-close>
      <el-descriptions v-if="current" :column="1" size="small" border class="handle-info">
        <el-descriptions-item label="订单号">{{ current.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ money(current.amount) }}</el-descriptions-item>
        <el-descriptions-item label="退款原因">{{ current.reason }}</el-descriptions-item>
        <el-descriptions-item v-if="current.description" label="问题描述">{{ current.description }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="88px">
        <el-form-item label="审核备注">
          <el-input v-model="handleNote" type="textarea" :rows="3" placeholder="选填，将展示给会员" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitHandle(false)">拒 绝</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle(true)">通过并退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { confirmReturnedStock, returnPage, returnHandle } from '@/api/order'
import { money, RETURN_STATUS } from '@/utils/format'

const userStore = useUserStore()
const route = useRoute()

const loading = ref(true)
const listError = ref(false)
let loadVersion = 0
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10, orderSn: '', status: null })

const handleVisible = ref(false)
const saving = ref(false)
const current = ref(null)
const handleNote = ref('')
const stockConfirmingId = ref(null)

function applyRouteFilters() {
  query.orderSn = typeof route.query.orderSn === 'string' ? route.query.orderSn : ''
  query.status = typeof route.query.status === 'string' && RETURN_STATUS[route.query.status]
    ? Number(route.query.status)
    : null
  query.pageNum = 1
  load()
}

onMounted(applyRouteFilters)
watch(() => [route.query.status, route.query.orderSn], () => {
  if (route.path === '/admin/order/return') applyRouteFilters()
})

async function load() {
  const version = ++loadVersion
  loading.value = true
  listError.value = false
  try {
    const data = await returnPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      orderSn: query.orderSn || undefined,
      status: query.status ?? undefined
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

function handleSearch() {
  query.pageNum = 1
  load()
}

function handleReset() {
  query.orderSn = ''
  query.status = null
  query.pageNum = 1
  load()
}

function openHandle(row) {
  current.value = row
  handleNote.value = ''
  handleVisible.value = true
}

async function handleReturnedStock(row) {
  try {
    await ElMessageBox.confirm('请确认退回商品已验收入库。确认后将提交退款。', '确认退货入库', {
      type: 'warning'
    })
  } catch {
    return
  }
  stockConfirmingId.value = row.id
  try {
    await confirmReturnedStock(row.id)
    ElMessage.success('已确认退货入库，退款处理中')
    await load()
  } finally {
    stockConfirmingId.value = null
  }
}

async function submitHandle(approve) {
  try {
    await ElMessageBox.confirm(
      approve
        ? '已收货订单须先确认退货入库，退款完成后将关闭订单并回补库存。确定通过申请吗？'
        : '确定拒绝该退款申请吗？',
      '审核确认',
      { type: approve ? 'warning' : 'info' }
    )
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await returnHandle(current.value.id, { approve, handleNote: handleNote.value || undefined })
    ElMessage.success(approve ? '审核通过，请查看退款状态' : '已拒绝该退款申请')
    handleVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.amount {
  color: #dc2626;
  font-weight: 600;
}

.handled-text {
  color: #9ca3af;
  font-size: 12px;
}

.handle-info {
  margin-bottom: 16px;
}

.table-toolbar h2 { margin: 0; }
.el-table :deep(.el-tag) { white-space: nowrap; }
</style>
