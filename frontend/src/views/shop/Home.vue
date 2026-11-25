<template>
  <div class="home">
    <!-- 促销信息条 -->
    <div class="promo-strip">
      <span>新人注册即领券</span><i></i>
      <span>全场满 99 包邮</span><i></i>
      <span>金卡 / 钻石会员全场免运费</span><i></i>
      <span>积分商城上线：100 积分 = 1 元</span>
    </div>

    <!-- 首屏三栏：分类导航 / 轮播 / 会员卡+公告 -->
    <div class="first-screen">
      <aside class="category-nav">
        <div class="nav-title">全部分类</div>
        <div v-for="category in categories" :key="category.id" class="cat-item">
          <router-link :to="`/shop/products?categoryId=${category.id}`" class="cat-main">
            {{ category.name }}<span class="arrow">›</span>
          </router-link>
          <div class="cat-sub">
            <router-link v-for="child in (category.children || []).slice(0, 4)" :key="child.id"
                         :to="`/shop/products?categoryId=${child.id}`">{{ child.name }}</router-link>
          </div>
        </div>
      </aside>

      <section class="banner-col">
        <el-carousel height="318px" :interval="5000" arrow="hover" indicator-position="none">
          <el-carousel-item v-for="banner in banners" :key="banner.title">
            <div class="banner" :style="{ background: banner.bg }" @click="$router.push(banner.link)">
              <div class="banner-copy">
                <div class="banner-tag">{{ banner.tag }}</div>
                <h2>{{ banner.title }}</h2>
                <p>{{ banner.subtitle }}</p>
                <span class="banner-btn">{{ banner.cta }}</span>
              </div>
              <div class="banner-art">
                <img :src="banner.img1" class="art art1" alt="" />
                <img :src="banner.img2" class="art art2" alt="" />
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>

        <div class="quick-entry">
          <router-link v-for="entry in quickEntries" :key="entry.text" :to="entry.link" class="entry">
            <span class="entry-icon" :style="{ background: entry.color }">{{ entry.short }}</span>
            <span>{{ entry.text }}</span>
          </router-link>
        </div>
      </section>

      <aside class="notice-col">
        <div class="member-panel">
          <template v-if="shopStore.token">
            <div class="m-row">
              <el-avatar :size="42" :src="shopStore.info?.avatar || '/img/avatar.svg'" />
              <div class="m-info">
                <div class="m-name">
                  {{ shopStore.info?.nickname }}
                  <level-badge :level="shopStore.info?.level || 1" />
                </div>
                <div class="m-points">积分 {{ shopStore.info?.points ?? 0 }}</div>
              </div>
            </div>
            <div class="m-actions">
              <el-button size="small" @click="$router.push('/shop/member')">会员中心</el-button>
              <el-button size="small" type="primary" @click="$router.push('/shop/points')">积分商城</el-button>
            </div>
          </template>
          <template v-else>
            <div class="m-guest">Hi，欢迎来到青柠商城</div>
            <div class="m-actions">
              <el-button size="small" @click="$router.push('/shop/login?mode=register')">注册</el-button>
              <el-button size="small" type="primary" @click="$router.push('/shop/login')">登录</el-button>
            </div>
          </template>
        </div>

        <div class="notice-panel">
          <div class="notice-title">商城公告</div>
          <ul>
            <li v-for="notice in notices" :key="notice">{{ notice }}</li>
          </ul>
        </div>
      </aside>
    </div>

    <!-- 限时特惠 -->
    <section class="floor">
      <div class="floor-head">
        <div class="floor-title">限时特惠 <span class="floor-sub">今日好价</span></div>
        <div class="floor-extra">距本场结束 <b class="clock">{{ countdown }}</b></div>
      </div>
      <div class="deal-grid">
        <div v-for="product in dealProducts" :key="product.id" class="deal-card"
             @click="$router.push(`/shop/product/${product.id}`)">
          <img :src="product.mainPic || '/img/logo.svg'" alt="" />
          <div class="deal-info">
            <div class="deal-name">{{ product.name }}</div>
            <div class="deal-price">
              <b>{{ money(product.price) }}</b>
              <del v-if="product.originalPrice">{{ money(product.originalPrice) }}</del>
            </div>
          </div>
          <span class="deal-off">省 {{ discountOf(product) }} 元</span>
        </div>
      </div>
    </section>

    <!-- 热销榜单 -->
    <section class="floor">
      <div class="floor-head">
        <div class="floor-title">热销榜单</div>
        <router-link class="floor-more" to="/shop/products?sort=sales">更多热销 ›</router-link>
      </div>
      <div class="grid">
        <product-card v-for="product in hotProducts" :key="product.id" :product="product" />
      </div>
    </section>

    <!-- 积分商城专区 -->
    <section class="floor points-floor">
      <div class="points-intro">
        <div class="pi-title">积分商城</div>
        <p class="pi-desc">
          购物得积分，100 积分 = 1 元，可兑换全场优惠券<br />
          金卡会员积分 1.2 倍 · 钻石会员 1.5 倍
        </p>
        <el-button type="primary" @click="$router.push('/shop/points')">去兑换</el-button>
      </div>
      <div class="points-coupons">
        <div v-for="coupon in pointsPreview" :key="coupon.id" class="pc" @click="$router.push('/shop/points')">
          <div class="pc-amount"><i>¥</i>{{ Number(coupon.amount) }}</div>
          <div class="pc-cond">{{ Number(coupon.minAmount) > 0 ? `满 ${Number(coupon.minAmount)} 元可用` : '无门槛' }}</div>
          <div class="pc-points">{{ coupon.pointsPrice }} 积分</div>
        </div>
      </div>
    </section>

    <!-- 新品上架 -->
    <section class="floor">
      <div class="floor-head">
        <div class="floor-title">新品上架</div>
        <router-link class="floor-more" to="/shop/products">查看全部 ›</router-link>
      </div>
      <div class="grid">
        <product-card v-for="product in newProducts" :key="product.id" :product="product" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getHome, getExchangeCoupons } from '@/api/portal'
