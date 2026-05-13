<template>
  <div class="home-container" @mousemove="onMouseMove">
    <div class="bg-orb orb-1"></div>
    <div class="bg-orb orb-2"></div>
    <div class="bg-orb orb-3"></div>
    
    <div class="header">
      <div class="tech-badge">Powered by AI Agent</div>
      <h1 class="title">AI超级智能体</h1>
      <p class="subtitle">专注 AI 体验，探索更多可能</p>
    </div>
    
    <div class="apps-container">
      <div 
        class="app-card" 
        ref="cardRef"
        @click="navigateTo('/love-master')"
        @mousemove="onCardMouseMove"
        @mouseleave="onCardMouseLeave"
      >
        <div class="card-glow"></div>
        <div class="card-particles"></div>

        <div class="app-icon-wrapper">
          <div class="app-icon interview-icon">
            <span class="emoji-icon">💼</span> 
          </div>
        </div>
        
        <div class="app-info">
          <div class="app-title">AI面试官</div>
          <p class="app-desc-wrapper">
            <span class="app-desc">智能面试辅导，帮你准备面试</span>
            <span class="cursor">_</span>
          </p>
        </div>
        
        <div class="app-button-wrapper">
          <button class="app-button">
            <span class="btn-text">立即体验</span>
            <span class="btn-icon">→</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'

// 设置页面标题和元数据 (保持原样)
useHead({
  title: 'AI超级智能体应用平台 - 首页',
  meta: [
    { name: 'description', content: 'AI超级智能体应用平台提供ai面试官和AI超级智能体服务' },
    { name: 'keywords', content: 'AI智能体,AI应用,ai面试官,首页' }
  ]
})

const router = useRouter()
const cardRef = ref(null)

const navigateTo = (path) => {
  router.push(path)
}

// 8. 全局鼠标跟随光晕效果
const onMouseMove = (e) => {
  const { clientX, clientY } = e
  document.documentElement.style.setProperty('--cursor-x', `${clientX}px`)
  document.documentElement.style.setProperty('--cursor-y', `${clientY}px`)
}

// 9. 卡片 3D 倾斜交互逻辑
const onCardMouseMove = (e) => {
  if (!cardRef.value) return
  const card = cardRef.value
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left - rect.width / 2
  const y = e.clientY - rect.top - rect.height / 2
  
  // 计算倾斜角度 (缩放到 5度以内)
  const tiltX = (y / (rect.height / 2)) * -5
  const tiltY = (x / (rect.width / 2)) * 5
  
  card.style.transform = `perspective(1000px) rotateX(${tiltX}deg) rotateY(${tiltY}deg) translateY(-10px)`
  
  // 更新卡片内部发光点位置
  const glowX = (x / rect.width) * 100 + 50
  const glowY = (y / rect.height) * 100 + 50
  card.style.setProperty('--glow-x', `${glowX}%`)
  card.style.setProperty('--glow-y', `${glowY}%`)
}

const onCardMouseLeave = () => {
  if (!cardRef.value) return
  const card = cardRef.value
  card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0)'
}
</script>

<style scoped>
/* 保持原有配色和高级感布局 */
.home-container {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 40px 20px;
  background-color: #f6f9ff; /* 基础底色 */
  background-image: linear-gradient(160deg, #eef3ff 0%, #f8fbff 45%, #f7f7ff 100%);
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 10. 全局鼠标跟随光晕 - CSS 实现 */
.home-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at var(--cursor-x) var(--cursor-y), rgba(37, 99, 235, 0.03) 0%, transparent 400px);
  pointer-events: none;
  z-index: 10;
}

/* 11. 原有光斑的趣味性动画：轻微漂浮呼吸 */
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 1;
  animation: orb-float 15s infinite alternate ease-in-out;
}
.orb-1 { width: 350px; height: 350px; top: -100px; left: -100px; background: rgba(59, 130, 246, 0.2); }
.orb-2 { width: 450px; height: 450px; bottom: -150px; right: -120px; background: rgba(99, 102, 241, 0.15); animation-delay: -5s;}
.orb-3 { width: 280px; height: 280px; top: 35%; right: 16%; background: rgba(249, 115, 22, 0.1); animation-delay: -10s;}

@keyframes orb-float {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(30px, 20px) scale(1.05); }
}

