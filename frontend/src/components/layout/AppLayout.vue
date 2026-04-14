<template>
  <div class="layout-shell" :class="{ 'is-sidebar-collapsed': isSidebarCollapsed }">
    <aside class="sidebar surface-card glass-panel">
      <div class="sidebar-head">
        <button class="brand" type="button" :title="isSidebarCollapsed ? 'CoderNote' : ''" @click="router.push('/index')">
          <span class="brand-main">CoderNote</span>
          <span class="brand-mini">CN</span>
        </button>
        <button
          v-if="!isSidebarCollapsed"
          class="collapse-toggle"
          type="button"
          :aria-label="isSidebarCollapsed ? '展开侧边栏' : '折叠侧边栏'"
          @click="toggleSidebar"
        >
          <el-icon class="collapse-toggle-icon">
            <ArrowLeft />
          </el-icon>
        </button>
      </div>

      <div class="user-box">
        <button class="avatar-trigger" type="button" @click="router.push('/user/center')">
          <UserAvatar :src="authStore.profile?.avatarUrl" :nickname="authStore.profile?.nickname" :size="52" />
        </button>
        <div class="user-meta">
          <p class="nickname">{{ authStore.profile?.nickname || '同学' }}</p>
          <p class="major">{{ displayMajor }}</p>
        </div>
        <button
          v-if="isSidebarCollapsed"
          class="collapse-toggle collapse-toggle-under-avatar"
          type="button"
          aria-label="Expand sidebar"
          @click="toggleSidebar"
        >
          <el-icon class="collapse-toggle-icon">
            <ArrowRight />
          </el-icon>
        </button>
      </div>

      <el-menu
        :default-active="activePath"
        :collapse="isSidebarCollapsed"
        :collapse-transition="false"
        popper-effect="light"
        class="menu"
        @select="handleMenu"
      >
        <el-menu-item v-for="item in sidebarMenuItems" :key="item.path" :index="item.path">
          <el-icon :aria-label="item.ariaLabel">
            <component :is="item.icon" />
          </el-icon>
          <template #title>
            <span class="menu-label">{{ item.label }}</span>
          </template>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="content-area">
      <div class="topbar surface-card glass-panel">
        <el-input
          v-model="keyword"
          placeholder="全局搜索：错题 / 资料 / 笔记 / 标签 / 语言"
          clearable
          @keyup.enter="goSearch"
        >
          <template #append>
            <el-button type="primary" @click="goSearch">搜索</el-button>
          </template>
        </el-input>

        <div class="actions">
          <el-button type="primary" plain @click="router.push('/note/add')">新建笔记</el-button>
          <el-button class="profile-entry" @click="router.push('/user/center')">
            <UserAvatar :src="authStore.profile?.avatarUrl" :nickname="authStore.profile?.nickname" :size="26" />
            <span>个人中心</span>
          </el-button>
          <el-button type="danger" plain @click="onLogout">退出登录</el-button>
        </div>
      </div>

      <div class="decor-divider layout-divider" aria-hidden="true"></div>

      <div class="view surface-card glass-panel" :class="{ 'fade-in': !disableViewFadeIn, 'no-scroll': disableViewScroll }">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserAvatar from '../common/UserAvatar.vue'
import { SIDEBAR_MENU_ITEMS, resolveSidebarActivePath } from '../../config/sidebar-menu'
import { logout } from '../../api/user'
import { useAuthStore } from '../../store/auth'
import { toZhMajorLabel } from '../../utils/major'

const SIDEBAR_STORAGE_KEY = 'eb_sidebar_collapsed'
const SIDEBAR_TOGGLE_EVENT = 'eb:sidebar-toggle-request'
const LAYOUT_SCROLL_LOCK_CLASS = 'app-layout-scroll-locked'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const keyword = ref(route.query.keyword || '')

const sidebarMenuItems = SIDEBAR_MENU_ITEMS
const sidebarCollapsed = ref(false)
const hasSidebarPreference = ref(false)
let layoutDisposed = false

const isSidebarCollapsed = computed(() => sidebarCollapsed.value)

const displayMajor = computed(() => {
  const major = authStore.profile?.major
  return major ? toZhMajorLabel(major) : '未设置专业'
})

const activePath = computed(() => resolveSidebarActivePath(route.path))
const disableViewFadeIn = computed(() => Boolean(route.meta?.disableViewFadeIn))
const disableViewScroll = computed(() => Boolean(route.meta?.disableViewScroll))

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = value || ''
  }
)

function restoreSidebarPreference() {
  try {
    const value = localStorage.getItem(SIDEBAR_STORAGE_KEY)
    if (value === '1' || value === '0') {
      sidebarCollapsed.value = value === '1'
      hasSidebarPreference.value = true
    }
  } catch (_) {
    hasSidebarPreference.value = false
  }
}

