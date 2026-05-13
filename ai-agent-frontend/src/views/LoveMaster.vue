<template>
  <div class="love-master-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">ai面试官</h1>
      <div class="user-area">
        <template v-if="currentUser">
          <span v-if="memberActive" class="member-badge">会员</span>
          <span class="user-name">{{ currentUser.nickname || currentUser.username }}</span>
          <button v-if="paymentEnabled" type="button" class="auth-btn member-pay-btn" @click="goMembership">开通会员</button>
          <button class="auth-btn" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <button class="auth-btn login-entry-btn" @click="openLogin">登录</button>
          <button class="auth-btn register-entry-btn" @click="openRegister">注册</button>
        </template>
      </div>
    </div>
    
    <div class="main-layout">
      <aside class="history-sidebar">
        <div class="history-header">
          <span class="history-title">历史对话</span>
          <button class="new-chat-btn" @click="onCreateNewChatClick">+ 新对话</button>
        </div>
        <ul class="history-list">
          <li
            v-for="item in chatHistory"
            :key="item.sessionCode"
            class="history-item"
            :class="{ active: item.sessionCode === chatId }"
            @click="switchChat(item)"
          >
            <div class="history-item-content">
              <span class="history-item-title">{{ item.title }}</span>
              <span class="history-item-time">{{ formatHistoryTime(item.updatedAt) }}</span>
            </div>
            <button class="history-delete-btn" @click.stop="onDeleteHistoryClick(item)" aria-label="删除历史对话">×</button>
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
            :session-code="chatId"
            :user-id="currentUser?.userId || ''"
            :draft-key="`love_master_${currentUser?.userId || 'guest'}_${chatId || 'new'}`"
            @send-message="onSendMessageClick"
          />
          </div>
        </div>
      </div>
    </div>

    <div v-if="showLogin" class="auth-mask">
      <div class="auth-shell auth-shell-login">
        <button class="auth-close-x" @click="showLogin = false" aria-label="关闭">✕</button>
        <div class="auth-glow auth-glow-a"></div>
        <div class="auth-glow auth-glow-b"></div>
        <div class="auth-left">
          <div>
            <div class="auth-left-tag">INTERVIEW AI</div>
            <div class="auth-left-title">欢迎回来</div>
            <div class="auth-left-subtitle">登录后继续你的面试冲刺计划，生成专属训练轨迹。</div>
          </div>
          <div class="auth-fun">
            <div class="fun-bubble">智能追问</div>
            <div class="fun-bubble">即时反馈</div>
            <div class="fun-bubble">岗位定制</div>
          </div>
        </div>
        <div class="auth-right">
          <div class="auth-tabs">
            <button class="tab-item active" type="button">密码登录</button>
            <button class="tab-item" type="button" @click="switchToRegister">注册账号</button>
          </div>
          <div class="auth-mascot" :class="{ 'cover-eyes': loginCoverEyes }" aria-hidden="true">
            <div class="mascot-face">
              <span class="mascot-eye left"></span>
              <span class="mascot-eye right"></span>
              <span class="mascot-mouth"></span>
            </div>
            <span class="mascot-hand hand-left"></span>
            <span class="mascot-hand hand-right"></span>
          </div>
          <div class="auth-form">
            <div class="auth-input-wrap">
              <span class="auth-input-label">账号</span>
              <input v-model.trim="loginForm.username" class="auth-input" placeholder="请输入账号" />
            </div>
            <div class="auth-input-wrap">
              <span class="auth-input-label">密码</span>
            <input
              v-model.trim="loginForm.password"
              type="password"
              class="auth-input"
              placeholder="请输入密码"
              @focus="onPasswordFocus('login')"
              @blur="onPasswordBlur('login')"
            />
            </div>
            <div class="auth-actions">
              <button class="auth-btn auth-primary" @click="onLoginClick">登录</button>
              <button class="auth-btn auth-secondary" @click="switchToRegister">去注册</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showRegister" class="auth-mask">
      <div class="auth-shell auth-shell-register">
        <button class="auth-close-x" @click="showRegister = false" aria-label="关闭">✕</button>
        <div class="auth-glow auth-glow-a"></div>
        <div class="auth-glow auth-glow-b"></div>
        <div class="auth-left auth-left-register">
          <div>
            <div class="auth-left-tag">GET STARTED</div>
            <div class="auth-left-title">加入我们</div>
            <div class="auth-left-subtitle">注册账号，开启你的 AI 面试训练与成长档案。</div>
          </div>
          <div class="auth-fun">
            <div class="fun-bubble">模拟面试</div>
            <div class="fun-bubble">能力评估</div>
            <div class="fun-bubble">报告导出</div>
          </div>
        </div>
        <div class="auth-right">
          <div class="auth-tabs">
            <button class="tab-item" type="button" @click="switchToLogin">密码登录</button>
            <button class="tab-item active" type="button">注册账号</button>
          </div>
          <div class="auth-mascot" :class="{ 'cover-eyes': registerCoverEyes }" aria-hidden="true">
            <div class="mascot-face">
              <span class="mascot-eye left"></span>
              <span class="mascot-eye right"></span>
              <span class="mascot-mouth"></span>
            </div>
            <span class="mascot-hand hand-left"></span>
            <span class="mascot-hand hand-right"></span>
          </div>
          <div class="auth-form">
            <div class="auth-input-wrap">
              <span class="auth-input-label">账号</span>
              <input v-model.trim="registerForm.username" class="auth-input" placeholder="请设置账号" />
            </div>
            <div class="auth-input-wrap">
              <span class="auth-input-label">昵称（可选）</span>
              <input v-model.trim="registerForm.nickname" class="auth-input" placeholder="请输入昵称" />
            </div>
            <div class="auth-input-wrap">
              <span class="auth-input-label">密码</span>
            <input
              v-model.trim="registerForm.password"
              type="password"
              class="auth-input"
              placeholder="请设置密码"
              @focus="onPasswordFocus('register')"
              @blur="onPasswordBlur('register')"
            />
            </div>
            <div class="password-strength" :class="passwordStrength.className">
              <div class="strength-track">
                <span class="strength-fill" :style="{ width: `${passwordStrength.level * 33.33}%` }"></span>
              </div>
              <span class="strength-text">密码强度：{{ passwordStrength.text }}</span>
            </div>
            <div class="auth-input-wrap">
              <span class="auth-input-label">确认密码</span>
              <input
                v-model.trim="registerForm.confirmPassword"
                type="password"
                class="auth-input"
                placeholder="请再次输入密码"
                @focus="onPasswordFocus('register')"
                @blur="onPasswordBlur('register')"
              />
            </div>
            <div class="auth-actions">
              <button class="auth-btn auth-primary" @click="onRegisterClick">注册</button>
              <button class="auth-btn auth-secondary" @click="switchToLogin">去登录</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="toast.show" class="page-toast" :class="`toast-${toast.type}`">{{ toast.text }}</div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatStream, sessionPage, messagePage, login, register, getCurrentUser, setCurrentUser, clearCurrentUser, membershipCheckout, membershipStatus } from '../api'
