<template>
  <div class="home-shell">
    <section class="home page-shell-fluid">
      <div class="hero-stack surface-card fade-in" :class="{ 'with-review': authStore.isLoggedIn }">
        <div class="decor decor-one" />
        <div class="decor decor-two" />
        <div class="decor decor-three" />
        <div class="scan-line" />
        <div class="streak streak-one" />
        <div class="streak streak-two" />
        <div class="texture-grid" />
        <div class="spark spark-one" />
        <div class="spark spark-two" />
        <span class="ambient-tag ambient-one"># Vue3</span>
        <span class="ambient-tag ambient-two"># SQL</span>
        <span class="ambient-tag ambient-three"># LeetCode</span>
        <span class="ambient-tag ambient-four"># SystemDesign</span>

        <PublicTopNav />
        <div v-if="authStore.isLoggedIn" class="review-reminder">
          <div class="review-reminder-left">
            <button type="button" class="review-title-link" @click="goReviewCenter">今日待复习</button>
            <p class="review-count">
              错题 {{ reviewSummary.todayQuestionCount || 0 }} 题 · 笔记 {{ reviewSummary.todayNoteCount || 0 }} 条
            </p>
            <p v-if="reviewSummary.overdueCount > 0" class="review-overdue">
              逾期 {{ reviewSummary.overdueCount }} 项，建议优先处理
            </p>
          </div>
          <div class="review-glance" aria-hidden="true">
            <span class="glance-item">今日总任务 {{ reviewSummary.todayTotalCount || 0 }}</span>
            <span class="glance-item" :class="{ 'is-hot': reviewSummary.overdueCount > 0 }">
              {{ reviewSummary.overdueCount > 0 ? `逾期 ${reviewSummary.overdueCount}` : '节奏稳定' }}
            </span>
          </div>
        </div>

        <div class="hero">
          <div class="hero-content">
            <p class="eyebrow">Campus Coding Companion</p>
            <h1>{{ copy.title }}</h1>
            <p class="subtitle">{{ copy.subtitle }}</p>

            <div class="meta-strip">
              <span>{{ copy.metaA }}</span>
              <span>{{ copy.metaB }}</span>
              <span>{{ copy.metaC }}</span>
              <span>{{ copy.metaD }}</span>
              <span>{{ copy.metaE }}</span>
            </div>

            <div class="momentum-strip" aria-hidden="true">
              <article class="momentum-item">
                <p>学习动量</p>
                <strong>+24%</strong>
              </article>
              <article class="momentum-item">
                <p>资料联动率</p>
                <strong>87%</strong>
              </article>
              <article class="momentum-item">
                <p>本周轨迹</p>
                <strong>6 Days</strong>
              </article>
            </div>

            <div class="accent-row" aria-hidden="true">
              <div class="accent-line" />
              <div class="accent-dots">
                <span class="dot-item" />
                <span class="dot-item" />
                <span class="dot-item" />
                <span class="dot-item" />
                <span class="dot-item" />
              </div>
            </div>

            <div class="micro-grid" aria-hidden="true">
              <article class="micro-card">
                <p class="micro-label">Auto Match</p>
                <strong>Tag Linking</strong>
              </article>
              <article class="micro-card">
                <p class="micro-label">Knowledge Flow</p>
                <strong>Review Ready</strong>
              </article>
            </div>
          </div>

          <div class="hero-visual" aria-hidden="true">
            <div class="orb orb-a" />
            <div class="orb orb-b" />
            <div class="orb orb-c" />

            <div class="code-card hover-lift">
              <div class="card-head">
                <span class="dot red" />
                <span class="dot yellow" />
                <span class="dot green" />
                <strong>link-engine.ts</strong>
              </div>
              <pre class="code-preview"><code>{{ codeSnippet }}</code></pre>
            </div>

            <span class="float-tag tag-one"># Java</span>
            <span class="float-tag tag-two"># SpringBoot</span>
            <span class="float-tag tag-three"># DataStructure</span>
            <span class="float-tag tag-four"># Algorithm</span>
            <span class="float-tag tag-five"># Redis</span>

            <div class="signal-panel" aria-hidden="true">
              <p class="signal-title">Knowledge Pulse</p>
              <div class="signal-bars">
                <span style="--h: 36%"></span>
                <span style="--h: 54%"></span>
                <span style="--h: 44%"></span>
                <span style="--h: 70%"></span>
                <span style="--h: 60%"></span>
                <span style="--h: 82%"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
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

