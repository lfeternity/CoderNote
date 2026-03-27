<template>
  <section class="fade-in stats-page" v-loading="loading">
    <header class="stats-head">
      <div>
        <h2>个人统计</h2>
        <p>用图形化看板快速查看错题结构、复习效率与薄弱知识点分布。</p>
      </div>
      <div class="head-metrics">
        <span class="metric-chip">
          <small>掌握率</small>
          <strong>{{ masteryRateText }}</strong>
        </span>
        <span class="metric-chip">
          <small>今日完成率</small>
          <strong>{{ todayRateText }}</strong>
        </span>
      </div>
    </header>

    <div class="cards">
      <article
        v-for="card in kpiCards"
        :key="card.key"
        class="surface-card stat-card"
        :class="`tone-${card.tone}`"
      >
        <div class="stat-meta">
          <p>{{ card.label }}</p>
          <span class="stat-pill">{{ card.percentText }}</span>
        </div>
        <h3>{{ card.value }}</h3>
        <div class="stat-track">
          <i class="stat-fill" :style="{ width: card.barPercent }"></i>
        </div>
      </article>
    </div>

    <div class="insight-grid">
      <article class="surface-card block">
        <div class="block-head">
          <h3>错题状态总览</h3>
          <span class="caption">共 {{ totalQuestions }} 题</span>
        </div>
        <div class="overview-layout">
          <div class="donut-wrap" :class="{ empty: totalQuestions === 0 }">
            <div class="donut-ring" :style="{ background: statusDonutBackground }">
              <div class="donut-inner">
                <small>掌握率</small>
                <strong>{{ masteryRateText }}</strong>
              </div>
            </div>
          </div>
          <ul class="status-legend">
            <li v-for="item in statusSegments" :key="item.key">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <div class="legend-text">
                <p>{{ item.label }}</p>
                <small>{{ item.percentText }}</small>
              </div>
              <strong>{{ item.value }}</strong>
            </li>
          </ul>
        </div>
      </article>

      <article class="surface-card block">
        <div class="block-head">
          <h3>复习趋势速览</h3>
          <span class="caption">{{ trendCompletedTotal }} / {{ trendDueTotal }}</span>
        </div>

        <el-empty
          v-if="!reviewTrendRows.length"
          description="暂无趋势数据"
          :image-size="56"
        />

        <template v-else>
          <div class="sparkline-card">
            <svg class="trend-sparkline" viewBox="0 0 100 36" preserveAspectRatio="none">
              <polyline class="trend-sparkline-guide" points="4,34 96,34" />
              <polygon class="trend-sparkline-area" :points="sparklineAreaPoints" />
              <polyline class="trend-sparkline-line" :points="sparklinePoints" />
              <circle
                v-for="(node, index) in sparklineNodes"
                :key="`${node.date}-${index}`"
                class="trend-sparkline-dot"
                :cx="node.x"
                :cy="node.y"
                r="1.4"
              />
            </svg>
            <div class="sparkline-footer">
              <span>{{ trendStartDate }}</span>
              <span>{{ trendEndDate }}</span>
            </div>
          </div>

          <ul class="status-bars">
            <li v-for="item in reviewTrendRows" :key="item.rawDate || item.date">
              <span class="status-day">{{ item.date }}</span>
              <div class="status-bar-track">
                <i class="status-bar-fill" :style="{ width: trendCompletedBarPercent(item) }"></i>
              </div>
              <span class="status-value">{{ item.completedCount }} / {{ item.dueTotal }}</span>
              <span class="status-rate">{{ formatPercent(item.completionRate, 0) }}</span>
            </li>
          </ul>
        </template>
      </article>
    </div>

    <div class="knowledge-grid">
      <article class="surface-card block">
        <div class="block-head">
          <h3>易错知识点图形排行</h3>
          <span class="caption">Top {{ topKnowledgeRows.length }}</span>
        </div>

        <el-empty
          v-if="!topKnowledgeRows.length"
          description="暂无统计数据"
          :image-size="56"
        />

        <ul v-else class="knowledge-chart">
          <li v-for="(row, index) in topKnowledgeRows" :key="row.tagId ?? `${row.tagName}-${index}`">
            <div class="knowledge-meta">
              <span class="rank-badge">{{ row.rank }}</span>
              <span class="knowledge-name">{{ row.tagName }}</span>
              <strong>{{ row.count }}</strong>
            </div>
            <div class="knowledge-bar-bg">
              <div class="knowledge-bar-fill" :style="knowledgeBarStyle(row, index)"></div>
            </div>
          </li>
        </ul>
      </article>

      <article class="surface-card block">
        <div class="block-head">
          <h3>知识点明细表</h3>
          <span class="caption">占比视图</span>
        </div>

        <el-empty
          v-if="!topKnowledgeRows.length"
          description="暂无明细数据"
          :image-size="56"
        />

        <el-table v-else :data="topKnowledgeRows" stripe size="small" style="width: 100%">
          <el-table-column prop="rank" label="排名" width="72" />
          <el-table-column prop="tagName" label="知识点标签" min-width="180" />
          <el-table-column prop="count" label="错题数量" width="96" />
          <el-table-column label="占比" min-width="180">
            <template #default="{ row }">
              <div class="table-share">
                <div class="table-share-track">
                  <span class="table-share-fill" :style="{ width: `${row.sharePercent.toFixed(2)}%` }"></span>
                </div>
                <span class="table-share-text">{{ formatPercent(row.sharePercent, 1) }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </div>

    <div class="review-grid">
      <article class="surface-card block">
        <h3>复习完成情况</h3>

        <div class="review-kpi">
          <div class="review-kpi-card">
            <p>今日复习完成 / 总待复习</p>
            <h4>{{ todayCompletedCount }} / {{ todayDueCount }}</h4>
          </div>
          <div class="review-kpi-card">
            <p>累计复习次数</p>
            <h4>{{ reviewTotalCount }}</h4>
          </div>
          <div class="review-kpi-card">
            <p>知识点掌握率</p>
            <h4>{{ masteryRateText }}</h4>
          </div>
        </div>

        <h4 class="sub-title">近 7 天复习趋势</h4>
        <el-empty
          v-if="!reviewTrendRows.length"
          description="暂无趋势数据"
          :image-size="56"
        />
        <div v-else class="trend-list">
          <div v-for="item in reviewTrendRows" :key="item.rawDate || item.date" class="trend-item">
            <span class="trend-date">{{ item.date }}</span>
            <div class="trend-bar-bg">
              <div class="trend-bar-fill" :style="{ width: `${item.completionRate.toFixed(2)}%` }"></div>
            </div>
            <span class="trend-text">{{ item.completedCount }} / {{ item.dueTotal }}</span>
            <span class="trend-rate">{{ formatPercent(item.completionRate, 0) }}</span>
          </div>
        </div>
      </article>

      <article class="surface-card block">
        <h3>薄弱知识点排名</h3>
        <el-empty
          v-if="!weakTagRows.length"
          description="暂无薄弱标签数据"
          :image-size="60"
        />
        <ul v-else class="weak-list">
          <li v-for="(item, index) in weakTagRows" :key="item.tagId ?? `${item.tagName}-${index}`">
            <div class="weak-top">
              <span class="rank">{{ index + 1 }}</span>
              <span class="name">{{ item.tagName }}</span>
              <strong class="count">{{ item.pendingCount }}</strong>
            </div>
            <div class="weak-bar-bg">
              <i class="weak-bar-fill" :style="{ width: weakTagBarPercent(item) }"></i>
            </div>
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { getStatisticsOverview } from '../api/statistics'

const KNOWLEDGE_GRADIENTS = [
  'linear-gradient(90deg, #1e40af, #3b82f6)',
  'linear-gradient(90deg, #2563eb, #60a5fa)',
  'linear-gradient(90deg, #0284c7, #38bdf8)',
  'linear-gradient(90deg, #0f766e, #14b8a6)',
  'linear-gradient(90deg, #475569, #64748b)'
]

const loading = ref(false)
const detail = reactive({
  questionTotal: 0,
  masteredQuestionCount: 0,
  reviewingQuestionCount: 0,
  notMasteredQuestionCount: 0,
  materialTotal: 0,
  topKnowledgePoints: [],
  reviewTodayCompletedCount: 0,
  reviewTodayDueTotal: 0,
  reviewTotalCount: 0,
  reviewMasteryRate: 0,
  reviewWeakTags: [],
  reviewTrend: []
})

function toNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function clampPercent(value) {
  return Math.max(0, Math.min(100, value))
}

function formatPercent(value, digits = 1) {
  return `${clampPercent(toNumber(value)).toFixed(digits)}%`
}

function formatDateLabel(dateText) {
  const text = String(dateText || '').trim()
  if (!text) return '--'
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return text.slice(5)
  }
  return text
}

