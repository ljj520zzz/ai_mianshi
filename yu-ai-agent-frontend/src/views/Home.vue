<template>
  <div class="home-container">
    <div class="header">
      <div class="glitch-wrapper">
        <h1 class="glitch-title">AI超级智能体</h1>
      </div>
      <p class="subtitle">/ 探索AI的无限可能 /</p>
      <div class="cyber-line"></div>
    </div>
    
    <div class="apps-container">
      <div class="app-card" @click="navigateTo('/love-master')">
        <div class="card-glow"></div>
        <div class="app-icon interview-icon">💼</div>
        <div class="app-info">
          <div class="app-title">ai面试官</div>
          <div class="app-desc">智能面试辅导，帮你准备面试</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
    </div>
    
    <div class="cyber-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
// 设置页面标题和元数据
useHead({
  title: 'AI超级智能体应用平台 - 首页',
  meta: [
    {
      name: 'description',
      content: 'AI超级智能体应用平台提供ai面试官和AI超级智能体服务，满足您的各种AI对话需求'
    },
    {
      name: 'keywords',
      content: 'AI智能体,AI应用,ai面试官,AI助手,智能对话,AI超级智能体,首页'
    }
  ]
})

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;700&display=swap');

/* 全局样式变量 */
:root {
  --neon-blue: #00f0ff;
  --neon-purple: #9000ff;
  --neon-pink: #ff00d4;
  --cyber-black: #0a0a12;
  --cyber-dark: #111122;
  --cyber-light: #edf7ff;
}

.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  margin: 0;
  padding: 0;
  background-color: #0a0a12;
  background-image: 
    linear-gradient(180deg, rgba(0, 15, 30, 0.95) 0%, rgba(5, 8, 20, 0.98) 100%),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="30" height="30"><defs><pattern id="g" width="30" height="30"><path d="M30 0H0V30" fill="none" stroke="%2300f0ff" stroke-width="0.2" opacity="0.18"/></pattern></defs><rect width="30" height="30" fill="url(%23g)"/></svg>');
  background-size: auto, 30px 30px;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
}

.home-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 240, 255, 0.02) 2px,
    rgba(0, 240, 255, 0.02) 4px
  );
  pointer-events: none;
  z-index: 10;
}

/* 赛博朋克风格标题 */
.header {
  padding: 70px 20px 50px;
  text-align: center;
  background-color: transparent;
  position: relative;
  z-index: 2;
}

.glitch-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.glitch-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 3.2rem;
  font-weight: 700;
  color: var(--cyber-light);
  text-shadow: 
    0 0 8px rgba(0, 240, 255, 0.9),
    0 0 16px rgba(0, 240, 255, 0.6),
    0 0 32px rgba(0, 240, 255, 0.4);
  letter-spacing: 4px;
  position: relative;
  animation: glitch 3s infinite;
}

.glitch-title::before,
.glitch-title::after {
  content: 'AI超级智能体';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.8;
}

.glitch-title::before {
  color: var(--neon-pink);
  z-index: -1;
  animation: glitch-anim 2s infinite;
}

.glitch-title::after {
  color: var(--neon-blue);
  z-index: -2;
  animation: glitch-anim-2 3s infinite;
}

.subtitle {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.2rem;
  color: rgba(0, 240, 255, 0.8);
  max-width: 600px;
  margin: 0 auto 20px;
  letter-spacing: 6px;
  text-transform: uppercase;
  text-shadow: 0 0 10px rgba(0, 240, 255, 0.3);
}

.cyber-line {
  height: 2px;
  width: 80%;
  max-width: 600px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent 0%, var(--neon-blue) 20%, var(--neon-blue) 80%, transparent 100%);
  box-shadow: 0 0 15px var(--neon-blue), 0 0 30px rgba(0, 240, 255, 0.3);
  position: relative;
}

.cyber-line::before,
.cyber-line::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  border: 2px solid var(--neon-blue);
  transform: translateY(-50%) rotate(45deg);
  box-shadow: 0 0 12px var(--neon-blue);
}

.cyber-line::before {
  left: 20%;
}

.cyber-line::after {
  right: 20%;
}

.apps-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 50px;
  max-width: 1200px;
  margin: 60px auto;
  padding: 0 24px;
  flex: 1;
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

