import request from '@/utils/request'

/** 商品分页 */
export function productPage(params) {
  return request.get('/admin/products', { params })
}

/** 商品详情（含SKU） */
export function productDetail(id) {
  return request.get(`/admin/products/${id}`)
}

/** 新增商品 */
export function productCreate(data) {
  return request.post('/admin/products', data)
}

/** 编辑商品 */
export function productUpdate(data) {
  return request.put('/admin/products', data)
}

/** 上架/下架 */
export function productPublish(id, status) {
  return request.put(`/admin/products/${id}/publish`, null, { params: { status } })
}

/** 调整 SKU 库存 */
export function updateSkuStock(skuId, stock) {
  return request.put(`/admin/products/skus/${skuId}/stock`, null, { params: { stock } })
}

/** 删除商品 */
export function productDelete(id) {
  return request.delete(`/admin/products/${id}`)
}

/* ---------------- 品牌 ---------------- */

export function brandPage(params) {
  return request.get('/admin/brands', { params })
}

export function brandOptions() {
  return request.get('/admin/brands/options')
}

export function brandCreate(data) {
  return request.post('/admin/brands', data)
}

export function brandUpdate(data) {
  return request.put('/admin/brands', data)
}

export function brandDelete(id) {
  return request.delete(`/admin/brands/${id}`)
}

/* ---------------- 分类 ---------------- */

export function categoryTree() {
  return request.get('/admin/categories/tree')
}

export function categoryCreate(data) {
  return request.post('/admin/categories', data)
}

export function categoryUpdate(data) {
  return request.put('/admin/categories', data)
}

export function categoryDelete(id) {
  return request.delete(`/admin/categories/${id}`)
}
