import request from '@/utils/request'

/** 订单分页 */
export function orderPage(params) {
  return request.get('/admin/orders', { params })
}

/** 订单详情 */
export function orderDetail(id) {
  return request.get(`/admin/orders/${id}`)
}

/** 订单发货 */
export function orderDeliver(id, data) {
  return request.post(`/admin/orders/${id}/deliver`, data)
}

/** 关闭订单 */
export function orderClose(id) {
  return request.put(`/admin/orders/${id}/close`)
}

/** 退款申请分页 */
export function returnPage(params) {
  return request.get('/admin/returns', { params })
}

/** 审核退款 */
export function returnHandle(id, data) {
  return request.put(`/admin/returns/${id}/handle`, data)
}

export function confirmReturnedStock(id) {
  return request.put(`/admin/returns/${id}/returned-stock`)
}
