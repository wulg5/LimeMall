<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">评价管理</span>
      </div>

      <load-state v-if="listError" :error="listError" @retry="load" />
      <el-table v-else v-loading="loading" :empty-text="loading ? '正在加载…' : '暂无符合条件的记录'" :data="list">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">{{ row.productName }}</template>
        </el-table-column>
        <el-table-column label="会员" width="170">
          <template #default="{ row }">
            <span class="member-cell">
              {{ row.memberNickname }}
              <level-badge :level="row.memberLevel" />
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('pms:comment:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listError" class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="pageNum" :page-size="pageSize"
                       @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { commentDelete, commentPage } from '@/api/comment'
import LevelBadge from '@/components/LevelBadge.vue'

const userStore = useUserStore()

const loading = ref(true)
const listError = ref(false)
let loadVersion = 0
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

onMounted(load)

async function load() {
  const version = ++loadVersion
  loading.value = true
  listError.value = false
  try {
    const data = await commentPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (version !== loadVersion) return
    list.value = data.records
    total.value = data.total
  } catch {
    if (version === loadVersion) listError.value = true
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('删除后该评价不可恢复，确定删除吗？（仅用于违规内容处理）', '删除评价', { type: 'warning' })
    await commentDelete(row.id)
    ElMessage.success('评价已删除')
    load()
  } catch (e) {
    /* 取消 */
  }
}
</script>

<style scoped>
.member-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
