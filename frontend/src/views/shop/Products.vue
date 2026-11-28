<template>
  <div class="products-page">
    <h1 class="shop-page-title">全部商品</h1>
    <form class="filter-card page-card" role="search" @submit.prevent="handleSearch">
      <el-input v-model="keyword" placeholder="搜索商品名称" aria-label="商品名称" clearable @clear="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-select v-model="categoryId" placeholder="全部分类" aria-label="商品分类" clearable @change="handleSearch">
        <el-option-group v-for="group in categories" :key="group.id" :label="group.name">
          <el-option :label="`全部${group.name}`" :value="group.id" />
          <el-option v-for="child in group.children" :key="child.id" :label="child.name" :value="child.id" />
        </el-option-group>
      </el-select>

      <el-select v-model="sort" aria-label="商品排序" @change="handleSearch">
        <el-option label="综合排序" value="" />
        <el-option label="销量优先" value="sales" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
      </el-select>

      <el-button type="primary" native-type="submit">搜索</el-button>
    </form>

    <load-state v-if="loading || listError" :loading="loading" :error="listError" @retry="load" />
    <div v-else-if="list.length" class="grid">
      <product-card v-for="product in list" :key="product.id" :product="product" />
    </div>
    <el-empty v-else description="没有找到相关商品">
      <el-button @click="router.push({ query: {} })">清除筛选</el-button>
    </el-empty>

    <div v-if="total > pageSize" class="pager">
      <el-pagination background layout="prev, pager, next" :total="total"
                     v-model:current-page="pageNum" :page-size="pageSize" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { onMounted, ref, watch } from 'vue'
import { isNavigationFailure, NavigationFailureType, useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getCategories, getProducts } from '@/api/portal'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const categoryId = ref(null)
const sort = ref('')
const categories = ref([])
const list = ref([])
const loading = ref(true)
const listError = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

let latestRequest = 0

onMounted(async () => {
  categories.value = await getCategories().catch(() => [])
})

watch(() => route.query, (query) => {
  keyword.value = query.name || ''
  categoryId.value = query.categoryId ? Number(query.categoryId) : null
  sort.value = query.sort || ''
  resetResults()
}, { immediate: true })

async function load() {
  const request = ++latestRequest
  loading.value = true
  listError.value = false
  try {
    const data = await getProducts({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: route.query.name || undefined,
      categoryId: route.query.categoryId || undefined,
      sort: route.query.sort || undefined
    })
    if (request === latestRequest) {
      list.value = data.records
      total.value = data.total
    }
  } catch {
    if (request === latestRequest) {
      list.value = []
      total.value = 0
      listError.value = true
    }
  } finally {
    if (request === latestRequest) loading.value = false
  }
}

async function handleSearch() {
  const failure = await router.push({ query: {
    name: keyword.value || undefined,
    categoryId: categoryId.value || undefined,
    sort: sort.value || undefined
  } })
  if (isNavigationFailure(failure, NavigationFailureType.duplicated)) resetResults()
}

function resetResults() {
  pageNum.value = 1
  list.value = []
  total.value = 0
  load()
}
</script>

<style scoped>
.filter-card { display: grid; grid-template-columns: minmax(170px, 2fr) minmax(135px, 1fr) minmax(135px, 1fr) auto; gap: 10px; margin-bottom: 20px; }
.grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.pager { display: flex; justify-content: center; margin-top: 24px; }
@media (max-width: 740px) {
  .filter-card { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; }
}
</style>
