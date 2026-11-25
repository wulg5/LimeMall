import request from '@/utils/request'

/** 会员分页 */
export function memberPage(params) {
  return request.get('/admin/members', { params })
}

/** 启用/禁用会员 */
export function updateMemberStatus(id, status) {
  return request.put(`/admin/members/${id}/status`, null, { params: { status } })
}
