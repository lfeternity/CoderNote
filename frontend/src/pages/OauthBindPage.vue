<template>
  <div class="oauth-shell">
    <PublicTopNav />

    <section class="oauth-stage">
      <div class="surface-card oauth-card fade-in">
        <p class="platform-chip">{{ pending.platformName || '第三方平台' }} 登录</p>
        <h2>欢迎使用 {{ pending.platformName || '第三方账号' }} 登录</h2>
        <p class="desc">
          当前账号尚未绑定 CoderNote，请先绑定已有账号，或自动创建新账号继续。
        </p>

        <div class="profile-preview">
          <img v-if="pending.avatarUrl" :src="pending.avatarUrl" alt="平台头像">
          <div v-else class="avatar-fallback">{{ (pending.nickname || 'U').slice(0, 1).toUpperCase() }}</div>
          <div class="profile-meta">
            <span>平台昵称</span>
            <strong>{{ pending.nickname || '-' }}</strong>
          </div>
        </div>

        <div class="mode-switch">
          <button :class="{ active: mode === 'auto' }" @click="mode = 'auto'">自动创建新账号</button>
          <button :class="{ active: mode === 'bind' }" @click="mode = 'bind'">绑定已有账号</button>
        </div>

        <el-form v-if="mode === 'bind'" label-width="94px" class="mode-form">
          <el-form-item label="账号昵称">
            <el-input v-model="bindForm.nickname" placeholder="请输入已有账号昵称" />
          </el-form-item>
          <el-form-item label="账号密码">
            <el-input v-model="bindForm.password" type="password" show-password placeholder="请输入账号密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="onBindExisting">绑定并登录</el-button>
          </el-form-item>
        </el-form>

        <el-form v-else label-width="94px" class="mode-form">
          <el-form-item label="昵称">
            <el-input v-model="autoForm.nickname" maxlength="10" placeholder="可编辑平台昵称" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="onAutoRegister">创建并登录</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PublicTopNav from '../components/layout/PublicTopNav.vue'
import { getProfile, getOauthPending, oauthAutoRegister, oauthBindExisting } from '../api/user'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const mode = ref('auto')

const pending = reactive({
  platform: '',
  platformName: '',
  nickname: '',
  avatarUrl: '',
  redirectPath: '/error-question/list'
})

const bindForm = reactive({
  nickname: '',
  password: ''
})

const autoForm = reactive({
  nickname: ''
})

const bindToken = computed(() => String(route.query.bindToken || ''))

function safeRedirectPath(path) {
  if (!path || typeof path !== 'string' || !path.startsWith('/')) {
    return '/error-question/list'
  }
  if (path.startsWith('//')) {
    return '/error-question/list'
  }
  return path
}

async function finishLoginSuccess(successMessage) {
  const profile = await getProfile()
  authStore.setProfile(profile)
  ElMessage.success(successMessage)
  router.replace(safeRedirectPath(pending.redirectPath))
}

async function loadPending() {
  if (!bindToken.value) {
    ElMessage.error('绑定凭证无效，请重新登录')
    router.replace('/login')
    return
  }
  try {
    const data = await getOauthPending(bindToken.value)
    Object.assign(pending, data || {})
    if (!autoForm.nickname) {
      autoForm.nickname = data?.nickname || ''
    }
  } catch (_) {
    router.replace('/login')
  }
}

async function onBindExisting() {
  if (!bindForm.nickname || !bindForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await oauthBindExisting({
      bindToken: bindToken.value,
      nickname: bindForm.nickname,
      password: bindForm.password
    })
    await finishLoginSuccess('绑定成功，已登录')
  } finally {
    loading.value = false
  }
}

async function onAutoRegister() {
  loading.value = true
  try {
    await oauthAutoRegister({
      bindToken: bindToken.value,
      nickname: autoForm.nickname
    })
    await finishLoginSuccess('创建成功，已登录')
  } finally {
    loading.value = false
  }
}

watch(bindToken, () => {
  loadPending()
}, { immediate: true })
</script>

<style scoped>
.oauth-shell {
  min-height: 100vh;
  padding-bottom: 20px;
}

.oauth-stage {
  width: min(900px, calc(100% - 28px));
  margin: 18px auto 0;
}

.oauth-card {
  padding: 24px;
}

.platform-chip {
  display: inline-flex;
  margin: 0;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-accent);
  font-size: 12px;
  align-items: center;
}

.oauth-card h2 {
  margin-top: 12px;
  font-size: clamp(32px, 3.2vw, 44px);
  line-height: 1.16;
}

.desc {
  margin: 10px 0 0;
  color: var(--text-sub);
}

.profile-preview {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid var(--border-soft);
  border-radius: 14px;
  background: var(--surface-soft);
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-preview img,
.avatar-fallback {
  width: 52px;
  height: 52px;
  border-radius: 12px;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 24px;
}

.profile-meta span {
  color: var(--text-sub);
  font-size: 13px;
}

.profile-meta strong {
  display: block;
  margin-top: 4px;
  color: var(--text-main);
}

.mode-switch {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.mode-switch button {
  min-height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  color: var(--text-accent);
  cursor: pointer;
}

.mode-switch button.active {
  border-color: var(--primary);
  background: rgba(201, 100, 66, 0.12);
  color: var(--primary);
}

.mode-form {
  margin-top: 16px;
}

@media (max-width: 700px) {
  .oauth-stage {
    width: calc(100% - 16px);
    margin-top: 10px;
  }

  .oauth-card {
    padding: 16px 14px;
  }

  .mode-switch {
    grid-template-columns: 1fr;
  }
}
</style>
