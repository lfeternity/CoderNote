<template>
  <section class="fade-in relation-page" v-loading="loading">
    <header class="surface-card hero-card">
      <div class="hero-copy">
        <p class="hero-eyebrow">Tag Linkage</p>
        <div class="page-title-back hero-title-row">
          <el-button class="back-chevron-btn" plain aria-label="返回标签库" @click="$router.push('/tag/list')">&lt;</el-button>
          <h2>标签关联页</h2>
        </div>
        <p class="hero-desc">围绕当前标签聚合错题、资料与笔记，支持一键跳转到对应详情。</p>
        <div class="hero-tagline">
          <span>当前标签</span>
          <strong>{{ detail.tagName || '未命名标签' }}</strong>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-total">
          <small>关联总量</small>
          <strong>{{ totalRelations }}</strong>
        </div>
      </div>
    </header>

    <section class="surface-card summary-card">
      <div class="summary-row">
        <div class="summary-item">
          <span>标签名称</span>
          <strong>{{ detail.tagName || '-' }}</strong>
        </div>
        <div class="summary-item">
          <span>使用次数</span>
          <strong>{{ detail.usageCount ?? 0 }}</strong>
        </div>
      </div>

      <div class="metric-grid">
        <article
          v-for="item in relationMetrics"
          :key="item.key"
          class="metric-card"
          :class="`tone-${item.tone}`"
        >
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </article>
      </div>
    </section>

    <div class="panel-grid">
      <article class="surface-card relation-card">
        <div class="card-head">
          <h3>关联错题</h3>
          <span>{{ relatedQuestionCount }} 条</span>
        </div>
        <el-empty v-if="!relatedQuestionCount" description="暂无关联错题" :image-size="64" />
        <el-table v-else :data="detail.relatedQuestions" class="relation-table" style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="180">
            <template #default="{ row }">
              <el-link :underline="false" type="primary" @click="$router.push(`/error-question/detail/${row.id}`)">{{ row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="language" label="语言" width="110" />
          <el-table-column label="掌握状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.masteryStatus)">{{ statusLabel(row.masteryStatus) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="surface-card relation-card">
        <div class="card-head">
          <h3>关联资料</h3>
          <span>{{ relatedMaterialCount }} 条</span>
        </div>
        <el-empty v-if="!relatedMaterialCount" description="暂无关联资料" :image-size="64" />
        <el-table v-else :data="detail.relatedMaterials" class="relation-table" style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="180">
            <template #default="{ row }">
              <el-link :underline="false" type="primary" @click="$router.push(`/study-material/detail/${row.id}`)">{{ row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="materialTypeZh" label="资料类型" width="130" />
          <el-table-column prop="languageZh" label="语言" width="110" />
        </el-table>
      </article>

      <article class="surface-card relation-card">
        <div class="card-head">
          <h3>关联笔记</h3>
          <span>{{ relatedNoteCount }} 条</span>
        </div>
        <el-empty v-if="!relatedNoteCount" description="暂无关联笔记" :image-size="64" />
        <el-table v-else :data="detail.relatedNotes" class="relation-table" style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="180">
            <template #default="{ row }">
              <el-link :underline="false" type="primary" @click="$router.push(`/note/detail/${row.id}`)">{{ row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="languageZh" label="语言" width="110" />
          <el-table-column label="收藏" width="90">
            <template #default="{ row }">
              <el-tag :type="row.favorite ? 'warning' : 'info'" effect="light">{{ row.favorite ? '已收藏' : '未收藏' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getTagRelation } from '../api/tag'
import { toZhLanguageLabel, toZhMaterialTypeLabel } from '../utils/material'

const route = useRoute()
const loading = ref(false)
const detail = reactive({
  tagName: '',
  usageCount: 0,
  relatedQuestions: [],
  relatedMaterials: [],
  relatedNotes: []
})

const relatedQuestionCount = computed(() => (detail.relatedQuestions || []).length)
const relatedMaterialCount = computed(() => (detail.relatedMaterials || []).length)
const relatedNoteCount = computed(() => (detail.relatedNotes || []).length)

const totalRelations = computed(() => relatedQuestionCount.value + relatedMaterialCount.value + relatedNoteCount.value)

const relationMetrics = computed(() => ([
  {
    key: 'question',
    label: '关联错题',
    value: relatedQuestionCount.value,
    hint: relatedQuestionCount.value ? '可直达错题详情页' : '当前标签暂无错题',
    tone: 'danger'
  },
  {
    key: 'material',
    label: '关联资料',
    value: relatedMaterialCount.value,
    hint: relatedMaterialCount.value ? '已建立资料联动' : '当前标签暂无资料',
    tone: 'primary'
  },
  {
    key: 'note',
    label: '关联笔记',
    value: relatedNoteCount.value,
    hint: relatedNoteCount.value ? '支持继续沉淀总结' : '当前标签暂无笔记',
    tone: 'success'
  }
]))

function statusLabel(value) {
  if (value === 'MASTERED') return '已掌握'
  if (value === 'REVIEWING') return '复习中'
  return '未复习'
}

function statusType(value) {
  if (value === 'MASTERED') return 'success'
  if (value === 'REVIEWING') return 'warning'
  return 'danger'
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getTagRelation(route.params.tagId)
    Object.assign(detail, {
      ...data,
      relatedMaterials: (data.relatedMaterials || []).map((item) => ({
        ...item,
        materialTypeZh: toZhMaterialTypeLabel(item.materialType),
        languageZh: toZhLanguageLabel(item.language)
      })),
      relatedNotes: (data.relatedNotes || []).map((item) => ({
        ...item,
        languageZh: toZhLanguageLabel(item.language)
      }))
    })
  } finally {
    loading.value = false
  }
}

loadDetail()
</script>
<style scoped>
.relation-page {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 2px;
  max-width: 100%;
  overflow-x: hidden;
}

.relation-page > * {
  position: relative;
  z-index: 1;
}

.relation-page::before,
.relation-page::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(30px);
  opacity: 0.38;
  z-index: 0;
}

.relation-page::before {
  width: 230px;
  height: 230px;
  right: -60px;
  top: 28px;
  background: rgba(201, 100, 66, 0.2);
}

.relation-page::after {
  width: 170px;
  height: 170px;
  left: 12%;
  top: 290px;
  background: rgba(16, 185, 129, 0.1);
}

.hero-card {
  padding: 14px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(
    120deg,
    var(--surface) 0%,
    color-mix(in srgb, var(--surface-soft) 84%, var(--surface)) 100%
  );
}

.hero-copy {
  min-width: 0;
}

.hero-eyebrow {
  margin: 0;
  color: var(--text-accent);
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  font-weight: 600;
}

.hero-copy h2 {
  margin: 2px 0 0;
  color: var(--primary);
  font-size: clamp(30px, 2.35vw, 38px);
  line-height: 1.1;
}

.hero-title-row {
  margin-top: 2px;
}

.hero-desc {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.5;
}

.hero-tagline {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 88%, transparent);
  padding: 5px 12px;
}

.hero-tagline span {
  color: var(--text-sub);
  font-size: 12px;
}

.hero-tagline strong {
  color: var(--text-accent-strong);
}

.hero-side {
  min-width: 180px;
  display: grid;
  align-content: space-between;
  justify-items: end;
  gap: 8px;
}

.hero-total {
  width: 100%;
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 92%, transparent);
  padding: 10px 12px;
  text-align: right;
}

.hero-total small {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
}

.hero-total strong {
  display: block;
  margin-top: 2px;
  font-size: clamp(28px, 2vw, 34px);
  line-height: 1;
  color: var(--text-main);
  font-family: 'DIN Alternate', 'Bahnschrift', 'Segoe UI', sans-serif;
}

.summary-card {
  padding: 12px;
  border: 1px solid var(--border-soft);
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary-item {
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(145deg, var(--surface) 0%, var(--surface-soft) 120%);
  padding: 10px 12px;
}

.summary-item span {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin-top: 4px;
  color: var(--text-main);
  font-size: clamp(20px, 1.8vw, 26px);
  line-height: 1.2;
}

.metric-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  --tone-color: var(--primary);
  position: relative;
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(150deg, var(--surface) 0%, var(--surface-soft) 130%);
  padding: 10px 11px;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  top: 0;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--tone-color), rgba(255, 255, 255, 0));
}

.metric-card.tone-primary {
  --tone-color: var(--primary);
}

.metric-card.tone-success {
  --tone-color: var(--success);
}

.metric-card.tone-danger {
  --tone-color: var(--danger);
}

.metric-card p {
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 4px;
  color: var(--tone-color);
  font-size: clamp(24px, 1.9vw, 30px);
  line-height: 1;
  font-family: 'DIN Alternate', 'Bahnschrift', 'Segoe UI', sans-serif;
}

.metric-card small {
  display: block;
  margin-top: 4px;
  color: var(--text-sub);
  line-height: 1.45;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.relation-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(
    165deg,
    var(--surface) 0%,
    color-mix(in srgb, var(--surface-soft) 76%, var(--surface)) 100%
  );
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 10px;
}

.card-head h3 {
  margin: 0;
  color: var(--primary);
  font-size: 28px;
  line-height: 1.15;
  letter-spacing: 0.4px;
}

.card-head span {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 90%, transparent);
  color: var(--text-accent);
  font-size: 12px;
}

:deep(.relation-card .el-empty) {
  padding: 26px 0;
}

:deep(.relation-table .el-table__header-wrapper th.el-table__cell) {
  background: var(--surface-soft);
  color: var(--text-accent-strong);
  font-weight: 600;
  border-bottom-color: var(--border-soft);
}

:deep(.relation-table .el-table__body-wrapper td.el-table__cell) {
  border-bottom-color: var(--border);
}

:deep(.relation-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.relation-table tr:hover > td.el-table__cell) {
  background: rgba(201, 100, 66, 0.08);
}

:deep(.relation-table .el-link) {
  font-weight: 500;
}

:global(html[data-theme='dark']) .relation-page .hero-card {
  background: linear-gradient(120deg, rgba(44, 31, 25, 0.96) 0%, rgba(58, 39, 31, 0.92) 100%);
}

:global(html[data-theme='dark']) .relation-page .hero-tagline,
:global(html[data-theme='dark']) .relation-page .hero-total,
:global(html[data-theme='dark']) .relation-page .card-head span {
  background: rgba(32, 24, 20, 0.72);
}

:global(html[data-theme='dark']) .relation-page .relation-card,
:global(html[data-theme='dark']) .relation-page .summary-card {
  background: linear-gradient(160deg, rgba(36, 28, 23, 0.94) 0%, rgba(47, 34, 28, 0.9) 100%);
}

:global(html[data-theme='dark']) .relation-page .metric-card,
:global(html[data-theme='dark']) .relation-page .summary-item {
  background: linear-gradient(150deg, rgba(31, 24, 20, 0.95) 0%, rgba(43, 31, 26, 0.92) 100%);
}

:global(html[data-theme='dark']) .relation-page::before {
  background: rgba(201, 100, 66, 0.16);
}

:global(html[data-theme='dark']) .relation-page::after {
  background: rgba(111, 127, 104, 0.14);
}

:global(html[data-theme='dark']) .relation-page .hero-card,
:global(html[data-theme='dark']) .relation-page .summary-card,
:global(html[data-theme='dark']) .relation-page .relation-card,
:global(html[data-theme='dark']) .relation-page .hero-tagline,
:global(html[data-theme='dark']) .relation-page .hero-total,
:global(html[data-theme='dark']) .relation-page .card-head span {
  border-color: var(--border-soft);
}

:global(html[data-theme='dark']) .relation-page .hero-copy h2,
:global(html[data-theme='dark']) .relation-page .card-head h3 {
  color: var(--color-brand-soft);
}

:global(html[data-theme='dark']) .relation-page .hero-eyebrow,
:global(html[data-theme='dark']) .relation-page .hero-desc,
:global(html[data-theme='dark']) .relation-page .hero-tagline span,
:global(html[data-theme='dark']) .relation-page .hero-total small,
:global(html[data-theme='dark']) .relation-page .metric-card p,
:global(html[data-theme='dark']) .relation-page .metric-card small {
  color: var(--text-sub);
}

:global(html[data-theme='dark']) .relation-page .hero-tagline strong,
:global(html[data-theme='dark']) .relation-page .hero-total strong {
  color: var(--text-main);
}

@media (max-width: 1480px) {
  .panel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .hero-card {
    flex-direction: column;
  }

  .hero-side {
    width: 100%;
    justify-items: stretch;
  }

  .summary-row {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-copy h2 {
    font-size: 32px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>

