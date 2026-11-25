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
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getCategories, getProducts } from '@/api/portal'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()

const keyword = ref(route.query.name || '')
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)
const categories = ref([])
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)

onMounted(async () => {
  categories.value = await getCategories()
  load()
})

async function load() {
  const data = await getProducts({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    name: keyword.value || undefined,
    categoryId: categoryId.value || undefined
  })
  list.value = data.records
  total.value = data.total
}

function handleSearch() {
  pageNum.value = 1
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
