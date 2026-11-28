<template>
  <div class="home">
    <section class="discovery" aria-label="商城导购">
      <nav class="categories" aria-label="商品分类">
        <h2><el-icon><Grid /></el-icon> 全部分类</h2>
        <div v-for="(category, index) in categories" :key="category.id" class="category">
          <router-link :to="{ path: '/shop/products', query: { categoryId: category.id } }" class="category-main">
            <el-icon><component :is="categoryIcons[index % categoryIcons.length]" /></el-icon>
            <span>{{ category.name }}</span><el-icon><ArrowRight /></el-icon>
          </router-link>
          <div class="category-sub">
            <router-link v-for="child in (category.children || []).slice(0, 4)" :key="child.id"
                         :to="{ path: '/shop/products', query: { categoryId: child.id } }">{{ child.name }}</router-link>
          </div>
        </div>
        <router-link class="category-all" to="/shop/products">浏览全部商品 <el-icon><ArrowRight /></el-icon></router-link>
      </nav>

      <div class="feature">
        <article v-if="featuredBanner" class="feature-story" :class="{ 'feature-without-image': !featuredImages.length }">
          <div class="feature-copy">
            <span v-if="featuredBanner.tag" class="feature-tag">{{ featuredBanner.tag }}</span>
            <h1><span v-for="(part, index) in titleLines" :key="index">{{ part }}</span></h1>
            <p>{{ featuredBanner.subtitle }}</p>
            <router-link v-if="featuredBanner.linkUrl" class="ui-link-button" :to="featuredBanner.linkUrl">
              {{ featuredBanner.ctaText || '查看活动' }} <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
          <div v-if="featuredImages.length" class="feature-media" :style="bannerAccent">
            <product-image :src="featuredImages[0]" class="feature-primary" alt="" eager />
            <product-image v-if="featuredImages[1]" :src="featuredImages[1]" class="feature-secondary" alt="" eager />
          </div>
        </article>
        <nav v-if="banners.length > 1" class="feature-tabs" aria-label="商城活动">
          <button v-for="(banner, index) in banners" :key="banner.id || banner.title" type="button"
                  :aria-pressed="activeBanner === index" @click="activeBanner = index">{{ banner.tag || banner.title }}</button>
        </nav>
      </div>
    </section>

    <div v-if="homeError" class="home-feedback" role="alert">
      <span>首页商品加载失败，请重试。</span><el-button :loading="loading" @click="loadHome">重新加载</el-button>
    </div>
    <el-skeleton v-else-if="loading" :rows="4" animated class="home-feedback" />

    <section v-else class="section" aria-labelledby="offers-heading">
      <header class="section-head">
        <div>
          <h2 id="offers-heading"><el-icon><Lightning /></el-icon> {{ activeDeals.length ? '限时秒杀' : '限时特惠' }}</h2>
          <p v-if="activeDeals.length" class="countdown">最近一场结束 <b>{{ dealCountdown }}</b></p>
          <p v-else>查看当前在售商品的优惠价</p>
        </div>
        <router-link class="section-link" to="/shop/products">查看全部商品 <el-icon><ArrowRight /></el-icon></router-link>
      </header>
      <div v-if="dealItems.length" class="deals">
        <router-link v-for="item in dealItems" :key="item.key" class="deal"
                     :to="{ path: '/shop/product/' + item.productId, query: { skuId: item.skuId } }">
          <product-image :src="item.pic" class="deal-image" alt="" />
          <div class="deal-content">
            <span class="offer-label">{{ item.flash ? '秒杀' : '省 ' + item.save + ' 元' }}</span>
            <h3>{{ item.name }}</h3>
            <div class="deal-price"><strong>{{ money(item.price) }}</strong><del v-if="item.originalPrice > item.price">{{ money(item.originalPrice) }}</del></div>
            <span v-if="item.flash" class="deal-time">结束时间 {{ dayjs(item.endTime).format('MM-DD HH:mm') }}</span>
          </div>
          <span class="deal-cta">去选购 <el-icon><ArrowRight /></el-icon></span>
        </router-link>
      </div>
      <p v-else class="home-feedback">当前暂无优惠商品。</p>
    </section>

    <section v-if="!loading && !homeError" class="section" aria-labelledby="hot-heading">
      <header class="section-head">
        <h2 id="hot-heading">热销商品</h2>
        <router-link class="section-link" to="/shop/products?sort=sales">查看热销 <el-icon><ArrowRight /></el-icon></router-link>
      </header>
      <div v-if="hotProducts.length" class="product-grid">
        <product-card v-for="product in hotProducts" :key="product.id" :product="product" />
      </div>
      <p v-else class="home-feedback">暂无热销商品。</p>
    </section>

    <section class="points-section" aria-labelledby="points-heading">
      <div>
        <h2 id="points-heading">积分商城</h2>
        <p>100 积分 = 1 元，可兑换积分专享券。</p>
        <router-link class="ui-link-button" to="/shop/points">去兑换 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div v-if="pointsPreview.length" class="points-coupons">
        <router-link v-for="coupon in pointsPreview" :key="coupon.id" class="coupon" to="/shop/points">
          <strong>{{ coupon.type === 1 ? '¥' : '' }}{{ Number(coupon.amount) }}{{ coupon.type === 2 ? '折' : '' }}</strong>
          <span>{{ Number(coupon.minAmount) > 0 ? '满 ' + Number(coupon.minAmount) + ' 元可用' : '无门槛' }}</span>
          <b>{{ coupon.pointsPrice }} 积分</b>
        </router-link>
      </div>
    </section>

    <section v-if="!loading && !homeError" class="section" aria-labelledby="new-heading">
      <header class="section-head">
        <h2 id="new-heading">新品上架</h2>
        <router-link class="section-link" to="/shop/products">查看全部 <el-icon><ArrowRight /></el-icon></router-link>
      </header>
      <div v-if="newProducts.length" class="product-grid">
        <product-card v-for="product in newProducts" :key="product.id" :product="product" />
      </div>
      <p v-else class="home-feedback">暂无新品。</p>
    </section>

    <section v-if="notices.length" class="shop-notices" aria-labelledby="notices-heading">
      <h2 id="notices-heading">商城公告</h2>
      <ul><li v-for="(notice, index) in notices" :key="index">{{ notice }}</li></ul>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRight, Apple, Box, Cpu, Goods, Grid, Lightning, MagicStick } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getBanners, getExchangeCoupons, getFlashSales, getHome, getNotices } from '@/api/portal'
