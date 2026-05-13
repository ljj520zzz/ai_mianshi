<template>
  <div class="chat-container">
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <div
          v-if="!msg.isUser"
          class="message ai-message"
          :class="[msg.type]"
        >
          <span
            class="ai-sparkle"
            :class="{
              'ai-sparkle--hidden': !showAiSparkle(index) && !isSparkleBreathing(index),
              'ai-sparkle--breathing': isSparkleBreathing(index)
            }"
            aria-hidden="true"
          >✦</span>
          <div class="ai-body">
            <div class="message-content">
              <div class="ai-rich-content" :class="{ 'ai-rich-content--interview': aiType === 'interview' }" v-html="formatAiContent(msg.content)"></div>
              <span
                v-if="connectionStatus === 'connecting' && index === messages.length - 1"
                class="typing-indicator"
              >▋</span>
            </div>
            <button
              v-if="canDownloadReport(msg.content)"
              type="button"
              class="pdf-download-btn"
              @click="handleReportDownload(msg.content)"
            >
              下载 PDF
            </button>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <div v-else class="message user-message" :class="[msg.type]">
          <div class="user-bubble">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-container">
      <div class="input-inner">
        <div class="input-pill">
          <textarea
            v-model="inputMessage"
            @keydown="handleInputKeydown"
            @input="autoResizeInput"
            placeholder="输入消息..."
            class="input-box"
            rows="1"
            :disabled="connectionStatus === 'connecting'"
          ></textarea>
          <button
            type="button"
            class="send-button"
            :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
            aria-label="发送"
            @click="sendMessage"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" fill="currentColor"/>
            </svg>
          </button>
        </div>
        <p class="input-disclaimer">内容由 AI 生成，仅供参考</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import { downloadInterviewReport } from '../api'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'default'
  },
  draftKey: {
    type: String,
    default: ''
  },
  sessionCode: {
    type: String,
    default: ''
  },
  userId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['send-message'])

const inputMessage = ref('')
const messagesContainer = ref(null)
const lastSentMessage = ref('')
const prevConnectionStatus = ref('disconnected')
const INPUT_MIN_HEIGHT = 30
const markdown = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
})

const getStorageBaseKey = () => {
  const base = props.draftKey || props.aiType || 'default'
  return `chatroom_draft_${base}`
}

const getDraftStorageKey = () => `${getStorageBaseKey()}_input`
const getRetryStorageKey = () => `${getStorageBaseKey()}_retry`

const saveDraftToStorage = (message) => {
  localStorage.setItem(getDraftStorageKey(), message || '')
}

const saveRetryToStorage = (message) => {
  localStorage.setItem(getRetryStorageKey(), message || '')
}

const clearRetryFromStorage = () => {
  localStorage.removeItem(getRetryStorageKey())
}

const showAiSparkle = (index) => {
  if (index === 0) return true
  return props.messages[index - 1]?.isUser === true
}

const isSparkleBreathing = (index) => {
  const last = props.messages.length - 1
  if (last < 0 || index !== last) return false
  const msg = props.messages[last]
  return props.connectionStatus === 'connecting' && msg && !msg.isUser
}

const sendMessage = () => {
  if (!inputMessage.value.trim()) return
  const message = inputMessage.value.trim()
  lastSentMessage.value = message
  saveRetryToStorage(message)
  emit('send-message', message)
  inputMessage.value = ''
  saveDraftToStorage('')
  nextTick(() => {
    const textarea = document.querySelector('.input-box')
    if (textarea) {
      textarea.style.height = `${INPUT_MIN_HEIGHT}px`
    }
  })
}

const handleInputKeydown = (event) => {
  if (event.key !== 'Enter') return
  if (event.shiftKey) return
  event.preventDefault()
  sendMessage()
}