const copy = {
  title: 'CoderNote',
  subtitle:
    '\u8bb0\u5f55\u9519\u9898\uff0c\u6c89\u6dc0\u601d\u8def\uff1b\u901a\u8fc7\u77e5\u8bc6\u70b9\u6807\u7b7e\u81ea\u52a8\u8054\u52a8\u8d44\u6599\uff0c\u505a\u5230\u201c\u9519\u9898\u4e00\u51fa\u73b0\uff0c\u590d\u4e60\u8d44\u6599\u7acb\u523b\u5c31\u4f4d\u201d\u3002',
  metaA: '\u6807\u7b7e\u81ea\u52a8\u8054\u52a8',
  metaB: '\u5168\u5c40\u641c\u7d22\u5feb\u901f\u5b9a\u4f4d',
  metaC: '\u4e2a\u4eba\u5b66\u4e60\u8f68\u8ff9\u79ef\u7d2f',
  metaD: '\u8054\u52a8\u590d\u4e60\u8282\u594f\u7ba1\u7406',
  metaE: '\u77e5\u8bc6\u70b9\u7f51\u7edc\u5316\u6c89\u6dc0'
}

const codeSnippet = [
  'const matched = tags.filter((t) => materialTags.has(t));',
  'const score = matched.length;',
  '',
  'if (score > 0) {',
  '  pushLinkedMaterial(questionId, materialId, score);',
  '}',
  '',
  '// review flow keeps evolving \u2728'
].join('\n')

function goReviewCenter() {
  router.push('/review/center')
}

onMounted(async () => {
  await authStore.bootstrap()
  if (!authStore.isLoggedIn) return
  try {
    const data = await getReviewSummary()
    Object.assign(reviewSummary, data || {})
  } catch (_) {
    // ignore non-blocking summary failure on home page
  }
})
</script>

<style scoped>
.home-shell {
  height: 100dvh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.public-nav) {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 12px 16px;
  position: relative;
  z-index: 2;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  background: transparent;
  flex-shrink: 0;
}

.page-shell-fluid {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.home {
  display: flex;
  flex-direction: column;
  min-height: 0;
  gap: 0;
}

.hero-stack {
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  border: 1px solid #c5d6ff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: none;
  background:
    linear-gradient(125deg, rgba(255, 255, 255, 0.9), rgba(244, 248, 255, 0.92)),
    url('../assets/home-tech-bg.svg') center/cover no-repeat;
}

.review-reminder {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 16px;
  border: 0;
  background: transparent;
  flex-shrink: 0;
}

.review-reminder-left {
  display: flex;
  flex-direction: column;
}

.review-glance {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.glance-item {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid #c8d7ff;
  background: rgba(255, 255, 255, 0.6);
  color: #3552a0;
  font-size: 12px;
  font-weight: 600;
}

.glance-item.is-hot {
  border-color: #f5b0b0;
  background: rgba(255, 236, 236, 0.72);
  color: #c13d3d;
}

.review-title-link {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--text-accent-strong);
}

.review-title-link:hover {
  color: #1c3f97;
}

.review-count {
  margin: 4px 0 0;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 600;
}

.review-overdue {
  margin: 4px 0 0;
  color: var(--danger);
  font-size: 11px;
}

.hero {
  position: relative;
  z-index: 2;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: clamp(12px, 1vw, 18px);
  padding: clamp(18px, 2.3vh, 30px) clamp(20px, 1.8vw, 32px);
  min-height: 0;
  flex: 1;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.decor {
  position: absolute;
  z-index: 0;
  border-radius: 999px;
  filter: blur(2px);
  pointer-events: none;
}

.texture-grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(110, 140, 214, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 140, 214, 0.08) 1px, transparent 1px);
  background-size: 160px 160px;
  mask-image: radial-gradient(circle at 52% 48%, rgba(0, 0, 0, 0.9), transparent 86%);
}

.spark {
  position: absolute;
  z-index: 1;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(66, 133, 244, 0.7);
  box-shadow: 0 0 0 10px rgba(66, 133, 244, 0.08);
  pointer-events: none;
}

.spark-one {
  left: 17%;
  top: 13%;
  animation: twinkle 4.3s ease-in-out infinite;
}

.spark-two {
  right: 24%;
  bottom: 18%;
  animation: twinkle 4.8s ease-in-out infinite reverse;
}

.ambient-tag {
  position: absolute;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #c6d5ff;
  background: rgba(255, 255, 255, 0.75);
  color: #2f4ea6;
  font-size: 12px;
  box-shadow: 0 8px 16px rgba(30, 64, 175, 0.1);
  pointer-events: none;
}

.ambient-one {
  left: 35%;
  top: 16%;
  animation: floatSlow 7.2s ease-in-out infinite;
}

.ambient-two {
  left: 58%;
  top: 11%;
  animation: floatSlow 6.6s ease-in-out infinite reverse;
}

.ambient-three {
  left: 65%;
  top: 34%;
  animation: floatSlow 7.5s ease-in-out infinite;
}

