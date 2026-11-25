<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号 / 昵称 / 手机号" clearable style="width: 220px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="已禁用" :value="0" />
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
        <span class="title">会员列表</span>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column label="会员" min-width="200">
          <template #default="{ row }">
            <div class="cell-product">
              <el-avatar :size="34" :src="row.avatar || '/img/avatar.svg'" />
              <div>
                <div>{{ row.nickname }}</div>
                <div class="username">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户编号" width="140">
          <template #default="{ row }"><span class="user-no">{{ row.userNo || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="等级" width="120">
          <template #default="{ row }">
            <el-tag :type="MEMBER_LEVEL[row.level]?.type || 'info'">{{ MEMBER_LEVEL[row.level]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" />
        <el-table-column prop="registerTime" label="注册时间" width="170" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1"
                       :disabled="!userStore.hasPerm('ums:member:update')"
                       active-text="正常" inactive-text="禁用"
                       inline-prompt
                       @change="(value) => handleToggle(row, value)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
                       v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                       @current-change="load" @size-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { memberPage, updateMemberStatus } from '@/api/member'
import { MEMBER_LEVEL } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: null })

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await memberPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
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
  query.keyword = ''
  query.status = null
  query.pageNum = 1
  load()
}

async function handleToggle(row, value) {
  try {
    if (!value) {
      await ElMessageBox.confirm(`确定禁用会员「${row.nickname}」吗？禁用后该会员将无法登录商城。`, '操作确认', { type: 'warning' })
    }
    await updateMemberStatus(row.id, value ? 1 : 0)
    ElMessage.success(value ? '会员已启用' : '会员已禁用')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>

<style scoped>
.username {
  font-size: 12px;
  color: #9ca3af;
}

.user-no {
  font-size: 12.5px;
  color: #6b7280;
  font-family: Consolas, monospace;
}
</style>