import { useShopStore } from '@/stores/shop'
import { money } from '@/utils/format'
import ProductCard from '@/components/ProductCard.vue'
import LevelBadge from '@/components/LevelBadge.vue'

const shopStore = useShopStore()

const categories = ref([])
const hotProducts = ref([])
const newProducts = ref([])
const exchangeCoupons = ref([])
const nowTs = ref(Date.now())
let timer = null

const banners = [
  {
    tag: '数码焕新',
    title: '轻装出行 · 桌面焕新',
    subtitle: '充电器 / 阅读器 / 键鼠周边，精选好物满 99 包邮',
    cta: '立即选购',
    link: '/shop/products?categoryId=1',
    bg: 'linear-gradient(105deg, #0b3d33 0%, #0f766e 55%, #0d9488 100%)',
    img1: '/img/products/p02.svg',
    img2: '/img/products/p03.svg'
  },
  {
    tag: '积分商城',
    title: '积分当钱花',
    subtitle: '100 积分抵 1 元，全场优惠券持续兑换中',
    cta: '去兑换',
    link: '/shop/points',
    bg: 'linear-gradient(105deg, #7c2d12 0%, #b45309 55%, #d97706 100%)',
    img1: '/img/products/p18.svg',
    img2: '/img/products/p21.svg'
  },
  {
    tag: '会员权益',
    title: '金卡 / 钻石全场免运费',
    subtitle: '购物累计成长值，等级自动升级，积分加速获取',
    cta: '查看会员权益',
    link: '/shop/member',
    bg: 'linear-gradient(105deg, #1e1b4b 0%, #4338ca 55%, #6366f1 100%)',
    img1: '/img/products/p17.svg',
    img2: '/img/products/p26.svg'
  }
]

const quickEntries = [
  { short: '券', text: '领券中心', color: '#dc2626', link: '/shop/coupons' },
  { short: '积', text: '积分商城', color: '#d97706', link: '/shop/points' },
  { short: '单', text: '我的订单', color: '#0d9488', link: '/shop/orders' },
  { short: '会', text: '会员中心', color: '#4338ca', link: '/shop/member' }
]