import { money } from '@/utils/format'
import { PRODUCT_PLACEHOLDER, productImageSource } from '@/utils/productImages'
import ProductCard from '@/components/ProductCard.vue'
import ProductImage from '@/components/ProductImage.vue'

const categories = ref([])
const hotProducts = ref([])
const newProducts = ref([])
const exchangeCoupons = ref([])
const flashSales = ref([])
const notices = ref([])
const banners = ref([])
const activeBanner = ref(0)
const loading = ref(true)
const homeError = ref(false)
const nowTs = ref(Date.now())
const categoryIcons = [Cpu, Goods, Apple, MagicStick, Box]
let timer

const fallbackBanner = {
  title: '青柠商城',
  subtitle: '按分类探索数码、服饰、美食与居家日用。',
  ctaText: '浏览商品',
  linkUrl: '/shop/products'
}

const featuredBanner = computed(() => banners.value[activeBanner.value] || fallbackBanner)
const titleLines = computed(() => (featuredBanner.value.title || '青柠商城').split(' · '))
const featuredImages = computed(() => [...new Set([featuredBanner.value.image1, featuredBanner.value.image2])]
  .filter(source => source && productImageSource(source) !== PRODUCT_PLACEHOLDER))
const bannerAccent = computed(() => {
  const safeColor = value => /^#[0-9a-f]{3}([0-9a-f]{3})?$/i.test(value || '')
  const { bgFrom, bgTo } = featuredBanner.value
  return {
    '--banner-from': safeColor(bgFrom) ? bgFrom : 'var(--pine)',
    '--banner-to': safeColor(bgTo) ? bgTo : 'var(--action)'
  }
})
const activeDeals = computed(() => flashSales.value
  .filter(flash => dayjs(flash.endTime).valueOf() > nowTs.value)
  .slice(0, 4))

const dealItems = computed(() => {
  if (activeDeals.value.length) {
    return activeDeals.value.map(flash => ({
      key: 'f-' + flash.id,
      flash: true,
      productId: flash.productId,
      skuId: flash.skuId,
      name: flash.productName,
      pic: flash.mainPic,
      price: Number(flash.flashPrice),
      originalPrice: Number(flash.originalPrice),
      endTime: flash.endTime
    }))
  }
  return [...hotProducts.value]
    .filter(product => product.stock > 0 && discountOf(product) > 0)
    .sort((first, second) => discountOf(second) - discountOf(first))
    .slice(0, 4)
    .map(product => ({
      key: 'p-' + product.id,
      productId: product.id,
      name: product.name,
      pic: product.mainPic,
      price: offerPrice(product),
      originalPrice: Number(product.originalPrice),
      save: discountOf(product)
    }))
})

