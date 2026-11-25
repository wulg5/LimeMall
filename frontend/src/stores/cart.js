import { defineStore } from 'pinia'

const STORAGE_KEY = 'lime_cart'

function loadCart() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch (e) {
    return []
  }
}

/**
 * 购物车 Store（localStorage 持久化）。
 * 购物车属于前端态设计：由前端维护，结算时才与后端库存校验，符合 B2C 电商常见做法。
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: loadCart()
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
        existing.quantity = Math.min(existing.quantity + item.quantity, existing.stock || 999)
        existing.checked = true
      } else {
        this.items.push({ ...item, checked: true })
      }
      this.persist()
    },

    /** 立即购买：仅保留该商品为勾选状态 */
    setOnly(item) {
      this.items.forEach((it) => { it.checked = false })
      const existing = this.items.find((it) => it.skuId === item.skuId)
      if (existing) {
        existing.quantity = item.quantity
        existing.checked = true
      } else {
        this.items.push({ ...item, checked: true })
      }
      this.persist()
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

    /** 下单成功后移除已购买商品 */
    removeChecked() {
      this.items = this.items.filter((it) => !it.checked)
      this.persist()
    }
  }
})
