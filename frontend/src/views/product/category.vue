<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">商品分类（两级）</span>
        <el-button v-if="userStore.hasPerm('pms:category:create')" type="primary" :icon="Plus"
                   @click="openDialog()">新增分类</el-button>
      </div>

      <el-table v-loading="loading" :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="分类名称" min-width="240" />
        <el-table-column label="层级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'success' : 'primary'">
              {{ row.level === 1 ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('pms:category:update')" link type="primary"
                       @click="openDialog(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('pms:category:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" :disabled="!!form.id" style="width: 100%">
            <el-option label="无（作为一级分类）" :value="0" />
            <el-option v-for="node in tree" :key="node.id" :label="node.name" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
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
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { categoryTree, categoryCreate, categoryUpdate, categoryDelete } from '@/api/product'

const userStore = useUserStore()

const loading = ref(false)
const tree = ref([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, parentId: 0, name: '', sort: 0, status: 1 })

const rules = {
  parentId: [{ required: true, message: '请选择上级分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    tree.value = await categoryTree()
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, row
    ? { id: row.id, parentId: row.parentId, name: row.name, sort: row.sort, status: row.status }
    : { id: null, parentId: 0, name: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await categoryUpdate({ ...form })
    } else {
      await categoryCreate({ ...form })
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
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await categoryDelete(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>
