<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="query.orderSn" placeholder="输入订单号" clearable style="width: 200px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="(item, key) in RETURN_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">退款申请列表</span>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column prop="orderSn" label="订单号" width="190" />
        <el-table-column prop="memberNickname" label="会员" width="120" />
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">
            <span class="amount">{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="问题描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="RETURN_STATUS[row.status]?.type">{{ RETURN_STATUS[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 && userStore.hasPerm('oms:return:handle')"
                       link type="primary" @click="openHandle(row)">审核</el-button>
            <span v-else class="handled-text">
              {{ row.handleNote || (row.status === 3 ? '等待渠道确认' : '已处理') }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { returnPage, returnHandle } from '@/api/order'
import { money, RETURN_STATUS } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10, orderSn: '', status: null })

const handleVisible = ref(false)
const saving = ref(false)
const current = ref(null)
const handleNote = ref('')

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await returnPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      orderSn: query.orderSn || undefined,
      status: query.status ?? undefined
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
  query.pageNum = 1
  load()
}

function openHandle(row) {
  current.value = row
  handleNote.value = ''
  handleVisible.value = true
}

async function submitHandle(approve) {
  try {
    await ElMessageBox.confirm(
      approve
        ? '支付宝确认退款后，订单将关闭并回补库存和优惠券。确定通过该退款申请吗？'
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
    ElMessage.success(approve ? '退款已提交，等待支付宝确认' : '已拒绝该退款申请')
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
</style>
