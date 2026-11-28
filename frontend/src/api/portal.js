import request from '@/utils/request'

/* ---------------- 认证 ---------------- */

export function portalRegister(data) {
  return request.post('/portal/auth/register', data)
}

export function portalLogin(data) {
  return request.post('/portal/auth/login', data)
}

export function getMe() {
  return request.get('/portal/auth/me')
}

/* ---------------- 商品浏览（公开） ---------------- */

export function getHome() {
  return request.get('/portal/public/home')
}

export function getCategories() {
  return request.get('/portal/public/categories')
}

export function getProducts(params) {
  return request.get('/portal/public/products', { params })
}

export function getProductDetail(id) {
  return request.get(`/portal/public/products/${id}`)
}

/** 搜索联想 */
export function searchSuggest(keyword) {
  return request.get('/portal/public/search/suggest', { params: { keyword } })
}

/* ---------------- 内容（轮播/公告/秒杀） ---------------- */

export function getBanners() {
  return request.get('/portal/public/banners')
}

export function getNotices() {
  return request.get('/portal/public/notices')
}

export function getFlashSales() {
  return request.get('/portal/public/flash-sales')
}

/* ---------------- 优惠券 ---------------- */

export function getAvailableCoupons() {
  return request.get('/portal/coupons/available')
}

export function receiveCoupon(id) {
  return request.post(`/portal/coupons/${id}/receive`)
}

export function getMyCoupons() {
  return request.get('/portal/coupons/mine')
}

/** 积分商城：可兑换优惠券 */
export function getExchangeCoupons() {
  return request.get('/portal/coupons/exchange')
}

/** 积分兑换优惠券 */
export function exchangeCoupon(id, expectedPoints) {
  return request.post(`/portal/coupons/${id}/exchange`, undefined, { params: { expectedPoints } })
}

/* ---------------- 商品评价 ---------------- */

/** 商品公开评价 */
export function getProductComments(productId, params) {
  return request.get(`/portal/public/products/${productId}/comments`, { params })
}

/** 提交订单评价 */
export function submitComments(data) {
  return request.post('/portal/comments', data)
}

/* ---------------- 订单 ---------------- */

export function getOrderQuote(data) {
  return request.post('/portal/orders/quote', data)
}

export function createOrder(data) {
  return request.post('/portal/orders', data)
}

export function getCheckoutResult(requestId) {
  return request.get(`/portal/orders/checkout/${requestId}`)
}

export function getMyOrders(params) {
  return request.get('/portal/orders', { params })
}

export function getMyOrderDetail(id) {
  return request.get(`/portal/orders/${id}`)
}

export function payOrder(id) {
  return request.post(`/portal/orders/${id}/pay`, undefined, { params: { returnHost: window.location.hostname } })
}

export function cancelOrder(id) {
  return request.post(`/portal/orders/${id}/cancel`)
}

export function receiveOrder(id) {
  return request.post(`/portal/orders/${id}/receive`)
}

export function refundOrder(id, data) {
  return request.post(`/portal/orders/${id}/refund`, data)
}

/* ---------------- 个人中心 ---------------- */

export function updateMemberProfile(data) {
  return request.put('/portal/member/profile', data)
}

export function updateMemberPassword(data) {
  return request.put('/portal/member/password', data)
}

/* ---------------- 积分 ---------------- */

export function getPointsRecords(params) {
  return request.get('/portal/points/records', { params })
}

/* ---------------- 客服聊天 ---------------- */

export function getChatCurrent() {
  return request.get('/portal/chat/current')
}

export function getChatMessages(sessionId, beforeId) {
  return request.get(`/portal/chat/sessions/${sessionId}/messages`, { params: { beforeId } })
}

export function sendChatMessage(content) {
  return request.post('/portal/chat/messages', { content })
}
