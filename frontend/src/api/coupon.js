import request from '@/utils/request'

/** 优惠券分页 */
export function couponPage(params) {
  return request.get('/admin/coupons', { params })
}

/** 新增优惠券 */
export function couponCreate(data) {
  return request.post('/admin/coupons', data)
}

/** 编辑优惠券 */
export function couponUpdate(data) {
  return request.put('/admin/coupons', data)
}

/** 上线/下线 */
export function couponPublish(id, status) {
  return request.put(`/admin/coupons/${id}/publish`, null, { params: { status } })
}

/** 领取记录 */
export function couponRecords(id, params) {
  return request.get(`/admin/coupons/${id}/records`, { params })
}