.header {
  text-align: center;
  position: relative;
  z-index: 2;
  margin-bottom: 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 新增科技 Badge */
.tech-badge {
  display: inline-block;
  padding: 6px 14px;
  background: rgba(37, 99, 235, 0.05);
  border: 1px solid rgba(37, 99, 235, 0.1);
  color: #2563eb;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.title {
  font-size: clamp(36px, 8vw, 58px); /* 响应式字体 */
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(110deg, #0f172a 10%, #1e40af 50%, #2563eb 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0;
}
.subtitle {
  margin-top: 14px;
  font-size: 18px;
  color: #64748b;
}

.apps-container {
  width: min(520px, 100%);
  position: relative;
  z-index: 2;
  perspective: 1000px; /* 开启卡片的 3D 视图 */
}

/* 12. 卡片升级：玻璃拟态 2.0 + 3D 倾斜 */
.app-card {
  position: relative;
  width: 100%;
  padding: 48px 40px;
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(16px) saturate(180%);
  box-shadow: 0 30px 90px rgba(30, 64, 175, 0.12), 
              0 10px 30px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  
  display: flex;
  flex-direction: column;
  align-items: center;
  
  /* 基础动画设置 */
  transition: transform 0.15s ease-out, box-shadow 0.3s ease;
  overflow: hidden;
}

/* 卡片内部跟随鼠标的发光效果 */
.card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at var(--glow-x, 50%) var(--glow-y, 50%), rgba(255, 255, 255, 0.6) 0%, transparent 60%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.app-card:hover .card-glow { opacity: 1; }

/* 13. 卡片 hover 时的动态粒子背景 (CSS 实现) */
.card-particles {
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(#2563eb 1px, transparent 1px),
    radial-gradient(#f97316 1px, transparent 1px);
  background-size: 50px 50px;
  background-position: 0 0, 25px 25px;
  opacity: 0.02;
  transition: opacity 0.3s ease;
}
.app-card:hover .card-particles {
  opacity: 0.06;
  animation: particles-move 10s linear infinite;
}
@keyframes particles-move {
  from { background-position: 0 0, 25px 25px; }
  to { background-position: 100px 100px, 125px 125px; }
}

.app-card:hover {
  box-shadow: 0 45px 110px rgba(30, 64, 175, 0.2);
}

.app-icon-wrapper {
  position: relative;
  margin-bottom: 24px;
}
/* 给图标加一层软软的背光，像一个装置 */
.app-icon-wrapper::after {
  content: '';
  position: absolute;
  inset: -10px;
  background: radial-gradient(circle, rgba(249, 115, 22, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(8px);
}

.app-icon {
  font-size: 54px;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}
.emoji-icon {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.app-card:hover .emoji-icon {
  transform: scale(1.1) rotate(-5deg); /* 图标 hover 时轻微晃动，增加生动感 */
}

/* 保持原有图标渐变色 */
.interview-icon {
  background: linear-gradient(135deg, #fda4af 0%, #fb7185 45%, #f97316 100%);
  box-shadow: 0 16px 35px rgba(249, 115, 22, 0.25),
              inset 0 0 20px rgba(255, 255, 255, 0.4);
}

.app-info {
  text-align: center;
  margin-bottom: 30px;
  width: 100%;
}
.app-title {
  font-size: 34px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}
.app-desc {
  font-size: 16px;
  color: #64748b;
  line-height: 1.6;
}

/* 14. 描述文字的趣味性：Hover 时显示光标打字机效果 */
.cursor {
  display: inline-block;
  opacity: 0;
  color: #2563eb;
  font-weight: bold;
}
.app-card:hover .cursor {
  opacity: 1;
  animation: blink 0.7s step-start infinite;
}
@keyframes blink { 50% { opacity: 0; } }

/* 15. 按钮：磁吸和涟漪效果的基础设置 */
.app-button-wrapper {
  position: relative;
  margin-top: auto;
}

.app-button {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  padding: 16px 36px;
  border-radius: 16px;
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  border: none;
  cursor: pointer;
  
  /* 原有阴影，hover 时会增强 */
  box-shadow: 0 14px 35px rgba(37, 99, 235, 0.3);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

/* 按钮 hover 时磁吸感和发光 */
.app-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 45px rgba(37, 99, 235, 0.45);
  background: linear-gradient(135deg, #4f46e5, #2563eb);
}

.btn-text { margin-right: 10px; }
.btn-icon {
  font-size: 20px;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.app-button:hover .btn-icon {
  transform: translateX(6px) scale(1.1); /* 箭头 hover 时更有弹性的位移 */
}

/* 响应式调整 (保持原样并微调) */
@media (max-width: 768px) {
  .title { font-size: 46px; }
  .subtitle { font-size: 16px; }
  .app-card { padding: 40px 24px; }
  .app-title { font-size: 28px; }
  .app-icon { width: 90px; height: 90px; font-size: 48px; }
}

@media (max-width: 480px) {
  .title { font-size: 36px; }
  .app-card { padding: 32px 20px; }
  .app-icon { width: 80px; height: 80px; font-size: 44px; }
  .app-title { font-size: 24px; }
  .app-button { padding: 14px 28px; font-size: 15px; }
}
</style>