function applyResponsiveDefault() {
  if (typeof window === 'undefined' || hasSidebarPreference.value) {
    return
  }
  sidebarCollapsed.value = window.innerWidth < 1200
}

function persistSidebarPreference() {
  hasSidebarPreference.value = true
  try {
    localStorage.setItem(SIDEBAR_STORAGE_KEY, sidebarCollapsed.value ? '1' : '0')
  } catch (_) {
    // ignore storage write failure
  }
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  persistSidebarPreference()
}

function handleMenu(index) {
  router.push(index)
}

function goSearch() {
  if (!keyword.value?.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  router.push({ path: '/search/result', query: { keyword: keyword.value.trim() } })
}

async function onLogout() {
  await logout()
  authStore.clearProfile()
  ElMessage.success('已退出登录')
  router.push('/index')
}

function shouldIgnoreShortcutTarget(target) {
  if (!target) {
    return false
  }
  const tagName = target.tagName?.toLowerCase?.()
  return tagName === 'input'
    || tagName === 'textarea'
    || tagName === 'select'
    || target.isContentEditable
}

function onGlobalKeydown(event) {
  if (!(event.ctrlKey && String(event.key).toLowerCase() === 'b')) {
    return
  }
  if (shouldIgnoreShortcutTarget(event.target)) {
    return
  }
  event.preventDefault()
  toggleSidebar()
}

function onWindowResize() {
  applyResponsiveDefault()
}

function onExternalSidebarToggle() {
  toggleSidebar()
}

function setLayoutScrollLocked(locked) {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.classList.toggle(LAYOUT_SCROLL_LOCK_CLASS, locked)
  document.body.classList.toggle(LAYOUT_SCROLL_LOCK_CLASS, locked)
}

onMounted(async () => {
  layoutDisposed = false
  setLayoutScrollLocked(true)
  await authStore.bootstrap()
  if (layoutDisposed) {
    return
  }
  restoreSidebarPreference()
  applyResponsiveDefault()
  window.addEventListener('keydown', onGlobalKeydown)
  window.addEventListener('resize', onWindowResize)
  window.addEventListener(SIDEBAR_TOGGLE_EVENT, onExternalSidebarToggle)
})

onBeforeUnmount(() => {
  layoutDisposed = true
  setLayoutScrollLocked(false)
  window.removeEventListener('keydown', onGlobalKeydown)
  window.removeEventListener('resize', onWindowResize)
  window.removeEventListener(SIDEBAR_TOGGLE_EVENT, onExternalSidebarToggle)
})
</script>

<style scoped>
:global(html.app-layout-scroll-locked),
:global(body.app-layout-scroll-locked) {
  overflow-y: hidden;
}

.layout-shell {
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr);
  gap: 16px;
  height: 100vh;
  min-height: 100vh;
  padding: 16px;
  overflow: hidden;
  position: relative;
  isolation: isolate;
}

.layout-shell::before,
.layout-shell::after {
  content: '';
  position: absolute;
  pointer-events: none;
  z-index: -1;
}

.layout-shell::before {
  inset: 0;
  background:
    radial-gradient(circle at 12% 7%, rgba(201, 100, 66, 0.11), rgba(201, 100, 66, 0) 38%),
    radial-gradient(circle at 88% 24%, rgba(98, 124, 101, 0.12), rgba(98, 124, 101, 0) 34%);
}

.layout-shell::after {
  width: clamp(180px, 18vw, 300px);
  height: clamp(60px, 8vw, 100px);
  right: 14px;
  top: 10px;
  border-radius: 999px;
  border: 1px dashed rgba(201, 100, 66, 0.32);
  transform: rotate(-7deg);
  opacity: 0.5;
}

.layout-shell.is-sidebar-collapsed {
  grid-template-columns: 88px minmax(0, 1fr);
}

.sidebar {
  --sidebar-menu-item-min-height: 38px;
  --sidebar-menu-item-gap: clamp(4px, 0.7vh, 8px);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 16px;
  align-self: start;
  height: calc(100vh - 32px);
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  background:
    linear-gradient(170deg, color-mix(in srgb, var(--surface-soft) 44%, transparent) 0%, transparent 38%),
    color-mix(in srgb, var(--surface) 92%, transparent);
}

