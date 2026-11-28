<template>
  <div>
    <!-- 悬浮客服按钮 -->
    <button ref="launcherRef" class="chat-fab" type="button" aria-controls="shop-chat-panel" :aria-expanded="visible" @click="togglePanel">
      <el-icon :size="22"><Service /></el-icon>
      <span class="fab-text">客服</span>
    </button>

    <!-- 聊天面板 -->
    <transition name="chat-pop">
      <section v-if="visible" id="shop-chat-panel" class="chat-panel" aria-label="在线客服" @keydown.esc.stop="closeFromControl">
        <div class="chat-header">
          <div>
            <div class="chat-title">在线客服</div>
            <div class="chat-sub">{{ agentSubTitle }}</div>
          </div>
          <button ref="closeRef" type="button" class="close-btn" aria-label="关闭在线客服" @click="closeFromControl"><el-icon><Close /></el-icon></button>
        </div>

        <div ref="listRef" class="chat-messages" role="log" aria-label="客服消息">
          <div v-if="!shopStore.token" class="chat-empty">
            <p>登录后即可与客服对话</p>
            <el-button type="primary" size="small" @click="goLogin">去登录</el-button>
          </div>
          <template v-else>
            <el-button v-if="hasOlder" link type="primary" :loading="loadingOlder" @click="loadOlderMessages">加载更早消息</el-button>
            <load-state v-if="chatLoading || chatError" :loading="chatLoading" :error="chatError" @retry="loadMessages" />
            <div v-else-if="!messages.length" class="chat-empty">
              <p>您好，这里是青柠商城客服，请问有什么可以帮您？</p>
            </div>
            <div v-for="msg in messages" :key="msg.id" class="chat-msg" :class="{ mine: msg.fromType === 1 }">
              <div class="bubble">{{ msg.content }}</div>
              <div class="time">{{ msg.createTime }}</div>
            </div>
          </template>
        </div>

        <div v-if="shopStore.token" class="chat-input">
          <el-input v-model="draft" aria-label="客服消息内容" placeholder="请输入您的问题，回车发送" maxlength="500"
                    @keyup.enter="handleSend" />
          <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="handleSend">
            发送
          </el-button>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Service } from '@element-plus/icons-vue'
import { getChatCurrent, getChatMessages, sendChatMessage } from '@/api/portal'
import { connectChatSocket, disconnectChatSocket } from '@/utils/chatSocket'
import { CHAT_PAGE_SIZE, mergeRecentMessages } from '@/utils/chatMessages'
import { useShopStore } from '@/stores/shop'

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()

const visible = ref(false)
const chatLoading = ref(false)
const chatError = ref(false)
const closeRef = ref()
const launcherRef = ref()
const messages = ref([])
const session = ref(null)
const hasOlder = ref(false)
const loadingOlder = ref(false)
const draft = ref('')
const sending = ref(false)
const listRef = ref()
let pollTimer = null
let latestMessageRequest = 0

/** 头部副标题：展示当前接待客服（一对一分派后由后端返回） */
const agentSubTitle = computed(() => {
  if (!shopStore.token) return '登录后即可咨询'
  if (!session.value) return '发送消息后为您分配客服'
  if (session.value.status === 1) return '会话已结束，发送消息将开启新咨询'
  if (session.value.agentNickname) return `客服 ${session.value.agentNickname} 为您服务`
  return '等待客服接入...'
})

function togglePanel() {
  if (!visible.value && !shopStore.token) {
    goLogin()
    return
  }
  visible.value = !visible.value
  if (visible.value) {
    chatLoading.value = true
    loadMessages()
    nextTick(() => closeRef.value?.focus())
    // 实时通道：服务端推送变更事件即刷新；同时保留低频轮询兜底
    connectChatSocket(shopStore.token, () => loadMessages())
    startPoll()
    document.addEventListener('visibilitychange', refreshWhenVisible)
  } else {
    closePanel()
  }
}

function closeFromControl() {
  closePanel()
  launcherRef.value?.focus()
}

function refreshWhenVisible() {
  if (!document.hidden) loadMessages()
}

function closePanel() {
  visible.value = false
  latestMessageRequest++
  document.removeEventListener('visibilitychange', refreshWhenVisible)
  stopPoll()
  disconnectChatSocket()
}

watch(() => shopStore.token, () => {
  closePanel()
  session.value = null
  messages.value = []
  hasOlder.value = false
  loadingOlder.value = false
  draft.value = ''
})

function goLogin() {
  router.push({ path: '/shop/login', query: { redirect: route.fullPath } })
}

