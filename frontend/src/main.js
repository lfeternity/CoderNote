import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import './styles/theme.css'
import { initGlobalMotion } from './utils/motion'

const THEME_STORAGE_KEY = 'eb_theme_mode'

function applyInitialTheme() {
  let mode = 'light'
  try {
    mode = localStorage.getItem(THEME_STORAGE_KEY) === 'dark' ? 'dark' : 'light'
  } catch (_) {
    mode = 'light'
  }

  document.documentElement.setAttribute('data-theme', mode)
  document.documentElement.classList.toggle('dark', mode === 'dark')
}

applyInitialTheme()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')

initGlobalMotion(router)
