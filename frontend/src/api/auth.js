import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request.post('/admin/auth/login', data)
}

/** 当前登录管理员信息（角色/权限/菜单） */
export function getInfo() {
  return request.get('/admin/auth/info')
}

/** 退出登录 */
export function logout() {
  return request.post('/admin/auth/logout')
}

/** 修改密码 */
export function updatePassword(data) {
  return request.put('/admin/auth/password', data)
}
