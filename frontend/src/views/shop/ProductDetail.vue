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
        <div v-if="detail.commentCount > 0" class="rating-line">
          <el-rate :model-value="Number(detail.avgRating)" disabled allow-half size="small" />
          <b>{{ Number(detail.avgRating).toFixed(1) }}</b>
          <span class="rating-count">{{ detail.commentCount }} 条评价</span>
        </div>
        <p class="sub-title">{{ detail.subTitle }}</p>

        <div class="price-bar">
          <span class="price">{{ money(selectedSku ? (selectedSku.flashPrice || selectedSku.price) : detail.price) }}</span>
          <del v-if="detail.originalPrice">{{ money(detail.originalPrice) }}</del>
          <span v-if="selectedSku && selectedSku.flashPrice" class="flash-badge">秒杀价</span>
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
              <b>{{ money(sku.flashPrice || sku.price) }}</b>
              <em v-if="sku.flashPrice" class="sku-flash">秒杀</em>
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

    <!-- 商品评价 -->
    <div class="detail-html-card">
      <div class="comments-head">
        <h2>商品评价（{{ detail.commentCount || 0 }}）</h2>
        <span v-if="detail.commentCount > 0" class="comments-score">
          综合评分 <b>{{ Number(detail.avgRating).toFixed(1) }}</b>
        </span>
      </div>
      <template v-if="comments.length">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="c-head">
            <el-avatar :size="26" src="/img/avatar.svg" />
            <span class="c-name">{{ comment.memberNickname }}</span>
            <level-badge :level="comment.memberLevel" />
            <el-rate :model-value="comment.rating" disabled size="small" class="c-rate" />
            <span class="c-time">{{ comment.createTime }}</span>
          </div>
          <p class="c-content">{{ comment.content }}</p>
        </div>
        <div v-if="commentTotal > commentPageSize" class="comments-pager">
          <el-pagination background small layout="prev, pager, next" :total="commentTotal"
                         v-model:current-page="commentPage" :page-size="commentPageSize"
                         @current-change="loadComments" />
        </div>
      </template>
      <el-empty v-else description="暂无评价，购买并确认收货后可发表评价" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getProductComments, getProductDetail } from '@/api/portal'
import { useCartStore } from '@/stores/cart'
import { money, specsToText } from '@/utils/format'
import LevelBadge from '@/components/LevelBadge.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const detail = ref(null)
const selectedSkuId = ref(null)
const quantity = ref(1)
const currentPic = ref('')

const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = 5

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

let latestCommentRequest = 0

watch([() => route.params.id, () => route.query.skuId], async ([id, skuId], _, onCleanup) => {
  if (!id) return
  let active = true
  onCleanup(() => { active = false })
  detail.value = null
  selectedSkuId.value = null
  quantity.value = 1
  comments.value = []
  commentTotal.value = 0
  commentPage.value = 1
  latestCommentRequest++
  try {
    const product = await getProductDetail(id)
    if (!active) return
    detail.value = product
    currentPic.value = product.mainPic
    const skus = product.skus || []
    const selected = skus.find((sku) => String(sku.id) === skuId)
      || skus.find((sku) => sku.stock > 0) || skus[0]
    selectedSkuId.value = selected?.id ?? null
    loadComments()
  } catch {
    // 请求错误由拦截器提示。
  }
}, { immediate: true })

async function loadComments() {
  const request = ++latestCommentRequest
  const id = route.params.id
  try {
    const data = await getProductComments(id, {
      pageNum: commentPage.value,
      pageSize: commentPageSize
    })
    if (request === latestCommentRequest && id === route.params.id) {
      comments.value = data.records
      commentTotal.value = data.total
    }
  } catch {
    // 请求错误由拦截器提示。
  }
}

function selectSku(sku) {
  selectedSkuId.value = sku.id
  quantity.value = 1
}

async function addToCart(buyNow) {
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
    price: Number(selectedSku.value.flashPrice || selectedSku.value.price),
    stock: selectedSku.value.stock,
    quantity: Math.min(quantity.value, selectedSku.value.stock)
  }
  if (buyNow) {
    cartStore.setBuyNow(item)
    router.push({ path: '/shop/checkout', query: { buyNow: '1' } })
    return
  }
  await cartStore.add(item)
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

.flash-badge {
  background: linear-gradient(120deg, #b91c1c, #ef4444);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 1px;
  align-self: center;
}

.sku-flash {
  font-style: normal;
  font-size: 10px;
  color: #ffffff;
  background: #ef4444;
  border-radius: 4px;
  padding: 1px 5px;
  align-self: flex-start;
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

.rating-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #6b7280;
}

.rating-line b {
  color: #d97706;
}

.rating-count {
  color: #9ca3af;
}

.comments-head {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 16px;
}

.comments-head h2 {
  margin: 0;
}

.comments-score {
  font-size: 13px;
  color: #9ca3af;
}

.comments-score b {
  color: #d97706;
  font-size: 16px;
}

.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid #f5f8f6;
}

.comment-item:last-of-type {
  border-bottom: none;
}

.c-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.c-name {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.c-rate {
  margin-left: 4px;
}

.c-time {
  margin-left: auto;
  font-size: 12px;
  color: #b6c0bb;
}

.c-content {
  margin: 8px 0 0 34px;
  font-size: 13.5px;
  color: #4b5563;
  line-height: 1.8;
}

.comments-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
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
