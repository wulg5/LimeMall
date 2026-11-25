import request from '@/utils/request'

/** 顶部统计卡片 */
export function getSummary() {
  return request.get('/admin/dashboard/summary')
}

/** 销售趋势 */
export function getTrend(days = 30) {
  return request.get('/admin/dashboard/trend', { params: { days } })
}

/** 分类销售占比 */
export function getCategorySales() {
  return request.get('/admin/dashboard/category-sales')
}

/** 热销商品排行 */
export function getTopProducts(limit = 10) {
  return request.get('/admin/dashboard/top-products', { params: { limit } })
}

/** 订单状态分布 */
export function getOrderStatus() {
  return request.get('/admin/dashboard/order-status')
}

/** 最新订单 */
export function getLatestOrders(limit = 8) {
  return request.get('/admin/dashboard/latest-orders', { params: { limit } })
}