import { debounceAction } from '../utils/debounce'

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

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const chatHistory = ref([])
const connectionStatus = ref('disconnected')
const currentUser = ref(getCurrentUser())
const showLogin = ref(false)
const showRegister = ref(false)
const loginCoverEyes = ref(false)
const registerCoverEyes = ref(false)
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', confirmPassword: '', nickname: '' })
const toast = ref({ show: false, text: '', type: 'success' })
const memberActive = ref(false)
const paymentEnabled = import.meta.env.VITE_PAYMENT_ENABLED === 'true'
const passwordStrength = computed(() => {
  const password = registerForm.value.password || ''
  if (!password) return { level: 0, text: '未设置', className: '' }
  let score = 0
  if (password.length >= 8) score += 1
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1
  if (/\d/.test(password)) score += 1
  if (/[^A-Za-z0-9]/.test(password)) score += 1
  if (score <= 1) return { level: 1, text: '弱', className: 'strength-weak' }
  if (score <= 3) return { level: 2, text: '中', className: 'strength-medium' }
  return { level: 3, text: '强', className: 'strength-strong' }
})

let toastTimer = null
let eventSource = null
const sessionStoragePrefix = 'interview_session_code_'
const sessionHiddenPrefix = 'interview_hidden_session_codes_'

const generateChatId = () => {
  return 'interview_' + Math.random().toString(36).substring(2, 10)
}

const getSessionStorageKey = () => {
  const userId = currentUser.value?.userId
  if (!userId) return ''
  return `${sessionStoragePrefix}${userId}`
}