.sidebar::before {
  content: '';
  position: absolute;
  top: -42px;
  right: -38px;
  width: 160px;
  height: 112px;
  border-radius: 999px;
  background: radial-gradient(circle at center, rgba(201, 100, 66, 0.2), rgba(201, 100, 66, 0));
  pointer-events: none;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.brand {
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}

.brand-main {
  font-family: var(--font-serif);
  font-size: 27px;
  color: var(--text-main);
  white-space: nowrap;
  transition: opacity 0.2s ease, width 0.2s ease;
}

.brand-mini {
  width: 0;
  opacity: 0;
  overflow: hidden;
  font-family: var(--font-serif);
  font-size: 22px;
  color: var(--text-main);
  transition: opacity 0.2s ease, width 0.2s ease;
}

.layout-shell.is-sidebar-collapsed .brand-main {
  width: 0;
  opacity: 0;
  overflow: hidden;
}

.layout-shell.is-sidebar-collapsed .brand-mini {
  width: auto;
  opacity: 1;
}

.layout-shell.is-sidebar-collapsed .sidebar-head {
  justify-content: center;
}

.collapse-toggle {
  width: 34px;
  min-width: 34px;
  height: 34px;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface-soft);
  color: var(--text-main);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.collapse-toggle:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.collapse-toggle-icon {
  font-size: 16px;
}

.user-box {
  border-radius: 14px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  padding: 8px 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  overflow: hidden;
}

.avatar-trigger {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}

.user-meta {
  min-width: 0;
  transition: opacity 0.2s ease, width 0.2s ease;
}

.nickname {
  margin: 0;
  font-size: 15px;
  color: var(--text-main);
}

.major {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-sub);
}

.layout-shell.is-sidebar-collapsed .user-box {
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  padding: 8px 6px;
  min-height: 0;
}

.layout-shell.is-sidebar-collapsed .user-meta {
  display: none;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}

.layout-shell.is-sidebar-collapsed .avatar-trigger {
  width: 100%;
}

.layout-shell.is-sidebar-collapsed .avatar-trigger :deep(.user-avatar) {
  margin: 0 auto;
}

.collapse-toggle-under-avatar {
  width: 30px;
  min-width: 30px;
  height: 30px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: var(--sidebar-menu-item-gap);
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: hidden;
  overflow-x: hidden;
  padding-bottom: 0;
}

.menu :deep(.el-menu-item) {
  border-radius: 10px;
  flex: 1 1 0;
  min-height: var(--sidebar-menu-item-min-height);
  height: auto;
  line-height: 1.25;
  margin: 0;
  display: flex;
  align-items: center;
  color: var(--text-accent);
  transition: transform 0.22s ease, border-color 0.22s ease, background-color 0.22s ease, color 0.22s ease;
}

.menu :deep(.el-menu-item .el-icon) {
  transition: transform 0.22s ease;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(201, 100, 66, 0.08);
  color: var(--primary);
  transform: translateY(-1px);
}

.menu :deep(.el-menu-item:hover .el-icon) {
  transform: translateX(1px) rotate(-6deg);
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(201, 100, 66, 0.15);
  color: var(--primary);
  border: 1px solid rgba(201, 100, 66, 0.35);
}

.layout-shell.is-sidebar-collapsed .menu :deep(.el-menu-item) {
  padding: 0 !important;
  margin: 0;
  justify-content: center;
}

.content-area {
  min-width: 0;
  min-height: 0;
  height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
}

.topbar {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  overflow: hidden;
}

.topbar::after {
  content: '';
  position: absolute;
  right: -22px;
  top: -22px;
  width: 140px;
  height: 84px;
  border-radius: 999px;
  border: 1px dashed rgba(201, 100, 66, 0.34);
  opacity: 0.5;
  pointer-events: none;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.layout-divider {
  margin: -2px 0;
}

.view {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 18px;
  scroll-behavior: smooth;
  background:
    linear-gradient(178deg, color-mix(in srgb, var(--surface) 90%, transparent) 0%, color-mix(in srgb, var(--surface-soft) 60%, transparent) 100%);
}

.view::before {
  content: '';
  position: absolute;
  right: 14px;
  top: 12px;
  width: 92px;
  height: 42px;
  border-radius: 999px;
  border: 1px dashed rgba(201, 100, 66, 0.28);
  opacity: 0.35;
  pointer-events: none;
}

.view.no-scroll {
  overflow: hidden;
}

.glass-panel {
  backdrop-filter: blur(10px);
}

@media (max-width: 960px) {
  .layout-shell {
    grid-template-columns: 1fr;
    height: auto;
    min-height: auto;
    padding: 8px;
    overflow: visible;
  }

  .layout-shell.is-sidebar-collapsed {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    top: auto;
    align-self: stretch;
    height: auto;
    min-height: auto;
  }

  .menu {
    max-height: none;
  }

  .topbar {
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: flex-start;
  }

  .content-area {
    height: auto;
  }

  .view {
    min-height: calc(100vh - 240px);
    overflow: visible;
  }

  .layout-shell::after,
  .topbar::after,
  .view::before {
    opacity: 0.24;
  }
}
</style>