.ambient-four {
  left: 23%;
  bottom: 19%;
  animation: floatSlow 6.9s ease-in-out infinite reverse;
}

.decor-one {
  width: 320px;
  height: 320px;
  right: -80px;
  top: -90px;
  background: radial-gradient(circle, rgba(30, 64, 175, 0.24), rgba(30, 64, 175, 0.01));
  animation: floatSlow 7s ease-in-out infinite;
}

.decor-two {
  width: 260px;
  height: 260px;
  left: -70px;
  bottom: -70px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.22), rgba(16, 185, 129, 0.01));
  animation: floatSlow 8s ease-in-out infinite reverse;
}

.decor-three {
  width: 180px;
  height: 180px;
  left: 420px;
  top: 80px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.16), rgba(14, 165, 233, 0));
  animation: floatSlow 6.2s ease-in-out infinite;
}

.scan-line {
  position: absolute;
  z-index: 1;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(30, 64, 175, 0.45), transparent);
  animation: scanMove 4.8s linear infinite;
}

.streak {
  position: absolute;
  z-index: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(30, 64, 175, 0.35), transparent);
  pointer-events: none;
}

.streak-one {
  width: 380px;
  left: 90px;
  top: 170px;
}

.streak-two {
  width: 320px;
  left: 120px;
  top: 510px;
}

.hero-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.eyebrow {
  margin: 0 0 10px;
  color: #2e5bd1;
  font-size: 13px;
  letter-spacing: 0.11em;
  text-transform: uppercase;
  font-weight: 700;
}

.hero-content h1 {
  margin: 0;
  line-height: 1.28;
  color: #153a9f;
  font-size: clamp(30px, 2.8vw, 52px);
  text-wrap: balance;
}

.subtitle {
  margin: clamp(10px, 1.4vh, 14px) 0 clamp(14px, 2.4vh, 22px);
  max-width: 780px;
  color: #415477;
  line-height: 1.65;
  font-size: clamp(14px, 1vw, 17px);
}

.meta-strip {
  margin-top: 2px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-strip span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #c6d5ff;
  background: rgba(255, 255, 255, 0.72);
  color: #3150ad;
  font-size: 12px;
  backdrop-filter: blur(1px);
}

.momentum-strip {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-width: 660px;
}

.momentum-item {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #cbdbff;
  background: rgba(255, 255, 255, 0.62);
}

.momentum-item p {
  margin: 0;
  font-size: 12px;
  color: #5a74b5;
}

.momentum-item strong {
  display: block;
  margin-top: 4px;
  font-size: 16px;
  color: #2047a6;
}

.accent-row {
  margin-top: clamp(10px, 1.8vh, 18px);
  display: flex;
  align-items: center;
  gap: 12px;
}

.accent-line {
  width: 260px;
  max-width: 50%;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(30, 64, 175, 0.65), rgba(30, 64, 175, 0.08));
}

.accent-dots {
  display: flex;
  gap: 8px;
}

.dot-item {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #90aaf4;
  box-shadow: 0 0 0 3px rgba(144, 170, 244, 0.18);
}

.micro-grid {
  margin-top: clamp(10px, 1.8vh, 18px);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  max-width: 520px;
}

.micro-card {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #cadeff;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 8px 20px rgba(30, 64, 175, 0.08);
}

.micro-label {
  margin: 0;
  font-size: 12px;
  color: #5272c8;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.micro-card strong {
  display: block;
  margin-top: 4px;
  color: #1e3f9f;
  font-size: 15px;
}

.hero-visual {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.orb {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.orb-a {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(45, 92, 232, 0.25), rgba(45, 92, 232, 0));
  right: 55px;
  top: 5px;
  animation: drift 6.2s ease-in-out infinite;
}

.orb-b {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.22), rgba(16, 185, 129, 0));
  left: 30px;
  bottom: 16px;
  animation: drift 5.4s ease-in-out infinite reverse;
}

.orb-c {
  width: 110px;
  height: 110px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.28), rgba(245, 158, 11, 0));
  right: 30px;
  bottom: 20px;
  animation: drift 4.6s ease-in-out infinite;
}

.code-card {
  position: relative;
  width: min(100%, 540px);
  max-height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  border: 1px solid #c6d7ff;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 248, 255, 0.96));
  box-shadow: 0 16px 34px rgba(30, 64, 175, 0.14);
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  border-bottom: 1px solid #d4e0ff;
  background: rgba(255, 255, 255, 0.9);
  flex-shrink: 0;
}

.card-head strong {
  margin-left: 6px;
  font-size: 12px;
  letter-spacing: 0.06em;
  color: #3b4d78;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
}

.dot.red {
  background: #f87171;
}

.dot.yellow {
  background: #fbbf24;
}

