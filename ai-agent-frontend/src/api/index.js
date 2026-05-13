import axios from 'axios'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const USER_KEY = 'ai_mianshi_user'
const USER_ACTIVE_AT_KEY = 'ai_mianshi_user_active_at'
const LOGIN_EXPIRE_MS = 30 * 60 * 1000

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

const post = async (url, data) => {
  const resp = await request.post(url, data)
  return resp.data
}

const get = async (url, config = {}) => {
  const resp = await request.get(url, config)
  return resp.data
}

const triggerBlobDownload = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || 'interview_report.pdf'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export const connectSSE = (url, params = {}, onMessage, onError) => {
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  const fullUrl = `${API_BASE_URL}${url}${queryString ? `?${queryString}` : ''}`
  const eventSource = new EventSource(fullUrl)
  eventSource.onmessage = event => {
    const data = event.data
    if (onMessage) onMessage(data)
  }
  eventSource.onerror = error => {
    if (onError) onError(error)
  }
  return eventSource
}

export const getCurrentUser = () => {
  try {
    if (isUserSessionExpired()) {
      clearCurrentUser()
      return null
    }
    const user = localStorage.getItem(USER_KEY)
    return user ? JSON.parse(user) : null
  } catch (e) {
    return null
  }
}

export const setCurrentUser = user => {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  touchUserActivity()
}

export const clearCurrentUser = () => {
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(USER_ACTIVE_AT_KEY)
}

export const touchUserActivity = () => {
  localStorage.setItem(USER_ACTIVE_AT_KEY, String(Date.now()))
}

export const isUserSessionExpired = () => {
  const user = localStorage.getItem(USER_KEY)
  if (!user) return false
  const activeAt = Number(localStorage.getItem(USER_ACTIVE_AT_KEY) || 0)
  if (!activeAt) return true
  return Date.now() - activeAt > LOGIN_EXPIRE_MS
}

export const register = requestBody => {
  return post('/user/register', requestBody)
}

export const login = requestBody => {
  return post('/user/login', requestBody)
}

export const chat = requestBody => {
  return post('/conversation/chat', requestBody)
}

export const chatStream = (userId, sessionCode, message) => {
  return connectSSE('/conversation/chat/sse', { userId, sessionCode, message })
}

export const sessionPage = requestBody => {
  return post('/conversation/session/page', requestBody)
}

export const messagePage = requestBody => {
  return post('/conversation/message/page', requestBody)
}

export const promptSave = requestBody => {
  return post('/store/prompt/save', requestBody)
}

export const promptPage = requestBody => {
  return post('/store/prompt/page', requestBody)
}

export const configSave = requestBody => {
  return post('/store/config/save', requestBody)
}

export const configPage = requestBody => {
  return post('/store/config/page', requestBody)
}

export const adminUserPage = requestBody => {
  return post('/admin-api/user/page', requestBody)
}

export const adminUserDelete = requestBody => {
  return post('/admin-api/user/delete', requestBody)
}

export const adminSessionPage = requestBody => {
  return post('/admin-api/session/page', requestBody)
}

export const adminSessionDelete = requestBody => {
  return post('/admin-api/session/delete', requestBody)
}

export const adminMessagePage = requestBody => {
  return post('/admin-api/message/page', requestBody)
}

export const adminMessageDelete = requestBody => {
  return post('/admin-api/message/delete', requestBody)
}

export const adminPromptPage = requestBody => {
  return post('/admin-api/prompt/page', requestBody)
}

export const adminPromptDelete = requestBody => {
  return post('/admin-api/prompt/delete', requestBody)
}

export const adminConfigPage = requestBody => {
  return post('/admin-api/config/page', requestBody)
}

export const adminConfigValue = requestBody => {
  return post('/admin-api/config/value', requestBody)
}

export const adminConfigDelete = requestBody => {
  return post('/admin-api/config/delete', requestBody)
}

export const getLoveAppReportDownloadUrl = (fileName) => {
  return `${API_BASE_URL}/ai/love_app/report/download?fileName=${encodeURIComponent(fileName)}`
}

export const getSessionReportDownloadUrl = (userId, sessionCode) => {
  return `${API_BASE_URL}/conversation/report/download?userId=${encodeURIComponent(userId)}&sessionCode=${encodeURIComponent(sessionCode)}`
}

export const downloadInterviewReport = async ({ userId, sessionCode, fileName, reportContent }) => {
  const useSession = userId && sessionCode
  const resp = useSession
    ? await request.post('/conversation/report/export', {
        userId,
        sessionCode,
        reportContent
      }, {
        responseType: 'blob'
      })
    : await request.get('/ai/love_app/report/download', {
        params: { fileName },
        responseType: 'blob'
      })
  const contentType = resp.headers['content-type'] || ''
  if (!contentType.includes('application/pdf')) {
    const text = await resp.data.text()
    throw new Error(text || '下载失败')
  }
  const downloadName = fileName || (useSession ? `interview_report_${sessionCode}.pdf` : 'interview_report.pdf')
  triggerBlobDownload(resp.data, downloadName)
}

export const membershipCheckout = requestBody => post('/membership/checkout', requestBody)

export const membershipStatus = userId =>
  get(`/membership/status?userId=${encodeURIComponent(userId)}`)

export const membershipConfirm = requestBody => post('/membership/confirm', requestBody)

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

export default {
  register,
  login,
  chat,
  chatStream,
  sessionPage,
  messagePage,
  promptSave,
  promptPage,
  configSave,
  configPage,
  adminUserPage,
  adminUserDelete,
  adminSessionPage,
  adminSessionDelete,
  adminMessagePage,
  adminMessageDelete,
  adminPromptPage,
  adminPromptDelete,
  adminConfigPage,
  adminConfigDelete,
  getCurrentUser,
  setCurrentUser,
  clearCurrentUser,
  getLoveAppReportDownloadUrl,
  getSessionReportDownloadUrl,
  downloadInterviewReport,
  membershipCheckout,
  membershipStatus,
  membershipConfirm,
  chatWithManus
} 