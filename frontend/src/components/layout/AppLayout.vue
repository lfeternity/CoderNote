<template>
  <div class="layout-shell" :class="{ 'is-sidebar-collapsed': isSidebarCollapsed }">
    <aside class="sidebar surface-card">
      <div class="brand" :title="isSidebarCollapsed ? 'CoderNote' : ''">
        <span class="brand-main">CoderNote</span>
        <span class="brand-mini">CN</span>
      </div>

      <div class="user-box">
        <div class="user-top">
          <el-tooltip content="更换头像" placement="right">
            <button class="avatar-trigger" type="button" @click="router.push('/user/center')">
              <UserAvatar :src="authStore.profile?.avatarUrl" :nickname="authStore.profile?.nickname" :size="54" />
            </button>
          </el-tooltip>
          <div class="user-meta">
            <p class="nickname">{{ authStore.profile?.nickname || '同学' }}</p>
            <p class="major">{{ displayMajor }}</p>
          </div>
        </div>
      </div>

      <button
        class="collapse-toggle"
        type="button"
        :aria-label="isSidebarCollapsed ? '展开侧边栏' : '折叠侧边栏'"
        @click="toggleSidebar"
      >
        <el-icon class="collapse-toggle-icon">
          <ArrowRight v-if="isSidebarCollapsed" />
          <ArrowLeft v-else />
        </el-icon>
        <span class="collapse-toggle-text">{{ isSidebarCollapsed ? '展开侧边栏' : '折叠侧边栏' }}</span>
      </button>

      <el-menu
        :default-active="activePath"
        :collapse="isSidebarCollapsed"
        :collapse-transition="false"
        popper-effect="light"
        class="menu"
        @select="handleMenu"
      >
        <el-menu-item
          v-for="item in sidebarMenuItems"
          :key="item.path"
          :index="item.path"
        >
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
      <div class="topbar surface-card">
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
            <UserAvatar :src="authStore.profile?.avatarUrl" :nickname="authStore.profile?.nickname" :size="28" />
            <span>个人中心</span>
          </el-button>
          <el-button type="danger" plain @click="onLogout">退出登录</el-button>
        </div>
      </div>

      <div class="view surface-card" :class="{ 'fade-in': !disableViewFadeIn }">
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

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const keyword = ref(route.query.keyword || '')

const sidebarMenuItems = SIDEBAR_MENU_ITEMS
const sidebarCollapsed = ref(false)
const hasSidebarPreference = ref(false)

const isSidebarCollapsed = computed(() => sidebarCollapsed.value)

const displayMajor = computed(() => {
  const major = authStore.profile?.major
  return major ? toZhMajorLabel(major) : '未设置专业'
})

const activePath = computed(() => resolveSidebarActivePath(route.path))
const disableViewFadeIn = computed(() => Boolean(route.meta?.disableViewFadeIn))

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
onMounted(async () => {
  await authStore.bootstrap()
  restoreSidebarPreference()
  applyResponsiveDefault()
  window.addEventListener('keydown', onGlobalKeydown)
  window.addEventListener('resize', onWindowResize)
  window.addEventListener(SIDEBAR_TOGGLE_EVENT, onExternalSidebarToggle)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
  window.removeEventListener('resize', onWindowResize)
  window.removeEventListener(SIDEBAR_TOGGLE_EVENT, onExternalSidebarToggle)
})
</script>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 14px;
  height: 100vh;
  padding: 14px;
  overflow: hidden;
  transition: grid-template-columns 0.3s ease;
}

.layout-shell.is-sidebar-collapsed {
  grid-template-columns: 90px minmax(0, 1fr);
}

.sidebar {
  padding: 16px;
  position: sticky;
  top: 14px;
  height: calc(100vh - 28px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: padding 0.3s ease;
}

.layout-shell.is-sidebar-collapsed .sidebar {
  padding: 12px 10px;
}

.brand {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  min-height: 30px;
}

.brand-main {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary);
  white-space: nowrap;
  transition: opacity 0.25s ease, width 0.25s ease;
}

.brand-mini {
  width: 0;
  opacity: 0;
  overflow: hidden;
  font-size: 18px;
  font-weight: 800;
  color: var(--primary);
  transition: opacity 0.25s ease, width 0.25s ease;
}

.layout-shell.is-sidebar-collapsed .brand {
  justify-content: center;
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

.user-box {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
  padding: 10px;
  margin-bottom: 10px;
  transition: padding 0.3s ease;
}

.user-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.layout-shell.is-sidebar-collapsed .user-box {
  padding: 8px;
}

.layout-shell.is-sidebar-collapsed .user-top {
  justify-content: center;
}

.avatar-trigger {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.user-meta {
  min-width: 0;
  transition: opacity 0.25s ease, width 0.25s ease, margin 0.25s ease;
}

.layout-shell.is-sidebar-collapsed .user-meta {
  width: 0;
  margin: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.nickname {
  margin: 0;
  font-weight: 600;
  white-space: nowrap;
}

.major {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 12px;
  white-space: nowrap;
}

.collapse-toggle {
  width: 100%;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  border-radius: 10px;
  min-height: 36px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-accent);
  margin-bottom: 10px;
  transition: all 0.2s ease;
}

.collapse-toggle:hover {
  background: var(--surface-soft-hover);
  border-color: var(--primary);
  color: var(--text-accent-strong);
}

.collapse-toggle-icon {
  font-size: 16px;
}

.collapse-toggle-text {
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  transition: opacity 0.25s ease, width 0.25s ease;
}

.layout-shell.is-sidebar-collapsed .collapse-toggle {
  padding: 0;
}

.layout-shell.is-sidebar-collapsed .collapse-toggle-text {
  width: 0;
  opacity: 0;
  overflow: hidden;
}

.menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 8px;
  -webkit-overflow-scrolling: touch;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.menu::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.menu :deep(.el-menu-item) {
  border-radius: 10px;
  margin: 4px 0;
  transition: background-color 0.2s ease;
}

.menu :deep(.el-menu-item .el-icon),
.menu :deep(.el-menu-item .menu-label) {
  transition: transform 0.2s ease;
}

.menu :deep(.el-menu-item:hover .el-icon),
.menu :deep(.el-menu-item:hover .menu-label) {
  transform: scale(1.05);
}

.layout-shell.is-sidebar-collapsed .menu :deep(.el-menu-item) {
  padding: 0 !important;
  justify-content: center;
}

.content-area {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.topbar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.profile-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.view {
  padding: 16px;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

@media (max-width: 768px) {
  .layout-shell {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .sidebar {
    position: static;
    height: auto;
  }

  .content-area {
    overflow: visible;
  }

  .view {
    overflow: visible;
    min-height: calc(100vh - 140px);
  }

  .topbar {
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: flex-start;
  }
}
</style>
