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
              {{ msg.content }}
              <span
                v-if="connectionStatus === 'connecting' && index === messages.length - 1"
                class="typing-indicator"
              >▋</span>
            </div>
            <a
              v-if="getPdfDownloadUrl(msg.content)"
              class="pdf-download-btn"
              :href="getPdfDownloadUrl(msg.content)"
              target="_blank"
            >
              下载 PDF
            </a>
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
            @keydown.enter.prevent="sendMessage"
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
import { getLoveAppReportDownloadUrl } from '../api'

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
  }
})

const emit = defineEmits(['send-message'])

const inputMessage = ref('')
const messagesContainer = ref(null)

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
  emit('send-message', inputMessage.value)
  inputMessage.value = ''
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const extractPdfFileName = (content) => {
  if (!content) return ''
  const successMatch = content.match(/PDF generated successfully to:\s*.*[\\/](.+?\.pdf)/i)
  if (successMatch?.[1]) return successMatch[1]
  const fallbackMatch = content.match(/PDF结果[:：][\s\S]*?([A-Za-z0-9._-]+\.pdf)/i)
  if (fallbackMatch?.[1]) return fallbackMatch[1]
  return ''
}

const getPdfDownloadUrl = (content) => {
  const fileName = extractPdfFileName(content)
  if (!fileName) return ''
  return getLoveAppReportDownloadUrl(fileName)
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

onMounted(() => {
  scrollToBottom()
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
  overflow-y: auto;
  padding: 28px 36px 124px;
  width: 100%;
  max-width: none;
  margin: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.message-wrapper {
  margin-bottom: 32px;
  display: flex;
  flex-direction: column;
  width: 100%;
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
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  line-height: 1;
  color: #bfdbfe;
  margin-top: 0;
  border-radius: 12px;
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.42) 0%, rgba(30, 58, 138, 0.42) 100%);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.4);
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
  font-size: 17px;
  line-height: 1.72;
  color: #e5e7eb;
  white-space: pre-wrap;
}

.ai-message .message-time {
  font-size: 13px;
  color: #94a3b8;
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
  background: rgba(59, 130, 246, 0.22);
  border: 1px solid rgba(147, 197, 253, 0.55);
  color: #dbeafe;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.pdf-download-btn:hover {
  background: rgba(59, 130, 246, 0.34);
  border-color: rgba(191, 219, 254, 0.95);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.user-bubble {
  padding: 16px 20px;
  border-radius: 24px;
  background: linear-gradient(150deg, rgba(59, 130, 246, 0.34) 0%, rgba(37, 99, 235, 0.24) 100%);
  color: #f8fafc;
  text-align: left;
  border: 1px solid rgba(147, 197, 253, 0.4);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.user-message .message-content {
  font-size: 16px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.user-message .message-time {
  font-size: 12px;
  color: #cbd5e1;
  margin-top: 8px;
  text-align: right;
}

.chat-input-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 14px 36px 18px;
  background: linear-gradient(to top, rgba(11, 18, 32, 0.95) 68%, rgba(11, 18, 32, 0));
  z-index: 100;
  pointer-events: none;
}

.chat-input-container .input-inner {
  pointer-events: auto;
  width: 100%;
  max-width: none;
  margin: 0;
  box-sizing: border-box;
}

.input-pill {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 12px 12px 20px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 999px;
  box-shadow: 0 12px 28px rgba(2, 6, 23, 0.38);
  transition: box-shadow 0.2s, border-color 0.2s;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.input-pill:focus-within {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15), 0 14px 30px rgba(2, 6, 23, 0.42);
}

.input-box {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 16px;
  line-height: 1.55;
  resize: none;
  min-height: 24px;
  max-height: 120px;
  outline: none;
  color: #f8fafc;
  font-family: inherit;
  padding: 6px 0;
}

.input-box::placeholder {
  color: #94a3b8;
}

.send-button {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(145deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, opacity 0.2s;
}

.send-button:hover:not(:disabled) {
  background: linear-gradient(145deg, #60a5fa 0%, #3b82f6 100%);
}

.send-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-disclaimer {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
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

@media (max-width: 768px) {
  .chat-messages {
    padding: 18px 14px 112px;
  }

  .user-message {
    max-width: 92%;
  }
}

@media (max-width: 480px) {
  .input-pill {
    border-radius: 28px;
    padding: 8px 8px 8px 14px;
  }

  .send-button {
    width: 40px;
    height: 40px;
  }
}
</style>
