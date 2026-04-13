<template>
  <div class="home-shell">
    <PublicTopNav />

    <main class="home-main">
      <section class="hero surface-card" data-reveal>
        <div class="hero-copy">
          <p class="hero-overline">Campus Learning Ledger</p>
          <h1>把每一道错题，变成下次更稳的答案。</h1>
          <p class="hero-subtitle">
            CoderNote 以标签为核心，把错题、资料、笔记串成一条可复盘的学习链路。
            你不只是在记录，而是在不断编排自己的知识地图。
          </p>

          <div class="hero-tags">
            <span class="hero-tag">错题闭环</span>
            <span class="hero-tag">标签联动</span>
            <span class="hero-tag">复习节奏</span>
          </div>

          <div class="hero-actions">
            <el-button v-if="authStore.isLoggedIn" type="primary" @click="$router.push('/error-question/list')">
              <span>进入错题库</span>
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button v-if="authStore.isLoggedIn" plain @click="$router.push('/note/add')">
              <span>新建笔记</span>
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <template v-else>
              <el-button type="primary" @click="$router.push('/register')">
                <span>立即开始</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
              <el-button @click="$router.push('/login')">
                <span>已有账号，去登录</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </template>
          </div>
        </div>

        <div class="hero-illustration" aria-hidden="true">
          <div class="hero-orb orb-a"></div>
          <div class="hero-orb orb-b"></div>
          <div class="hero-wave wave-a"></div>
          <div class="hero-wave wave-b"></div>
          <div class="doodle-sheet sheet-a"></div>
          <div class="doodle-sheet sheet-b"></div>
          <div class="doodle-line line-a"></div>
          <div class="doodle-line line-b"></div>
          <div class="doodle-dot dot-a"></div>
          <div class="doodle-dot dot-b"></div>
          <div class="floating-badge">Flow</div>
        </div>
      </section>

      <section v-if="authStore.isLoggedIn" class="review-strip surface-card" data-reveal>
        <div class="review-title-wrap">
          <p class="review-overline">Today Rhythm</p>
          <button class="review-title-link" type="button" @click="goReviewCenter">今日待复习</button>
        </div>
        <div class="review-metrics">
          <span class="metric-chip">
            <small>错题</small>
            <strong>{{ animatedSummary.todayQuestionCount }}</strong>
          </span>
          <span class="metric-chip">
            <small>笔记</small>
            <strong>{{ animatedSummary.todayNoteCount }}</strong>
          </span>
          <span class="metric-chip">
            <small>总计</small>
            <strong>{{ animatedSummary.todayTotalCount }}</strong>
          </span>
          <span class="metric-chip" :class="{ hot: reviewSummary.overdueCount > 0 }">
            <small>{{ reviewSummary.overdueCount > 0 ? '逾期' : '状态' }}</small>
            <strong>{{ reviewSummary.overdueCount > 0 ? animatedSummary.overdueCount : '稳定' }}</strong>
          </span>
        </div>
      </section>

      <div class="decor-divider section-divider" aria-hidden="true"></div>

      <section class="feature-grid" data-reveal>
        <article class="feature-card surface-card hover-lift">
          <span class="feature-corner">错题</span>
          <div class="feature-head">
            <span class="feature-icon">
              <el-icon><Memo /></el-icon>
            </span>
            <p class="feature-overline">01</p>
          </div>
          <h3>错题沉淀</h3>
          <p>记录报错、原因、解法和附件，形成可检索的个人错题档案。</p>
          <div class="decor-divider feature-divider" aria-hidden="true"></div>
          <button class="feature-link" type="button" @click="$router.push('/error-question/list')">
            查看模块
            <el-icon><ArrowRight /></el-icon>
          </button>
        </article>

        <article class="feature-card surface-card hover-lift">
          <span class="feature-corner">关联</span>
          <div class="feature-head">
            <span class="feature-icon">
              <el-icon><CollectionTag /></el-icon>
            </span>
            <p class="feature-overline">02</p>
          </div>
          <h3>标签联动</h3>
          <p>同一知识点自动关联资料和笔记，减少重复查找与上下文切换。</p>
          <div class="decor-divider feature-divider" aria-hidden="true"></div>
          <button class="feature-link" type="button" @click="$router.push('/tag/list')">
            查看模块
            <el-icon><ArrowRight /></el-icon>
          </button>
        </article>

        <article class="feature-card surface-card hover-lift">
          <span class="feature-corner">节奏</span>
          <div class="feature-head">
            <span class="feature-icon">
              <el-icon><AlarmClock /></el-icon>
            </span>
            <p class="feature-overline">03</p>
          </div>
          <h3>复习调度</h3>
          <p>按今日、逾期、近期分类推进，持续修正薄弱点，稳定提升掌握率。</p>
          <div class="decor-divider feature-divider" aria-hidden="true"></div>
          <button class="feature-link" type="button" @click="$router.push('/review/center')">
            查看模块
            <el-icon><ArrowRight /></el-icon>
          </button>
        </article>
      </section>

      <section class="dark-section" data-reveal>
        <div class="dark-inner">
          <header>
            <p class="dark-overline">Workflow</p>
            <h2>像读一本手册一样学习，而不是在页面间来回跳转。</h2>
          </header>

          <div class="workflow-grid">
            <article class="workflow-card">
              <span class="workflow-icon">Capture</span>
              <h4>Capture</h4>
              <p>错题、资料、笔记统一录入，保留上下文与原始素材。</p>
            </article>
            <article class="workflow-card">
              <span class="workflow-icon">Connect</span>
              <h4>Connect</h4>
              <p>标签关系自动聚合，你的学习内容开始互相解释彼此。</p>
            </article>
            <article class="workflow-card">
              <span class="workflow-icon">Review</span>
              <h4>Review</h4>
              <p>复习中心按优先级推送任务，学习节奏可视化、可执行。</p>
            </article>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { AlarmClock, ArrowRight, CollectionTag, Memo } from '@element-plus/icons-vue'
