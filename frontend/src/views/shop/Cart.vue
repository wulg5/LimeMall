<template>
  <div class="cart-page">
    <h1 class="page-title">我的购物车</h1>

    <template v-if="cartStore.items.length">
      <div class="cart-card">
        <div class="cart-header">
          <el-checkbox :model-value="allChecked" @change="handleToggleAll">全选</el-checkbox>
          <span class="col-product">商品</span>
          <span class="col-price">单价</span>
          <span class="col-qty">数量</span>
          <span class="col-subtotal">小计</span>
          <span class="col-action">操作</span>
        </div>

        <div v-for="item in cartStore.items" :key="item.skuId" class="cart-row">
          <el-checkbox :model-value="item.checked" @change="(value) => cartStore.toggle(item.skuId, value)" />
          <div class="col-product product-info" @click="$router.push(`/shop/product/${item.productId}`)">
            <img :src="item.pic || '/img/logo.svg'" alt="" />
            <div>
              <div class="name">{{ item.name }}</div>
              <div class="specs">{{ item.specs }}</div>
            </div>
          </div>
          <span class="col-price">{{ money(item.price) }}</span>
          <div class="col-qty">
            <el-input-number :model-value="item.quantity" :min="1" :max="item.stock"
                             size="small" @change="(value) => cartStore.updateQuantity(item.skuId, value)" />
          </div>
          <span class="col-subtotal subtotal">{{ money(item.price * item.quantity) }}</span>
          <span class="col-action">
            <el-button link type="danger" @click="handleRemove(item)">删除</el-button>
          </span>
        </div>
      </div>

      <div class="checkout-bar">
        <el-checkbox :model-value="allChecked" @change="handleToggleAll">全选</el-checkbox>
        <span class="summary">
          已选 <b>{{ checkedCount }}</b> 件，合计
          <b class="amount">{{ money(cartStore.checkedAmount) }}</b>
        </span>
        <el-button type="primary" size="large" class="checkout-btn"
                   :disabled="!cartStore.checkedItems.length" @click="$router.push('/shop/checkout')">
          去结算
        </el-button>
      </div>
    </template>

    <div v-else class="empty-card">
      <el-empty description="购物车还是空的，去挑选心仪的商品吧">
        <el-button type="primary" @click="$router.push('/shop/products')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { money } from '@/utils/format'

const cartStore = useCartStore()

const allChecked = computed(() =>
  cartStore.items.length > 0 && cartStore.items.every((item) => item.checked)
)

const checkedCount = computed(() =>
  cartStore.checkedItems.reduce((sum, item) => sum + item.quantity, 0)
)

function handleToggleAll(value) {
  cartStore.toggleAll(value)
}

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(`确定从购物车移除「${item.name}」吗？`, '提示', { type: 'warning' })
    await cartStore.remove(item.skuId)
  } catch (e) {
    /* 取消 */
  }
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin: 0 0 18px;
  color: #111827;
}

.cart-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 8px 20px;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.cart-header,
.cart-row {
  display: grid;
  grid-template-columns: 40px 1fr 120px 140px 120px 80px;
  align-items: center;
}

.cart-header {
  padding: 14px 0;
  color: #9ca3af;
  font-size: 13px;
  border-bottom: 1px solid #f1f5f3;
}

.cart-row {
  padding: 16px 0;
  border-bottom: 1px solid #f8faf9;
}

.cart-row:last-child {
  border-bottom: none;
}

.product-info {
  display: flex;
  gap: 14px;
  align-items: center;
  cursor: pointer;
  min-width: 0;
}

.product-info img {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-info .name {
  font-size: 14px;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-info .specs {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 6px;
}

.subtotal {
  color: #dc2626;
  font-weight: 600;
}

.checkout-bar {
  background: #ffffff;
  border-radius: 16px;
  margin-top: 16px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  position: sticky;
  bottom: 16px;
  box-shadow: 0 6px 20px rgba(15, 46, 36, 0.08);
}

.summary {
  margin-left: auto;
  color: #4b5563;
}

.summary b {
  color: #111827;
}

.summary .amount {
  color: #dc2626;
  font-size: 22px;
}

.checkout-btn {
  min-width: 140px;
  border-radius: 12px;
}

.empty-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 40px;
}
</style>
