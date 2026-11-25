<template>
  <div>
    <!-- 悬浮客服按钮 -->
    <div class="chat-fab" title="在线客服" @click="togglePanel">
      <el-icon :size="22"><Service /></el-icon>
      <span class="fab-text">客服</span>
    </div>

    <!-- 聊天面板 -->
    <transition name="chat-pop">
      <div v-if="visible" class="chat-panel">
        <div class="chat-header">
          <div>
            <div class="chat-title">在线客服</div>
            <div class="chat-sub">{{ agentSubTitle }}</div>
          </div>
          <el-icon class="close-btn" @click="visible = false"><Close /></el-icon>
        </div>

        <div ref="listRef" class="chat-messages">
          <div v-if="!shopStore.token" class="chat-empty">
            <p>登录后即可与客服对话</p>
            <el-button type="primary" size="small" @click="goLogin">去登录</el-button>
          </div>
          <template v-else>
            <div v-if="!messages.length" class="chat-empty">
              <p>您好，这里是青柠商城客服，请问有什么可以帮您？</p>
            </div>
            <div v-for="msg in messages" :key="msg.id" class="chat-msg" :class="{ mine: msg.fromType === 1 }">
              <div class="bubble">{{ msg.content }}</div>
              <div class="time">{{ msg.createTime }}</div>
            </div>
          </template>
        </div>

        <div v-if="shopStore.token" class="chat-input">
          <el-input v-model="draft" placeholder="请输入您的问题，回车发送" maxlength="500"
                    @keyup.enter="handleSend" />
          <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="handleSend">
            发送
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Service } from '@element-plus/icons-vue'
import { getChatCurrent, sendChatMessage } from '@/api/portal'
import { useShopStore } from '@/stores/shop'

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()

const visible = ref(false)
const messages = ref([])
const session = ref(null)
const draft = ref('')
const sending = ref(false)
const listRef = ref()
let pollTimer = null

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
    loadMessages()
    startPoll()
  } else {
    stopPoll()
  }
}

function goLogin() {
  router.push({ path: '/shop/login', query: { redirect: route.fullPath } })
}

async function loadMessages() {
  if (!shopStore.token) return
  try {
    const data = await getChatCurrent()
    session.value = data.session || null
    messages.value = data.messages || []
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  } catch (e) {
    /* 登录态失效由拦截器统一处理 */
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    await sendChatMessage(content)
    draft.value = ''
    await loadMessages()
  } finally {
    sending.value = false
  }
}

function startPoll() {
  stopPoll()
  // 轮询拉取客服回复（5 秒），课设环境下无 WebSocket 依赖、Nginx 下零配置
  pollTimer = setInterval(loadMessages, 5000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onBeforeUnmount(stopPoll)
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
</style>
