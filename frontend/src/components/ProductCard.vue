<template>
  <div class="product-card" @click="$router.push(`/shop/product/${product.id}`)">
    <div class="pic-wrap">
      <img :src="product.mainPic || '/img/logo.svg'" :alt="product.name" loading="lazy" />
      <span v-if="discount > 0" class="discount-tag">立省 {{ discount }} 元</span>
    </div>
    <div class="info">
      <div class="name">{{ product.name }}</div>
      <div class="sub">{{ product.subTitle }}</div>
      <div class="bottom">
        <div class="price">
          <b>{{ money(product.price) }}</b>
          <del v-if="product.originalPrice && Number(product.originalPrice) > Number(product.price)">
            {{ money(product.originalPrice) }}
          </del>
        </div>
        <div class="sales">已售 {{ product.sales }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { money } from '@/utils/format'

const props = defineProps({
  product: { type: Object, required: true }
})

const discount = computed(() => {
  const original = Number(props.product.originalPrice || 0)
  const price = Number(props.product.price || 0)
  return original > price ? (original - price).toFixed(0) : 0
})
</script>

<style scoped>
.product-card {
  background: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(16, 185, 129, 0.15);
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
  transform: scale(1.05);
}

.discount-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(220, 38, 38, 0.9);
  color: #ffffff;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 8px;
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

.bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
}

.price b {
  color: #dc2626;
  font-size: 18px;
}

.price del {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 6px;
}

.sales {
  font-size: 12px;
  color: #9ca3af;
}
</style>
