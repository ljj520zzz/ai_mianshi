<template>
  <div class="love-master-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">ai面试官</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>
    
    <div class="main-layout">
      <aside class="history-sidebar">
        <div class="history-header">
          <span class="history-title">历史对话</span>
          <button class="new-chat-btn" @click="createNewChat">+ 新对话</button>
        </div>
        <ul class="history-list">
          <li
            v-for="item in chatHistory"
            :key="item.id"
            class="history-item"
            :class="{ active: item.id === chatId }"
            @click="switchChat(item)"
          >
            <div class="history-item-content">
              <span class="history-item-title">{{ item.title }}</span>
              <span class="history-item-time">{{ formatHistoryTime(item.createTime) }}</span>
            </div>
            <button class="history-delete-btn" @click.stop="deleteHistory(item)" title="删除">删除</button>
          </li>
        </ul>
      </aside>
      
      <div class="chat-panel">
        <div class="chat-area">
          <div class="chat-room-wrapper">
          <ChatRoom 
            :messages="messages" 
            :connection-status="connectionStatus"
            ai-type="interview"
            @send-message="sendMessage"
          />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithLoveApp } from '../api'

// 设置页面标题和元数据
useHead({
  title: 'ai面试官 - AI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'ai面试官是AI超级智能体应用平台的智能面试辅导，帮你准备面试、模拟面试、解答面试问题'
    },
    {
      name: 'keywords',
      content: 'ai面试官,面试辅导,面试准备,AI聊天,面试问题,AI智能体'
    }
  ]
})

const HISTORY_KEY = 'interview_chat_history'
const ACTIVE_CHAT_KEY = 'interview_active_chat_id'

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const chatHistory = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

const loadHistory = () => {
  try {
    const data = localStorage.getItem(HISTORY_KEY)
    chatHistory.value = data ? JSON.parse(data) : []
  } catch (e) {
    chatHistory.value = []
  }
}

const saveHistory = () => {
  if (!chatId.value) return
  const idx = chatHistory.value.findIndex(h => h.id === chatId.value)
  const firstUserMsg = messages.value.find(m => m.isUser)?.content
  const title = firstUserMsg ? (firstUserMsg.slice(0, 18) + (firstUserMsg.length > 18 ? '...' : '')) : '新对话'
  const item = {
    id: chatId.value,
    title,
    messages: JSON.parse(JSON.stringify(messages.value)),
    createTime: idx >= 0 ? chatHistory.value[idx].createTime : Date.now()
  }
  if (idx >= 0) {
    chatHistory.value[idx] = item
  } else {
    chatHistory.value.unshift(item)
  }
  localStorage.setItem(HISTORY_KEY, JSON.stringify(chatHistory.value))
  sessionStorage.setItem(ACTIVE_CHAT_KEY, chatId.value)
}

const generateChatId = () => {
  return 'interview_' + Math.random().toString(36).substring(2, 10)
}

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

const createNewChat = () => {
  if (eventSource) eventSource.close()
  saveHistory()
  chatId.value = generateChatId()
  messages.value = []
  addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
  const item = { id: chatId.value, title: '新对话', messages: [...messages.value], createTime: Date.now() }
  chatHistory.value.unshift(item)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(chatHistory.value))
  sessionStorage.setItem(ACTIVE_CHAT_KEY, chatId.value)
}

const switchChat = (item) => {
  if (item.id === chatId.value) return
  if (eventSource) eventSource.close()
  saveHistory()
  chatId.value = item.id
  messages.value = item.messages?.length ? [...item.messages] : []
  if (!messages.value.length) {
    addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
  }
  sessionStorage.setItem(ACTIVE_CHAT_KEY, chatId.value)
}

const deleteHistory = (item) => {
  const idx = chatHistory.value.findIndex(h => h.id === item.id)
  if (idx < 0) return
  chatHistory.value.splice(idx, 1)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(chatHistory.value))
  if (item.id === chatId.value) {
    if (eventSource) eventSource.close()
    if (chatHistory.value.length > 0) {
      const target = chatHistory.value[0]
      chatId.value = target.id
      messages.value = target.messages?.length ? [...target.messages] : []
      if (!messages.value.length) {
        addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
      }
      sessionStorage.setItem(ACTIVE_CHAT_KEY, chatId.value)
    } else {
      createNewChat()
    }
  }
}

const formatHistoryTime = (ts) => {
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff < 604800000) return d.toLocaleDateString('zh-CN', { weekday: 'short' })
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const sendMessage = (message) => {
  addMessage(message, true)
  
  // 连接SSE
  if (eventSource) {
    eventSource.close()
  }
  
  // 创建一个空的AI回复消息
  const aiMessageIndex = messages.value.length
  addMessage('', false)
  
  connectionStatus.value = 'connecting'
  eventSource = chatWithLoveApp(message, chatId.value)
  
  // 监听SSE消息
  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      // 更新最新的AI消息内容，而不是创建新消息
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }
    
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
      saveHistory()
    }
  }
  
  // 监听SSE错误
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

