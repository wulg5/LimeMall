<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">秒杀管理</span>
        <el-button v-if="userStore.hasPerm('sms:flash:edit')" type="primary" :icon="Plus" @click="openDialog()">
          新增秒杀
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column label="秒杀 SKU" width="150">
          <template #default="{ row }">#{{ row.skuId }}</template>
        </el-table-column>
        <el-table-column label="秒杀价" width="120">
          <template #default="{ row }"><span class="flash-price">{{ money(row.flashPrice) }}</span></template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="220">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('sms:flash:edit')" link type="primary"
                       @click="openDialog(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('sms:flash:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="pageNum" :page-size="pageSize" @current-change="load" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑秒杀' : '新增秒杀'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="商品" prop="productId">
          <el-select v-model="form.productId" filterable placeholder="搜索并选择商品" style="width: 100%"
                     @change="handleProductChange">
            <el-option v-for="product in products" :key="product.id" :label="product.name" :value="product.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU" prop="skuId">
          <el-select v-model="form.skuId" placeholder="选择参与秒杀的规格" style="width: 100%">
            <el-option v-for="sku in skus" :key="sku.id"
                       :label="`${specsToText(sku.specs)}（售价 ${money(sku.price)}，库存 ${sku.stock}）`"
                       :value="sku.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="如：今日秒杀 · 云岚降噪耳机" />
        </el-form-item>
        <el-form-item label="秒杀价" prop="flashPrice">
          <el-input-number v-model="form.flashPrice" :min="0.01" :precision="2" :controls="false" style="width: 200px" />
          <span class="form-tip">必须低于 SKU 售价</span>
        </el-form-item>
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker v-model="form.timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                          start-placeholder="开始时间" end-placeholder="结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { productPage, productDetail } from '@/api/product'
import { flashCreate, flashDelete, flashPage, flashUpdate } from '@/api/content'
import { money, specsToText } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const formRef = ref()
const products = ref([])
const skus = ref([])
const form = reactive({})
const rules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  skuId: [{ required: true, message: '请选择 SKU', trigger: 'change' }],
  name: [{ required: true, message: '请填写活动名称', trigger: 'blur' }],
  flashPrice: [{ required: true, message: '请填写秒杀价', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择活动时间', trigger: 'change' }]
}

onMounted(async () => {
  load()
  const data = await productPage({ pageNum: 1, pageSize: 100 })
  products.value = data.records
})

async function load() {
  loading.value = true
  try {
    const data = await flashPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openDialog(row) {
  Object.keys(form).forEach((key) => delete form[key])
  skus.value = []
  if (row) {
    Object.assign(form, {
      id: row.id,
      productId: null,
      skuId: row.skuId,
      name: row.name,
      flashPrice: Number(row.flashPrice),
      timeRange: [row.startTime, row.endTime],
      status: row.status
    })
    // 反查 SKU 所属商品（便于展示与编辑）
    for (const product of products.value) {
      const detail = await productDetail(product.id)
      if ((detail.skus || []).some((sku) => sku.id === row.skuId)) {
        form.productId = product.id
        skus.value = detail.skus
        break
      }
    }
  } else {
    Object.assign(form, {
      id: null, productId: null, skuId: null, name: '',
      flashPrice: null, timeRange: null, status: 1
    })
  }
  dialogVisible.value = true
}

async function handleProductChange(productId) {
  form.skuId = null
  const detail = await productDetail(productId)
  skus.value = detail.skus || []
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      skuId: form.skuId,
      flashPrice: form.flashPrice,
      startTime: form.timeRange[0],
      endTime: form.timeRange[1],
      status: form.status
    }
    await (form.id ? flashUpdate(payload) : flashCreate(payload))
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除秒杀「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await flashDelete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 取消 */ }
}
</script>

<style scoped>
.flash-price {
  color: #dc2626;
  font-weight: 600;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
