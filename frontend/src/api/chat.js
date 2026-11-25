import request from '@/utils/request'

/** 待接入会话队列 */
export function chatQueue() {
  return request.get('/admin/chat/queue')
}

/** 我的接待中会话 */
export function chatMine() {
  return request.get('/admin/chat/mine')
}

/** 全部会话历史（只读分页） */
export function chatHistory(params) {
  return request.get('/admin/chat/sessions', { params })
}

/** 会话消息记录（接待客服查看时自动标记已读） */
export function chatSessionMessages(sessionId) {
  return request.get(`/admin/chat/sessions/${sessionId}/messages`)
}

/** 接入会话（一对一分派，客服专员权限） */
export function chatClaim(sessionId) {
  return request.post(`/admin/chat/sessions/${sessionId}/claim`)
}

/** 结束会话 */
export function chatEnd(sessionId) {
  return request.post(`/admin/chat/sessions/${sessionId}/end`)
}

/** 回复会员（仅接待客服本人） */
export function chatReply(sessionId, content) {
  return request.post(`/admin/chat/sessions/${sessionId}/messages`, { content })
}