import { onBeforeUnmount, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import PublicTopNav from '../components/layout/PublicTopNav.vue'
import { getReviewSummary } from '../api/review'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()

const reviewSummary = reactive({
  todayQuestionCount: 0,
  todayNoteCount: 0,
  todayTotalCount: 0,
  overdueCount: 0
})

const animatedSummary = reactive({
  todayQuestionCount: 0,
  todayNoteCount: 0,
  todayTotalCount: 0,
  overdueCount: 0
})

const counterFrames = {}
const reviewFields = ['todayQuestionCount', 'todayNoteCount', 'todayTotalCount', 'overdueCount']

const reduceMotion = typeof window !== 'undefined'
  && typeof window.matchMedia === 'function'
  && window.matchMedia('(prefers-reduced-motion: reduce)').matches

function toSafeCount(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return 0
  }
  return Math.max(0, Math.round(parsed))
}

function animateCounter(field, targetValue) {
  const target = toSafeCount(targetValue)
  if (reduceMotion) {
    animatedSummary[field] = target
    return
  }

  if (counterFrames[field]) {
    cancelAnimationFrame(counterFrames[field])
  }

  const start = toSafeCount(animatedSummary[field])
  const delta = target - start
  if (delta === 0) {
    return
  }

  const duration = Math.min(980, 420 + Math.abs(delta) * 32)
  const startedAt = performance.now()

  const step = (now) => {
    const progress = Math.min(1, (now - startedAt) / duration)
    const eased = 1 - (1 - progress) ** 3
    animatedSummary[field] = Math.round(start + delta * eased)
    if (progress < 1) {
      counterFrames[field] = requestAnimationFrame(step)
      return
    }
    animatedSummary[field] = target
    delete counterFrames[field]
  }

  counterFrames[field] = requestAnimationFrame(step)
}

function applySummary(data) {
  reviewFields.forEach((field) => {
    const safeValue = toSafeCount(data?.[field])
    reviewSummary[field] = safeValue
    animateCounter(field, safeValue)
  })
}