const totalQuestions = computed(() => toNumber(detail.questionTotal))
const masteredCount = computed(() => toNumber(detail.masteredQuestionCount))
const reviewingCount = computed(() => toNumber(detail.reviewingQuestionCount))
const notMasteredCount = computed(() => toNumber(detail.notMasteredQuestionCount))
const materialTotal = computed(() => toNumber(detail.materialTotal))
const todayCompletedCount = computed(() => toNumber(detail.reviewTodayCompletedCount))
const todayDueCount = computed(() => toNumber(detail.reviewTodayDueTotal))
const reviewTotalCount = computed(() => toNumber(detail.reviewTotalCount))

const masteryRate = computed(() => {
  const raw = detail.reviewMasteryRate
  if (raw !== null && raw !== undefined && raw !== '') {
    return clampPercent(toNumber(raw))
  }
  if (totalQuestions.value <= 0) {
    return 0
  }
  return clampPercent((masteredCount.value / totalQuestions.value) * 100)
})

const todayRate = computed(() => {
  if (todayDueCount.value <= 0) {
    return 0
  }
  return clampPercent((todayCompletedCount.value / todayDueCount.value) * 100)
})

const masteryRateText = computed(() => formatPercent(masteryRate.value, 2))
const todayRateText = computed(() => formatPercent(todayRate.value, 0))

