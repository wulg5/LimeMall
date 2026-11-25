import request from '@/utils/request'

/** 客服工作台：会话列表 */
export function chatConversations() {
  return request.get('/admin/chat/conversations')
}

/** 客服工作台：某会员消息记录（拉取即标记已读） */
export function chatMessages(memberId) {
  return request.get(`/admin/chat/conversations/${memberId}/messages`)
}

/** 客服工作台：回复会员 */
export function chatReply(memberId, content) {
  return request.post(`/admin/chat/conversations/${memberId}/messages`, { content })
}
