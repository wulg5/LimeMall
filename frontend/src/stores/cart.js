import { defineStore } from 'pinia'

export const CART_STORAGE_KEY = 'lime_cart'
const BUY_NOW_KEY = 'lime_buy_now'
const CHECKOUT_HISTORY_LIMIT = 100

function readCart() {
  try {
    const value = JSON.parse(localStorage.getItem(CART_STORAGE_KEY) || '[]')
    if (Array.isArray(value)) return { items: value, completedCheckouts: [] }
    if (!Array.isArray(value?.items)) return null
    return { items: value.items, completedCheckouts: value.completedCheckouts || [] }
  } catch {
    return null
  }
}

function loadBuyNowItem() {
  try {
    return JSON.parse(sessionStorage.getItem(BUY_NOW_KEY) || 'null')
  } catch {
    return null
  }
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    ...(readCart() || { items: [], completedCheckouts: [] }),
    buyNowItem: loadBuyNowItem(),
    useStoredCart: true
  }),

  getters: {
    totalCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    checkedItems: (state) => state.items.filter((item) => item.checked),
    checkedAmount() {
      return this.checkedItems.reduce((sum, item) => sum + item.price * item.quantity, 0)
    }
  },

  actions: {
    syncFromStorage() {
      if (!this.useStoredCart) return
      const stored = readCart()
      if (stored) this.$patch(stored)
    },

    updateCart(change) {
      const update = () => {
        this.syncFromStorage()
        change()
        try {
          localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({
            items: this.items,
            completedCheckouts: this.completedCheckouts
          }))
        } catch {
          this.useStoredCart = false
        }
      }
      const locks = globalThis.navigator?.locks
      return locks ? locks.request(CART_STORAGE_KEY, update) : update()
    },

    add(item) {
      return this.updateCart(() => {
        const existing = this.items.find((it) => it.skuId === item.skuId)
        if (existing) {
          const quantity = Math.min(existing.quantity + item.quantity, item.stock ?? 999)
          Object.assign(existing, item, { quantity, checked: true })
        } else {
          this.items.push({ ...item, checked: true })
        }
      })
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
      return this.updateCart(() => {
        const item = this.items.find((it) => it.skuId === skuId)
        if (item) item.quantity = quantity
      })
    },

    toggle(skuId, checked) {
      return this.updateCart(() => {
        const item = this.items.find((it) => it.skuId === skuId)
        if (item) item.checked = checked
      })
    },

    toggleAll(checked) {
      return this.updateCart(() => {
        this.items.forEach((item) => { item.checked = checked })
      })
    },

    remove(skuId) {
      return this.updateCart(() => {
        this.items = this.items.filter((it) => it.skuId !== skuId)
      })
    },

    removePurchased(requestId, items) {
      return this.updateCart(() => {
        if (this.completedCheckouts.includes(requestId)) return
        this.completedCheckouts = [...this.completedCheckouts, requestId].slice(-CHECKOUT_HISTORY_LIMIT)
        const quantities = new Map(items.map(({ skuId, quantity }) => [skuId, quantity]))
        this.items = this.items.filter(item => {
          if (!quantities.has(item.skuId)) return true
          const remaining = item.quantity - quantities.get(item.skuId)
          if (remaining <= 0) return false
          item.quantity = remaining
          return true
        })
      })
    }
  }
})
