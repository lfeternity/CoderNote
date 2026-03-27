<template>
  <section class="fade-in review-mode-page" v-loading="loading">
    <div class="mode-head">
      <div class="mode-head-main">
        <div class="page-title-back">
          <el-button class="back-chevron-btn" plain aria-label="返回复习中心" @click="$router.push('/review/center')">&lt;</el-button>
          <h2>复习模式</h2>
        </div>
        <p class="mode-sub">
          {{ modeLabel }} · {{ doneCount }} / {{ totalCount }} 已处理
        </p>
      </div>
    </div>

    <article v-if="!currentItem && totalCount === 0" class="surface-card empty-block">
      <el-empty description="当前没有可复习内容" />
    </article>

    <article v-else-if="!currentItem && totalCount > 0" class="surface-card done-block">
      <h3>今日复习已完成</h3>
      <p>做得不错，继续保持这个节奏。你已经完成了 {{ doneCount }} 项复习任务。</p>
    </article>

    <article v-else class="surface-card content-block">
      <header class="content-head">
        <div>
          <el-tag size="small">{{ reviewContentTypeLabel(currentItem.contentType) }}</el-tag>
          <span class="content-title">{{ currentItem.title }}</span>
        </div>
        <div class="content-meta">
          <span>{{ currentItem.language || '-' }}</span>
          <span>第 {{ currentIndex + 1 }} / {{ totalCount }} 项</span>
        </div>
      </header>

      <div class="chip-list">
        <span v-for="tag in currentItem.tagNames || []" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <section v-if="currentItem.contentType === 'QUESTION'" class="qa-block">
        <h3>题目 / 错误代码</h3>
        <pre class="code-block">{{ currentItem.questionBody || '-' }}</pre>

        <div class="reveal-box">
          <el-button type="primary" plain @click="showAnswer = !showAnswer">
            {{ showAnswer ? '隐藏解析' : '查看解析' }}
          </el-button>
        </div>

        <div v-if="showAnswer">
          <h3>正确代码</h3>
          <pre class="code-block">{{ currentItem.answerBody || '-' }}</pre>
          <h3>解析</h3>
          <pre class="text-block">{{ currentItem.answerExplain || '-' }}</pre>
        </div>
      </section>

      <section v-else class="note-block">
        <h3>笔记预览</h3>
        <p class="note-title">{{ currentItem.title }}</p>
        <div class="reveal-box">
          <el-button type="primary" plain @click="showNote = !showNote">
            {{ showNote ? '收起笔记内容' : '展开笔记内容' }}
          </el-button>
        </div>
        <pre v-if="showNote" class="text-block">{{ currentItem.noteContent || '-' }}</pre>
      </section>

      <footer class="footer-actions">
        <el-button @click="onSkip">跳过</el-button>
        <el-button type="success" :loading="submitting" @click="onSubmit('MASTERED')">已掌握</el-button>
        <el-button type="warning" :loading="submitting" @click="onSubmit('CONTINUE')">继续复习</el-button>
        <el-button type="info" :loading="submitting" @click="onSubmit('POSTPONE')">暂缓一天</el-button>
      </footer>
    </article>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { executeReview, getReviewSessionItems } from '../api/review'
import { reviewContentTypeLabel } from '../utils/review'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const items = ref([])
const currentIndex = ref(0)
const doneCount = ref(0)
const showAnswer = ref(false)
const showNote = ref(false)

const modeCategory = computed(() => String(route.query.category || 'TODAY').toUpperCase())
const modeLabel = computed(() => {
  if (modeCategory.value === 'OVERDUE') return '过期复习'
  if (modeCategory.value === 'UPCOMING') return '近期复习'
  return '今日复习'
})
const totalCount = computed(() => items.value.length)
const currentItem = computed(() => items.value[currentIndex.value] || null)

function resetRevealState() {
  showAnswer.value = false
  showNote.value = false
}

function moveToNext() {
  doneCount.value += 1
  currentIndex.value += 1
  resetRevealState()
}

async function loadSessionItems() {
  loading.value = true
  try {
    const data = await getReviewSessionItems({ category: modeCategory.value })
    items.value = data || []
    currentIndex.value = 0
    doneCount.value = 0
    resetRevealState()
  } finally {
    loading.value = false
  }
}

function onSkip() {
  if (!currentItem.value) return
  moveToNext()
  ElMessage.info('已跳过当前条目')
}

async function onSubmit(action) {
  if (!currentItem.value) return
  submitting.value = true
  try {
    await executeReview({
      planId: currentItem.value.planId,
      action
    })
    moveToNext()
    if (action === 'MASTERED') {
      ElMessage.success('已标记为掌握')
      return
    }
    if (action === 'POSTPONE') {
      ElMessage.success('已暂缓一天')
      return
    }
    ElMessage.success('已记录继续复习')
  } catch (error) {
    ElMessage.error(error?.message || '提交复习结果失败')
  } finally {
    submitting.value = false
  }
}

loadSessionItems()
</script>

<style scoped>
.review-mode-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mode-head {
  display: flex;
  align-items: flex-start;
}

.mode-head h2 {
  margin: 0;
  color: var(--primary);
}

.mode-sub {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.content-block,
.empty-block,
.done-block {
  padding: 16px;
}

.done-block h3 {
  margin: 0;
  color: var(--success);
}

.done-block p {
  margin: 10px 0 0;
  color: var(--text-sub);
}

.content-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.content-title {
  margin-left: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
}

.content-meta {
  display: flex;
  gap: 8px;
  color: var(--text-sub);
  font-size: 12px;
}

.qa-block h3,
.note-block h3 {
  margin: 14px 0 8px;
  color: var(--text-accent-strong);
}

.text-block {
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  line-height: 1.65;
  color: var(--text-main);
}

.reveal-box {
  margin: 10px 0;
}

.note-title {
  margin: 0;
  font-size: 16px;
  color: var(--text-main);
}

.footer-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
