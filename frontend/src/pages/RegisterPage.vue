<template>
  <div class="auth-shell">
    <PublicTopNav />

    <section class="auth-stage">
      <aside class="surface-card intro-card fade-in">
        <p class="intro-overline">Join CoderNote</p>
        <h2>从今天开始，建立你的长期学习档案。</h2>
        <p class="intro-text">
          记录错题只是第一步。通过标签联动和复习计划，把零散练习变成可持续积累。
        </p>
        <div class="intro-chips">
          <span>题解归档</span>
          <span>资料沉淀</span>
          <span>知识网络</span>
        </div>

        <section class="intro-decoration" aria-hidden="true">
          <div class="decoration-head">
            <span>Onboarding Map</span>
            <strong>Start in 3 Steps</strong>
          </div>
          <ul class="decoration-list">
            <li><i></i><span>完成基础信息与专业绑定</span></li>
            <li><i></i><span>录入第一条错题与标签</span></li>
            <li><i></i><span>生成首个复习节奏面板</span></li>
          </ul>
          <div class="decoration-grid">
            <article>
              <p>档案位</p>
              <strong>01</strong>
            </article>
            <article>
              <p>标签层</p>
              <strong>03</strong>
            </article>
            <article>
              <p>复习轨道</p>
              <strong>Ready</strong>
            </article>
          </div>
        </section>
      </aside>

      <div class="surface-card form-card fade-in">
        <h2>注册</h2>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" maxlength="10" show-word-limit />
          </el-form-item>
          <el-form-item label="学号">
            <el-input v-model="form.studentNo" />
          </el-form-item>
          <el-form-item label="专业" prop="major">
            <el-select
              v-model="form.major"
              placeholder="请选择或输入专业"
              style="width: 100%"
              filterable
              allow-create
              default-first-option
            >
              <el-option v-for="item in majorOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password />
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
            <el-button type="primary" :loading="loading" @click="onSubmit">注册</el-button>
            <el-button @click="$router.push('/login')">已有账号？去登录</el-button>
          </el-form-item>
        </el-form>

        <section class="form-decoration" aria-hidden="true">
          <div class="form-decoration-head">
            <span>Register Benefits</span>
            <strong>创建后立即拥有学习工作台</strong>
          </div>
          <div class="form-decoration-row">
            <article>
              <p>错题面板</p>
              <small>支持附件、语言和掌握状态</small>
            </article>
            <article>
              <p>资料中心</p>
              <small>统一管理题解、文档和笔记</small>
            </article>
            <article>
              <p>复习日历</p>
              <small>按日期聚合待办和逾期项</small>
            </article>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PublicTopNav from '../components/layout/PublicTopNav.vue'
import { getRegisterCaptcha, register } from '../api/user'
import { getOptions } from '../api/public'
import { toZhMajorOptions } from '../utils/major'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const majorOptions = ref([])
const captchaImage = ref('')

const form = reactive({
  nickname: '',
  studentNo: '',
  major: '',
  password: '',
  confirmPassword: '',
  captchaCode: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 10, message: '昵称长度需在 1-10 个字符', trigger: 'blur' }
  ],
  major: [
    { required: true, message: '请选择或输入专业', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (!String(value || '').trim()) {
          callback(new Error('请选择或输入专业'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,16}$/, message: '密码需为 6-16 位字母+数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadOptions() {
  const data = await getOptions()
  majorOptions.value = toZhMajorOptions(data.majors)
}

async function loadCaptcha() {
  const data = await getRegisterCaptcha()
  captchaImage.value = data?.imageBase64 ? `data:image/png;base64,${data.imageBase64}` : ''
  form.captchaCode = ''
}

loadOptions()
loadCaptcha()

function onSubmit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = {
        ...form,
        major: String(form.major || '').trim()
      }
      await register(payload)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
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
  grid-template-columns: minmax(300px, 0.9fr) minmax(460px, 1fr);
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

  .decoration-grid,
  .form-decoration-row {
    grid-template-columns: 1fr;
  }
}
</style>