const autoResizeInput = (event) => {
  const textarea = event?.target
  if (!textarea) return
  textarea.style.height = `${INPUT_MIN_HEIGHT}px`
  textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const SUMMARY_SECTION_TITLES = ['总体表现', '核心优势', '主要短板', '岗位匹配度', '改进计划', '下一轮模拟建议']

const extractPdfFileName = (content) => {
  if (!content) return ''
  const markerMatch = content.match(/PDF结果[:：]\s*([A-Za-z0-9._-]+\.pdf)/i)
  if (markerMatch?.[1]) return markerMatch[1]
  const successMatch = content.match(/PDF generated successfully to:\s*.*[\\/](.+?\.pdf)/i)
  if (successMatch?.[1]) return successMatch[1]
  const fallbackMatch = content.match(/PDF结果[:：][\s\S]*?([A-Za-z0-9._-]+\.pdf)/i)
  if (fallbackMatch?.[1]) return fallbackMatch[1]
  return ''
}

const hasSummaryReport = (content) => {
  if (!content) return false
  let matchedCount = 0
  for (const title of SUMMARY_SECTION_TITLES) {
    if (content.includes(title)) {
      matchedCount += 1
    }
  }
  return matchedCount >= 3
}

const getReportDownloadTarget = (content) => {
  const reportContent = stripPdfResultSection(content)
  if (props.aiType === 'interview' && hasSummaryReport(reportContent) && props.userId && props.sessionCode) {
    return {
      userId: String(props.userId),
      sessionCode: props.sessionCode,
      fileName: extractPdfFileName(content) || `interview_report_${props.sessionCode}.pdf`,
      reportContent
    }
  }
  const fileName = extractPdfFileName(content)
  if (fileName) {
    return { fileName }
  }
  return null
}

const canDownloadReport = (content) => !!getReportDownloadTarget(content)

const handleReportDownload = async (content) => {
  const target = getReportDownloadTarget(content)
  if (!target) return
  try {
    await downloadInterviewReport(target)
  } catch (e) {
    let message = 'PDF下载失败'
    const errorData = e?.response?.data
    if (errorData instanceof Blob) {
      const text = await errorData.text()
      if (text) {
        message = text
      }
    } else if (e?.message) {
      message = e.message
    }
    window.alert(message)
  }
}

const INTERVIEW_SECTION_PATTERN = /^(?:#{1,6}\s*)?(?:\*\*)?(题目|评价|参考答案)(?:\*\*)?[:：]?\s*(.*)$/

const enhanceInterviewSections = (content) => {
  if (props.aiType !== 'interview' || !content) return content
  const lines = content.replace(/\r\n/g, '\n').split('\n')
  const output = []
  let hasSection = false
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) {
      if (output.length && output[output.length - 1] !== '') {
        output.push('')
      }
      continue
    }
    const sectionMatch = trimmed.match(INTERVIEW_SECTION_PATTERN)
    if (sectionMatch) {
      if (hasSection) {
        output.push('')
        output.push('---')
        output.push('')
      }
      output.push(`## ${sectionMatch[1]}`)
      if (sectionMatch[2]) {
        output.push(sectionMatch[2])
      }
      hasSection = true
      continue
    }
    output.push(line)
  }
  return output.join('\n')
}

const applyInterviewSectionStyles = (html) => {
  if (props.aiType !== 'interview' || !html) return html
  return html
    .replace(/<h2>题目<\/h2>/g, '<h2 class="interview-section-title interview-section-title--question">题目</h2>')
    .replace(/<h2>评价<\/h2>/g, '<h2 class="interview-section-title interview-section-title--review">评价</h2>')
    .replace(/<h2>参考答案<\/h2>/g, '<h2 class="interview-section-title interview-section-title--answer">参考答案</h2>')
    .replace(/<hr>/g, '<hr class="interview-section-divider">')
}

const stripPdfResultSection = (content) => {
  if (!content) return ''
  return content.replace(/\r\n/g, '\n').replace(/\n*PDF结果[:：][\s\S]*$/, '').trimEnd()
}