const dealCountdown = computed(() => {
  const end = Math.min(...activeDeals.value.map(flash => dayjs(flash.endTime).valueOf()))
  const seconds = Math.max(0, Math.floor((end - nowTs.value) / 1000))
  return [Math.floor(seconds / 3600), Math.floor(seconds / 60) % 60, seconds % 60]
    .map(value => String(value).padStart(2, '0')).join(':')
})

const pointsPreview = computed(() => exchangeCoupons.value.slice(0, 3))

function offerPrice(product) {
  return Number(product.displayPrice ?? product.price ?? 0)
}

function discountOf(product) {
  return Math.max(0, Number(product.originalPrice || 0) - offerPrice(product)).toFixed(0)
}

async function loadHome() {
  loading.value = true
  homeError.value = false
  const [home, banner, notice, flash, coupons] = await Promise.allSettled([
    getHome(), getBanners(), getNotices(), getFlashSales(), getExchangeCoupons()
  ])
  if (home.status === 'fulfilled') {
    categories.value = home.value?.categories || []
    hotProducts.value = home.value?.hotProducts || []
    newProducts.value = home.value?.newProducts || []
  } else {
    homeError.value = true
  }
  if (banner.status === 'fulfilled') {
    banners.value = banner.value || []
    activeBanner.value = 0
  }
  if (notice.status === 'fulfilled') notices.value = (notice.value || []).map(item => item.content)
  if (flash.status === 'fulfilled') flashSales.value = flash.value || []
  if (coupons.status === 'fulfilled') exchangeCoupons.value = coupons.value || []
  loading.value = false
}

