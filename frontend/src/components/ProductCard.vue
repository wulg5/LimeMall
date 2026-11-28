<template>
  <div class="product-card" @click="$router.push(`/shop/product/${product.id}`)">
    <div class="pic-wrap">
      <img :src="product.mainPic || '/img/logo.svg'" :alt="product.name" loading="lazy" />
      <span v-if="product.flashPrice" class="flash-tag">秒杀</span>
      <span v-else-if="discount > 0" class="discount-tag">立省 {{ discount }} 元</span>
      <button class="quick-add" title="加入购物车" :disabled="adding" @click.stop="quickAdd">
        <el-icon><ShoppingCart /></el-icon>
        <span>{{ adding ? '添加中' : '加入购物车' }}</span>
      </button>
    </div>
    <div class="info">
      <div class="name">{{ product.name }}</div>
      <div class="sub">{{ product.subTitle }}</div>
      <div class="rating" v-if="product.commentCount > 0">
        <span class="stars">★★★★★</span>
        <b>{{ Number(product.avgRating).toFixed(1) }}</b>
        <span class="count">{{ product.commentCount }} 条评价</span>
      </div>
      <div class="bottom">
        <div class="price">
          <b>{{ money(currentPrice) }}</b>
          <del v-if="origPrice > currentPrice">{{ money(origPrice) }}</del>
        </div>
        <div class="sales">已售 {{ product.sales }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/portal'
import { useCartStore } from '@/stores/cart'
import { money, specsToText } from '@/utils/format'

const props = defineProps({
  product: { type: Object, required: true }
})

const cartStore = useCartStore()
const router = useRouter()
const adding = ref(false)

const discount = computed(() => {
  const original = Number(props.product.originalPrice || 0)
  const price = currentPrice.value
  return original > price ? (original - price).toFixed(0) : 0
})

/** 展示及快捷加购都以有货规格的最低当前价为准。 */
const currentPrice = computed(() =>
  Number(props.product.displayPrice ?? props.product.flashPrice ?? props.product.price ?? 0)
)

const origPrice = computed(() =>
  Number((props.product.flashPrice ? props.product.flashOriginalPrice : props.product.originalPrice) ?? 0)
)

const skuPrice = (sku) => Number(sku.flashPrice ?? sku.price)

/** 同价时优先选择参加秒杀的规格，其次按 SKU ID。 */
async function quickAdd() {
  adding.value = true
  try {
    const detail = await getProductDetail(props.product.id)
    const [sku] = (detail.skus || []).filter(item => item.stock > 0).sort((a, b) =>
      skuPrice(a) - skuPrice(b) || Number(!a.flashPrice) - Number(!b.flashPrice) || a.id - b.id
    )
    if (!sku) {
      ElMessage.warning('该商品暂时缺货')
      return
    }
    if (skuPrice(sku) !== currentPrice.value) {
      ElMessage.warning('商品价格已变化，请在详情页确认规格和价格')
      await router.push({ path: `/shop/product/${detail.id}`, query: { skuId: String(sku.id) } })
      return
    }
    await cartStore.add({
      skuId: sku.id,
      productId: detail.id,
      name: detail.name,
      pic: sku.pic || detail.mainPic,
      specs: specsToText(sku.specs),
      price: skuPrice(sku),
      stock: sku.stock,
      quantity: 1
    })
    ElMessage.success('已加入购物车')
  } finally {
    adding.value = false
  }
}
</script>

<style scoped>
.product-card {
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.22s;
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(15, 46, 36, 0.1);
  border-color: var(--el-color-primary-light-5);
}

.pic-wrap {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
}

.pic-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .pic-wrap img {
  transform: scale(1.04);
}

.discount-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(220, 38, 38, 0.92);
  color: #ffffff;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 8px;
}

.flash-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: linear-gradient(120deg, #b91c1c, #ef4444);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 2px;
  padding: 3px 9px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(185, 28, 28, 0.4);
}

.quick-add {
  position: absolute;
  right: 10px;
  bottom: 10px;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  padding: 7px 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(6px);
  transition: all 0.22s;
  box-shadow: 0 4px 12px rgba(15, 46, 36, 0.16);
}

.product-card:hover .quick-add {
  opacity: 1;
  transform: translateY(0);
}

.quick-add:hover {
  background: var(--el-color-primary);
  color: #ffffff;
}

.info {
  padding: 12px 14px 14px;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rating {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
}

.stars {
  color: #f59e0b;
  font-size: 11px;
  letter-spacing: 1px;
}

.rating b {
  color: #d97706;
}

.rating .count {
  color: #9ca3af;
}

.bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 8px;
}

.price b {
  color: #dc2626;
  font-size: 18px;
}

.price del {
  color: #c3ccc8;
  font-size: 12px;
  margin-left: 6px;
}

.sales {
  font-size: 12px;
  color: #9ca3af;
}
</style>
