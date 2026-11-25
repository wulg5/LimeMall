import request from '@/utils/request'

/** 评价分页（管理端） */
export function commentPage(params) {
  return request.get('/admin/comments', { params })
}

/** 删除评价 */
export function commentDelete(id) {
  return request.delete(`/admin/comments/${id}`)
}