const getHiddenSessionStorageKey = () => {
  const userId = currentUser.value?.userId
  if (!userId) return ''
  return `${sessionHiddenPrefix}${userId}`
}

const getHiddenSessionCodes = () => {
  const key = getHiddenSessionStorageKey()
  if (!key) return []
  try {
    const raw = localStorage.getItem(key)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch (e) {
    return []
  }
}

const saveHiddenSessionCodes = (codes) => {
  const key = getHiddenSessionStorageKey()
  if (!key) return
  localStorage.setItem(key, JSON.stringify(codes))
}

const saveCurrentSessionCode = (sessionCode) => {
  const key = getSessionStorageKey()
  if (!key) return
  if (!sessionCode) {
    localStorage.removeItem(key)
    return
  }
  localStorage.setItem(key, sessionCode)
}

const loadSavedSessionCode = () => {
  const key = getSessionStorageKey()
  if (!key) return ''
  return localStorage.getItem(key) || ''
}

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

const createNewChat = () => {
  chatId.value = generateChatId()
  saveCurrentSessionCode(chatId.value)
  messages.value = []
  addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
}

const loadSessions = async () => {
  if (!currentUser.value) {
    chatHistory.value = []
    return
  }
  const res = await sessionPage({ userId: currentUser.value.userId, pageNum: 1, pageSize: 50 })
  if (res.code !== '200') return
  chatHistory.value = (res.data?.records || []).map(i => ({
    sessionCode: i.sessionCode,
    title: i.title || '新对话',
    updatedAt: i.updatedAt ? new Date(i.updatedAt).getTime() : Date.now()
  }))
  const hiddenCodes = getHiddenSessionCodes()
  if (hiddenCodes.length) {
    const hiddenSet = new Set(hiddenCodes)
    chatHistory.value = chatHistory.value.filter(i => !hiddenSet.has(i.sessionCode))
  }
  if (!chatId.value && chatHistory.value.length > 0) {
    const savedSessionCode = loadSavedSessionCode()
    const targetSession = chatHistory.value.find(i => i.sessionCode === savedSessionCode) || chatHistory.value[0]
    chatId.value = targetSession.sessionCode
    saveCurrentSessionCode(chatId.value)
    await loadMessages(chatId.value)
    return
  }
  if (!chatId.value && chatHistory.value.length === 0) {
    createNewChat()
  }
}

const loadMessages = async sessionCode => {
  const res = await messagePage({ sessionCode, pageNum: 1, pageSize: 200 })
  if (res.code !== '200') return
  messages.value = (res.data?.records || []).map(i => ({
    content: i.content,
    isUser: i.roleType === 'user',
    time: i.createdAt ? new Date(i.createdAt).getTime() : Date.now()
  }))
  if (!messages.value.length) {
    addMessage('欢迎来到ai面试官，请告诉我你要准备的岗位或面试问题，我会帮你模拟面试、解答疑问。', false)
  }
}

const switchChat = async (item) => {
  if (item.sessionCode === chatId.value) return
  chatId.value = item.sessionCode
  saveCurrentSessionCode(chatId.value)
  await loadMessages(chatId.value)
}

const onDeleteHistoryClick = async (item) => {
  if (!item?.sessionCode) return
  const hiddenCodes = getHiddenSessionCodes()
  if (!hiddenCodes.includes(item.sessionCode)) {
    hiddenCodes.push(item.sessionCode)
    saveHiddenSessionCodes(hiddenCodes)
  }
  chatHistory.value = chatHistory.value.filter(i => i.sessionCode !== item.sessionCode)
  if (chatId.value === item.sessionCode) {
    if (chatHistory.value.length > 0) {
      chatId.value = chatHistory.value[0].sessionCode
      saveCurrentSessionCode(chatId.value)
      await loadMessages(chatId.value)
    } else {
      createNewChat()
    }
  }
  showToast('已删除历史对话', 'success')
}

const formatHistoryTime = (ts) => {
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff < 604800000) return d.toLocaleDateString('zh-CN', { weekday: 'short' })
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const sendMessage = async (message) => {
  if (!currentUser.value) {
    showLogin.value = true
    return
  }
  addMessage(message, true)
  connectionStatus.value = 'connecting'
  addMessage('', false)
  const aiMessageIndex = messages.value.length - 1
  if (eventSource) {
    eventSource.close()
  }
  const currentSessionCode = chatId.value || generateChatId()
  chatId.value = currentSessionCode
  saveCurrentSessionCode(currentSessionCode)
  eventSource = chatStream(currentUser.value.userId, currentSessionCode, message)
  eventSource.onmessage = async (event) => {
    const data = event.data
    if (!data) {
      return
    }
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
      await loadSessions()
      return
    }
    if (aiMessageIndex < messages.value.length) {
      messages.value[aiMessageIndex].content += data
    }
  }
  eventSource.onerror = () => {
    connectionStatus.value = 'error'
    if (eventSource) {
      eventSource.close()
    }
  }
}

