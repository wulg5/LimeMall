<template>
  <div class="page-container">
    <div class="chat-card">
      <!-- 左侧：会话列表 -->
      <aside class="side">
        <el-tabs v-model="tab" stretch class="side-tabs" @tab-change="handleTabChange">
          <el-tab-pane :label="`待接入 ${queue.length}`" name="queue" />
          <el-tab-pane :label="`我的会话 ${mine.length}`" name="mine" />
          <el-tab-pane label="历史记录" name="history" />
        </el-tabs>

        <div class="list-scroll">
          <!-- 待接入 / 我的会话 -->
          <template v-if="tab !== 'history'">
            <div v-for="session in currentList" :key="session.id" class="session-item"
                 :class="{ active: active && active.id === session.id }" @click="openSession(session)">
              <el-avatar :size="36" :src="session.memberAvatar || '/img/avatar.svg'" />
              <div class="s-info">
                <div class="s-name">
                  {{ session.memberNickname || session.memberUsername }}
                  <el-badge v-if="session.unreadCount > 0" :value="session.unreadCount" :max="99" class="s-badge" />
                </div>
                <div class="s-last">{{ session.lastContent || '（暂无消息）' }}</div>
              </div>
              <div class="s-time">{{ formatTime(session.lastMessageTime) }}</div>
            </div>
            <el-empty v-if="!currentList.length"
                      :description="tab === 'queue' ? '暂无待接入会话' : '你还没有接待中的会话'" :image-size="70" />
          </template>

          <!-- 历史记录 -->
          <template v-else>
            <div v-for="session in historyList" :key="session.id" class="session-item"
                 :class="{ active: active && active.id === session.id }" @click="openSession(session)">
              <el-avatar :size="36" :src="session.memberAvatar || '/img/avatar.svg'" />
              <div class="s-info">
                <div class="s-name">
                  {{ session.memberNickname || session.memberUsername }}
                  <el-tag size="small" :type="session.status === 0 ? 'success' : 'info'" class="s-tag">
                    {{ session.status === 0 ? '进行中' : '已结束' }}
                  </el-tag>
                </div>
                <div class="s-last">客服：{{ session.agentNickname || '未接入' }}</div>
              </div>
              <div class="s-time">{{ formatTime(session.lastMessageTime) }}</div>
            </div>
            <el-empty v-if="!historyList.length" description="暂无历史会话" :image-size="70" />
            <div class="history-pager">
              <el-pagination small layout="prev, pager, next" :total="historyTotal"
                             v-model:current-page="historyPage" :page-size="10" @current-change="loadHistory" />
            </div>
          </template>
        </div>
      </aside>

      <!-- 右侧：聊天区 -->
      <section class="main">
        <template v-if="active">
          <div class="main-header">
            <div class="mh-left">
              <span class="mh-name">{{ active.memberNickname || active.memberUsername }}</span>
              <level-badge :level="active.memberLevel || 1" />
              <span v-if="active.memberUserNo" class="mh-user-no">{{ active.memberUserNo }}</span>
              <el-tag size="small" :type="active.status === 0 ? 'success' : 'info'">
                {{ active.status === 0 ? '进行中' : '已结束' }}
              </el-tag>
              <span class="mh-agent">
                接待客服：{{ active.agentNickname || '待接入' }}
              </span>
            </div>
            <div class="mh-actions">
              <el-button v-if="canClaim" type="primary" size="small" :loading="claiming"
                         @click="handleClaim">接入会话</el-button>
              <el-button v-if="canEnd" type="danger" size="small" plain
                         @click="handleEnd">结束会话</el-button>
            </div>
          </div>

          <div ref="threadRef" class="thread">
            <div v-if="!messages.length" class="thread-empty">暂无消息</div>
            <div v-for="msg in messages" :key="msg.id" class="thread-msg"
                 :class="{ mine: msg.fromType === 2 }">
              <div class="bubble">{{ msg.content }}</div>
              <div class="time">{{ msg.createTime }}</div>
            </div>
          </div>

          <div class="reply-bar">
            <template v-if="canSend">
              <el-input v-model="draft" type="textarea" :rows="2" maxlength="500" resize="none"
                        placeholder="输入回复内容（Ctrl + Enter 发送）" @keydown.ctrl.enter="handleSend" />
              <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="handleSend">
                发送回复
              </el-button>
            </template>
            <div v-else class="readonly-tip">
              <el-icon><Lock /></el-icon>
              <span>{{ readonlyReason }}</span>
            </div>
          </div>
        </template>
        <el-empty v-else description="从左侧选择会话查看" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { chatClaim, chatEnd, chatHistory, chatMine, chatQueue, chatReply, chatSessionMessages } from '@/api/chat'
import { connectChatSocket, disconnectChatSocket } from '@/utils/chatSocket'
import LevelBadge from '@/components/LevelBadge.vue'

const userStore = useUserStore()

const tab = ref('queue')
const queue = ref([])
const mine = ref([])
const historyList = ref([])
const historyTotal = ref(0)
const historyPage = ref(1)

const active = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const claiming = ref(false)
const threadRef = ref()
let pollTimer = null

