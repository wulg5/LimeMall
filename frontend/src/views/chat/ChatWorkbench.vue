<template>
  <div class="page-container">
    <div class="chat-card">
      <!-- 会话列表 -->
      <aside class="conv-list">
        <div class="conv-header">会员会话（{{ conversations.length }}）</div>
        <div class="conv-scroll">
          <div v-for="conv in conversations" :key="conv.memberId" class="conv-item"
               :class="{ active: conv.memberId === activeId }" @click="selectConv(conv)">
            <el-avatar :size="38" :src="conv.avatar || '/img/avatar.svg'" />
            <div class="conv-info">
              <div class="conv-name">{{ conv.nickname || conv.username }}</div>
              <div class="conv-last">{{ conv.lastContent }}</div>
            </div>
            <div class="conv-meta">
              <div class="conv-time">{{ formatTime(conv.lastTime) }}</div>
              <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" :max="99" />
            </div>
          </div>
          <el-empty v-if="!conversations.length" description="暂无会员咨询" :image-size="70" />
        </div>
      </aside>

      <!-- 聊天区 -->
      <section class="chat-main">
        <template v-if="activeId">
          <div class="chat-main-header">
            <span>{{ activeConv?.nickname || activeConv?.username }}</span>
            <span class="hint">会员咨询 · 消息每 5 秒自动刷新</span>
          </div>
          <div ref="threadRef" class="thread">
            <div v-for="msg in messages" :key="msg.id" class="thread-msg" :class="{ mine: msg.fromType === 2 }">
              <div class="bubble">{{ msg.content }}</div>
              <div class="time">{{ msg.createTime }}</div>
            </div>
          </div>
          <div class="reply-bar">
            <el-input v-model="draft" type="textarea" :rows="2" maxlength="500" resize="none"
                      placeholder="输入回复内容（Ctrl + Enter 发送）" @keydown.ctrl.enter="handleSend" />
            <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="handleSend">
              发送回复
            </el-button>
          </div>
        </template>
        <el-empty v-else description="从左侧选择会话开始回复" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { chatConversations, chatMessages, chatReply } from '@/api/chat'

const conversations = ref([])
const messages = ref([])
const activeId = ref(null)
const draft = ref('')
const sending = ref(false)
const threadRef = ref()
let pollTimer = null

const activeConv = computed(() => conversations.value.find((conv) => conv.memberId === activeId.value))

onMounted(async () => {
  await loadConversations()
  pollTimer = setInterval(refresh, 5000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function loadConversations() {
  conversations.value = await chatConversations()
}

async function refresh() {
  await loadConversations()
  if (activeId.value) {
    await loadMessages()
  }
}

function selectConv(conv) {
  activeId.value = conv.memberId
  loadMessages()
}

async function loadMessages() {
  if (!activeId.value) return
  messages.value = await chatMessages(activeId.value)
  await nextTick()
  if (threadRef.value) {
    threadRef.value.scrollTop = threadRef.value.scrollHeight
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || !activeId.value) return
  sending.value = true
  try {
    await chatReply(activeId.value, content)
    draft.value = ''
    await refresh()
  } finally {
    sending.value = false
  }
}

function formatTime(time) {
  return time ? dayjs(time).format('MM-DD HH:mm') : ''
}
</script>

<style scoped>
.chat-card {
  display: flex;
  height: calc(100vh - 120px);
  background: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
}

.conv-list {
  width: 270px;
  border-right: 1px solid #eef2f0;
  display: flex;
  flex-direction: column;
}

.conv-header {
  padding: 16px;
  font-weight: 600;
  color: #111827;
  border-bottom: 1px solid #f1f5f3;
}

.conv-scroll {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f8faf9;
  transition: background 0.15s;
}

.conv-item:hover {
  background: #f7fbf9;
}

.conv-item.active {
  background: #e7f8f2;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.conv-last {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 130px;
}

.conv-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.conv-time {
  font-size: 11px;
  color: #b6c0bb;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-main-header {
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f3;
  font-weight: 600;
  color: #111827;
}

.chat-main-header .hint {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 400;
  margin-left: 10px;
}

.thread {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
  background: #f7faf8;
}

.thread-msg {
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.thread-msg.mine {
  align-items: flex-end;
}

.bubble {
  max-width: 60%;
  background: #ffffff;
  padding: 10px 14px;
  border-radius: 12px;
  border-top-left-radius: 2px;
  font-size: 13px;
  line-height: 1.7;
  color: #1f2937;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(15, 46, 36, 0.06);
}

.thread-msg.mine .bubble {
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

.reply-bar {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #eef2f0;
  align-items: flex-end;
}

.reply-bar .el-button {
  height: 54px;
}
</style>
