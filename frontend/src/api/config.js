import request from '@/utils/request'

/** 支付配置视图（密钥脱敏预览） */
export function getPayConfig() {
  return request.get('/admin/pay-config')
}

/** 保存支付配置（密钥留空保持不变，保存后立即生效） */
export function savePayConfig(data) {
  return request.put('/admin/pay-config', data)
}