const loadMembershipStatus = async () => {
  if (!currentUser.value?.userId) {
    memberActive.value = false
    return
  }
  try {
    const res = await membershipStatus(currentUser.value.userId)
    if (res.code !== '200') return
    memberActive.value = !!res.data?.memberActive
  } catch (e) {
    memberActive.value = false
  }
}

const goMembership = async () => {
  if (!currentUser.value) {
    showLogin.value = true
    return
  }
  try {
    const res = await membershipCheckout({ userId: currentUser.value.userId })
    if (res.code !== '200') {
      showToast(res.msg || '无法发起支付', 'error')
      return
    }
    if (res.data?.url) {
      window.location.href = res.data.url
    }
  } catch (e) {
    showToast('网络异常，请稍后重试', 'error')
  }
}

const doLogin = async () => {
  const res = await login({
    username: loginForm.value.username,
    password: loginForm.value.password
  })
  if (res.code !== '200') {
    alert(res.msg || '登录失败')
    connectionStatus.value = 'error'
    return
  }
  setCurrentUser(res.data)
  currentUser.value = res.data
  showLogin.value = false
  showRegister.value = false
  connectionStatus.value = 'disconnected'
  showToast('登录成功', 'success')
  await loadSessions()
  await loadMembershipStatus()
}

const doRegister = async () => {
  if (!registerForm.value.confirmPassword) {
    showToast('请再次输入密码', 'error')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    showToast('两次输入的密码不一致', 'error')
    return
  }
  const res = await register({
    username: registerForm.value.username,
    password: registerForm.value.password,
    nickname: registerForm.value.nickname
  })
  if (res.code !== '200') {
    alert(res.msg || '注册失败')
    return
  }
  loginForm.value.username = registerForm.value.username
  loginForm.value.password = registerForm.value.password
  showToast('注册成功', 'success')
  showRegister.value = false
  showLogin.value = true
  await doLogin()
}

const handleLogout = () => {
  saveCurrentSessionCode('')
  clearCurrentUser()
  currentUser.value = null
  memberActive.value = false
  chatHistory.value = []
  createNewChat()
}

// 返回主页
const goBack = () => {
  router.push('/')
}

const openLogin = () => {
  showRegister.value = false
  showLogin.value = true
}

const openRegister = () => {
  showLogin.value = false
  showRegister.value = true
  registerForm.value.nickname = ''
}

const switchToRegister = () => {
  showLogin.value = false
  showRegister.value = true
  loginCoverEyes.value = false
  registerForm.value.confirmPassword = ''
  registerForm.value.nickname = ''
}

const switchToLogin = () => {
  showRegister.value = false
  showLogin.value = true
  registerCoverEyes.value = false
  registerForm.value.confirmPassword = ''
  registerForm.value.nickname = ''
}

const onPasswordFocus = (type) => {
  if (type === 'login') loginCoverEyes.value = true
  if (type === 'register') registerCoverEyes.value = true
}

const onPasswordBlur = (type) => {
  if (type === 'login') loginCoverEyes.value = false
  if (type === 'register') registerCoverEyes.value = false
}

const showToast = (text, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, text, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 1800)
}

const onCreateNewChatClick = debounceAction(async () => {
  createNewChat()
}, 500)

const onSendMessageClick = debounceAction(sendMessage, 700)
const onLoginClick = debounceAction(doLogin, 700)
const onRegisterClick = debounceAction(doRegister, 700)

onMounted(async () => {
  if (currentUser.value) {
    await loadSessions()
    await loadMembershipStatus()
    return
  }
  createNewChat()
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
})

</script>