const statusSegments = computed(() => {
  const total = totalQuestions.value
  const base = [
    { key: 'mastered', label: '已掌握', value: masteredCount.value, color: '#10b981' },
    { key: 'reviewing', label: '复习中', value: reviewingCount.value, color: '#f59e0b' },
    { key: 'notMastered', label: '未复习', value: notMasteredCount.value, color: '#ef4444' }
  ]
  const covered = base.reduce((sum, item) => sum + item.value, 0)
  const other = Math.max(0, total - covered)
  if (other > 0) {
    base.push({ key: 'other', label: '其他状态', value: other, color: '#64748b' })
  }
  return base.map((item) => {
    const percent = total > 0 ? clampPercent((item.value / total) * 100) : 0
    return {
      ...item,
      percent,
      percentText: formatPercent(percent, 1)
    }
  })
})

const statusDonutBackground = computed(() => {
  if (totalQuestions.value <= 0) {
    return 'conic-gradient(var(--border-soft) 0deg 360deg)'
  }

  let start = 0
  const pieces = statusSegments.value
    .filter((item) => item.value > 0)
    .map((item) => {
      const angle = (item.percent / 100) * 360
      const end = Math.min(360, start + angle)
      const part = `${item.color} ${start.toFixed(2)}deg ${end.toFixed(2)}deg`
      start = end
      return part
    })

  if (start < 360) {
    pieces.push(`var(--border-soft) ${start.toFixed(2)}deg 360deg`)
  }

  return `conic-gradient(${pieces.join(', ')})`
})

