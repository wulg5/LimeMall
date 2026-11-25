<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="品牌名称">
          <el-input v-model="keyword" placeholder="输入品牌名称" clearable style="width: 220px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">品牌列表</span>
        <el-button v-if="userStore.hasPerm('pms:brand:create')" type="primary" :icon="Plus"
                   @click="openDialog()">新增品牌</el-button>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="品牌" min-width="180">
          <template #default="{ row }">
            <div class="brand-cell">
              <span class="brand-badge">{{ (row.name || '?').charAt(0) }}</span>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="firstLetter" label="首字母" width="90" />
        <el-table-column prop="description" label="品牌介绍" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('pms:brand:update')" link type="primary"
                       @click="openDialog(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('pms:brand:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="pageNum" v-model:page-size="pageSize"
                       @current-change="load" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑品牌' : '新增品牌'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="品牌名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入品牌名称" />
        </el-form-item>
        <el-form-item label="首字母">
          <el-input v-model="form.firstLetter" maxlength="1" placeholder="如 Q" style="width: 120px" />
        </el-form-item>
        <el-form-item label="品牌介绍">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
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
import { Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { brandPage, brandCreate, brandUpdate, brandDelete } from '@/api/product'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', firstLetter: '', description: '', sort: 0, status: 1 })

const rules = {
  name: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await brandPage({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  load()
}

function openDialog(row) {
  Object.assign(form, row
    ? { id: row.id, name: row.name, firstLetter: row.firstLetter, description: row.description, sort: row.sort, status: row.status }
    : { id: null, name: '', firstLetter: '', description: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await brandUpdate({ ...form })
    } else {
      await brandCreate({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除品牌「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await brandDelete(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>

<style scoped>
.brand-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-badge {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #34d399, #059669);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}
</style>
