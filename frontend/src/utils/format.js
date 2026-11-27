/**
 * 通用格式化工具。
 */

/** 金额格式化：399 → ¥399.00 */
export function money(value) {
  const num = Number(value ?? 0)
  return '¥' + num.toFixed(2)
}

/** 订单状态字典 */
export const ORDER_STATUS = {
  0: { text: '待付款', type: 'info' },
  1: { text: '待发货', type: 'warning' },
  2: { text: '已发货', type: 'primary' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已关闭', type: 'danger' }
}

/** 退款状态字典 */
export const RETURN_STATUS = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '历史审核通过', type: 'info' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '退款处理中', type: 'warning' },
  4: { text: '已退款', type: 'success' },
  5: { text: '待退货入库', type: 'warning' }
}

export function isRefundActive(status) {
  return status === 0 || status === 3 || status === 5
}

/** 会员等级字典 */
export const MEMBER_LEVEL = {
  1: { text: '普通会员', type: 'info' },
  2: { text: '银卡会员', type: 'primary' },
  3: { text: '金卡会员', type: 'warning' },
  4: { text: '钻石会员', type: 'danger' }
}

/** 优惠券状态（0未使用 1已使用 2已过期） */
export const COUPON_MEMBER_STATUS = {
  0: { text: '未使用', type: 'warning' },
  1: { text: '已使用', type: 'success' },
  2: { text: '已过期', type: 'info' },
  3: { text: '暂不可用', type: 'info' }
}

/**
 * SKU 规格 JSON 与文本互转。
 * 存储格式：[{"key":"颜色","value":"云雾白"},{"key":"尺码","value":"M"}]
 * 编辑文本：颜色:云雾白;尺码:M
 */
export function specsToText(specsJson) {
  if (!specsJson) return ''
  try {
    const arr = JSON.parse(specsJson)
    return arr.map((item) => `${item.key}:${item.value}`).join(';')
  } catch (e) {
    return specsJson
  }
}

export function textToSpecs(text) {
  if (!text) return '[]'
  const arr = text
    .split(';')
    .map((pair) => pair.trim())
    .filter(Boolean)
    .map((pair) => {
      const index = pair.indexOf(':')
      if (index === -1) {
        return { key: '规格', value: pair }
      }
      return { key: pair.slice(0, index).trim(), value: pair.slice(index + 1).trim() }
    })
  return JSON.stringify(arr)
}