.app-card {
  width: 340px;
  background: linear-gradient(135deg, rgba(8, 20, 40, 0.9) 0%, rgba(4, 12, 28, 0.95) 100%);
  backdrop-filter: blur(12px);
  border-radius: 4px;
  border: 1px solid rgba(0, 240, 255, 0.3);
  box-shadow: 
    0 0 20px rgba(0, 240, 255, 0.15),
    0 0 40px rgba(0, 240, 255, 0.05),
    inset 0 0 60px rgba(0, 240, 255, 0.02);
  padding: 30px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.app-card::before,
.app-card::after {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  border: 2px solid var(--neon-blue);
  opacity: 0.6;
}

.app-card::before {
  top: 8px;
  left: 8px;
  border-right: none;
  border-bottom: none;
}

.app-card::after {
  bottom: 8px;
  right: 8px;
  border-left: none;
  border-top: none;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle at center,
    rgba(0, 240, 255, 0.15) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.5s;
  pointer-events: none;
}

.app-card:hover {
  transform: translateY(-15px) scale(1.03);
  border-color: rgba(0, 240, 255, 0.6);
  box-shadow: 
    0 0 40px rgba(0, 240, 255, 0.4),
    0 0 80px rgba(0, 240, 255, 0.2),
    inset 0 0 80px rgba(0, 240, 255, 0.05);
}

.app-card:hover::before,
.app-card:hover::after {
  opacity: 1;
  box-shadow: 0 0 8px var(--neon-blue);
}

.app-card:hover .card-glow {
  opacity: 1;
}

.app-icon {
  font-size: 4rem;
  margin-bottom: 25px;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 2px solid rgba(0, 240, 255, 0.3);
  position: relative;
  z-index: 1;
}

.interview-icon {
  background: linear-gradient(135deg, #ff007a, #ff5722);
  box-shadow: 0 0 25px rgba(255, 0, 122, 0.6), inset 0 0 20px rgba(255, 255, 255, 0.1);
}

.robot-icon {
  background: linear-gradient(135deg, #00b2ff, #4f56ff);
  box-shadow: 0 0 25px rgba(0, 178, 255, 0.6), inset 0 0 20px rgba(255, 255, 255, 0.1);
}

.app-info {
  text-align: center;
  margin-bottom: 30px;
  width: 100%;
}

.app-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 1.6rem;
  font-weight: bold;
  color: white;
  margin-bottom: 12px;
  text-shadow: 0 0 10px rgba(0, 240, 255, 0.5);
}

.app-desc {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.app-button {
  background: linear-gradient(90deg, rgba(0, 136, 255, 0.8), rgba(0, 178, 255, 0.8));
  color: white;
  padding: 12px 28px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.3s;
  margin-top: auto;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: 2px solid rgba(0, 240, 255, 0.5);
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.3);
}

.app-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.7s;
}

.app-button:hover {
  box-shadow: 0 0 25px rgba(0, 240, 255, 0.8);
  border-color: var(--neon-blue);
  transform: scale(1.05);
}

.app-button:hover::before {
  left: 100%;
}

.btn-text {
  margin-right: 8px;
  letter-spacing: 1px;
}

.btn-icon {
  font-size: 1.2rem;
  transition: transform 0.3s;
}

.app-button:hover .btn-icon {
  transform: translateX(4px);
}

/* 背景圆圈动画 */
.cyber-circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
  filter: blur(60px);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  animation: float 15s infinite alternate;
}

.circle-2 {
  width: 500px;
  height: 500px;
  bottom: -200px;
  left: -200px;
  background: linear-gradient(135deg, var(--neon-purple), var(--neon-pink));
  animation: float 20s infinite alternate-reverse;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  right: 15%;
  background: linear-gradient(135deg, var(--neon-pink), var(--neon-blue));
  animation: float 12s infinite alternate;
}

/* 动画效果 */
@keyframes float {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  100% {
    transform: translate(50px, 50px) rotate(10deg);
  }
}

@keyframes glitch {
  0% {
    text-shadow: 
      0 0 5px rgba(0, 240, 255, 0.7),
      0 0 10px rgba(0, 240, 255, 0.5);
  }
  50% {
    text-shadow: 
      0 0 5px rgba(0, 240, 255, 0.7),
      0 0 10px rgba(0, 240, 255, 0.5),
      0 0 20px rgba(0, 240, 255, 0.3);
  }
  100% {
    text-shadow: 
      0 0 5px rgba(0, 240, 255, 0.7),
      0 0 10px rgba(0, 240, 255, 0.5);
  }
}

@keyframes glitch-anim {
  0%, 100% {
    transform: translate(0);
  }
  20% {
    transform: translate(-5px, 5px);
  }
  40% {
    transform: translate(-5px, -5px);
  }
  60% {
    transform: translate(5px, 5px);
  }
  80% {
    transform: translate(5px, -5px);
  }
}

@keyframes glitch-anim-2 {
  0%, 100% {
    transform: translate(0);
  }
  20% {
    transform: translate(3px, -3px);
  }
  40% {
    transform: translate(3px, 3px);
  }
  60% {
    transform: translate(-3px, -3px);
  }
  80% {
    transform: translate(-3px, 3px);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .glitch-title {
    font-size: 2.5rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .apps-container {
    gap: 30px;
    margin: 40px auto;
  }
  
  .app-card {
    width: 100%;
    max-width: 420px;
    padding: 25px;
  }
  
  .app-icon {
    font-size: 3.5rem;
    width: 80px;
    height: 80px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 50px 15px 40px;
  }
  
  .glitch-title {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 0.9rem;
    letter-spacing: 2px;
  }
  
  .apps-container {
    margin: 30px auto;
    padding: 0 15px;
  }
  
  .app-card {
    padding: 20px;
  }
  
  .app-icon {
    font-size: 3rem;
    margin-bottom: 20px;
    width: 70px;
    height: 70px;
  }
  
  .app-title {
    font-size: 1.4rem;
  }
  
  .app-desc {
    font-size: 0.9rem;
  }
  
  .circle-1, .circle-2, .circle-3 {
    opacity: 0.1;
  }
}
</style> 