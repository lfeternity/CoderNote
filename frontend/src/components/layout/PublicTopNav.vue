<template>
  <header class="public-nav surface-card">
    <div class="logo" @click="go('/index')">
      <span class="brand-main">CoderNote</span>
      <span class="brand-sub">码记错题，智联资料</span>
    </div>

    <div class="nav-links">
      <template v-if="authStore.isLoggedIn">
        <el-button text @click="go('/index')">首页</el-button>
        <el-button text @click="go('/error-question/list')">错题管理</el-button>
        <el-button text @click="go('/study-material/list')">资料管理</el-button>
        <el-button text @click="go('/note/add')">新建笔记</el-button>
        <el-button text @click="go('/study-material/favorite')">我的收藏</el-button>

        <el-button class="profile-btn" @click="go('/user/center')">
          <UserAvatar :src="authStore.profile?.avatarUrl" :nickname="authStore.profile?.nickname" :size="26" />
          <span>个人中心</span>
        </el-button>
      </template>

      <template v-else>
        <el-button
          class="guest-nav-btn"
          :class="{ active: isGuestActive('/index') }"
          @click="go('/index')"
        >
          首页
        </el-button>
        <el-button
          class="guest-nav-btn"
          :class="{ active: isGuestActive('/login') }"
          @click="go('/login')"
        >
          登录
        </el-button>
        <el-button
          class="guest-nav-btn"
          :class="{ active: isGuestActive('/register') }"
          @click="go('/register')"
        >
          注册
        </el-button>
      </template>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserAvatar from '../common/UserAvatar.vue'
import { useAuthStore } from '../../store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentPath = computed(() => route.path)

function go(path) {
  router.push(path)
}

function isGuestActive(path) {
  if (path === '/index') {
    return currentPath.value === '/index' || currentPath.value === '/'
  }
  return currentPath.value === path
}

onMounted(async () => {
  await authStore.bootstrap()
})
</script>

<style scoped>
.public-nav {
  margin: 14px auto;
  max-width: 1320px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
}

.logo {
  display: inline-flex;
  align-items: baseline;
  gap: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-main {
  font-size: 18px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.brand-sub {
  font-size: 14px;
  font-weight: 600;
  color: #4f628f;
  line-height: 1;
}

.nav-links {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-btn {
  height: 36px;
  border-radius: 999px;
  border: 1px solid #c7d7ff;
  background: linear-gradient(180deg, #f8fbff 0%, #edf3ff 100%);
  color: #2f4f96;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  font-weight: 600;
}

.profile-btn:hover {
  border-color: #9fbcff;
  color: #23469b;
  background: #f3f8ff;
}

.guest-nav-btn {
  min-width: 72px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid #ccd7eb;
  background: rgba(255, 255, 255, 0.86);
  color: #3f557f;
  font-weight: 600;
}

.guest-nav-btn:hover {
  border-color: #afc1e6;
  color: #2f4f96;
  background: #f5f9ff;
}

.guest-nav-btn.active {
  border-color: #2d73ea;
  background: linear-gradient(180deg, #4ea1ff 0%, #2f8af0 100%);
  color: #fff;
}

@media (max-width: 1000px) {
  .logo {
    flex-wrap: wrap;
    gap: 6px;
  }

  .brand-sub {
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .public-nav {
    align-items: center;
    padding: 10px 12px;
  }

  .nav-links {
    gap: 6px;
  }

  .guest-nav-btn {
    min-width: 66px;
    height: 34px;
    padding: 0 10px;
  }
}
</style>