function goReviewCenter() {
  router.push('/review/center')
}

onMounted(async () => {
  await authStore.bootstrap()
  if (!authStore.isLoggedIn) return
  try {
    const data = await getReviewSummary()
    applySummary(data || {})
  } catch (_) {
    applySummary(reviewSummary)
  }
})

onBeforeUnmount(() => {
  Object.keys(counterFrames).forEach((field) => {
    cancelAnimationFrame(counterFrames[field])
  })
})
</script>

<style scoped>
.home-shell {
  min-height: 100vh;
  position: relative;
  padding-bottom: clamp(20px, 2vw, 34px);
}

.home-shell::before,
.home-shell::after {
  content: '';
  position: absolute;
  pointer-events: none;
  z-index: 0;
}

.home-shell::before {
  inset: 0;
  background:
    radial-gradient(circle at 8% 18%, rgba(201, 100, 66, 0.12), rgba(201, 100, 66, 0) 38%),
    radial-gradient(circle at 84% 20%, rgba(98, 124, 101, 0.1), rgba(98, 124, 101, 0) 34%),
    radial-gradient(circle at 86% 76%, rgba(201, 100, 66, 0.08), rgba(201, 100, 66, 0) 36%);
}

.home-shell::after {
  width: min(520px, 48vw);
  height: min(220px, 20vw);
  right: clamp(-120px, -6vw, -42px);
  top: clamp(130px, 16vh, 210px);
  border: 1px dashed rgba(201, 100, 66, 0.34);
  border-radius: 999px;
  transform: rotate(-9deg);
  opacity: 0.5;
}

.home-main {
  position: relative;
  z-index: 1;
  width: calc(100% - clamp(20px, 3vw, 56px));
  margin: clamp(12px, 1.2vw, 20px) auto 0;
  min-height: calc(100vh - 116px);
  display: grid;
  gap: clamp(12px, 1.5vw, 20px);
  align-content: start;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: clamp(12px, 1.3vw, 18px);
  padding: clamp(18px, 2vw, 30px);
  border-radius: 24px;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  left: clamp(18px, 2vw, 28px);
  right: clamp(18px, 2vw, 28px);
  bottom: clamp(16px, 2vw, 24px);
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(201, 100, 66, 0.48) 50%, transparent 100%);
  opacity: 0.56;
  pointer-events: none;
}

.hero-copy {
  min-width: 0;
}

.hero-overline {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-sub);
}

.hero h1 {
  margin-top: 12px;
  font-size: clamp(38px, 5.2vw, 64px);
  line-height: 1.1;
  max-width: 780px;
}

.hero-subtitle {
  margin: 16px 0 0;
  max-width: 760px;
  font-size: clamp(16px, 2vw, 20px);
  line-height: 1.62;
  color: var(--text-sub);
}

.hero-tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px dashed color-mix(in srgb, var(--primary) 56%, var(--border-soft));
  background: color-mix(in srgb, var(--surface-soft) 84%, transparent);
  color: var(--text-accent);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.hero-actions {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions :deep(.el-button .el-icon) {
  margin-left: 2px;
}

.hero-illustration {
  position: relative;
  min-height: 280px;
  border-radius: 24px;
  border: 1px solid var(--border-soft);
  background:
    radial-gradient(circle at 24% 20%, rgba(201, 100, 66, 0.22), rgba(201, 100, 66, 0)),
    radial-gradient(circle at 80% 82%, rgba(100, 120, 97, 0.2), rgba(100, 120, 97, 0)),
    linear-gradient(165deg, rgba(247, 244, 233, 0.82) 0%, rgba(236, 231, 217, 0.84) 100%);
  overflow: hidden;
  backdrop-filter: blur(8px);
}

.hero-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(1px);
  animation: orbFloat 5.8s ease-in-out infinite;
}