const kpiCards = computed(() => {
  const total = totalQuestions.value
  const materialRatio = total > 0 ? (materialTotal.value / total) * 100 : 0
  const rows = [
    {
      key: 'questionTotal',
      label: '错题总数',
      value: total,
      tone: 'primary',
      percent: total > 0 ? 100 : 0,
      percentText: total > 0 ? '错题基准 100%' : '暂无数据'
    },
    {
      key: 'masteredQuestionCount',
      label: '已掌握',
      value: masteredCount.value,
      tone: 'success',
      percent: total > 0 ? (masteredCount.value / total) * 100 : 0,
      percentText: `占错题 ${formatPercent(total > 0 ? (masteredCount.value / total) * 100 : 0, 0)}`
    },
    {
      key: 'reviewingQuestionCount',
      label: '复习中',
      value: reviewingCount.value,
      tone: 'warning',
      percent: total > 0 ? (reviewingCount.value / total) * 100 : 0,
      percentText: `占错题 ${formatPercent(total > 0 ? (reviewingCount.value / total) * 100 : 0, 0)}`
    },
    {
      key: 'notMasteredQuestionCount',
      label: '未复习',
      value: notMasteredCount.value,
      tone: 'danger',
      percent: total > 0 ? (notMasteredCount.value / total) * 100 : 0,
      percentText: `占错题 ${formatPercent(total > 0 ? (notMasteredCount.value / total) * 100 : 0, 0)}`
    },
    {
      key: 'materialTotal',
      label: '资料总数',
      value: materialTotal.value,
      tone: 'indigo',
      percent: clampPercent(materialRatio),
      percentText: total > 0 ? `资料/错题 ${formatPercent(materialRatio, 0)}` : '资料沉淀'
    }
  ]

  return rows.map((item) => {
    const minVisible = item.value > 0 ? 10 : 0
    const width = Math.max(minVisible, clampPercent(item.percent))
    return {
      ...item,
      barPercent: `${width.toFixed(2)}%`
    }
  })
})

