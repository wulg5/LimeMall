<template>
  <div class="page-container">
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="管理员 / 模块 / 接口" clearable style="width: 220px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
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
        <span class="title">操作日志（自动记录后台写操作）</span>
      </div>

      <load-state v-if="listError" :error="listError" @retry="load" />
      <el-table v-else v-loading="loading" :empty-text="loading ? '正在加载…' : '暂无符合条件的记录'" :data="list">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="adminUsername" label="管理员" width="110" />
        <el-table-column label="操作" min-width="200">
          <template #default="{ row }">
            <div>{{ row.module }}.{{ row.action }}</div>
            <div class="uri">{{ row.method }} {{ row.uri }}</div>
          </template>
        </el-table-column>
        <el-table-column label="参数" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="params">{{ row.params }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="来源IP" width="130" />
        <el-table-column prop="costMs" label="耗时" width="90">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
            <el-tooltip v-if="row.errorMsg" :content="row.errorMsg" placement="top">
              <el-icon class="err-icon"><Warning /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listError" class="pagination-wrapper">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
                       v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                       @current-change="load" @size-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search, Warning } from '@element-plus/icons-vue'
import { logPage } from '@/api/content'

const loading = ref(true)
const listError = ref(false)
let loadVersion = 0
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: null })

onMounted(load)

async function load() {
  const version = ++loadVersion
  loading.value = true
  listError.value = false
  try {
    const data = await logPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
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
  query.keyword = ''
  query.status = null
  query.pageNum = 1
  load()
}
</script>

<style scoped>
.uri {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.params {
  font-size: 12px;
  color: #6b7280;
}

.err-icon {
  color: #f59e0b;
  margin-left: 6px;
  vertical-align: -2px;
}
</style>