<style scoped>
.love-master-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  width: 100%;
  background: radial-gradient(1200px 520px at 0% 0%, #dbeafe 0%, transparent 65%),
    radial-gradient(1000px 420px at 100% 0%, #dcfce7 0%, transparent 70%), #f6f8fc;
  font-size: 16px;
  color: #0f172a;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.78);
  color: #0f172a;
  border-bottom: 1px solid #e2e8f0;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.08);
  flex-shrink: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  color: #2563eb;
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

.user-area {
  display: flex;
  gap: 8px;
  align-items: center;
}

.user-name {
  font-size: 14px;
  color: #475569;
}

.member-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
  font-weight: 600;
}

.member-pay-btn {
  border-color: #fcd34d;
  color: #b45309;
  background: #fffbeb;
}

.auth-btn {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #dbe3ee;
  background: #ffffff;
  color: #0f172a;
  transition: all 0.2s ease;
}

.auth-btn:hover {
  transform: translateY(-1px);
}

.login-entry-btn {
  border-color: #bfdbfe;
  color: #1d4ed8;
  background: #eff6ff;
}

.register-entry-btn {
  border-color: #c7d2fe;
  color: #4338ca;
  background: #eef2ff;
}

.main-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.history-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.68);
  border-right: 1px solid #e2e8f0;
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
  border-bottom: 1px solid #e2e8f0;
}

.history-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.new-chat-btn {
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 500;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  cursor: pointer;
  border-radius: 999px;
  transition: background 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.new-chat-btn:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
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
  border: 1px solid #e2e8f0;
  border-left: 3px solid transparent;
  border-radius: 14px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.history-item:hover {
  background: linear-gradient(180deg, #ffffff 0%, #f1f5f9 100%);
  border-color: #bfdbfe;
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.12);
}

.history-item.active {
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
  border-left-color: #3b82f6;
  border-color: #93c5fd;
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.2);
}

.history-item-content {
  flex: 1;
  min-width: 0;
}

.history-item-title {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-time {
  display: block;
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.history-delete-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  padding: 0;
  font-size: 18px;
  font-weight: 500;
  line-height: 1;
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.14);
  border: 1px solid rgba(248, 113, 113, 0.42);
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.2s ease;
  opacity: 0;
  transform: scale(0.9);
}

.history-delete-btn:hover {
  color: #ffffff;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border-color: rgba(252, 165, 165, 0.88);
  box-shadow: 0 8px 16px rgba(220, 38, 38, 0.32);
}

.history-item:hover .history-delete-btn,
.history-item.active .history-delete-btn {
  opacity: 1;
  transform: scale(1);
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.22);
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.58) 0%, rgba(248, 250, 252, 0.42) 100%);
}

.chat-room-wrapper {
  flex: 1;
  min-height: 0;
  display: flex;
}

.auth-mask {
  position: fixed;
  inset: 0;
  background: radial-gradient(circle at 15% 20%, rgba(56, 189, 248, 0.2), transparent 40%),
    radial-gradient(circle at 85% 80%, rgba(167, 139, 250, 0.22), transparent 36%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.5), rgba(15, 23, 42, 0.62));
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  backdrop-filter: blur(8px);
}

.auth-shell {
  position: relative;
  width: 860px;
  max-width: 92vw;
  min-height: 430px;
  border-radius: 28px;
  overflow: hidden;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(250, 252, 255, 0.88));
  border: 1px solid rgba(255, 255, 255, 0.72);
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  box-shadow: 0 30px 90px rgba(15, 23, 42, 0.34), 0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.auth-glow {
  position: absolute;
  z-index: 0;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(50px);
}

.auth-glow-a {
  width: 220px;
  height: 220px;
  background: rgba(56, 189, 248, 0.32);
  left: -40px;
  top: -50px;
}

.auth-glow-b {
  width: 200px;
  height: 200px;
  background: rgba(167, 139, 250, 0.3);
  right: -30px;
  bottom: -40px;
}

.auth-close-x {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid #dbe3ee;
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
  font-size: 16px;
  line-height: 1;
  padding: 0;
}

.auth-close-x:hover {
  color: #0f172a;
  border-color: #cbd5e1;
  background: #fff;
}