const notices = [
  '全场满 99 元包邮，金卡及以上会员无门槛免运费',
  '支持 7 天无理由退款，退款审核 24 小时内处理',
  '客服在线 9:00 - 21:00，点击右下角悬浮窗咨询'
]

/** 限时特惠：从热销商品中挑选折扣力度最大的 4 件 */
const dealProducts = computed(() =>
  [...hotProducts.value]
    .filter((product) => Number(product.originalPrice) > Number(product.price))
    .sort((a, b) => discountOf(b) - discountOf(a))
    .slice(0, 4)
)

const pointsPreview = computed(() => {
  if (exchangeCoupons.value.length) {
    return exchangeCoupons.value.slice(0, 3)
  }
  return [
    { id: 'a', amount: 3, minAmount: 0, pointsPrice: 300 },
    { id: 'b', amount: 5, minAmount: 50, pointsPrice: 500 },
    { id: 'c', amount: 10, minAmount: 100, pointsPrice: 1000 }
  ]
})

/** 距当日 24:00 的倒计时 */
const countdown = computed(() => {
  const end = dayjs(nowTs.value).endOf('day')
  const diff = Math.max(0, end.valueOf() - nowTs.value)
  const hours = String(Math.floor(diff / 3600000)).padStart(2, '0')
  const minutes = String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0')
  const seconds = String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
})

function discountOf(product) {
  const original = Number(product.originalPrice || 0)
  const price = Number(product.price || 0)
  return original > price ? (original - price).toFixed(0) : 0
}

onMounted(async () => {
  const data = await getHome()
  categories.value = data.categories || []
  hotProducts.value = data.hotProducts || []
  newProducts.value = data.newProducts || []
  if (shopStore.token) {
    try {
      exchangeCoupons.value = await getExchangeCoupons()
    } catch (e) {
      exchangeCoupons.value = []
    }
  }
  timer = setInterval(() => { nowTs.value = Date.now() }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 顶部促销条 */
.promo-strip {
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 10px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12.5px;
  color: #6b7280;
  overflow-x: auto;
  white-space: nowrap;
}

.promo-strip i {
  width: 1px;
  height: 12px;
  background: #e5e7eb;
}

/* 首屏 */
.first-screen {
  display: grid;
  grid-template-columns: 212px 1fr 252px;
  gap: 14px;
}

.category-nav {
  background: #ffffff;
  border-radius: 12px;
  padding: 10px 0;
  border: 1px solid #eef2f0;
}

.nav-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  padding: 4px 16px 10px;
  border-bottom: 1px solid #f5f8f6;
}

.cat-item {
  padding: 7px 16px;
  transition: background 0.15s;
}

.cat-item:hover {
  background: #f6faf8;
}

.cat-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #1f2937;
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none;
}

.cat-main:hover {
  color: var(--el-color-primary);
}

.cat-main .arrow {
  color: #c3ccc8;
  font-size: 14px;
}

.cat-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 4px;
}

.cat-sub a {
  font-size: 12px;
  color: #9ca3af;
  text-decoration: none;
}

.cat-sub a:hover {
  color: var(--el-color-primary);
}

/* 轮播 */
.banner-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.banner {
  height: 100%;
  border-radius: 12px;
  padding: 36px 38px;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.banner-copy {
  position: relative;
  z-index: 2;
  max-width: 60%;
}

.banner-tag {
  display: inline-block;
  font-size: 12px;
  padding: 3px 10px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.banner h2 {
  font-size: 30px;
  margin: 0 0 12px;
  letter-spacing: 1px;
}

.banner p {
  font-size: 13.5px;
  opacity: 0.85;
  margin: 0 0 22px;
  line-height: 1.7;
}

.banner-btn {
  display: inline-block;
  background: #ffffff;
  color: #14532d;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 22px;
  border-radius: 8px;
  transition: transform 0.2s;
}

.banner:hover .banner-btn {
  transform: translateX(3px);
}

.banner-art {
  position: absolute;
  right: 26px;
  top: 0;
  bottom: 0;
  width: 280px;
}

.art {
  position: absolute;
  width: 150px;
  height: 150px;
  border-radius: 14px;
  border: 4px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.28);
}

.art1 {
  top: 42px;
  left: 10px;
  transform: rotate(-7deg);
}

.art2 {
  top: 108px;
  left: 118px;
  transform: rotate(6deg);
}

.quick-entry {
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 12px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  padding: 10px 6px;
}

.entry {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #374151;
  text-decoration: none;
  font-size: 13px;
  padding: 6px 0;
}

.entry:hover {
  color: var(--el-color-primary);
}

.entry-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 右侧 */
.notice-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-panel {
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 12px;
  padding: 16px;
}

.m-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.m-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 6px;
}

