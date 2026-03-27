<template>
  <div class="auth-shell">
    <PublicTopNav />

    <section class="auth-stage">
      <div class="bg-layer" aria-hidden="true">
        <span class="halo halo-a" />
        <span class="halo halo-b" />
        <span class="halo halo-c" />
        <span class="trace trace-a" />
        <span class="trace trace-b" />
      </div>

      <div class="auth-layout">
        <aside class="surface-card intro-card fade-in">
          <p class="eyebrow">WELCOME BACK</p>
          <h2>继续你的编程复盘</h2>
          <p class="intro-text">
            登录后即可回到你的学习轨道，快速查看错题管理、资料联动与个人复习进度。
          </p>

          <div class="chip-row">
            <span># 错题追踪</span>
            <span># 标签联动</span>
            <span># 复习节奏</span>
          </div>

          <div class="stat-grid">
            <article class="stat-item">
              <strong>Tag Link</strong>
              <p>知识点自动关联资料</p>
            </article>
            <article class="stat-item">
              <strong>Review</strong>
              <p>持续积累复习轨迹</p>
            </article>
          </div>
        </aside>

        <div class="surface-card form-card fade-in">
          <div class="card-decor" aria-hidden="true" />
          <h2>登录</h2>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password />
            </el-form-item>
            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="form.captchaCode"
                  maxlength="8"
                  placeholder="请输入右侧图形验证码"
                />
                <img
                  class="captcha-image"
                  :src="captchaImage"
                  alt="验证码"
                  title="点击刷新验证码"
                  @click="loadCaptcha"
                >
                <el-button class="captcha-refresh" native-type="button" @click="loadCaptcha">换一张</el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="onSubmit">登录</el-button>
              <el-button @click="$router.push('/register')">没有账号？去注册</el-button>
            </el-form-item>
          </el-form>

          <div class="oauth-divider">
            <span>其他登录方式</span>
          </div>

          <div class="oauth-grid">
            <button
              v-for="item in oauthPlatforms"
              :key="item.code"
              class="oauth-button"
              type="button"
              :style="{ '--brand': item.color }"
              @click="onOauthLogin(item)"
            >
              <span class="oauth-icon">{{ item.icon }}</span>
              <span>{{ item.label }}登录</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PublicTopNav from '../components/layout/PublicTopNav.vue'
import { getProfile, getRegisterCaptcha, login } from '../api/user'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref(null)
const captchaImage = ref('')
const oauthPlatforms = [
  { code: 'qq', label: 'QQ', icon: 'Q', color: '#12B7F5' },
  { code: 'wechat', label: '微信', icon: '微', color: '#07C160' },
  { code: 'github', label: 'GitHub', icon: 'GH', color: '#24292F' }
]

const form = reactive({
  nickname: '',
  password: '',
  captchaCode: ''
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  const data = await getRegisterCaptcha()
  captchaImage.value = data?.imageBase64 ? `data:image/png;base64,${data.imageBase64}` : ''
  form.captchaCode = ''
}

loadCaptcha()
onMounted(() => {
  const oauthMessage = typeof route.query.oauthMessage === 'string'
    ? route.query.oauthMessage.trim()
    : ''
  if (oauthMessage) {
    ElMessage.info(oauthMessage)
  }
})

function onOauthLogin(item) {
  const redirectPath = typeof route.query.redirect === 'string'
    ? route.query.redirect
    : '/error-question/list'
  const query = new URLSearchParams({
    clientBaseUrl: window.location.origin,
    redirect: redirectPath
  })
  window.location.href = `/api/v1/user/oauth/login/authorize/${item.code}?${query.toString()}`
}

function onSubmit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await login(form)
      const profile = await getProfile()
      authStore.setProfile(profile)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/error-question/list'
      router.push(redirect)
    } catch (error) {
      await loadCaptcha()
    } finally {
      loading.value = false
    }
  })
}
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
.auth-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(160deg, #f4f8ff 0%, #ebf1fd 55%, #e4ebf8 100%);
}

.auth-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(125deg, rgba(255, 255, 255, 0.86), rgba(240, 246, 255, 0.65)),
    url('../assets/home-tech-bg.svg') center/cover no-repeat;
  opacity: 0.42;
  pointer-events: none;
}

.auth-stage {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  width: 100%;
  padding: 18px 20px 34px;
}

.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.halo {
  position: absolute;
  border-radius: 999px;
  filter: blur(1px);
}

.halo-a {
  width: 360px;
  height: 360px;
  left: -70px;
  top: 80px;
  background: radial-gradient(circle, rgba(30, 64, 175, 0.2), rgba(30, 64, 175, 0));
  animation: floatHalo 8s ease-in-out infinite;
}