.auth-left {
  position: relative;
  z-index: 1;
  padding: 30px 26px;
  background: linear-gradient(155deg, #dbeafe 0%, #c7d2fe 58%, #ddd6fe 100%);
  border-right: 1px solid rgba(191, 219, 254, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.auth-left-register {
  background: linear-gradient(155deg, #fbcfe8 0%, #ddd6fe 52%, #bfdbfe 100%);
}

.auth-left-tag {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.08em;
  font-weight: 700;
  color: #1e3a8a;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.auth-left-title {
  margin-top: 12px;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.5px;
}

.auth-left-subtitle {
  margin-top: 10px;
  font-size: 15px;
  color: #334155;
  line-height: 1.5;
}

.auth-fun {
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fun-bubble {
  width: fit-content;
  padding: 9px 13px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.85);
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.auth-right {
  position: relative;
  z-index: 1;
  padding: 30px 26px 26px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.auth-tabs {
  display: flex;
  gap: 18px;
  border-bottom: 1px solid #dbe3f2;
  padding-bottom: 10px;
}

.tab-item {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 17px;
  font-weight: 600;
  padding: 0 0 8px;
  border-bottom: 2px solid transparent;
  transition: color 0.2s ease;
}

.tab-item.active {
  color: #0f172a;
  border-bottom-color: #6366f1;
}

.auth-form {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #dbe3f2;
  box-shadow: inset 0 1px 0 #ffffff, 0 14px 30px rgba(15, 23, 42, 0.06);
}

.auth-input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.auth-input-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.auth-mascot {
  position: relative;
  width: 76px;
  height: 76px;
  margin: 16px auto 2px;
}

.mascot-face {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  border: 2px solid #93c5fd;
  background: linear-gradient(180deg, #eff6ff, #dbeafe);
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.2);
}

.mascot-eye {
  position: absolute;
  top: 28px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #1e3a8a;
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.mascot-eye.left { left: 24px; }
.mascot-eye.right { right: 24px; }

.mascot-mouth {
  position: absolute;
  left: 50%;
  bottom: 18px;
  width: 18px;
  height: 8px;
  border-bottom: 2px solid #1e3a8a;
  border-radius: 0 0 10px 10px;
  transform: translateX(-50%);
}

.mascot-hand {
  position: absolute;
  top: 38px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #f8fafc;
  border: 2px solid #93c5fd;
  transition: transform 0.28s ease;
}

.hand-left { left: -2px; transform: rotate(20deg); }
.hand-right { right: -2px; transform: rotate(-20deg); }

.auth-mascot.cover-eyes .hand-left {
  transform: translate(22px, -18px) rotate(-18deg);
}

.auth-mascot.cover-eyes .hand-right {
  transform: translate(-22px, -18px) rotate(18deg);
}

.auth-mascot.cover-eyes .mascot-eye {
  transform: scaleY(0.2);
  opacity: 0.5;
}

.auth-input {
  height: 46px;
  border-radius: 13px;
  border: 1px solid #d4deef;
  background: #fff;
  color: #0f172a;
  padding: 0 14px;
  transition: all 0.2s ease;
}

.auth-input:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.16);
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: -2px;
  margin-bottom: 2px;
}

.strength-track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.strength-fill {
  display: block;
  height: 100%;
  width: 0;
  border-radius: 999px;
  background: #94a3b8;
  transition: width 0.2s ease, background-color 0.2s ease;
}

.strength-text {
  min-width: 88px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.password-strength.strength-weak .strength-fill {
  background: #ef4444;
}

.password-strength.strength-medium .strength-fill {
  background: #f59e0b;
}

.password-strength.strength-strong .strength-fill {
  background: #22c55e;
}

.auth-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.auth-primary {
  border: 1px solid #4f46e5;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  padding: 9px 16px;
  border-radius: 11px;
}

.auth-primary:hover {
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.35);
}

.auth-secondary {
  color: #334155;
  background: #f8fafc;
  border: 1px solid #d6deea;
  border-radius: 11px;
}

.page-toast {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 120;
  padding: 10px 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid #bbf7d0;
  background: #ecfdf3;
  color: #166534;
  box-shadow: 0 8px 22px rgba(22, 101, 52, 0.14);
}

@media (max-width: 720px) {
  .auth-shell {
    grid-template-columns: 1fr;
    min-height: 0;
  }
  .auth-left {
    border-right: none;
    border-bottom: 1px solid #dbeafe;
  }
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