onMounted(() => {
  loadHome()
  timer = window.setInterval(() => { nowTs.value = Date.now() }, 1000)
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.home { display: grid; gap: clamp(31px, 3.4vw, 49px); }
.discovery { display: grid; grid-template-columns: 230px minmax(0, 1fr); gap: 16px; }
.categories { display: flex; flex-direction: column; background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-panel); padding: 18px 16px 12px; }
.categories h2 { display: flex; align-items: center; gap: 8px; margin: 0 0 9px; padding: 0 6px 13px; font-size: 16px; border-bottom: 1px solid var(--line); }
.category { padding: 7px 6px; border-radius: 8px; }
.category:hover, .category:focus-within { background: var(--page); }
.category-main, .category-all { display: flex; align-items: center; gap: 8px; font-weight: 650; text-decoration: none; color: var(--pine); }
.category-main span { flex: 1; }
.category-main > .el-icon:first-child { color: var(--action); font-size: 17px; }
.category-main > .el-icon:last-child { color: var(--muted); font-size: 12px; }
.category-sub { margin: 4px 0 0 25px; display: flex; flex-wrap: wrap; gap: 3px 8px; }
.category-sub a { font-size: 12px; color: var(--muted); text-decoration: none; }
.category-sub a:hover { text-decoration: underline; color: var(--pine); }
.category-all { margin-top: auto; font-size: 13px; padding: 10px 6px 2px; }
.feature { min-width: 0; display: flex; flex-direction: column; border: 1px solid var(--line); border-radius: var(--radius-panel); overflow: clip; background: var(--paper); }
.feature-story { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, .9fr); flex: 1; min-height: 316px; background: var(--lime); }
.feature-copy { padding: clamp(25px, 3.1vw, 44px) clamp(26px, 4.2vw, 62px); align-self: center; min-width: 0; }
.feature-tag { display: inline-block; font-weight: 700; color: var(--pine); padding-bottom: 4px; border-bottom: 2px solid currentColor; font-size: 13px; }
.feature h1 { font-size: clamp(34px, 4.2vw, 57px); line-height: 1.08; margin: 15px 0 12px; color: var(--pine); letter-spacing: -.055em; text-wrap: balance; }
.feature h1 span { display: block; }
.feature p { line-height: 1.65; margin: 0 0 24px; max-width: 32ch; color: var(--pine); }
.feature-media { min-width: 0; position: relative; background: linear-gradient(120deg, var(--banner-from), var(--banner-to)); }
.feature-primary { width: 100%; height: 100%; position: absolute; object-fit: cover; object-position: 72% center; }
.feature-secondary { position: absolute; right: 4%; bottom: 5%; width: 32%; height: 38%; object-fit: cover; border: 3px solid var(--banner-to); border-radius: 8px; }
.feature-media:has(.feature-secondary) .feature-primary { height: 99%; width: 98%; }
.feature-without-image { grid-template-columns: 1fr; }
.feature-tabs { display: flex; gap: 20px; padding: 0 22px; min-height: 47px; overflow-x: auto; }
.feature-tabs button { background: none; border: 0; border-bottom: 3px solid transparent; padding: 8px 1px 5px; color: var(--muted); font: inherit; font-weight: 600; cursor: pointer; white-space: nowrap; }
.feature-tabs button[aria-pressed='true'] { color: var(--pine); border-bottom-color: var(--action); }
.section-head { display: flex; align-items: end; justify-content: space-between; gap: 12px; padding: 0 0 15px; margin-bottom: 15px; border-bottom: 1px solid var(--line); }
.section-head h2, .points-section h2 { margin: 0; font-size: clamp(23px, 2.6vw, 33px); color: var(--pine); letter-spacing: -.035em; }
.section-head h2 .el-icon { color: var(--price); vertical-align: -.1em; }
.section-head p { margin: 5px 0 0; color: var(--muted); font-size: 13px; }
.countdown b { margin-left: 5px; color: var(--price); font-variant-numeric: tabular-nums; }
.section-link, .deal-cta { display: inline-flex; gap: 5px; align-items: center; color: var(--action); font-weight: 650; font-size: 13px; text-decoration: none; white-space: nowrap; }
.section-link:hover, .deal:hover .deal-cta { text-decoration: underline; }
.deals { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.deal { display: grid; grid-template-columns: 108px 1fr auto; align-items: center; gap: 15px; min-width: 0; padding: 10px 14px 10px 10px; background: var(--paper); border-radius: var(--radius-panel); border: 1px solid var(--line); color: inherit; text-decoration: none; }
.deal:hover { border-color: var(--action); }
.deal-image { width: 108px; height: 108px; object-fit: cover; border-radius: 7px; background: var(--page); }
.deal h3 { margin: 4px 0; font-size: 15px; line-height: 1.45; overflow-wrap: anywhere; }
.offer-label { font-size: 12px; font-weight: 700; color: var(--price); }
.deal-price { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 9px; }
.deal-price strong { color: var(--price); font-size: 22px; font-variant-numeric: tabular-nums; }
.deal-price del, .deal-time { color: var(--muted); font-size: 12px; }
.product-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.points-section { display: grid; grid-template-columns: minmax(200px, .8fr) minmax(0, 1.2fr); align-items: center; gap: 18px; background: var(--soft-green); color: var(--pine); padding: clamp(18px, 4vw, 42px); border-radius: var(--radius-panel); }
.points-section p { margin: 9px 0 16px; }
.points-coupons { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.coupon { display: grid; gap: 4px; border-radius: 8px; padding: 12px; color: var(--pine); background: var(--paper); text-decoration: none; border: 1px solid var(--line); }
.coupon:hover { border-color: var(--action); }
.coupon strong { font-size: 23px; color: var(--price); }
.coupon span, .coupon b { font-size: 12px; }
.shop-notices { border-top: 1px solid var(--line); color: var(--muted); font-size: 13px; }
.shop-notices h2 { font-size: 16px; color: var(--pine); }
.shop-notices ul { display: flex; flex-wrap: wrap; column-gap: 30px; padding-left: 18px; }
.home-feedback { padding: 20px; margin: 0; border: 1px solid var(--line); border-radius: var(--radius-panel); background: var(--paper); color: var(--muted); }
.home-feedback .el-button { margin-left: 12px; }
@media (max-width: 980px) {
  .deal { grid-template-columns: 80px 1fr; }
  .deal-image { width: 80px; height: 88px; }
  .deal-cta { grid-column: 2; }
}
@media (max-width: 760px) {
  .discovery { grid-template-columns: minmax(0, 1fr); gap: 10px; }
  .categories { grid-row: 2; flex-direction: row; gap: 8px; overflow-x: auto; padding: 8px; }
  .categories h2, .category-sub, .category-main > .el-icon:last-child { display: none; }
  .category { padding: 0; }
  .category-main, .category-all { gap: 5px; padding: 9px 8px; min-height: 42px; white-space: nowrap; }
  .category-all { margin: 0; }
  .feature-story { min-height: 230px; }
  .feature-copy { padding: 20px 14px 20px 18px; }
  .feature h1 { font-size: clamp(24px, 6.5vw, 42px); margin-top: 10px; }
  .feature p { font-size: 12px; margin-bottom: 15px; }
  .feature-tabs { padding-inline: 12px; gap: 15px; }
  .feature-secondary { width: 42%; height: 32%; }
  .section-head { align-items: start; }
  .deals { grid-template-columns: 1fr; }
  .deal { grid-template-columns: 72px minmax(0, 1fr) auto; gap: 8px; }
  .deal-image { width: 72px; height: 78px; }
  .deal-cta { grid-column: auto; }
  .deal-cta .el-icon { display: none; }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .points-section { grid-template-columns: 1fr; }
}
</style>