.orb-a {
  width: 96px;
  height: 96px;
  top: 14%;
  right: 14%;
  background: radial-gradient(circle at 30% 32%, rgba(255, 255, 255, 0.68), rgba(201, 100, 66, 0.38));
}

.orb-b {
  width: 70px;
  height: 70px;
  left: 20%;
  bottom: 16%;
  background: radial-gradient(circle at 38% 38%, rgba(255, 255, 255, 0.66), rgba(98, 124, 101, 0.34));
  animation-delay: 0.6s;
}

.hero-wave {
  position: absolute;
  border: 1px dashed rgba(77, 76, 72, 0.26);
  border-radius: 999px;
  opacity: 0.45;
}

.wave-a {
  width: 172px;
  height: 56px;
  left: 10%;
  top: 12%;
  transform: rotate(-11deg);
}

.wave-b {
  width: 190px;
  height: 66px;
  right: 8%;
  bottom: 10%;
  transform: rotate(8deg);
}

.doodle-sheet {
  position: absolute;
  border-radius: 18px;
  border: 1px solid rgba(77, 76, 72, 0.2);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(2px);
}

.sheet-a {
  inset: 16% 30% 18% 16%;
  transform: rotate(-7deg);
}

.sheet-b {
  inset: 24% 14% 26% 40%;
  transform: rotate(8deg);
}

.doodle-line {
  position: absolute;
  height: 2px;
  border-radius: 999px;
  background: rgba(77, 76, 72, 0.26);
}

.line-a {
  width: 110px;
  left: 24%;
  top: 36%;
  transform: rotate(-8deg);
}

.line-b {
  width: 120px;
  right: 20%;
  bottom: 33%;
  transform: rotate(11deg);
}

.doodle-dot {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.dot-a {
  left: 17%;
  top: 18%;
  background: var(--primary);
}

.dot-b {
  right: 15%;
  bottom: 16%;
  background: #627c65;
}

.floating-badge {
  position: absolute;
  right: 16px;
  top: 14px;
  min-width: 64px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(201, 100, 66, 0.5);
  background: rgba(250, 247, 238, 0.78);
  color: var(--primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  letter-spacing: 0.08em;
  animation: badgePulse 2.8s ease-in-out infinite;
}

.review-strip {
  position: relative;
  padding: 14px 16px;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px 20px;
  align-items: center;
  overflow: hidden;
}

.review-strip::after {
  content: '';
  position: absolute;
  right: 16px;
  top: 12px;
  width: 160px;
  height: 40px;
  border-radius: 999px;
  border: 1px dashed rgba(201, 100, 66, 0.32);
  opacity: 0.4;
}

.review-overline {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-sub);
}

.review-title-link {
  margin-top: 4px;
  border: none;
  background: transparent;
  padding: 0;
  font-family: var(--font-serif);
  font-size: clamp(24px, 2.7vw, 36px);
  line-height: 1.15;
  color: var(--text-main);
  cursor: pointer;
  transition: color 0.2s ease;
}

.review-title-link:hover {
  color: var(--primary);
}

.review-metrics {
  justify-self: end;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.metric-chip {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  min-height: 46px;
  min-width: 82px;
  padding: 4px 12px;
  border: 1px solid var(--border-soft);
  border-radius: 14px;
  background:
    linear-gradient(154deg, color-mix(in srgb, var(--surface-soft) 88%, transparent) 0%, color-mix(in srgb, var(--surface) 92%, transparent) 100%);
  color: var(--text-accent);
  text-align: center;
}

.metric-chip small {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-sub);
  text-transform: uppercase;
}

.metric-chip strong {
  font-size: 20px;
  line-height: 1.1;
  color: var(--text-main);
  font-family: var(--font-serif);
  font-variant-numeric: tabular-nums;
}

.metric-chip.hot {
  border-color: rgba(181, 51, 51, 0.36);
  background: rgba(181, 51, 51, 0.11);
}

.metric-chip.hot strong {
  color: var(--danger);
}

.section-divider {
  margin: -2px 0 2px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-card {
  position: relative;
  padding: 18px;
  overflow: hidden;
}

.feature-corner {
  position: absolute;
  top: 12px;
  right: 12px;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--primary) 56%, var(--border-soft));
  background: color-mix(in srgb, var(--surface-soft) 70%, transparent);
  color: var(--primary);
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  letter-spacing: 0.06em;
}

