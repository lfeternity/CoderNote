<template>
  <div class="auth-shell">
    <PublicTopNav />

    <section class="auth-stage">
      <aside class="surface-card intro-card fade-in">
        <p class="intro-overline">Welcome Back</p>
        <h2>回到你的学习现场。</h2>
        <p class="intro-text">
          登录后继续管理错题、资料和笔记，并在复习中心跟踪每日学习节奏。
        </p>
        <div class="intro-chips">
          <span>错题复盘</span>
          <span>标签联动</span>
          <span>复习调度</span>
        </div>

        <section class="intro-decoration" aria-hidden="true">
          <div class="decoration-head">
            <span>Learning Rhythm</span>
            <strong>7-Day Focus</strong>
          </div>
          <ul class="decoration-list">
            <li><i></i><span>记录错题上下文与解法</span></li>
            <li><i></i><span>按标签聚合资料与笔记</span></li>
            <li><i></i><span>按日推进复习优先级</span></li>
          </ul>
          <div class="decoration-grid">
            <article>
              <p>待复习</p>
              <strong>12</strong>
            </article>
            <article>
              <p>已沉淀</p>
              <strong>31</strong>
            </article>
            <article>
              <p>掌握率</p>
              <strong>89%</strong>
            </article>
          </div>
        </section>
      </aside>

      <div class="surface-card form-card fade-in">
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
            <span>{{ item.label }} 登录</span>
          </button>
        </div>

        <section class="form-decoration" aria-hidden="true">
          <div class="form-decoration-head">
            <span>After Sign In</span>
            <strong>继续今天的学习章节</strong>
          </div>
          <div class="form-decoration-row">
            <article>
              <p>错题管理</p>
              <small>复盘重点与解法差异</small>
            </article>
            <article>
              <p>标签库</p>
              <small>一键定位关联知识点</small>
            </article>
            <article>
              <p>复习中心</p>
              <small>按优先级持续推进计划</small>
            </article>
          </div>
        </section>
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
    } catch (_) {
      await loadCaptcha()
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  position: relative;
  padding-bottom: clamp(18px, 2vw, 28px);
}

.auth-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 22%, rgba(201, 100, 66, 0.08), rgba(201, 100, 66, 0) 32%),
    radial-gradient(circle at 84% 30%, rgba(98, 124, 101, 0.07), rgba(98, 124, 101, 0) 30%);
}

.auth-stage {
  position: relative;
  z-index: 1;
  width: calc(100% - clamp(20px, 3vw, 56px));
  margin: clamp(12px, 1.2vw, 20px) auto 0;
  min-height: calc(100vh - 116px);
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(450px, 1fr);
  gap: clamp(12px, 1.3vw, 18px);
  align-items: stretch;
}

.intro-card {
  padding: 24px 22px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.intro-overline {
  margin: 0;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-sub);
}

.intro-card h2 {
  margin-top: 10px;
  font-size: clamp(30px, 3vw, 42px);
  line-height: 1.16;
}

.intro-text {
  margin-top: 12px;
  color: var(--text-sub);
}

.intro-chips {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.intro-chips span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-accent);
  font-size: 12px;
}

.intro-decoration {
  margin-top: auto;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface-soft) 72%, var(--surface));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--border-soft) 76%, transparent);
  padding: 14px 14px 12px;
}

.decoration-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.decoration-head span {
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-sub);
}

.decoration-head strong {
  color: var(--text-accent-strong);
  font-size: 14px;
  font-family: var(--font-mono);
}

.decoration-list {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 7px;
}

.decoration-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-accent);
  font-size: 13px;
}

.decoration-list li i {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 84%, var(--surface-soft));
  box-shadow: 0 0 0 4px rgba(201, 100, 66, 0.11);
}

.decoration-grid {
  margin-top: 11px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.decoration-grid article {
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 88%, var(--surface-soft));
  padding: 8px 9px;
}

.decoration-grid p {
  margin: 0;
  color: var(--text-sub);
  font-size: 11px;
}

.decoration-grid strong {
  margin-top: 4px;
  display: block;
  color: var(--text-accent-strong);
  font-size: 18px;
  font-family: var(--font-serif);
  line-height: 1;
}

.form-card {
  padding: 24px 24px 20px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.form-card h2 {
  font-size: clamp(30px, 3vw, 40px);
  margin-bottom: 12px;
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
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface);
  cursor: pointer;
  object-fit: cover;
}

.captcha-refresh {
  height: 40px;
  border-radius: 10px;
}

.oauth-divider {
  margin: 14px 0 12px;
  position: relative;
  text-align: center;
}

.oauth-divider::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  border-top: 1px solid var(--border-soft);
}

.oauth-divider span {
  position: relative;
  padding: 0 12px;
  background: var(--surface);
  color: var(--text-sub);
  font-size: 13px;
}

.oauth-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.form-decoration {
  margin-top: auto;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 70%, var(--surface-soft));
  padding: 12px;
}

.form-decoration-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.form-decoration-head span {
  color: var(--text-sub);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.form-decoration-head strong {
  color: var(--text-accent-strong);
  font-size: 14px;
  font-family: var(--font-serif);
}

.form-decoration-row {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.form-decoration-row article {
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 88%, transparent);
  padding: 9px;
}

.form-decoration-row p {
  margin: 0;
  color: var(--text-accent-strong);
  font-size: 13px;
  font-family: var(--font-serif);
  line-height: 1.2;
}

.form-decoration-row small {
  display: block;
  margin-top: 5px;
  color: var(--text-sub);
  line-height: 1.4;
  font-size: 12px;
}

.oauth-button {
  height: 42px;
  border-radius: 10px;
  border: 1px solid var(--brand);
  background: var(--surface);
  color: var(--brand);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.oauth-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(20, 20, 19, 0.1);
}

.oauth-icon {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

@media (max-width: 1024px) {
  .auth-stage {
    width: calc(100% - 16px);
    margin-top: 10px;
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .intro-decoration,
  .form-decoration {
    margin-top: 14px;
  }
}

@media (max-width: 640px) {
  .intro-card,
  .form-card {
    padding: 16px 14px;
  }

  .captcha-row {
    flex-wrap: wrap;
  }

  .oauth-grid,
  .decoration-grid,
  .form-decoration-row {
    grid-template-columns: 1fr;
  }
}
</style>