// 返回主页
const goBack = () => {
  router.push('/')
}

onMounted(() => {
  loadHistory()
  const activeId = sessionStorage.getItem(ACTIVE_CHAT_KEY)
  const exists = activeId && chatHistory.value.some(h => h.id === activeId)
  if (exists) {
    const item = chatHistory.value.find(h => h.id === activeId)
    chatId.value = item.id
    messages.value = item.messages?.length ? [...item.messages] : []
    if (!messages.value.length) {
      addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
    }
  } else {
    chatId.value = generateChatId()
    addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
    const item = { id: chatId.value, title: '新对话', messages: JSON.parse(JSON.stringify(messages.value)), createTime: Date.now() }
    chatHistory.value.unshift(item)
    saveHistory()
  }
  window.addEventListener('beforeunload', saveHistory)
})

// 组件销毁前关闭SSE连接
onBeforeUnmount(() => {
  saveHistory()
  if (eventSource) {
    eventSource.close()
  }
  window.removeEventListener('beforeunload', saveHistory)
})
</script>

<style scoped>
.love-master-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background: radial-gradient(120% 120% at 0% 0%, #2f3a57 0%, #111827 48%, #0b1220 100%);
  font-size: 16px;
  color: #e5e7eb;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(17, 24, 39, 0.56);
  color: #f9fafb;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.28);
  flex-shrink: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  color: #93c5fd;
  transition: opacity 0.2s;
}

.back-button:hover {
  opacity: 0.85;
}

.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  letter-spacing: -0.02em;
}

.chat-id {
  font-size: 14px;
  color: #94a3b8;
}

.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.history-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: rgba(15, 23, 42, 0.46);
  border-right: 1px solid rgba(148, 163, 184, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.history-header {
  padding: 18px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.history-title {
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
}

.new-chat-btn {
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 500;
  background: rgba(59, 130, 246, 0.16);
  border: 1px solid rgba(96, 165, 250, 0.5);
  color: #bfdbfe;
  cursor: pointer;
  border-radius: 999px;
  transition: background 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.new-chat-btn:hover {
  background: rgba(59, 130, 246, 0.24);
  border-color: rgba(147, 197, 253, 0.8);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 10px 10px 12px;
}

.history-item {
  padding: 14px 14px;
  cursor: pointer;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-left: 3px solid transparent;
  border-radius: 14px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.68) 0%, rgba(15, 23, 42, 0.6) 100%);
  box-shadow: 0 10px 24px rgba(2, 6, 23, 0.26);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.history-item:hover {
  background: linear-gradient(180deg, rgba(51, 65, 85, 0.78) 0%, rgba(30, 41, 59, 0.7) 100%);
  border-color: rgba(147, 197, 253, 0.52);
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(2, 6, 23, 0.34);
}

.history-item.active {
  background: linear-gradient(180deg, rgba(30, 64, 175, 0.34) 0%, rgba(30, 58, 138, 0.24) 100%);
  border-left-color: #60a5fa;
  border-color: rgba(96, 165, 250, 0.62);
  box-shadow: 0 14px 30px rgba(30, 64, 175, 0.35);
}

.history-item-content {
  flex: 1;
  min-width: 0;
}

.history-item-title {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-time {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.history-delete-btn {
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  color: #fecaca;
  background: rgba(127, 29, 29, 0.36);
  border: 1px solid rgba(248, 113, 113, 0.46);
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.2s;
}

.history-delete-btn:hover {
  color: #ffffff;
  background: rgba(220, 38, 38, 0.9);
  border-color: rgba(252, 165, 165, 0.85);
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: rgba(15, 23, 42, 0.18);
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.34) 0%, rgba(15, 23, 42, 0.2) 100%);
}

.chat-room-wrapper {
  flex: 1;
  min-height: 0;
  display: flex;
}

@media (max-width: 768px) {
  .history-sidebar {
    width: 220px;
  }
  
  .header {
    padding: 12px 16px;
  }
  
  .title {
    font-size: 21px;
  }
  
  .chat-id {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .main-layout {
    flex-direction: column;
  }
  
  .history-sidebar {
    width: 100%;
    max-height: 150px;
    border-right: none;
    border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  }
  
  .history-list {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 8px;
    flex-wrap: nowrap;
  }
  
  .history-item {
    flex-shrink: 0;
    min-width: 160px;
    border-left: none;
    border-bottom: 3px solid transparent;
  }
  
  .history-item.active {
    border-left: none;
    border-bottom-color: #1a73e8;
  }
  
  .header {
    padding: 10px 12px;
  }
  
  .back-button {
    font-size: 14px;
  }
  
  .title {
    font-size: 16px;
  }
  
  .chat-id {
    display: none;
  }
}
</style> 