import { defineStore } from 'pinia'

const STORAGE_KEY = 'lime_cart'
const BUY_NOW_KEY = 'lime_buy_now'
const completedCartCheckouts = new Set()

function loadCart() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch (e) {
    return []
  }
}

function loadBuyNowItem() {
  try {
    return JSON.parse(sessionStorage.getItem(BUY_NOW_KEY) || 'null')
  } catch {
    return null
  }
}

/**
 * 购物车 Store（localStorage 持久化）。
 * 购物车属于前端态设计：由前端维护，结算时才与后端库存校验，符合 B2C 电商常见做法。
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: loadCart(),
    buyNowItem: loadBuyNowItem()
  }),

  getters: {
    /** 购物车商品总件数 */
    totalCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    /** 已勾选商品 */
    checkedItems: (state) => state.items.filter((item) => item.checked),
    /** 已勾选金额 */
    checkedAmount() {
      return this.checkedItems.reduce((sum, item) => sum + item.price * item.quantity, 0)
    }
  },

  actions: {
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.items))
    },

    /**
     * 加入购物车；同 SKU 累加数量，最多不超过库存。
     */
    add(item) {
      const existing = this.items.find((it) => it.skuId === item.skuId)
      if (existing) {
        const quantity = Math.min(existing.quantity + item.quantity, item.stock ?? 999)
        Object.assign(existing, item, { quantity, checked: true })
      } else {
        this.items.push({ ...item, checked: true })
      }
      this.persist()
    },

    setBuyNow(item) {
      this.buyNowItem = { ...item }
      try {
        sessionStorage.setItem(BUY_NOW_KEY, JSON.stringify(this.buyNowItem))
      } catch {
        // 存储不可用时，本页仍可立即购买。
      }
    },

    clearBuyNow(item) {
      if (this.buyNowItem !== item) return
      this.buyNowItem = null
      try {
        sessionStorage.removeItem(BUY_NOW_KEY)
      } catch {
        // 当前购买项已从内存清除。
      }
    },

    updateQuantity(skuId, quantity) {
      const item = this.items.find((it) => it.skuId === skuId)
      if (item) {
        item.quantity = quantity
        this.persist()
      }
    },

    toggle(skuId, checked) {
      const item = this.items.find((it) => it.skuId === skuId)
      if (item) {
        item.checked = checked
        this.persist()
      }
    },

    toggleAll(checked) {
      this.items.forEach((it) => { it.checked = checked })
      this.persist()
    },

    remove(skuId) {
      this.items = this.items.filter((it) => it.skuId !== skuId)
      this.persist()
    },

    removePurchased(requestId, items) {
      if (completedCartCheckouts.has(requestId)) return
      completedCartCheckouts.add(requestId)
      const quantities = new Map(items.map(({ skuId, quantity }) => [skuId, quantity]))
      this.items = this.items.filter(item => {
        if (!quantities.has(item.skuId)) return true
        const remaining = item.quantity - quantities.get(item.skuId)
        if (remaining <= 0) return false
        item.quantity = remaining
        return true
      })
      try {
        this.persist()
      } catch {
        // 下单已成功，当前页面仍使用更新后的购物车。
      }
    }
  }
})