.halo-b {
  width: 280px;
  height: 280px;
  right: -40px;
  top: 160px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.2), rgba(14, 165, 233, 0));
  animation: floatHalo 7.1s ease-in-out infinite reverse;
}

.halo-c {
  width: 260px;
  height: 260px;
  right: 24%;
  bottom: -90px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.16), rgba(16, 185, 129, 0));
  animation: floatHalo 9s ease-in-out infinite;
}

.trace {
  position: absolute;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.4), transparent);
}

.trace-a {
  width: 360px;
  left: 18%;
  top: 190px;
}

.trace-b {
  width: 320px;
  right: 12%;
  bottom: 180px;
}

.auth-layout {
  width: min(1240px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(460px, 1fr);
  gap: 18px;
  align-items: stretch;
}

.intro-card {
  position: relative;
  overflow: hidden;
  padding: 28px 24px;
  border: 1px solid #c7d8ff;
  background: linear-gradient(170deg, rgba(255, 255, 255, 0.9), rgba(243, 248, 255, 0.82));
}

.intro-card::after {
  content: '';
  position: absolute;
  width: 180px;
  height: 180px;
  right: -55px;
  top: -55px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(30, 64, 175, 0.2), rgba(30, 64, 175, 0));
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #2f5fd4;
  font-weight: 700;
}

.intro-card h2 {
  margin: 10px 0 12px;
  color: #173f9f;
  font-size: clamp(24px, 2.2vw, 30px);
}

.intro-text {
  margin: 0;
  color: #425985;
  line-height: 1.8;
}

.chip-row {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip-row span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #c7d8ff;
  background: rgba(255, 255, 255, 0.72);
  color: #2c50b0;
  font-size: 12px;
}

.stat-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-item {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #d3e0ff;
  background: rgba(248, 251, 255, 0.86);
}

.stat-item strong {
  color: #2849a5;
  font-size: 13px;
}

.stat-item p {
  margin: 6px 0 0;
  color: #5770a7;
  font-size: 12px;
}

.form-card {
  position: relative;
  overflow: hidden;
  padding: 26px 28px;
  border: 1px solid #bfd2fb;
  background: linear-gradient(175deg, rgba(255, 255, 255, 0.95), rgba(245, 249, 255, 0.92));
}

.card-decor {
  position: absolute;
  width: 220px;
  height: 220px;
  right: -88px;
  top: -110px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(45, 92, 232, 0.2), rgba(45, 92, 232, 0));
  pointer-events: none;
}

.form-card h2 {
  margin: 0 0 18px;
  color: var(--primary);
}

.captcha-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-row :deep(.el-input) {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border: 1px solid #c8d6ef;
  border-radius: 8px;
  background: #f5f9ff;
  cursor: pointer;
  object-fit: cover;
}

.captcha-refresh {
  height: 40px;
  border-radius: 8px;
}

.form-card :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.oauth-divider {
  margin: 16px 0 12px;
  position: relative;
  text-align: center;
}

.oauth-divider::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  border-top: 1px solid #d6e1f8;
}

.oauth-divider span {
  position: relative;
  padding: 0 10px;
  background: linear-gradient(175deg, rgba(255, 255, 255, 0.95), rgba(245, 249, 255, 0.92));
  color: #5d6f95;
  font-size: 13px;
}

.oauth-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.oauth-button {
  height: 44px;
  border-radius: 10px;
  border: 1px solid var(--brand);
  background: #ffffff;
  color: var(--brand);
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.oauth-button:hover {
  transform: scale(1.03);
  box-shadow: 0 10px 18px rgba(30, 64, 175, 0.16);
}

.oauth-icon {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  letter-spacing: 0.01em;
}

@keyframes floatHalo {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-12px);
  }
}

@media (max-width: 1024px) {
  .auth-stage {
    padding: 12px 14px 24px;
    align-items: flex-start;
  }

  .auth-layout {
    grid-template-columns: 1fr;
  }

  .intro-card {
    order: 2;
  }

  .form-card {
    order: 1;
  }
}

@media (max-width: 640px) {
  :deep(.public-nav) {
    width: calc(100% - 16px);
    margin: 8px 8px 0;
  }

  .auth-stage {
    padding: 8px 8px 16px;
  }

  .form-card,
  .intro-card {
    padding: 18px 14px;
  }

  .captcha-row {
    flex-wrap: wrap;
  }

  .captcha-image,
  .captcha-refresh {
    height: 36px;
  }

  .captcha-image {
    width: 108px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .oauth-grid {
    grid-template-columns: 1fr;
  }
}
</style>
