<template>
  <article class="product-card">
    <router-link class="product-link" :to="'/shop/product/' + product.id">
      <div class="pic-wrap">
        <product-image :src="product.mainPic" alt="" />
        <span v-if="product.flashPrice" class="flash-tag">秒杀</span>
        <span v-else-if="discount > 0" class="discount-tag">立省 {{ discount }} 元</span>
      </div>
      <div class="info">
        <h3>{{ product.name }}</h3>
        <p v-if="product.subTitle" class="sub">{{ product.subTitle }}</p>
        <div v-if="product.commentCount > 0" class="rating">
          <span aria-hidden="true">★</span> <b>{{ Number(product.avgRating).toFixed(1) }}</b>
          <span>{{ product.commentCount }} 条评价</span>
        </div>
        <div class="price">
          <strong>{{ money(currentPrice) }}</strong>
          <del v-if="origPrice > currentPrice">{{ money(origPrice) }}</del>
        </div>
      </div>
    </router-link>
    <footer>
      <span>已售 {{ product.sales }}</span>
      <button type="button" class="quick-add" :disabled="adding || outOfStock" :aria-label="addText + ' ' + product.name"
              @click="quickAdd"><el-icon v-if="!outOfStock"><ShoppingCart /></el-icon> {{ addText }}</button>
    </footer>
  </article>
</template>

<script setup>
import ProductImage from '@/components/ProductImage.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/portal'
import { useCartStore } from '@/stores/cart'
import { money, specsToText } from '@/utils/format'
import { cheapestAvailableSku, skuPrice } from '@/utils/sku'

const props = defineProps({
  product: { type: Object, required: true }
})

const cartStore = useCartStore()
const router = useRouter()
const adding = ref(false)
const outOfStock = computed(() => props.product.stock != null && Number(props.product.stock) <= 0)
const addText = computed(() => outOfStock.value ? '暂时缺货' : adding.value ? '添加中' : '加入购物车')

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

async function quickAdd() {
  adding.value = true
  try {
    const detail = await getProductDetail(props.product.id)
    const sku = cheapestAvailableSku(detail.skus || [])
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
  } catch {
    // 请求失败由统一提示处理，可以再次添加。
  } finally {
    adding.value = false
  }
}
</script>

<style scoped>
.product-card { display: flex; flex-direction: column; min-width: 0; background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-panel); }
.product-card:focus-within, .product-card:hover { border-color: var(--action); }
.product-link { display: flex; flex: 1; flex-direction: column; text-decoration: none; color: inherit; min-width: 0; border-radius: var(--radius-panel); }
.pic-wrap { position: relative; aspect-ratio: 1.16; overflow: hidden; border-radius: 11px 11px 0 0; background: var(--soft-green); }
.pic-wrap img { width: 100%; height: 100%; display: block; object-fit: cover; }
.flash-tag, .discount-tag { position: absolute; top: 8px; left: 8px; border-radius: 5px; padding: 4px 7px; background: #fff3ea; color: var(--price); font-size: 12px; font-weight: 700; }
.flash-tag { color: var(--paper); background: var(--price); }
.info { flex: 1; display: flex; flex-direction: column; gap: 5px; padding: 12px 12px 9px; }
.info h3 { font-size: 15px; line-height: 1.4; margin: 0; color: var(--pine); overflow-wrap: anywhere; }
.product-link:hover h3 { text-decoration: underline; text-underline-offset: 3px; }
.sub { margin: 0; font-size: 12px; color: var(--muted); display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 1; overflow: hidden; }
.rating { display: flex; flex-wrap: wrap; gap: 4px; align-items: baseline; color: var(--muted); font-size: 12px; }
.rating > span:first-child { color: #87500f; }
.price { display: flex; align-items: baseline; flex-wrap: wrap; gap: 2px 7px; margin-top: auto; padding-top: 4px; }
.price strong { color: var(--price); font-size: clamp(17px, 2.4vw, 23px); font-variant-numeric: tabular-nums; }
.price del { color: var(--muted); font-size: 12px; }
footer { display: flex; justify-content: space-between; flex-wrap: wrap; align-items: center; gap: 5px; margin: 0 11px; padding: 7px 0; border-top: 1px solid var(--line); }
footer > span { color: var(--muted); font-size: 12px; }
.quick-add { border: 0; border-radius: 6px; min-height: 37px; padding: 0 7px; display: inline-flex; gap: 4px; align-items: center; color: var(--action); background: var(--soft-green); font-size: 12px; font-weight: 650; cursor: pointer; }
.quick-add:hover:not(:disabled) { color: var(--paper); background: var(--action); }
.quick-add:disabled { color: var(--muted); background: var(--page); cursor: not-allowed; }
@media (max-width: 520px) { .info { padding-inline: 8px; } .info h3 { font-size: 13px; } footer { margin-inline: 8px; } .quick-add { min-height: 41px; } }
</style>
