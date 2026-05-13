import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createHead } from '@vueuse/head'
import { clearCurrentUser, getCurrentUser, isUserSessionExpired, touchUserActivity } from './api'
import './style.css'

const app = createApp(App)
const head = createHead()

app.use(router)
app.use(head)
app.mount('#app')

const userActivityEvents = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart']
const userActivityHandler = () => {
  if (getCurrentUser()) touchUserActivity()
}
userActivityEvents.forEach(eventName => window.addEventListener(eventName, userActivityHandler, { passive: true }))

window.setInterval(() => {
  if (isUserSessionExpired()) {
    clearCurrentUser()
  }
}, 60 * 1000)