const myAdminId = computed(() => userStore.info?.id)
const hasReplyRole = computed(() => userStore.hasPerm('ums:chat:reply'))

const currentList = computed(() => (tab.value === 'queue' ? queue.value : mine.value))

const canClaim = computed(() =>
  hasReplyRole.value && active.value && active.value.status === 0 && !active.value.agentId)

const canEnd = computed(() =>
  hasReplyRole.value && active.value && active.value.status === 0
  && active.value.agentId === myAdminId.value)

const canSend = computed(() => canEnd.value)

const readonlyReason = computed(() => {
  if (!hasReplyRole.value) return '管理员/运营仅可查看会话记录，回复权限属于客服专员'
  if (!active.value) return ''
  if (active.value.status !== 0) return '会话已结束，无法继续回复'
  if (!active.value.agentId) return '该会话尚未接入，请先点击「接入会话」'
  return '仅该会话的接待客服可回复'
})

const formatTime = (time) => (time ? dayjs(time).format('MM-DD HH:mm') : '')

onMounted(async () => {
  await refreshLists()
  // 实时通道：收到 CHAT_UPDATE 立即刷新列表与当前会话；15 秒轮询兜底
  connectChatSocket(userStore.token, async () => {
    await refreshLists()
    if (active.value) {
      await loadMessages()
    }
  })
  pollTimer = setInterval(refresh, 15000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  disconnectChatSocket()
})

async function refreshLists() {
  const tasks = [chatQueue(), chatMine()]
  const [q, m] = await Promise.all(tasks)
  queue.value = q
  mine.value = m
  if (active.value) {
    // 同步 active 的最新状态（可能被其他端结束）
    const latest = [...q, ...m].find((s) => s.id === active.value.id)
    if (latest) {
      active.value = { ...active.value, ...latest }
    }
  }
  if (tab.value === 'history') {
    await loadHistory()
  }
}

async function refresh() {
  await refreshLists()
  if (active.value) {
    await loadMessages()
  }
}

function handleTabChange(name) {
  if (name === 'history') loadHistory()
}

async function loadHistory() {
  const data = await chatHistory({ pageNum: historyPage.value, pageSize: 10 })
  historyList.value = data.records
  historyTotal.value = data.total
}

function openSession(session) {
  active.value = { ...session }
  draft.value = ''
  loadMessages()
}

async function loadMessages() {
  if (!active.value) return
  messages.value = await chatSessionMessages(active.value.id)
  await nextTick()
  if (threadRef.value) {
    threadRef.value.scrollTop = threadRef.value.scrollHeight
  }
}

async function handleClaim() {
  claiming.value = true
  try {
    await chatClaim(active.value.id)
    ElMessage.success('已接入会话，现在可以回复会员了')
    await refreshLists()
    if (active.value) {
      active.value.agentId = myAdminId.value
      active.value.agentNickname = userStore.info?.nickname
    }
    tab.value = 'mine'
  } finally {
    claiming.value = false
  }
}

async function handleEnd() {
  try {
    await ElMessageBox.confirm('结束会话后会员再次发消息将创建新的咨询会话。确定结束吗？', '结束会话', { type: 'warning' })
    await chatEnd(active.value.id)
    ElMessage.success('会话已结束')
    if (active.value) {
      active.value.status = 1
    }
    await refreshLists()
  } catch (e) {
    /* 取消 */
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || !active.value) return
  sending.value = true
  try {
    await chatReply(active.value.id, content)
    draft.value = ''
    await refreshLists()
    await loadMessages()
  } finally {
    sending.value = false
  }
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

.side {
  width: 290px;
  border-right: 1px solid #eef2f0;
  display: flex;
  flex-direction: column;
}

.side-tabs {
  padding: 0 10px;
}

.list-scroll {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f8faf9;
  transition: background 0.15s;
}

.session-item:hover {
  background: #f7fbf9;
}

.session-item.active {
  background: #e7f8f2;
}

.s-info {
  flex: 1;
  min-width: 0;
}

.s-name {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.s-badge {
  margin-left: 2px;
}

.s-tag {
  transform: scale(0.9);
}

.s-last {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}

.s-time {
  font-size: 11px;
  color: #b6c0bb;
  align-self: flex-start;
  margin-top: 2px;
}

.history-pager {
  display: flex;
  justify-content: center;
  padding: 10px;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.main-header {
  padding: 12px 20px;
  border-bottom: 1px solid #f1f5f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mh-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mh-name {
  font-weight: 600;
  color: #111827;
}

.mh-agent {
  font-size: 12px;
  color: #9ca3af;
}

.mh-user-no {
  font-size: 12px;
  color: #9ca3af;
  font-family: Consolas, monospace;
}

.thread {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
  background: #f7faf8;
}

.thread-empty {
  text-align: center;
  color: #b6c0bb;
  font-size: 13px;
  margin-top: 40px;
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

.readonly-tip {
  flex: 1;
  height: 54px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 13px;
  background: #f8faf9;
  border-radius: 10px;
  padding: 0 16px;
}
</style>