const topKnowledgeRows = computed(() => {
  const source = Array.isArray(detail.topKnowledgePoints) ? detail.topKnowledgePoints : []
  const normalized = source
    .map((item) => ({
      tagId: item?.tagId ?? null,
      tagName: String(item?.tagName || '未命名标签'),
      count: toNumber(item?.count)
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)

  const total = normalized.reduce((sum, item) => sum + item.count, 0)
  return normalized.map((item, index) => ({
    ...item,
    rank: index + 1,
    sharePercent: total > 0 ? clampPercent((item.count / total) * 100) : 0
  }))
})

const topKnowledgeMaxCount = computed(() => Math.max(...topKnowledgeRows.value.map((item) => item.count), 1))

function knowledgeBarStyle(row, index) {
  const ratio = topKnowledgeMaxCount.value > 0 ? clampPercent((row.count / topKnowledgeMaxCount.value) * 100) : 0
  const minVisible = row.count > 0 ? 10 : 0
  return {
    width: `${Math.max(minVisible, ratio).toFixed(2)}%`,
    background: KNOWLEDGE_GRADIENTS[index % KNOWLEDGE_GRADIENTS.length]
  }
}

const reviewTrendRows = computed(() => {
  const source = Array.isArray(detail.reviewTrend) ? detail.reviewTrend : []
  return source.map((item) => {
    const dueTotal = toNumber(item?.dueTotal)
    const completedCount = toNumber(item?.completedCount)
    return {
      rawDate: String(item?.date || ''),
      date: formatDateLabel(item?.date),
      dueTotal,
      completedCount,
      completionRate: dueTotal > 0 ? clampPercent((completedCount / dueTotal) * 100) : 0
    }
  })
})

const trendCompletedTotal = computed(() =>
  reviewTrendRows.value.reduce((sum, item) => sum + item.completedCount, 0)
)

const trendDueTotal = computed(() =>
  reviewTrendRows.value.reduce((sum, item) => sum + item.dueTotal, 0)
)

const trendCompletedMax = computed(() =>
  Math.max(...reviewTrendRows.value.map((item) => item.completedCount), 1)
)

const trendStartDate = computed(() =>
  reviewTrendRows.value.length ? reviewTrendRows.value[0].date : '--'
)

const trendEndDate = computed(() => {
  if (!reviewTrendRows.value.length) {
    return '--'
  }
  return reviewTrendRows.value[reviewTrendRows.value.length - 1].date
})

function trendCompletedBarPercent(item) {
  const ratio = trendCompletedMax.value > 0 ? (item.completedCount / trendCompletedMax.value) * 100 : 0
  const minVisible = item.completedCount > 0 ? 8 : 0
  return `${Math.max(minVisible, clampPercent(ratio)).toFixed(2)}%`
}

const sparklineNodes = computed(() => {
  const rows = reviewTrendRows.value
  if (!rows.length) return []

  const maxValue = Math.max(...rows.map((item) => item.completedCount), 1)
  if (rows.length === 1) {
    const y = 34 - (rows[0].completedCount / maxValue) * 24
    return [{ ...rows[0], x: 50, y: Number(y.toFixed(2)) }]
  }

  const step = 92 / (rows.length - 1)
  return rows.map((row, index) => {
    const x = 4 + step * index
    const y = 34 - (row.completedCount / maxValue) * 24
    return {
      ...row,
      x: Number(x.toFixed(2)),
      y: Number(y.toFixed(2))
    }
  })
})

const sparklinePoints = computed(() =>
  sparklineNodes.value.map((node) => `${node.x},${node.y}`).join(' ')
)

const sparklineAreaPoints = computed(() =>
  sparklinePoints.value ? `4,34 ${sparklinePoints.value} 96,34` : ''
)

const weakTagRows = computed(() => {
  const source = Array.isArray(detail.reviewWeakTags) ? detail.reviewWeakTags : []
  return source
    .map((item) => ({
      tagId: item?.tagId ?? null,
      tagName: String(item?.tagName || '未命名标签'),
      pendingCount: toNumber(item?.pendingCount)
    }))
    .sort((a, b) => b.pendingCount - a.pendingCount)
    .slice(0, 10)
})

const weakTagMaxPending = computed(() =>
  Math.max(...weakTagRows.value.map((item) => item.pendingCount), 1)
)

function weakTagBarPercent(item) {
  const ratio = weakTagMaxPending.value > 0 ? (item.pendingCount / weakTagMaxPending.value) * 100 : 0
  const minVisible = item.pendingCount > 0 ? 10 : 0
  return `${Math.max(minVisible, clampPercent(ratio)).toFixed(2)}%`
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getStatisticsOverview()
    Object.assign(detail, data)
  } finally {
    loading.value = false
  }
}

loadDetail()
</script>

<style scoped>
.stats-page {
  display: grid;
  gap: 12px;
}

.stats-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

h2 {
  margin: 0;
  color: var(--primary);
}

.stats-head p {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.head-metrics {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.metric-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  min-width: 96px;
}

.metric-chip small {
  color: var(--text-sub);
  font-size: 12px;
}

.metric-chip strong {
  color: var(--text-main);
  font-size: 16px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 14px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(140deg, var(--surface) 0%, var(--surface-soft) 120%);
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 3px;
  background: var(--tone-color, var(--primary));
}

.stat-card.tone-primary {
  --tone-color: var(--primary);
}

.stat-card.tone-success {
  --tone-color: var(--success);
}

.stat-card.tone-warning {
  --tone-color: var(--warning);
}

.stat-card.tone-danger {
  --tone-color: var(--danger);
}

.stat-card.tone-indigo {
  --tone-color: #4f46e5;
}

.stat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.stat-card p {
  margin: 0;
  color: var(--text-sub);
  font-size: 13px;
}

.stat-pill {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(30, 64, 175, 0.12);
  color: var(--text-accent);
  font-size: 11px;
}

.stat-card h3 {
  margin: 8px 0 10px;
  font-size: 28px;
  color: var(--text-main);
}

.stat-track {
  height: 6px;
  border-radius: 999px;
  background: var(--surface-soft);
  border: 1px solid var(--border-soft);
  overflow: hidden;
}

.stat-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--tone-color), color-mix(in srgb, var(--tone-color) 45%, #ffffff));
}

.insight-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 10px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 10px;
}

.block {
  padding: 14px;
}

.block-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

h3 {
  margin: 0;
  color: var(--primary);
}

.caption {
  font-size: 12px;
  color: var(--text-sub);
}

.overview-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.donut-wrap {
  display: flex;
  justify-content: center;
}

.donut-ring {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  position: relative;
  box-shadow: inset 0 0 0 1px rgba(30, 64, 175, 0.08);
}

.donut-wrap.empty .donut-ring {
  opacity: 0.6;
}

.donut-inner {
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
}

.donut-inner small {
  color: var(--text-sub);
  font-size: 12px;
}

.donut-inner strong {
  font-size: 22px;
  color: var(--text-main);
}