.dot.green {
  background: #34d399;
}

.code-preview {
  margin: 0;
  min-height: 0;
  max-height: clamp(160px, 26vh, 260px);
  padding: 12px;
  color: #264182;
  background: rgba(238, 244, 255, 0.74);
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.62;
  white-space: pre-wrap;
  overflow: hidden;
}

.float-tag {
  position: absolute;
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #c6d4ff;
  background: rgba(255, 255, 255, 0.78);
  color: #2e4ba9;
  font-size: 12px;
  box-shadow: 0 8px 18px rgba(30, 64, 175, 0.12);
}

.tag-one {
  left: 6px;
  top: 20px;
  animation: floatSlow 6.4s ease-in-out infinite;
}

.tag-two {
  right: 0;
  top: 85px;
  animation: floatSlow 7.1s ease-in-out infinite reverse;
}

.tag-three {
  left: 24px;
  bottom: 14px;
  animation: floatSlow 6.7s ease-in-out infinite;
}

.tag-four {
  right: 64px;
  bottom: 32px;
  animation: floatSlow 7.3s ease-in-out infinite reverse;
}

.tag-five {
  left: 56px;
  top: 118px;
  animation: floatSlow 6.1s ease-in-out infinite;
}

.signal-panel {
  position: absolute;
  right: 20px;
  bottom: -4px;
  width: 150px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid #cad8fc;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 10px 20px rgba(30, 64, 175, 0.12);
}

.signal-title {
  margin: 0;
  font-size: 11px;
  color: #4763ad;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.signal-bars {
  margin-top: 8px;
  height: 32px;
  display: flex;
  align-items: flex-end;
  gap: 5px;
}

.signal-bars span {
  width: 8px;
  height: var(--h);
  border-radius: 999px 999px 2px 2px;
  background: linear-gradient(180deg, rgba(71, 132, 255, 0.95), rgba(93, 179, 255, 0.62));
}

@keyframes drift {
  0%,
  100% {
    transform: translateY(0) translateX(0);
  }
  50% {
    transform: translateY(-12px) translateX(6px);
  }
}

@keyframes floatSlow {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes scanMove {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.2;
    transform: scale(0.9);
  }
  50% {
    opacity: 0.95;
    transform: scale(1.15);
  }
}

@media (max-width: 1180px) {
  .review-reminder {
    flex-direction: column;
    align-items: flex-start;
    padding: 10px 12px;
  }

  .review-glance {
    margin-left: 0;
  }

  .hero {
    grid-template-columns: 1fr;
    min-height: 0;
    flex: 1;
    padding: 24px;
  }

  .hero-visual {
    min-height: 320px;
    margin-top: 8px;
  }

  .micro-grid {
    max-width: 100%;
  }

  .momentum-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-width: 100%;
  }

  .ambient-three,
  .ambient-four,
  .signal-panel {
    display: none;
  }
}

@media (max-width: 720px) {
  :deep(.public-nav) {
    padding: 10px 12px;
  }

  .page-shell-fluid {
    padding: 0;
  }

  .subtitle {
    font-size: 15px;
    line-height: 1.7;
  }

  .hero {
    padding: 16px;
  }

  .accent-line {
    width: 150px;
  }

  .micro-grid {
    grid-template-columns: 1fr;
  }

  .momentum-strip {
    grid-template-columns: 1fr;
  }

  .ambient-tag,
  .spark,
  .tag-four,
  .tag-five {
    display: none;
  }

  .code-card {
    width: 100%;
  }
}

@media (max-height: 980px) and (min-width: 1181px) {
  :deep(.public-nav) {
    padding: 10px 14px;
  }

  .page-shell-fluid {
    padding: 0;
  }

  .review-reminder {
    padding: 8px 12px;
  }

  .hero {
    padding: 18px 20px;
  }

  .subtitle {
    margin: 8px 0 12px;
    line-height: 1.58;
  }

  .meta-strip span {
    padding: 5px 10px;
  }

  .accent-row {
    margin-top: 10px;
  }

  .micro-grid {
    margin-top: 10px;
  }

  .momentum-strip {
    margin-top: 10px;
  }

  .code-preview {
    max-height: 210px;
    padding: 10px 11px;
  }
}

@media (max-height: 860px) and (min-width: 1181px) {
  .hero-content h1 {
    font-size: clamp(28px, 2.3vw, 42px);
  }

  .subtitle {
    margin: 6px 0 10px;
    line-height: 1.5;
  }

  .accent-row,
  .momentum-strip,
  .micro-grid,
  .ambient-tag,
  .spark,
  .tag-three,
  .tag-four,
  .tag-five,
  .orb-c {
    display: none;
  }

  .code-preview {
    max-height: 170px;
  }
}
</style>