async function loadMessages() {
  if (document.hidden || !visible.value || !shopStore.token) return
  const request = ++latestMessageRequest
  try {
    const data = await getChatCurrent()
    if (request !== latestMessageRequest || !visible.value) return
    chatError.value = false
    const changedSession = session.value?.id !== data.session?.id
    session.value = data.session || null
    if (changedSession) loadingOlder.value = false
    const received = data.messages || []
    const thread = listRef.value
    const wasAtBottom = !thread || thread.scrollHeight - thread.scrollTop - thread.clientHeight < 48
    const result = mergeRecentMessages(changedSession ? [] : messages.value, received)
    messages.value = result.messages
    if (result.reset) hasOlder.value = received.length === CHAT_PAGE_SIZE
    await nextTick()
    if (request === latestMessageRequest && listRef.value && (result.reset || wasAtBottom)) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  } catch {
    if (request === latestMessageRequest) chatError.value = true
  } finally {
    if (request === latestMessageRequest) chatLoading.value = false
  }
}

async function loadOlderMessages() {
  const sessionId = session.value?.id
  const token = shopStore.token
  const beforeId = messages.value[0]?.id
  if (document.hidden || !sessionId || !beforeId || loadingOlder.value || !hasOlder.value) return
  loadingOlder.value = true
  try {
    const received = await getChatMessages(sessionId, beforeId)
    if (!visible.value || shopStore.token !== token || session.value?.id !== sessionId
        || messages.value[0]?.id !== beforeId) return
    const thread = listRef.value
    const oldHeight = thread?.scrollHeight ?? 0
    messages.value = [...received, ...messages.value]
    hasOlder.value = received.length === CHAT_PAGE_SIZE
    await nextTick()
    if (thread && session.value?.id === sessionId) thread.scrollTop += thread.scrollHeight - oldHeight
  } finally {
    if (session.value?.id === sessionId) loadingOlder.value = false
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    await sendChatMessage(content)
    draft.value = ''
    await loadMessages()
  } catch {
    // 保留输入内容以便重发。
  } finally {
    sending.value = false
  }
}

function startPoll() {
  stopPoll()
  // 低频轮询兜底（WS 不可用时代替实时推送）
  pollTimer = setInterval(loadMessages, 15000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onBeforeUnmount(closePanel)
</script>

<style scoped>
.chat-fab {
  position: fixed;
  right: 28px;
  bottom: 96px;
  z-index: 1500;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: linear-gradient(135deg, #34d399, #059669);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.4);
  transition: transform 0.2s;
}

.chat-fab:hover {
  transform: scale(1.06);
}

.fab-text {
  font-size: 10px;
}

.chat-panel {
  position: fixed;
  right: 28px;
  bottom: 166px;
  z-index: 1501;
  width: 350px;
  height: 480px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(15, 46, 36, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: linear-gradient(120deg, #065f46, #10b981);
  color: #ffffff;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
}

.chat-sub {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 2px;
}

.close-btn {
  cursor: pointer;
  opacity: 0.85;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: #f7faf8;
}

.chat-empty {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  margin-top: 40px;
}

.chat-msg {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.chat-msg.mine {
  align-items: flex-end;
}

.bubble {
  max-width: 82%;
  background: #ffffff;
  padding: 9px 13px;
  border-radius: 12px;
  border-top-left-radius: 2px;
  font-size: 13px;
  color: #1f2937;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(15, 46, 36, 0.06);
}

.chat-msg.mine .bubble {
  background: #10b981;
  color: #ffffff;
  border-top-left-radius: 12px;
  border-top-right-radius: 2px;
}

.time {
  font-size: 10px;
  color: #b6c0bb;
  margin-top: 3px;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #eef2f0;
}

.chat-pop-enter-active,
.chat-pop-leave-active {
  transition: all 0.18s ease;
}

.chat-pop-enter-from,
.chat-pop-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.chat-fab { display: flex; align-items: center; justify-content: center; gap: 6px; width: auto; min-height: 44px; height: auto; border: 1px solid #ffffff80; border-radius: 9px; padding: 7px 12px; right: max(12px, env(safe-area-inset-right)); bottom: max(12px, env(safe-area-inset-bottom)); color: var(--paper); background: var(--pine); box-shadow: 0 3px 11px #17392e40; font: inherit; }
.chat-fab:hover { background: var(--action); transform: none; }
.chat-fab .fab-text { font-weight: 650; font-size: 13px; }
.chat-panel { width: min(360px, calc(100vw - 24px)); height: min(480px, calc(100dvh - 82px)); right: max(12px, env(safe-area-inset-right)); bottom: 64px; border: 1px solid var(--line); border-radius: var(--radius-panel); }
.chat-header { background: var(--pine); }
.close-btn { flex: 0 0 37px; align-self: center; min-height: 37px; display: grid; place-items: center; color: var(--paper); background: #ffffff18; border: 0; border-radius: 6px; font: inherit; cursor: pointer; }
.close-btn:hover { background: #ffffff33; }
.chat-messages { background: var(--page); }
.chat-msg.mine .bubble { color: var(--pine); background: var(--soft-green); }
.bubble { overflow-wrap: anywhere; }
</style>
