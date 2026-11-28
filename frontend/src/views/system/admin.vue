<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="账号 / 昵称" clearable style="width: 220px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">管理员列表</span>
        <el-button v-if="userStore.hasPerm('ums:admin:create')" type="primary" :icon="Plus"
                   @click="openDialog()">新增管理员</el-button>
      </div>

      <load-state v-if="listError" :error="listError" @retry="load" />
      <el-table v-else v-loading="loading" :empty-text="loading ? '正在加载…' : '暂无符合条件的记录'" :data="list">
        <el-table-column label="账号" width="160">
          <template #default="{ row }">
            <div class="cell-product">
              <el-avatar :size="30" :src="row.avatar || '/img/avatar.svg'" />
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="roleNames" label="角色" min-width="150" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('ums:admin:update')" link type="primary"
                       @click="openDialog(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('ums:admin:reset')" link type="warning"
                       @click="handleReset(row)">重置密码</el-button>
            <el-button v-if="userStore.hasPerm('ums:admin:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listError" class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="pageNum" v-model:page-size="pageSize"
                       @current-change="load" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="500px" destroy-on-close>
      <el-form scroll-to-error ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="3-20 位字母数字下划线" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="留空默认 admin123" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
import LoadState from '@/components/LoadState.vue'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { adminPage, adminCreate, adminUpdate, adminResetPassword, adminDelete, roleList } from '@/api/system'
import { encryptRequired } from '@/utils/crypto'

const userStore = useUserStore()

const loading = ref(true)
const listError = ref(false)
let loadVersion = 0
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const roles = ref([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [],
  status: 1
})

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }]
}

onMounted(async () => {
  roles.value = await roleList()
  load()
})

async function load() {
  const version = ++loadVersion
  loading.value = true
  listError.value = false
  try {
    const data = await adminPage({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
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
  pageNum.value = 1
  load()
}

function openDialog(row) {
  Object.assign(form, row
    ? {
        id: row.id,
        username: row.username,
        password: '',
        nickname: row.nickname,
        email: row.email,
        phone: row.phone,
        roleIds: row.roleIds || [],
        status: row.status
      }
    : { id: null, username: '', password: '', nickname: '', email: '', phone: '', roleIds: [], status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    if (form.id) {
      const payload = { ...form }
      delete payload.password
      await adminUpdate(payload)
    } else {
      const payload = { ...form }
      if (payload.password) {
        // 新建密码强制加密传输（留空则由服务端使用默认密码）
        payload.password = await encryptRequired(payload.password)
        payload.encrypted = true
      } else {
        delete payload.password
      }
      await adminCreate(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleReset(row) {
  try {
    await ElMessageBox.confirm(`确定将「${row.username}」的密码重置为 admin123 吗？`, '重置密码', { type: 'warning' })
    await adminResetPassword(row.id)
    ElMessage.success('密码已重置为 admin123')
  } catch (e) {
    /* 取消 */
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除管理员「${row.username}」吗？`, '删除确认', { type: 'warning' })
    await adminDelete(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>