.feature-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface-soft) 82%, transparent);
  color: var(--primary);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary) 20%, transparent);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.feature-overline {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-sub);
}

.feature-card h3 {
  margin-top: 12px;
  font-size: clamp(24px, 2.1vw, 32px);
  line-height: 1.2;
}

.feature-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
}

.feature-divider {
  margin-top: 14px;
}

.feature-link {
  margin-top: 10px;
  border: none;
  background: transparent;
  padding: 0;
  color: var(--primary);
  font-size: 13px;
  letter-spacing: 0.04em;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: transform 0.2s ease, color 0.2s ease;
}

.feature-card:hover .feature-icon {
  transform: translateY(-2px) rotate(-6deg);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary) 40%, transparent), 0 10px 26px -18px rgba(201, 100, 66, 0.6);
}

.feature-card:hover .feature-link {
  transform: translateX(2px);
}

.dark-section {
  border-radius: 24px;
  background:
    radial-gradient(circle at 18% 26%, rgba(217, 119, 87, 0.16), rgba(217, 119, 87, 0) 44%),
    radial-gradient(circle at 82% 74%, rgba(112, 134, 113, 0.14), rgba(112, 134, 113, 0) 40%),
    var(--color-ink);
  border: 1px solid var(--color-border-dark);
  overflow: hidden;
}

.dark-inner {
  position: relative;
  padding: clamp(18px, 2.4vw, 34px);
}

.dark-inner::after {
  content: '';
  position: absolute;
  right: 20px;
  top: 16px;
  width: 190px;
  height: 76px;
  border-radius: 999px;
  border: 1px dashed rgba(255, 245, 236, 0.22);
  opacity: 0.42;
}

.dark-overline {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-warm-silver);
}

.dark-inner h2 {
  margin-top: 10px;
  max-width: 900px;
  color: var(--color-ivory);
  font-size: clamp(34px, 4.2vw, 52px);
  line-height: 1.15;
}

.workflow-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.workflow-card {
  border: 1px solid #3b3a36;
  border-radius: 14px;
  background: rgba(35, 35, 32, 0.84);
  padding: 16px;
  backdrop-filter: blur(6px);
}

.workflow-icon {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px dashed rgba(255, 244, 233, 0.3);
  color: rgba(255, 244, 233, 0.9);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  animation: iconNudge 2.8s ease-in-out infinite;
}

.workflow-card:nth-child(2) .workflow-icon {
  animation-delay: 0.25s;
}

.workflow-card:nth-child(3) .workflow-icon {
  animation-delay: 0.5s;
}

.workflow-card h4 {
  margin-top: 10px;
  color: var(--color-ivory);
  font-size: clamp(20px, 2.1vw, 28px);
}

.workflow-card p {
  margin: 8px 0 0;
  color: var(--color-warm-silver);
  line-height: 1.62;
}

@keyframes orbFloat {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-7px) scale(1.04);
  }
}

@keyframes badgePulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(201, 100, 66, 0.22);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(201, 100, 66, 0);
  }
}

@keyframes iconNudge {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

@media (max-width: 1024px) {
  .home-main {
    width: calc(100% - 16px);
    margin-top: 10px;
    min-height: auto;
  }

  .hero {
    grid-template-columns: 1fr;
  }

  .review-strip {
    grid-template-columns: 1fr;
  }

  .review-metrics {
    justify-self: start;
    justify-content: flex-start;
  }

  .feature-grid,
  .workflow-grid {
    grid-template-columns: 1fr;
  }

  .home-shell::after,
  .dark-inner::after,
  .review-strip::after {
    opacity: 0.35;
  }
}
</style>
