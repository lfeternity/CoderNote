<template>
  <header class="public-nav">
    <button class="brand" type="button" @click="go('/index')">
      <span class="brand-main">CoderNote</span>
      <span class="brand-sub">记录错题，沉淀认知</span>
    </button>

    <button class="menu-toggle" type="button" @click="menuOpen = !menuOpen">
      {{ menuOpen ? '关闭' : '菜单' }}
    </button>

    <nav class="nav-links" :class="{ 'is-open': menuOpen }">
      <template v-if="authStore.isLoggedIn">
        <el-button
          class="nav-btn"
          :class="{ 'is-active': isActive('/index') }"
          @click="go('/index')"
        >
          首页
        </el-button>
        <el-button
          class="nav-btn"
          :class="{ 'is-active': isActive('/error-question/list') }"
          @click="go('/error-question/list')"
        >
          错题
        </el-button>
        <el-button
          class="nav-btn"
          :class="{ 'is-active': isActive('/study-material/list') }"
          @click="go('/study-material/list')"
        >
          资料
        </el-button>
        <el-button
          class="nav-btn"
          :class="{ 'is-active': isActive('/review/center') }"
          @click="go('/review/center')"
        >
          复习
        </el-button>
        <el-button
          class="nav-btn"
          :class="{ 'is-active': isActive('/study-material/favorite') }"
          @click="go('/study-material/favorite')"
        >
          收藏
        </el-button>
        <el-button class="nav-btn nav-note-btn" @click="go('/note/add/fullscreen')">新建笔记</el-button>
        <el-button class="profile-btn" @click="go('/user/center')">
          <UserAvatar :src="authStore.profile?.avatarUrl" :nickname="authStore.profile?.nickname" :size="24" />
          <span>个人中心</span>
        </el-button>
      </template>

      <template v-else>
        <el-button class="nav-btn" :class="{ 'is-active': isActive('/index') }" @click="go('/index')">
          首页
        </el-button>
        <el-button class="nav-btn" :class="{ 'is-active': isActive('/login') }" @click="go('/login')">
          登录
        </el-button>
        <el-button class="nav-btn is-cta" :class="{ 'is-active': isActive('/register') }" @click="go('/register')">
          注册
        </el-button>
      </template>
    </nav>
  </header>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserAvatar from '../common/UserAvatar.vue'
import { useAuthStore } from '../../store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const menuOpen = ref(false)

const currentPath = computed(() => route.path)

function go(path) {
  menuOpen.value = false
  router.push(path)
}

function isActive(path) {
  if (path === '/index') {
    return currentPath.value === '/' || currentPath.value === '/index'
  }
  return currentPath.value.startsWith(path)
}

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)

onMounted(async () => {
  await authStore.bootstrap()
})
</script>

<style scoped>
.public-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  width: calc(100% - clamp(20px, 3vw, 56px));
  margin: clamp(10px, 1.2vw, 18px) auto 0;
  padding: clamp(10px, 1.2vw, 14px);
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 16px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 92%, transparent);
  backdrop-filter: blur(8px);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--border-soft) 76%, transparent), var(--shadow);
  overflow: hidden;
}

.public-nav::before,
.public-nav::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.public-nav::before {
  width: 170px;
  height: 80px;
  right: -40px;
  top: -30px;
  border-radius: 999px;
  background: radial-gradient(circle at center, rgba(201, 100, 66, 0.2), rgba(201, 100, 66, 0));
}

.public-nav::after {
  left: 18px;
  right: 18px;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(201, 100, 66, 0.38) 50%, transparent 100%);
  opacity: 0.66;
}

.brand {
  padding: 0;
  border: none;
  background: transparent;
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  cursor: pointer;
}

.brand-main {
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 500;
  line-height: 1;
  color: var(--text-main);
}

.brand-sub {
  font-size: 12px;
  line-height: 1.2;
  letter-spacing: 0.1em;
  color: var(--text-sub);
}

.menu-toggle {
  display: none;
  justify-self: end;
  min-width: 64px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-main);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.menu-toggle:hover {
  transform: translateY(-1px);
  border-color: var(--primary);
  color: var(--primary);
}

.nav-links {
  justify-self: end;
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.nav-btn {
  height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  color: var(--text-accent);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.nav-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px -24px rgba(201, 100, 66, 0.85);
}

.nav-btn.is-active {
  border-color: var(--primary);
  background: rgba(201, 100, 66, 0.12);
  color: var(--primary);
}

.nav-btn.is-cta {
  border-color: var(--primary);
  background: var(--primary);
  color: var(--color-ivory);
}

.nav-note-btn {
  border-color: rgba(201, 100, 66, 0.55);
  background: rgba(201, 100, 66, 0.12);
  color: var(--primary);
}

.profile-btn {
  height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  backdrop-filter: blur(4px);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.profile-btn:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--primary) 42%, var(--border-soft));
}

:global(html[data-theme='dark'] .public-nav) {
  border-color: var(--border);
  background: color-mix(in srgb, var(--surface) 94%, transparent);
  box-shadow: 0 0 0 1px rgba(63, 63, 58, 0.82), var(--shadow);
}

:global(html[data-theme='dark'] .public-nav .menu-toggle),
:global(html[data-theme='dark'] .public-nav .nav-btn),
:global(html[data-theme='dark'] .public-nav .profile-btn) {
  border-color: var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-main);
}

:global(html[data-theme='dark'] .public-nav .nav-btn.is-active) {
  border-color: rgba(217, 119, 87, 0.8);
  background: rgba(217, 119, 87, 0.2);
  color: #fbeee8;
}

:global(html[data-theme='dark'] .public-nav .nav-note-btn) {
  border-color: rgba(217, 119, 87, 0.75);
  background: rgba(217, 119, 87, 0.22);
  color: #ffe9df;
}

:global(html[data-theme='dark'] .public-nav::after) {
  background: linear-gradient(90deg, transparent 0%, rgba(217, 119, 87, 0.4) 50%, transparent 100%);
}

@media (max-width: 900px) {
  .public-nav {
    width: calc(100% - 16px);
    margin-top: 8px;
    grid-template-columns: 1fr auto;
    gap: 10px;
    padding: 10px;
  }

  .brand-main {
    font-size: 22px;
  }

  .menu-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .nav-links {
    display: none;
    grid-column: 1 / -1;
    width: 100%;
    justify-self: stretch;
    padding-top: 4px;
  }

  .nav-links.is-open {
    display: flex;
  }

  .nav-btn,
  .profile-btn {
    width: 100%;
    justify-content: center;
  }

  .public-nav::before {
    opacity: 0.55;
  }
}
</style>
