<template>
  <div class="page-container">
    <!-- 搜索区 -->
    <div class="page-card search-card">
      <el-form inline>
        <el-form-item label="商品名称">
          <el-input v-model="query.name" placeholder="输入商品名称" clearable style="width: 200px"
                    @keyup.enter="handleSearch" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-cascader v-model="query.categoryPath" :options="categoryOptions" :props="cascaderProps"
                       placeholder="全部分类" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="query.brandId" placeholder="全部品牌" clearable style="width: 160px">
            <el-option v-for="brand in brandList" :key="brand.id" :label="brand.name" :value="brand.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.publishStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="上架中" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表区 -->
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">商品列表</span>
        <el-button v-if="userStore.hasPerm('pms:product:create')" type="primary" :icon="Plus"
                   @click="$router.push('/product/edit')">新增商品</el-button>
      </div>

      <el-table v-loading="loading" :data="list">
        <el-table-column label="商品" min-width="320">
          <template #default="{ row }">
            <div class="cell-product">
              <img class="table-pic" :src="row.mainPic || '/img/logo.svg'" alt="" />
              <div>
                <div class="name">{{ row.name }}</div>
                <div class="sub">{{ row.subTitle }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column prop="brandName" label="品牌" width="110" />
        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="price">{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column prop="sales" label="销量" width="90" />
        <el-table-column label="上架" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.publishStatus === 1"
                       :disabled="!userStore.hasPerm('pms:product:publish')"
                       @change="(value) => handlePublish(row, value)" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('pms:product:update')" link type="primary"
                       @click="$router.push(`/product/edit?id=${row.id}`)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('pms:product:delete')" link type="danger"
                       @click="handleDelete(row)">删除</el-button>
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
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { productPage, productPublish, productDelete, brandOptions, categoryTree } from '@/api/product'
import { money } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const brandList = ref([])
const categoryOptions = ref([])

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  categoryPath: [],
  brandId: null,
  publishStatus: null
})

onMounted(async () => {
  ;[brandList.value, categoryOptions.value] = await Promise.all([brandOptions(), categoryTree()])
  load()
})

async function load() {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      brandId: query.brandId || undefined,
      publishStatus: query.publishStatus ?? undefined,
      categoryId: query.categoryPath?.length ? query.categoryPath[query.categoryPath.length - 1] : undefined
    }
    const data = await productPage(params)
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
  query.categoryPath = []
  query.brandId = null
  query.publishStatus = null
  query.pageNum = 1
  load()
}

async function handlePublish(row, value) {
  await productPublish(row.id, value ? 1 : 0)
  ElMessage.success(value ? '商品已上架' : '商品已下架')
  load()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？删除后订单记录不受影响。`, '删除确认', {
      type: 'warning'
    })
    await productDelete(row.id)
    ElMessage.success('商品已删除')
    load()
  } catch (e) {
    /* 取消删除 */
  }
}
</script>

<style scoped>
.sub {
  font-size: 12px;
  color: #9ca3af;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: #dc2626;
  font-weight: 600;
}
</style>
