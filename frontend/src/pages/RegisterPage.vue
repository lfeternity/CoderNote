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
          <p class="eyebrow">JOIN CODERNOTE</p>
          <h2>创建你的学习档案</h2>
          <p class="intro-text">
            从第一道错题开始，建立持续迭代的个人知识库，让复习资料与错题记录自动协同。
          </p>

          <div class="chip-row">
            <span># 错题沉淀</span>
            <span># 资料归档</span>
            <span># 标签组织</span>
          </div>

          <div class="stat-grid">
            <article class="stat-item">
              <strong>Notebook</strong>
              <p>记录问题与解决路径</p>
            </article>
            <article class="stat-item">
              <strong>Library</strong>
              <p>管理你的学习资源</p>
            </article>
          </div>
        </aside>

        <div class="surface-card form-card fade-in">
          <div class="card-decor" aria-hidden="true" />
          <h2>注册</h2>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" maxlength="10" show-word-limit />
            </el-form-item>
            <el-form-item label="学号">
              <el-input v-model="form.studentNo" />
            </el-form-item>
            <el-form-item label="专业" prop="major">
              <el-select v-model="form.major" placeholder="请选择专业" style="width: 100%">
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
        </div>
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
    { min: 1, max: 10, message: '昵称长度需在1-10个字符', trigger: 'blur' }
  ],
  major: [{ required: true, message: '请选择专业', trigger: 'change' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,16}$/, message: '密码需为6-16位字母+数字', trigger: 'blur' }
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
      await register(form)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
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
  grid-template-columns: minmax(280px, 0.9fr) minmax(500px, 1fr);
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
}
</style>