.status-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.status-legend li {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface-soft);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-text p {
  margin: 0;
  color: var(--text-main);
}

.legend-text small {
  color: var(--text-sub);
  font-size: 12px;
}

.status-legend strong {
  color: var(--text-main);
}

.sparkline-card {
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  background: linear-gradient(180deg, var(--surface-soft) 0%, color-mix(in srgb, var(--surface-soft) 40%, var(--surface)) 100%);
  padding: 10px 12px;
}

.trend-sparkline {
  width: 100%;
  height: 96px;
}

.trend-sparkline-guide {
  fill: none;
  stroke: rgba(100, 116, 139, 0.3);
  stroke-width: 0.6;
}

.trend-sparkline-area {
  fill: rgba(59, 130, 246, 0.18);
}

.trend-sparkline-line {
  fill: none;
  stroke: #2563eb;
  stroke-width: 1.4;
}

.trend-sparkline-dot {
  fill: #1e40af;
}

.sparkline-footer {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  color: var(--text-sub);
  font-size: 12px;
}

.status-bars {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  display: grid;
  gap: 8px;
}

.status-bars li {
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr) 76px 50px;
  align-items: center;
  gap: 8px;
}

.status-day {
  color: var(--text-sub);
  font-size: 12px;
}

.status-bar-track {
  height: 8px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  overflow: hidden;
}

.status-bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1d4ed8, #38bdf8);
}

.status-value {
  text-align: right;
  color: var(--text-sub);
  font-size: 12px;
}

.status-rate {
  text-align: right;
  color: var(--text-main);
  font-size: 12px;
  font-weight: 600;
}

.knowledge-chart {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.knowledge-chart li {
  padding: 10px;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface-soft);
}

.knowledge-meta {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 64, 175, 0.12);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}

.knowledge-name {
  min-width: 0;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-bar-bg {
  height: 10px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  overflow: hidden;
}

.knowledge-bar-fill {
  height: 100%;
  border-radius: inherit;
}

.table-share {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 54px;
  align-items: center;
  gap: 8px;
}

.table-share-track {
  height: 7px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  overflow: hidden;
}

.table-share-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1e3a8a, #3b82f6);
}

.table-share-text {
  text-align: right;
  color: var(--text-sub);
  font-size: 12px;
}

.review-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 10px;
}

.review-kpi {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.review-kpi-card {
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  padding: 10px;
  background: var(--surface-soft);
}

.review-kpi-card p {
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
}

.review-kpi-card h4 {
  margin: 6px 0 0;
  color: var(--text-main);
  font-size: 24px;
}

.sub-title {
  margin: 0 0 10px;
  color: var(--text-accent);
}

.trend-list {
  display: grid;
  gap: 8px;
}

.trend-item {
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr) 72px 48px;
  align-items: center;
  gap: 8px;
}

.trend-date {
  color: var(--text-sub);
  font-size: 12px;
}

.trend-bar-bg {
  height: 10px;
  border-radius: 999px;
  background: var(--surface-soft);
  border: 1px solid var(--border-soft);
  overflow: hidden;
}

.trend-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1e40af, #60a5fa);
}

.trend-text {
  text-align: right;
  color: var(--text-sub);
  font-size: 12px;
}

.trend-rate {
  text-align: right;
  color: var(--text-main);
  font-size: 12px;
  font-weight: 600;
}

.weak-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.weak-list li {
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  padding: 10px;
  background: var(--surface-soft);
}

.weak-top {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 64, 175, 0.12);
  color: var(--primary);
  font-size: 12px;
}

.name {
  color: var(--text-main);
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.count {
  color: var(--danger);
}

.weak-bar-bg {
  height: 8px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  overflow: hidden;
}

.weak-bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #f97316, #ef4444);
}

@media (max-width: 1320px) {
  .cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .insight-grid,
  .knowledge-grid,
  .review-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .stats-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-layout {
    grid-template-columns: 1fr;
  }

  .review-kpi {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .cards {
    grid-template-columns: 1fr;
  }

  .status-bars li,
  .trend-item {
    grid-template-columns: 48px minmax(0, 1fr) 68px 42px;
    gap: 6px;
  }
}
</style>