const formatAiContent = (content) => {
  if (!content) return ''
  const normalized = enhanceInterviewSections(stripPdfResultSection(content))
  return applyInterviewSectionStyles(markdown.render(normalized))
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

watch(() => props.messages.length, () => {
  scrollToBottom()
})

watch(() => props.messages.map(m => m.content).join(''), () => {
  scrollToBottom()
})

watch(inputMessage, value => {
  saveDraftToStorage(value)
})

watch(() => props.connectionStatus, status => {
  if (status !== 'error') return
  if (!lastSentMessage.value) return
  if (inputMessage.value.trim()) return
  inputMessage.value = lastSentMessage.value
  saveDraftToStorage(lastSentMessage.value)
})

watch(() => props.connectionStatus, status => {
  const prev = prevConnectionStatus.value
  prevConnectionStatus.value = status
  if (prev === 'connecting' && status === 'disconnected') {
    clearRetryFromStorage()
  }
})

onMounted(() => {
  scrollToBottom()
  const retryMessage = localStorage.getItem(getRetryStorageKey()) || ''
  const draftMessage = localStorage.getItem(getDraftStorageKey()) || ''
  if (retryMessage.trim()) {
    lastSentMessage.value = retryMessage
    inputMessage.value = retryMessage
  } else if (draftMessage.trim()) {
    inputMessage.value = draftMessage
  }
  const textarea = document.querySelector('.input-box')
  if (textarea) {
    textarea.style.height = `${INPUT_MIN_HEIGHT}px`
    if (inputMessage.value) {
      textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`
    }
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');

.chat-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: transparent;
  border-radius: 0;
  overflow: hidden;
  position: relative;
  border: none;
  font-family: 'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif;
  font-size: 16px;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: clamp(14px, 2vw, 28px) clamp(12px, 2.5vw, 36px) clamp(18px, 2vw, 24px);
  width: 100%;
  max-width: none;
  margin: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.message-wrapper {
  margin-bottom: clamp(18px, 2vw, 32px);
  display: flex;
  flex-direction: column;
  width: 100%;
  animation: message-fade-in 0.28s ease;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.user-message {
  margin-left: auto;
  max-width: min(85%, 900px);
  flex-direction: row-reverse;
}

.ai-message {
  margin-right: auto;
  max-width: min(100%, 960px);
}

.ai-sparkle {
  flex-shrink: 0;
  width: clamp(38px, 3vw, 48px);
  height: clamp(38px, 3vw, 48px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(20px, 1.5vw, 24px);
  line-height: 1;
  color: #3b82f6;
  margin-top: 0;
  border-radius: 12px;
  background: linear-gradient(145deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.18);
  transition: box-shadow 0.3s ease, transform 0.3s ease;
}

.ai-sparkle--hidden {
  visibility: hidden;
}

.ai-sparkle--breathing {
  animation: ai-sparkle-breathe 1.4s ease-in-out infinite;
}

@keyframes ai-sparkle-breathe {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
    box-shadow: 0 1px 2px rgba(26, 115, 232, 0.12), 0 0 0 0 rgba(26, 115, 232, 0.25);
  }
  50% {
    transform: scale(1.06);
    opacity: 0.92;
    box-shadow: 0 2px 8px rgba(26, 115, 232, 0.2), 0 0 16px rgba(26, 115, 232, 0.18);
  }
}

.ai-body {
  flex: 1;
  min-width: 0;
}

.ai-message .message-content {
  font-size: clamp(15px, 1.05vw, 17px);
  line-height: 1.72;
  color: #0f172a;
  transition: color 0.2s ease;
}

.ai-rich-content :deep(h1),
.ai-rich-content :deep(h2),
.ai-rich-content :deep(h3) {
  margin: 8px 0;
  line-height: 1.4;
}

.ai-rich-content :deep(h1) {
  font-size: clamp(20px, 1.7vw, 24px);
  font-weight: 700;
}

.ai-rich-content :deep(h2) {
  font-size: clamp(18px, 1.4vw, 20px);
  font-weight: 700;
}

.ai-rich-content :deep(h3) {
  font-size: clamp(16px, 1.2vw, 18px);
  font-weight: 700;
}

.ai-rich-content :deep(strong) {
  font-weight: 700;
}

.ai-rich-content :deep(p) {
  margin: 8px 0;
}

.ai-rich-content--interview :deep(.interview-section-title) {
  display: inline-flex;
  align-items: center;
  margin: 18px 0 10px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: clamp(14px, 1vw, 16px);
  font-weight: 700;
  letter-spacing: 0.04em;
}

.ai-rich-content--interview :deep(.interview-section-title--question) {
  color: #1d4ed8;
  background: #dbeafe;
  border: 1px solid #93c5fd;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.16);
}

.ai-rich-content--interview :deep(.interview-section-title--review) {
  color: #b45309;
  background: #fef3c7;
  border: 1px solid #fcd34d;
}

.ai-rich-content--interview :deep(.interview-section-title--answer) {
  color: #047857;
  background: #d1fae5;
  border: 1px solid #6ee7b7;
}

.ai-rich-content--interview :deep(.interview-section-title--question + p),
.ai-rich-content--interview :deep(.interview-section-title--question + ul),
.ai-rich-content--interview :deep(.interview-section-title--question + ol),
.ai-rich-content--interview :deep(.interview-section-title--question + blockquote) {
  margin-top: 0;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid #bfdbfe;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.1);
}

.ai-rich-content--interview :deep(.interview-section-divider) {
  border: none;
  border-top: 1px dashed #cbd5e1;
  margin: 18px 0;
}

.ai-rich-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
}

.ai-rich-content :deep(th),
.ai-rich-content :deep(td) {
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  text-align: left;
  vertical-align: top;
}

.ai-rich-content :deep(th) {
  background: #eff6ff;
  font-weight: 600;
}

.ai-rich-content :deep(tr:nth-child(even) td) {
  background: #f8fafc;
}

.ai-message .message-time {
  font-size: 13px;
  color: #64748b;
  margin-top: 8px;
  text-align: left;
}

.pdf-download-btn {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  margin-top: 12px;
  border-radius: 999px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.pdf-download-btn:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.user-bubble {
  padding: clamp(12px, 1.2vw, 16px) clamp(14px, 1.5vw, 20px);
  border-radius: 24px;
  background: linear-gradient(150deg, #dbeafe 0%, #bfdbfe 100%);
  color: #0f172a;
  text-align: left;
  border: 1px solid rgba(59, 130, 246, 0.35);
  box-shadow: 0 8px 18px rgba(148, 163, 184, 0.35);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.user-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(148, 163, 184, 0.42);
}

.user-message .message-content {
  font-size: clamp(14px, 1vw, 16px);
  line-height: 1.65;
  white-space: pre-wrap;
}

.user-message .message-time {
  font-size: 12px;
  color: #475569;
  margin-top: 8px;
  text-align: right;
}

.chat-input-container {
  flex-shrink: 0;
  padding: clamp(8px, 1.2vw, 14px) clamp(12px, 2.5vw, 36px) clamp(12px, 1.5vw, 18px);
  background: linear-gradient(to top, rgba(246, 248, 252, 0.98) 68%, rgba(246, 248, 252, 0));
  z-index: 2;
}

.input-pill {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 12px 12px 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.1);
  transition: box-shadow 0.2s, border-color 0.2s;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.input-pill:focus-within {
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14), 0 14px 30px rgba(15, 23, 42, 0.13);
}

.input-box {
  flex: 1;
  border: none;
  background: transparent;
  font-size: clamp(14px, 1vw, 16px);
  line-height: 1.55;
  resize: none;
  min-height: 30px;
  max-height: 120px;
  outline: none;
  color: #0f172a;
  font-family: inherit;
  padding: 6px 0;
}

.input-box::placeholder {
  color: #94a3b8;
}

.send-button {
  flex-shrink: 0;
  width: clamp(40px, 3vw, 46px);
  height: clamp(40px, 3vw, 46px);
  border-radius: 50%;
  border: none;
  background: linear-gradient(145deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
}

.send-button:hover:not(:disabled) {
  background: linear-gradient(145deg, #60a5fa 0%, #3b82f6 100%);
  transform: translateY(-1px) scale(1.03);
}

.send-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-disclaimer {
  text-align: center;
  font-size: 12px;
  color: #64748b;
  margin: 10px 0 0;
  line-height: 1.4;
}

.typing-indicator {
  display: inline-block;
  animation: blink 0.7s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

@keyframes message-fade-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  max-width: min(92%, 900px);
}
</style>
