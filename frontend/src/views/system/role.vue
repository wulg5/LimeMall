<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">角色列表</span>
        <el-button v-if="userStore.hasPerm('ums:role:create')" type="primary" :icon="Plus"
                   @click="openDialog()">新增角色</el-button>
      </div>

      <load-state v-if="listError" :error="listError" @retry="load" />
      <el-table v-else v-loading="loading" :empty-text="loading ? '正在加载…' : '暂无符合条件的记录'" :data="list">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="角色名称" width="160" />
        <el-table-column prop="description" label="角色描述" min-width="240" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('ums:role:update')" link type="primary"
                       @click="openDialog(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('ums:role:assign')" link type="success"
                       @click="openAssign(row)">分配权限</el-button>
            <el-button v-if="userStore.hasPerm('ums:role:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="460px" destroy-on-close>
      <el-form scroll-to-error ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：运营专员" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="assignVisible" :title="`分配权限 · ${currentRole?.name || ''}`" width="520px" destroy-on-close>
      <el-tree ref="treeRef" :data="menuTreeData" node-key="id" show-checkbox default-expand-all
               :props="{ label: 'name', children: 'children' }" :default-checked-keys="checkedKeys"
               class="menu-tree" />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSaving" @click="handleAssign">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  roleList, roleCreate, roleUpdate, roleDelete,
  roleMenuIds, roleAssignMenus, menuTree
} from '@/api/system'

const userStore = useUserStore()

const loading = ref(true)
const listError = ref(false)
let loadVersion = 0
const list = ref([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', description: '', sort: 0, status: 1 })

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const assignVisible = ref(false)
const assignSaving = ref(false)
const treeRef = ref()
const menuTreeData = ref([])
const checkedKeys = ref([])
const currentRole = ref(null)

onMounted(load)

async function load() {
  const version = ++loadVersion
  loading.value = true
  listError.value = false
  try {
    if (version !== loadVersion) return
    list.value = await roleList()
  } catch {
    if (version === loadVersion) listError.value = true
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, row
    ? { id: row.id, name: row.name, description: row.description, sort: row.sort, status: row.status }
    : { id: null, name: '', description: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    if (form.id) {
      await roleUpdate({ ...form })
    } else {
      await roleCreate({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openAssign(row) {
  currentRole.value = row
  assignVisible.value = true
  const [menus, menuIds] = await Promise.all([menuTree(), roleMenuIds(row.id)])
  menuTreeData.value = menus
  // 仅回显叶子节点，父节点由树组件自动联动
  const allIds = new Set()
  const collect = (nodes) => nodes.forEach((node) => {
    if (!node.children || !node.children.length) allIds.add(node.id)
    else collect(node.children)
  })
  collect(menus)
  checkedKeys.value = menuIds.filter((id) => allIds.has(id))
  await nextTick()
  treeRef.value?.setCheckedKeys(checkedKeys.value)
}

async function handleAssign() {
  assignSaving.value = true
  try {
    const checked = treeRef.value.getCheckedKeys()
    const halfChecked = treeRef.value.getHalfCheckedKeys()
    const menuIds = [...checked, ...halfChecked]
    await roleAssignMenus(currentRole.value.id, menuIds)
    ElMessage.success('权限分配成功，相关账号重新登录后生效')
    assignVisible.value = false
  } finally {
    assignSaving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await roleDelete(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>

<style scoped>
.menu-tree {
  max-height: 420px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #f0f3f1;
  border-radius: 10px;
}
</style>
