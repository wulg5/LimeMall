<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="优惠券名称">
          <el-input v-model="query.name" placeholder="输入名称" clearable style="width: 200px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="进行中" :value="1" />
            <el-option label="已停止" :value="0" />
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
        <span class="title">优惠券列表</span>
        <el-button v-if="userStore.hasPerm('sms:coupon:create')" type="primary" :icon="Plus"
                   @click="openDialog()">新建优惠券</el-button>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column prop="name" label="优惠券名称" min-width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'danger' : 'warning'">
              {{ row.type === 1 ? '满减券' : '折扣券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="180">
          <template #default="{ row }">
            <span v-if="row.type === 1" class="coupon-value">
              满 {{ Number(row.minAmount) }} 减 {{ Number(row.amount) }}
            </span>
            <span v-else class="coupon-value">
              {{ Number(row.amount) }} 折（满 {{ Number(row.minAmount) }} 可用）
            </span>
          </template>
        </el-table-column>
        <el-table-column label="领取 / 总量" width="130">
          <template #default="{ row }">{{ row.receiveCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column prop="usedCount" label="已使用" width="90" />
        <el-table-column label="积分价" width="100">
          <template #default="{ row }">
            <span v-if="row.pointsPrice > 0" class="points-price">{{ row.pointsPrice }} 分</span>
            <span v-else class="time-range">-</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }">
            <div class="time-range">{{ row.startTime }}</div>
            <div class="time-range">至 {{ row.endTime }}</div>
          </template>
        </el-table-column>
        <el-table-column label="上线" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1"
                       :disabled="!userStore.hasPerm('sms:coupon:publish')"
                       @change="(value) => handlePublish(row, value)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('sms:coupon:update')" link type="primary"
                       @click="openDialog(row)">编辑</el-button>
            <el-button link type="info" @click="openRecords(row)">领取记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
                       v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                       @current-change="load" @size-change="load" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新建优惠券'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="如：新人专享 满100减10" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.type === 1 ? '减免金额' : '折扣'" prop="amount">
          <el-input-number v-model="form.amount" :min="0.1" :max="form.type === 2 ? 9.9 : 100000"
                           :precision="form.type === 2 ? 1 : 2" :controls="false" style="width: 180px" />
          <span class="form-tip">{{ form.type === 2 ? '如 9.5 表示 95 折' : '元' }}</span>
        </el-form-item>
        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" :controls="false" style="width: 180px" />
          <span class="form-tip">订单满该金额可用（0 为无门槛）</span>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="每人限领" prop="perLimit">
          <el-input-number v-model="form.perLimit" :min="1" style="width: 180px" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="积分兑换">
          <el-switch v-model="form.exchangeEnabled" />
          <span class="form-tip">{{ form.exchangeEnabled ? `${Math.round((form.amount || 0) * 100)} 积分兑换` : '免费领取' }}</span>
        </el-form-item>
        <el-form-item v-else label="领取方式">免费领取</el-form-item>
        <el-form-item label="有效期" prop="timeRange">
          <el-date-picker v-model="form.timeRange" type="datetimerange" range-separator="至"
                          start-placeholder="开始时间" end-placeholder="结束时间"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 领取记录弹窗 -->
    <el-dialog v-model="recordsVisible" :title="`领取记录 · ${currentCoupon?.name || ''}`" width="640px" destroy-on-close>
      <el-table v-loading="recordsLoading" :data="records" size="small">
        <el-table-column prop="memberNickname" label="会员" width="140" />
        <el-table-column prop="memberUsername" label="账号" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="COUPON_MEMBER_STATUS[row.status]?.type">{{ COUPON_MEMBER_STATUS[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiveTime" label="领取时间" width="170" />
        <el-table-column prop="useTime" label="使用时间" min-width="170" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next" :total="recordsTotal"
                       v-model:current-page="recordsPage" :page-size="5"
                       @current-change="loadRecords" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { couponPage, couponCreate, couponUpdate, couponPublish, couponRecords } from '@/api/coupon'
import { COUPON_MEMBER_STATUS } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10, name: '', status: null })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  type: 1,
  amount: 10,
  minAmount: 0,
  totalCount: 100,
  perLimit: 1,
  exchangeEnabled: false,
  timeRange: []
})

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额或折扣', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入使用门槛', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }],
  perLimit: [{ required: true, message: '请输入每人限领数量', trigger: 'blur' }],
  timeRange: [{ required: true, type: 'array', len: 2, message: '请选择有效期', trigger: 'change' }]
}

const recordsVisible = ref(false)
const recordsLoading = ref(false)
const records = ref([])
const recordsTotal = ref(0)
const recordsPage = ref(1)
const currentCoupon = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await couponPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
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
  query.name = ''
  query.status = null
  query.pageNum = 1
  load()
}

function openDialog(row) {
  Object.assign(form, row
    ? {
        id: row.id,
        name: row.name,
        type: row.type,
        amount: Number(row.amount),
        minAmount: Number(row.minAmount),
        totalCount: row.totalCount,
        perLimit: row.perLimit,
        exchangeEnabled: row.type === 1 && row.pointsPrice > 0,
        timeRange: [row.startTime, row.endTime]
      }
    : {
        id: null,
        name: '',
        type: 1,
        amount: 10,
        minAmount: 0,
        totalCount: 100,
        perLimit: 1,
        exchangeEnabled: false,
        timeRange: []
      })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      type: form.type,
      amount: form.amount,
      minAmount: form.minAmount,
      totalCount: form.totalCount,
      perLimit: form.perLimit,
      pointsPrice: form.type === 1 && form.exchangeEnabled ? 1 : 0,
      startTime: form.timeRange[0],
      endTime: form.timeRange[1]
    }
    if (form.id) {
      await couponUpdate(payload)
    } else {
      await couponCreate(payload)
    }
    ElMessage.success('保存成功（新券默认停止状态，需手动上线）')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handlePublish(row, value) {
  await couponPublish(row.id, value ? 1 : 0)
  ElMessage.success(value ? '优惠券已上线' : '优惠券已下线')
  load()
}

function openRecords(row) {
  currentCoupon.value = row
  recordsVisible.value = true
  recordsPage.value = 1
  loadRecords()
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const data = await couponRecords(currentCoupon.value.id, { pageNum: recordsPage.value, pageSize: 5 })
    records.value = data.records
    recordsTotal.value = data.total
  } finally {
    recordsLoading.value = false
  }
}
</script>

<style scoped>
.coupon-value {
  color: #dc2626;
  font-weight: 600;
}

.time-range {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.6;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.points-price {
  color: #d97706;
  font-weight: 600;
  font-size: 12.5px;
}
</style>
