<script setup>
import HelloWorld from './components/HelloWorld.vue'
</script>

<template>
  <router-view />
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body {
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: clamp(14px, 0.75vw + 11px, 18px);
  color: #1f1f1f;
  background-color: #ffffff;
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: 100%;
  overflow-x: hidden;
}

#app {
  width: 100%;
  min-height: 100vh;
}

a {
  text-decoration: none;
  color: inherit;
}

button {
  cursor: pointer;
  border-radius: 10px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #0f172a;
  transition: all 0.22s ease;
}

button:hover {
  transform: translateY(-1px);
  border-color: #93c5fd;
}

button:active {
  transform: translateY(0);
}

button:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.14);
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f3f4;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #dadce0;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #bdc1c6;
}
</style>
