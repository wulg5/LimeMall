<template>
  <div class="products-page">
    <div class="filter-card">
      <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width: 280px"
                @keyup.enter="handleSearch" @clear="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-select v-model="categoryId" placeholder="全部分类" clearable style="width: 200px" @change="handleSearch">
        <el-option-group v-for="group in categories" :key="group.id" :label="group.name">
          <el-option :label="`全部${group.name}`" :value="group.id" />
          <el-option v-for="child in group.children" :key="child.id" :label="child.name" :value="child.id" />
        </el-option-group>
      </el-select>

      <el-select v-model="sort" style="width: 140px" @change="handleSearch">
        <el-option label="综合排序" value="" />
        <el-option label="销量优先" value="sales" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
      </el-select>

      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div v-if="list.length" class="grid">
      <product-card v-for="product in list" :key="product.id" :product="product" />
    </div>
    <el-empty v-else description="没有找到相关商品" />

    <div v-if="total > pageSize" class="pager">
      <el-pagination background layout="prev, pager, next" :total="total"
                     v-model:current-page="pageNum" :page-size="pageSize" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
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
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

let latestRequest = 0

onMounted(async () => {
  categories.value = await getCategories()
})

watch(() => route.query, (query) => {
  keyword.value = query.name || ''
  categoryId.value = query.categoryId ? Number(query.categoryId) : null
  sort.value = query.sort || ''
  resetResults()
}, { immediate: true })

async function load() {
  const request = ++latestRequest
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
    }
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
.filter-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
