import request from '@/utils/request'

/* ---------------- 管理员 ---------------- */

export function adminPage(params) {
  return request.get('/admin/admins', { params })
}

export function adminCreate(data) {
  return request.post('/admin/admins', data)
}

export function adminUpdate(data) {
  return request.put('/admin/admins', data)
}

export function adminResetPassword(id) {
  return request.put(`/admin/admins/${id}/reset-password`)
}

export function adminDelete(id) {
  return request.delete(`/admin/admins/${id}`)
}

/* ---------------- 角色 ---------------- */

export function roleList() {
  return request.get('/admin/roles')
}

export function roleCreate(data) {
  return request.post('/admin/roles', data)
}

export function roleUpdate(data) {
  return request.put('/admin/roles', data)
}

export function roleDelete(id) {
  return request.delete(`/admin/roles/${id}`)
}

export function roleMenuIds(id) {
  return request.get(`/admin/roles/${id}/menus`)
}

export function roleAssignMenus(id, menuIds) {
  return request.put(`/admin/roles/${id}/menus`, { menuIds })
}

/* ---------------- 菜单 ---------------- */

export function menuTree() {
  return request.get('/admin/menus/tree')
}
