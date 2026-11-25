<template>
  <div class="home">
    <!-- 首屏 Banner -->
    <section class="hero">
      <div class="hero-content">
        <h1>青柠商城 · 好物直达</h1>
        <p>数码家电 / 服饰鞋包 / 食品生鲜 / 美妆个护 / 家居生活，一站购齐</p>
        <div class="hero-actions">
          <el-button size="large" class="hero-btn" @click="$router.push('/shop/products')">立即逛逛</el-button>
          <el-button size="large" text class="hero-btn-plain" @click="$router.push('/shop/coupons')">
            领券中心 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="hero-circle c1"></div>
      <div class="hero-circle c2"></div>
      <div class="hero-circle c3"></div>
    </section>

    <!-- 分类快捷入口 -->
    <section class="category-bar">
      <router-link v-for="category in categories" :key="category.id" class="category-chip"
                   :to="`/shop/products?categoryId=${category.id}`">
        {{ category.name }}
      </router-link>
    </section>

    <!-- 热销榜 -->
    <section class="section">
      <div class="section-header">
        <h2>热销榜单</h2>
        <router-link to="/shop/products" class="more">查看更多 ></router-link>
      </div>
      <div class="grid">
        <product-card v-for="product in hotProducts" :key="product.id" :product="product" />
      </div>
    </section>

    <!-- 新品上架 -->
    <section class="section">
      <div class="section-header">
        <h2>新品上架</h2>
        <router-link to="/shop/products" class="more">查看更多 ></router-link>
      </div>
      <div class="grid">
        <product-card v-for="product in newProducts" :key="product.id" :product="product" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { getHome } from '@/api/portal'
import ProductCard from '@/components/ProductCard.vue'

const categories = ref([])
const hotProducts = ref([])
const newProducts = ref([])

onMounted(async () => {
  const data = await getHome()
  categories.value = data.categories || []
  hotProducts.value = data.hotProducts || []
  newProducts.value = data.newProducts || []
})
</script>

<style scoped>
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(130deg, #064e3b 0%, #059669 60%, #10b981 100%);
  color: #ffffff;
  padding: 56px 60px;
  margin-bottom: 20px;
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 560px;
}

.hero h1 {
  font-size: 34px;
  margin: 0 0 14px;
  letter-spacing: 1px;
}

.hero p {
  font-size: 15px;
  opacity: 0.88;
  margin: 0 0 26px;
}

.hero-btn {
  background: #ffffff;
  color: #047857;
  border: none;
  font-weight: 600;
  padding: 12px 30px;
  border-radius: 12px;
}

.hero-btn:hover {
  background: #ecfdf5;
  color: #065f46;
}

.hero-btn-plain {
  color: #ffffff;
  margin-left: 8px;
}

.hero-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.c1 {
  width: 380px;
  height: 380px;
  right: -100px;
  top: -120px;
}

.c2 {
  width: 220px;
  height: 220px;
  right: 15%;
  bottom: -100px;
  background: rgba(255, 255, 255, 0.06);
}

.c3 {
  width: 90px;
  height: 90px;
  right: 32%;
  top: 30%;
  background: rgba(255, 255, 255, 0.1);
}

.category-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.category-chip {
  background: #ffffff;
  padding: 10px 22px;
  border-radius: 12px;
  color: #374151;
  text-decoration: none;
  font-size: 14px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
  transition: all 0.2s;
}

.category-chip:hover {
  background: var(--el-color-primary);
  color: #ffffff;
  transform: translateY(-2px);
}

.section {
  margin-top: 28px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 20px;
  margin: 0;
  color: #111827;
}

.section-header h2::before {
  content: '';
  display: inline-block;
  width: 5px;
  height: 18px;
  background: var(--el-color-primary);
  border-radius: 3px;
  margin-right: 10px;
  vertical-align: -2px;
}

.more {
  color: #9ca3af;
  text-decoration: none;
  font-size: 13px;
}

.more:hover {
  color: var(--el-color-primary);
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
