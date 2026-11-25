<template>
  <div v-if="detail" class="detail-page">
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/shop' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/shop/products' }">全部商品</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.name }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="detail-card">
      <!-- 左侧图册 -->
      <div class="gallery">
        <img class="main-pic" :src="currentPic || detail.mainPic || '/img/logo.svg'" :alt="detail.name" />
        <div v-if="album.length > 1" class="thumbs">
          <img v-for="pic in album" :key="pic" :src="pic"
               :class="{ active: currentPic === pic }" @click="currentPic = pic" alt="" />
        </div>
      </div>

      <!-- 右侧信息 -->
      <div class="info">
        <h1>{{ detail.name }}</h1>
        <p class="sub-title">{{ detail.subTitle }}</p>

        <div class="price-bar">
          <span class="price">{{ money(selectedSku ? selectedSku.price : detail.price) }}</span>
          <del v-if="detail.originalPrice">{{ money(detail.originalPrice) }}</del>
          <span class="sales">已售 {{ detail.sales }} {{ detail.unit }}</span>
        </div>

        <div class="meta-row">
          <span class="label">分类</span>
          <span>{{ detail.categoryName }}</span>
          <span class="label" style="margin-left: 24px">品牌</span>
          <span>{{ detail.brandName || '-' }}</span>
        </div>

        <div class="sku-section">
          <div class="label">选择规格</div>
          <div class="sku-list">
            <div v-for="sku in detail.skus" :key="sku.id"
                 class="sku-item" :class="{ active: selectedSkuId === sku.id }"
                 @click="selectSku(sku)">
              <span>{{ specsToText(sku.specs) }}</span>
              <b>{{ money(sku.price) }}</b>
            </div>
          </div>
        </div>

        <div class="quantity-row">
          <span class="label">数量</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQuantity" />
          <span class="stock-text">库存 {{ selectedSku ? selectedSku.stock : 0 }} {{ detail.unit }}</span>
        </div>

        <div class="action-row">
          <el-button size="large" class="cart-btn" :disabled="!selectedSku || selectedSku.stock <= 0"
                     @click="addToCart(false)">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
          </el-button>
          <el-button type="primary" size="large" class="buy-btn" :disabled="!selectedSku || selectedSku.stock <= 0"
                     @click="addToCart(true)">
            立即购买
          </el-button>
          <el-button v-if="!selectedSku || selectedSku.stock <= 0" size="large" disabled>暂时缺货</el-button>
        </div>
      </div>
    </div>

    <!-- 商品详情 -->
    <div class="detail-html-card">
      <h2>商品详情</h2>
      <div class="detail-html" v-html="detail.detailHtml || '<p>暂无详情</p>'"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/portal'
import { useCartStore } from '@/stores/cart'
import { money, specsToText } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const detail = ref(null)
const selectedSkuId = ref(null)
const quantity = ref(1)
const currentPic = ref('')

const album = computed(() => {
  const raw = detail.value?.album || ''
  return [...new Set(raw.split(',').filter(Boolean))]
})

const selectedSku = computed(() =>
  detail.value?.skus?.find((sku) => sku.id === selectedSkuId.value) || null
)

const maxQuantity = computed(() =>
  Math.max(1, selectedSku.value ? selectedSku.value.stock : 1)
)

onMounted(async () => {
  detail.value = await getProductDetail(route.params.id)
  currentPic.value = detail.value.mainPic
  // 默认选中第一个有库存的 SKU
  const skus = detail.value.skus || []
  const first = skus.find((sku) => sku.stock > 0) || skus[0]
  selectedSkuId.value = first ? first.id : null
})

function selectSku(sku) {
  selectedSkuId.value = sku.id
  quantity.value = 1
}

function addToCart(buyNow) {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  const item = {
    skuId: selectedSku.value.id,
    productId: detail.value.id,
    name: detail.value.name,
    pic: selectedSku.value.pic || detail.value.mainPic,
    specs: specsToText(selectedSku.value.specs),
    price: Number(selectedSku.value.price),
    stock: selectedSku.value.stock,
    quantity: Math.min(quantity.value, selectedSku.value.stock)
  }
  if (buyNow) {
    cartStore.setOnly(item)
    router.push('/shop/checkout')
    return
  }
  cartStore.add(item)
  ElMessage.success('已加入购物车')
}
</script>

<style scoped>
.breadcrumb {
  margin-bottom: 16px;
}

.detail-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 28px;
  display: flex;
  gap: 36px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.gallery {
  width: 420px;
  flex-shrink: 0;
}

.main-pic {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 14px;
  object-fit: cover;
}

.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.thumbs img {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.thumbs img.active {
  border-color: var(--el-color-primary);
}

.info {
  flex: 1;
  min-width: 0;
}

.info h1 {
  font-size: 24px;
  margin: 0 0 10px;
  color: #111827;
}

.sub-title {
  color: #6b7280;
  margin: 0 0 18px;
}

.price-bar {
  background: linear-gradient(90deg, #fff7f5, #fefce8);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price {
  color: #dc2626;
  font-size: 30px;
  font-weight: 700;
}

.price-bar del {
  color: #9ca3af;
  font-size: 14px;
}

.sales {
  margin-left: auto;
  color: #9ca3af;
  font-size: 13px;
}

.meta-row {
  margin: 18px 0;
  color: #4b5563;
  font-size: 14px;
}

.label {
  color: #9ca3af;
  margin-right: 10px;
}

.sku-section {
  margin-bottom: 20px;
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.sku-item {
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 2px;
  transition: all 0.2s;
  font-size: 13px;
}

.sku-item b {
  color: #dc2626;
  font-size: 13px;
}

.sku-item:hover {
  border-color: var(--el-color-primary-light-5);
}

.sku-item.active {
  border-color: var(--el-color-primary);
  background: #e7f8f2;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.stock-text {
  color: #9ca3af;
  font-size: 13px;
}

.action-row {
  display: flex;
  gap: 14px;
}

.cart-btn {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  padding: 0 28px;
  border-radius: 12px;
}

.buy-btn {
  padding: 0 34px;
  border-radius: 12px;
}

.detail-html-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 28px;
  margin-top: 20px;
}

.detail-html-card h2 {
  font-size: 18px;
  margin: 0 0 18px;
  color: #111827;
}

.detail-html {
  color: #374151;
  line-height: 1.9;
}

@media (max-width: 900px) {
  .detail-card {
    flex-direction: column;
  }

  .gallery {
    width: 100%;
  }
}
</style>
