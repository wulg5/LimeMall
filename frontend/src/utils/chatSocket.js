/**
 * 客服实时通道（WebSocket）轻量客户端。
 *
 * - 服务端只推送「CHAT_UPDATE」失效事件，具体数据仍走 REST 拉取（简单、幂等）；
 * - 断线自动重连（5 秒退避），页面卸载或主动断开时停止；
 * - 业务侧同时保留低频轮询兜底（代理/网络不支持 WS 时功能不受影响）。
 */
let socket = null
let currentToken = ''
let listeners = new Set()
let reconnectTimer = null
let manuallyClosed = false

export function connectChatSocket(token, onEvent) {
  if (!token) return
  if (onEvent) listeners.add(onEvent)
  if (socket && currentToken === token) return
  disconnectChatSocket(false)
  manuallyClosed = false
  currentToken = token
  open()
}

function open() {
  if (!currentToken || manuallyClosed) return
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  try {
    socket = new WebSocket(`${protocol}://${location.host}/ws/chat?token=${encodeURIComponent(currentToken)}`)
  } catch (e) {
    scheduleReconnect()
    return
  }
  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      listeners.forEach((fn) => {
        try {
          fn(data)
        } catch (e) {
          /* 单个监听器异常不影响其它 */
        }
      })
    } catch (e) {
      /* 忽略非 JSON 帧 */
    }
  }
  socket.onclose = () => {
    socket = null
    if (!manuallyClosed) {
      scheduleReconnect()
    }
  }
  socket.onerror = () => {
    /* 由 onclose 统一处理重连 */
  }
}

function scheduleReconnect() {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(open, 5000)
}

/** 断开连接；keepListeners=true 时保留监听器以便重新连接 */
export function disconnectChatSocket(clearListeners = true) {
  manuallyClosed = true
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (socket) {
    try {
      socket.close()
    } catch (e) {
      /* ignore */
    }
    socket = null
  }
  if (clearListeners) {
    listeners = new Set()
  }
  currentToken = ''
}
