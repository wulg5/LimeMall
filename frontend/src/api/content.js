import request from '@/utils/request'

/** 后台内容管理（轮播/公告） */
export function bannerList() {
  return request.get('/admin/contents/banners')
}

export function bannerCreate(data) {
  return request.post('/admin/contents/banners', data)
}

export function bannerUpdate(data) {
  return request.put('/admin/contents/banners', data)
}

export function bannerDelete(id) {
  return request.delete(`/admin/contents/banners/${id}`)
}

export function noticeList() {
  return request.get('/admin/contents/notices')
}

export function noticeCreate(data) {
  return request.post('/admin/contents/notices', data)
}

export function noticeUpdate(data) {
  return request.put('/admin/contents/notices', data)
}

export function noticeDelete(id) {
  return request.delete(`/admin/contents/notices/${id}`)
}

/** 后台秒杀管理 */
export function flashPage(params) {
  return request.get('/admin/flash-sales', { params })
}

export function flashCreate(data) {
  return request.post('/admin/flash-sales', data)
}

export function flashUpdate(data) {
  return request.put('/admin/flash-sales', data)
}

export function flashDelete(id) {
  return request.delete(`/admin/flash-sales/${id}`)
}

/** 后台操作日志 */
export function logPage(params) {
  return request.get('/admin/logs', { params })
}