.m-points {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.m-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.m-actions .el-button {
  flex: 1;
}

.m-guest {
  font-size: 14px;
  color: #374151;
}

.notice-panel {
  flex: 1;
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.notice-title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 10px;
}

.notice-panel ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-panel li {
  font-size: 12.5px;
  color: #6b7280;
  line-height: 1.7;
  padding: 6px 0;
  border-bottom: 1px dashed #f1f5f3;
}

.notice-panel li:last-child {
  border-bottom: none;
}

/* 楼层 */
.floor {
  background: #ffffff;
  border: 1px solid #eef2f0;
  border-radius: 12px;
  padding: 18px 20px;
}

.floor-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
}

.floor-title {
  font-size: 19px;
  font-weight: 700;
  color: #111827;
}

.floor-sub {
  font-size: 12.5px;
  font-weight: 400;
  color: #9ca3af;
  margin-left: 10px;
}

.floor-extra {
  font-size: 12.5px;
  color: #9ca3af;
}

.clock {
  color: #dc2626;
  font-family: Consolas, monospace;
  font-size: 15px;
  margin-left: 4px;
}

.floor-more {
  font-size: 13px;
  color: #9ca3af;
  text-decoration: none;
}

.floor-more:hover {
  color: var(--el-color-primary);
}

/* 限时特惠 */
.deal-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.deal-card {
  position: relative;
  border: 1px solid #f1f5f3;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
}

.deal-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 6px 18px rgba(16, 185, 129, 0.12);
}

.deal-card img {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.deal-info {
  min-width: 0;
}

.deal-name {
  font-size: 13px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.deal-price {
  margin-top: 8px;
}

.deal-price b {
  color: #dc2626;
  font-size: 17px;
}

.deal-price del {
  color: #c3ccc8;
  font-size: 12px;
  margin-left: 6px;
}

.deal-off {
  position: absolute;
  top: 0;
  left: 0;
  background: #dc2626;
  color: #ffffff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 12px 0 10px 0;
}

/* 商品网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* 积分商城专区 */
.points-floor {
  display: flex;
  align-items: center;
  gap: 24px;
  background: linear-gradient(100deg, #fffbeb, #ffffff 55%);
}

.points-intro {
  flex: 1;
}

.pi-title {
  font-size: 21px;
  font-weight: 700;
  color: #92400e;
}

.pi-desc {
  font-size: 13px;
  color: #78716c;
  line-height: 1.8;
  margin: 10px 0 16px;
}

.points-coupons {
  display: flex;
  gap: 14px;
}

.pc {
  width: 132px;
  height: 96px;
  border: 1px dashed #f59e0b;
  background: #fffbeb;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
  transition: all 0.2s;
}

.pc:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(217, 119, 6, 0.18);
}

.pc-amount {
  font-size: 27px;
  font-weight: 700;
  color: #dc2626;
}

.pc-amount i {
  font-size: 14px;
  font-style: normal;
}

.pc-cond {
  font-size: 11.5px;
  color: #9ca3af;
}

.pc-points {
  font-size: 12px;
  color: #d97706;
  font-weight: 600;
}

@media (max-width: 1000px) {
  .first-screen {
    grid-template-columns: 1fr;
  }

  .category-nav,
  .notice-col {
    display: none;
  }

  .grid,
  .deal-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .points-floor {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
