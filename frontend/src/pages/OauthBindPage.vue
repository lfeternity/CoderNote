<template>
  <div class="oauth-shell">
    <PublicTopNav />

    <section class="oauth-stage">
      <div class="surface-card oauth-card fade-in">
        <p class="platform-chip">{{ pending.platformName || '第三方平台' }} 登录</p>
        <h2>欢迎使用 {{ pending.platformName || '第三方账号' }} 登录</h2>
        <p class="desc">当前账号尚未绑定 CoderNote，请先绑定已有账号或自动创建新账号。</p>

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
  } catch (error) {
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
:deep(.public-nav) {
  width: calc(100% - 32px);
  max-width: none;
  margin: 10px 16px 0;
  border: 0;
  box-shadow: none;
  background: transparent;
}

.oauth-shell {
  min-height: 100vh;
  background: linear-gradient(160deg, #f4f8ff 0%, #ebf1fd 55%, #e4ebf8 100%);
}

.oauth-stage {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px 16px 28px;
}

.oauth-card {
  padding: 24px 26px;
  border: 1px solid #c7d8ff;
  background: linear-gradient(175deg, rgba(255, 255, 255, 0.95), rgba(245, 249, 255, 0.92));
}

.platform-chip {
  display: inline-flex;
  margin: 0;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e7efff;
  color: #3153a8;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  margin: 12px 0 10px;
  color: #1e3a8a;
}

.desc {
  margin: 0;
  color: #4d628c;
  line-height: 1.7;
}

.profile-preview {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #d4e0f8;
  border-radius: 12px;
  background: #f7faff;
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-preview img,
.avatar-fallback {
  width: 54px;
  height: 54px;
  border-radius: 14px;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #4f7ad8, #3153a8);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.profile-meta span {
  color: #6b7da2;
  font-size: 13px;
}

.profile-meta strong {
  display: block;
  margin-top: 4px;
  color: #2d4b8f;
  font-size: 16px;
}

.mode-switch {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.mode-switch button {
  height: 40px;
  border-radius: 10px;
  border: 1px solid #ccd8f3;
  background: #f8fbff;
  color: #3e598f;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-switch button.active {
  border-color: #4d76d8;
  background: #edf3ff;
  color: #2348a3;
  font-weight: 700;
}

.mode-form {
  margin-top: 16px;
}

@media (max-width: 700px) {
  :deep(.public-nav) {
    width: calc(100% - 16px);
    margin: 8px 8px 0;
  }

  .oauth-stage {
    padding: 10px 8px 16px;
  }

  .oauth-card {
    padding: 16px 14px;
  }

  .mode-switch {
    grid-template-columns: 1fr;
  }
}
</style>
