<template>
  <section class="fade-in review-page" v-loading="loading">
    <div class="top-row">
      <div>
        <h2>我的复习</h2>
        <p class="sub-text">按艾宾浩斯节奏持续复盘，过期内容会优先置顶提醒。</p>
      </div>
      <div class="top-actions">
        <el-button type="primary" @click="goReviewMode('TODAY')">开始今日复习</el-button>
        <el-button plain type="danger" @click="onClearAllPlans">清空全部计划</el-button>
      </div>
    </div>

    <section class="surface-card insight-board">
      <div class="board-main">
        <div class="board-ring" :style="masteryDonutStyle">
          <div class="board-ring-inner">
            <small>掌握率</small>
            <strong>{{ masteryRateText }}</strong>
          </div>
        </div>
        <div class="board-copy">
          <h3>复习节奏看板</h3>
          <p>{{ boardStatusText }}</p>
          <div class="board-chips">
            <span class="board-chip">总计划 {{ planTotal }}</span>
            <span class="board-chip">过期 {{ summary.overdueCount || 0 }}</span>
            <span class="board-chip">今日待复习 {{ summary.todayTotalCount || 0 }}</span>
          </div>
        </div>
      </div>

      <ul class="board-bars">
        <li v-for="item in categoryVisualOptions" :key="`board-${item.value}`">
          <div class="board-bar-head">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </div>
          <div class="board-bar-track">
            <i class="board-bar-fill" :style="{ width: item.barPercent, background: item.color }"></i>
          </div>
          <small>{{ item.percentText }}</small>
        </li>
      </ul>
    </section>

    <div class="overview-grid">
      <article
        v-for="card in overviewCards"
        :key="card.key"
        class="surface-card stat-card"
        :class="`tone-${card.tone}`"
      >
        <div class="stat-meta">
          <p>{{ card.label }}</p>
          <span class="stat-pill">{{ card.hint }}</span>
        </div>
        <h3>{{ card.value }}</h3>
        <div class="stat-track">
          <i class="stat-fill" :style="{ width: card.barPercent }"></i>
        </div>
      </article>
    </div>

    <div class="center-grid">
      <aside class="surface-card category-panel">
        <div class="panel-head">
          <h3>复习分类</h3>
          <span>按任务量筛选</span>
        </div>
        <button
          v-for="item in categoryVisualOptions"
          :key="item.value"
          type="button"
          class="category-item"
          :class="{ active: query.category === item.value }"
          @click="changeCategory(item.value)"
        >
          <div class="category-line">
            <span>{{ item.label }}</span>
            <el-tag size="small" :type="query.category === item.value ? 'primary' : 'info'">{{ item.count }}</el-tag>
          </div>
          <div class="category-progress">
            <i :style="{ width: item.barPercent, background: item.color }"></i>
          </div>
        </button>
      </aside>

      <article class="surface-card list-panel">
        <div class="list-head">
          <div>
            <h3>复习计划列表</h3>
            <p>按内容类型、编程语言和知识点标签快速筛选</p>
          </div>
          <div class="list-head-meta">
            <span>{{ activeCategoryLabel }}</span>
            <strong>{{ total }}</strong>
          </div>
        </div>

        <div class="filter-row">
          <el-form inline>
            <el-form-item label="内容类型">
              <el-select v-model="query.contentType" clearable placeholder="全部" style="width: 130px" @change="loadList">
                <el-option label="错题" value="QUESTION" />
                <el-option label="笔记" value="NOTE" />
              </el-select>
            </el-form-item>
            <el-form-item label="编程语言">
              <el-select v-model="query.language" clearable placeholder="全部" style="width: 140px" @change="loadList">
                <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识点标签">
              <el-select v-model="query.tag" clearable filterable placeholder="全部" style="width: 170px" @change="loadList">
                <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <el-table
          :data="rows"
          style="width: 100%"
          empty-text="暂无复习计划"
          @row-click="onSelectRow"
          :row-class-name="rowClassName"
        >
          <el-table-column label="标题" min-width="220">
            <template #default="{ row }">
              <el-link :underline="false" type="primary" @click.stop="openContent(row)">
                {{ row.title }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small">{{ reviewContentTypeLabel(row.contentType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="language" label="语言" width="120" />
          <el-table-column label="标签" min-width="160">
            <template #default="{ row }">
              <div class="chip-list">
                <span v-for="tag in row.tagNames || []" :key="tag" class="tag-chip">{{ tag }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="reviewStatusTagType(row.masteryStatus)">
                {{ reviewStatusLabel(row.masteryStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下次复习" width="170">
            <template #default="{ row }">
              <span :class="{ overdue: row.overdue }">{{ formatDateTime(row.nextReviewAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="复习次数" width="90" prop="reviewCount" />
          <template #empty>
            <div class="table-empty">
              <div class="empty-graphic" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <p>当前筛选条件下暂无复习计划</p>
              <el-button type="primary" plain @click="goReviewMode(query.category)">进入复习模式</el-button>
            </div>
          </template>
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="total"
            :current-page="query.pageNo"
            :page-size="query.pageSize"
            :page-sizes="[10, 20, 50]"
            @current-change="onCurrentChange"
            @size-change="onSizeChange"
          />
        </div>
      </article>

      <aside class="surface-card action-panel">
        <div class="panel-head">
          <h3>操作区</h3>
          <span>条目详情与快捷动作</span>
        </div>

        <div class="action-shell" :class="{ 'is-empty': !selectedRow }">
          <div v-if="!selectedRow" class="action-empty-state">
            <div class="empty-spotlight" aria-hidden="true"></div>
            <h4>等待选择条目</h4>
            <p>点击中间列表中的任意条目，这里会显示详情和快捷操作。</p>
            <div class="empty-hints">
              <span>查看详情</span>
              <span>更新状态</span>
              <span>取消计划</span>
            </div>
          </div>
          <template v-else>
            <div class="selected-hero">
              <p class="selected-caption">当前选中</p>
              <h4 class="selected-title">{{ selectedRow.title }}</h4>
              <div class="selected-badges">
                <el-tag size="small">{{ reviewContentTypeLabel(selectedRow.contentType) }}</el-tag>
                <el-tag size="small" :type="reviewStatusTagType(selectedRow.masteryStatus)">
                  {{ reviewStatusLabel(selectedRow.masteryStatus) }}
                </el-tag>
              </div>
            </div>

            <div class="selected-grid">
              <div class="selected-item">
                <small>类型</small>
                <strong>{{ reviewContentTypeLabel(selectedRow.contentType) }}</strong>
              </div>
              <div class="selected-item">
                <small>语言</small>
                <strong>{{ selectedRow.language || '-' }}</strong>
              </div>
              <div class="selected-item">
                <small>状态</small>
                <strong>{{ reviewStatusLabel(selectedRow.masteryStatus) }}</strong>
              </div>
              <div class="selected-item">
                <small>复习次数</small>
                <strong>{{ selectedRow.reviewCount || 0 }}</strong>
              </div>
            </div>

            <div class="action-group">
              <el-button class="action-main-btn" type="primary" @click="goReviewMode(query.category)">进入复习模式</el-button>
              <div class="action-sub-grid">
                <el-button plain @click="openContent(selectedRow)">查看详情</el-button>
                <el-button type="success" plain @click="onUpdateStatus(selectedRow, 'MASTERED')">标记已掌握</el-button>
                <el-button type="warning" plain @click="onUpdateStatus(selectedRow, 'REVIEWING')">继续复习</el-button>
                <el-button type="danger" plain @click="onRemovePlan(selectedRow)">取消复习</el-button>
              </div>
            </div>
          </template>
        </div>

        <div class="weak-tag-block">
          <div class="panel-head weak-head">
            <h4>薄弱知识点</h4>
            <span>Top {{ weakTagRows.length }}</span>
          </div>
          <el-empty v-if="!weakTagRows.length" description="暂无数据" :image-size="46" />
          <ul v-else class="weak-tag-list">
            <li v-for="(item, index) in weakTagRows" :key="item.tagId">
              <div class="weak-tag-line">
                <div class="weak-tag-left">
                  <span class="weak-rank">#{{ index + 1 }}</span>
                  <span class="weak-name">{{ item.tagName }}</span>
                </div>
                <strong>{{ item.pendingCount }}</strong>
              </div>
              <div class="weak-tag-track">
                <i :style="{ width: item.barPercent }"></i>
              </div>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOptions } from '../api/public'
import {
  clearAllReviewPlans,
  getReviewList,
  getReviewOverview,
  getReviewSummary,
  removeReviewPlan,
  updateReviewStatus
} from '../api/review'
import { getTagList } from '../api/tag'
import { toZhLanguageOptions } from '../utils/material'
import { reviewContentTypeLabel, reviewStatusLabel, reviewStatusTagType } from '../utils/review'

const CATEGORY_COLORS = {
  TODAY: 'var(--primary)',
  OVERDUE: 'var(--danger)',
  UPCOMING: 'var(--warning)',
  COMPLETED: 'var(--success)'
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const selectedRow = ref(null)
const summary = reactive({
  todayTotalCount: 0,
  overdueCount: 0,
  upcomingCount: 0,
  completedCount: 0
})
const overview = reactive({
  todayCompletedCount: 0,
  todayDueTotal: 0,
  totalReviewCount: 0,
  masteryRate: 0,
  weakTags: [],
  trend: []
})
const languageOptions = ref([])
const tagOptions = ref([])
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  category: String(route.query.category || 'TODAY').toUpperCase(),
  contentType: '',
  language: '',
  tag: ''
})

const categoryOptions = computed(() => ([
  { value: 'TODAY', label: '今日待复习', count: summary.todayTotalCount || 0 },
  { value: 'OVERDUE', label: '已过期未复习', count: summary.overdueCount || 0 },
  { value: 'UPCOMING', label: '近期计划', count: summary.upcomingCount || 0 },
  { value: 'COMPLETED', label: '已完成 / 已掌握', count: summary.completedCount || 0 }
]))

const planTotal = computed(() => categoryOptions.value.reduce((sum, item) => sum + Number(item.count || 0), 0))

const pendingTotal = computed(
  () => Number(summary.todayTotalCount || 0) + Number(summary.overdueCount || 0) + Number(summary.upcomingCount || 0)
)

const completionRate = computed(() => {
  const dueTotal = Number(overview.todayDueTotal || 0)
  if (dueTotal <= 0) return 0
  return clampPercent((Number(overview.todayCompletedCount || 0) / dueTotal) * 100)
})

const masteryRatePercent = computed(() => clampPercent(overview.masteryRate))
const masteryRateText = computed(() => `${masteryRatePercent.value.toFixed(2)}%`)

const completedShare = computed(() => {
  if (!planTotal.value) return 0
  return clampPercent((Number(summary.completedCount || 0) / planTotal.value) * 100)
})

const todayLoadShare = computed(() => {
  if (!pendingTotal.value) return 0
  return clampPercent((Number(summary.todayTotalCount || 0) / pendingTotal.value) * 100)
})

const categoryVisualOptions = computed(() => {
  const totalCount = planTotal.value
  return categoryOptions.value.map((item) => {
    const count = Number(item.count || 0)
    const percent = totalCount > 0 ? (count / totalCount) * 100 : 0
    const fixed = percent >= 10 ? 0 : 1
    return {
      ...item,
      count,
      color: CATEGORY_COLORS[item.value] || 'var(--primary)',
      barPercent: `${clampPercent(percent).toFixed(2)}%`,
      percentText: `${clampPercent(percent).toFixed(fixed)}%`
    }
  })
})

const weakTagRows = computed(() => {
  const list = Array.isArray(overview.weakTags) ? overview.weakTags.slice(0, 6) : []
  const maxCount = Math.max(...list.map((item) => Number(item.pendingCount || 0)), 1)
  return list.map((item) => {
    const count = Number(item.pendingCount || 0)
    return {
      ...item,
      pendingCount: count,
      barPercent: `${clampPercent((count / maxCount) * 100).toFixed(2)}%`
    }
  })
})

const activeCategoryLabel = computed(
  () => categoryOptions.value.find((item) => item.value === query.category)?.label || '复习计划'
)

const boardStatusText = computed(() => {
  const overdue = Number(summary.overdueCount || 0)
  const dueToday = Number(summary.todayTotalCount || 0)
  if (!planTotal.value) return '当前暂无复习计划，建议先从错题或笔记建立计划。'
  if (overdue === 0 && dueToday === 0) return '今日无待处理条目，当前复习节奏稳定。'
  if (overdue > Math.max(2, dueToday)) return '过期条目较多，建议优先清理过期队列。'
  if (dueToday > 0) return '今日有待复习条目，建议分批完成保持节奏。'
  return '复习计划运行中，可继续推进近期任务。'
})

const masteryDonutStyle = computed(() => ({
  background: `conic-gradient(var(--primary) 0 ${masteryRatePercent.value.toFixed(2)}%, var(--surface-soft) ${masteryRatePercent.value.toFixed(2)}% 100%)`
}))

const overviewCards = computed(() => ([
  {
    key: 'today',
    tone: 'primary',
    label: '今日完成 / 总待复习',
    value: `${overview.todayCompletedCount || 0} / ${overview.todayDueTotal || 0}`,
    hint: `${completionRate.value.toFixed(0)}% 完成率`,
    barPercent: `${completionRate.value.toFixed(2)}%`
  },
  {
    key: 'total-review',
    tone: 'success',
    label: '累计复习次数',
    value: `${overview.totalReviewCount || 0}`,
    hint: `已完成占比 ${completedShare.value.toFixed(0)}%`,
    barPercent: `${completedShare.value.toFixed(2)}%`
  },
  {
    key: 'mastery',
    tone: 'accent',
    label: '掌握率',
    value: masteryRateText.value,
    hint: `过期 ${summary.overdueCount || 0} 项`,
    barPercent: `${masteryRatePercent.value.toFixed(2)}%`
  },
  {
    key: 'today-load',
    tone: 'warning',
    label: '今日待复习',
    value: `${summary.todayTotalCount || 0}`,
    hint: `待复习池占比 ${todayLoadShare.value.toFixed(0)}%`,
    barPercent: `${todayLoadShare.value.toFixed(2)}%`
  }
]))

function toNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function clampPercent(value) {
  return Math.min(100, Math.max(0, toNumber(value)))
}

function formatDateTime(value) {
  if (!value) return '-'
  const raw = String(value).replace('T', ' ')
  return raw.length > 16 ? raw.slice(0, 16) : raw
}

function rowKey(row) {
  return `${String(row?.contentType || '')}:${String(row?.contentId || '')}`
}

function rowClassName({ row }) {
  const classes = []
  if (row?.overdue) classes.push('row-overdue')
  if (selectedRow.value && rowKey(row) === rowKey(selectedRow.value)) classes.push('row-selected')
  return classes.join(' ')
}

function openContent(row) {
  if (!row) return
  if (row.contentType === 'QUESTION') {
    router.push(`/error-question/detail/${row.contentId}`)
    return
  }
  router.push(`/note/detail/${row.contentId}`)
}

function changeCategory(category) {
  query.category = category
  query.pageNo = 1
  selectedRow.value = null
  loadList()
}

function onSelectRow(row) {
  selectedRow.value = row
}

function onCurrentChange(page) {
  query.pageNo = page
  loadList()
}

function onSizeChange(size) {
  query.pageNo = 1
  query.pageSize = size
  loadList()
}

function goReviewMode(category) {
  router.push({
    path: '/review/mode',
    query: { category: String(category || query.category || 'TODAY').toUpperCase() }
  })
}

async function onUpdateStatus(row, status) {
  if (!row) return
  await updateReviewStatus({
    contentType: row.contentType,
    contentId: row.contentId,
    masteryStatus: status
  })
  ElMessage.success('状态已更新')
  await Promise.all([loadSummary(), loadOverview(), loadList()])
}

async function onRemovePlan(row) {
  if (!row) return
  await ElMessageBox.confirm('确认取消该条内容的复习提醒？', '提示', { type: 'warning' })
  await removeReviewPlan(row.contentType, row.contentId)
  ElMessage.success('已取消复习提醒')
  await Promise.all([loadSummary(), loadOverview(), loadList()])
  selectedRow.value = null
}

async function onClearAllPlans() {
  await ElMessageBox.confirm('确认清空所有复习计划？该操作不可撤销。', '危险操作', { type: 'warning' })
  await clearAllReviewPlans()
  ElMessage.success('已清空全部复习计划')
  selectedRow.value = null
  await Promise.all([loadSummary(), loadOverview(), loadList()])
}

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = toZhLanguageOptions(data.languages)
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadSummary() {
  const data = await getReviewSummary()
  Object.assign(summary, data || {})
}

async function loadOverview() {
  const data = await getReviewOverview()
  Object.assign(overview, data || {})
}

function syncSelectedRow() {
  if (!selectedRow.value) return
  const matched = rows.value.find((item) => rowKey(item) === rowKey(selectedRow.value))
  selectedRow.value = matched || null
}

async function loadList() {
  loading.value = true
  try {
    const data = await getReviewList(query)
    rows.value = data.records || []
    total.value = data.total || 0
    syncSelectedRow()
  } finally {
    loading.value = false
  }
}

async function bootstrap() {
  await Promise.all([
    loadOptions(),
    loadTags(),
    loadSummary(),
    loadOverview(),
    loadList()
  ])
}

bootstrap()
</script>

<style scoped>
.review-page {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 2px;
  max-width: 100%;
  overflow-x: hidden;
}

.review-page > * {
  position: relative;
  z-index: 1;
}

.review-page::before,
.review-page::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(28px);
  opacity: 0.4;
  z-index: 0;
}

.review-page::before {
  width: 210px;
  height: 210px;
  right: -60px;
  top: 38px;
  background: rgba(201, 100, 66, 0.2);
}

.review-page::after {
  width: 180px;
  height: 180px;
  left: 12%;
  top: 320px;
  background: rgba(16, 185, 129, 0.12);
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

h2 {
  margin: 0;
  color: var(--primary);
  font-size: clamp(32px, 2.6vw, 40px);
  letter-spacing: 0.6px;
  line-height: 1.1;
}

.sub-text {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.45;
}

.top-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

:deep(.top-actions .el-button) {
  height: 36px;
  padding: 0 16px;
  font-weight: 600;
}

.insight-board {
  padding: 12px;
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  background: linear-gradient(126deg, var(--surface) 0%, var(--surface-soft) 130%);
  border: 1px solid var(--border-soft);
}

.board-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.board-ring {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  padding: 9px;
  box-shadow: inset 0 0 0 1px var(--border-soft);
}

.board-ring-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--surface);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-shadow: 0 8px 20px rgba(96, 58, 42, 0.14);
}

.board-ring-inner small {
  font-size: 12px;
  color: var(--text-sub);
}

.board-ring-inner strong {
  margin-top: 2px;
  color: var(--text-main);
  font-size: 19px;
  line-height: 1;
  letter-spacing: 0.4px;
}

.board-copy h3 {
  margin: 0;
  color: var(--primary);
  font-size: 18px;
}

.board-copy p {
  margin: 6px 0 0;
  color: var(--text-sub);
  line-height: 1.5;
}

.board-chips {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.board-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  font-size: 11px;
  color: var(--text-accent);
}

.board-bars {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.board-bars li {
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  padding: 8px 9px;
  background: var(--surface);
}

.board-bar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.board-bar-head span {
  color: var(--text-main);
  font-size: 13px;
}

.board-bar-head strong {
  color: var(--text-main);
}

.board-bar-track {
  width: 100%;
  height: 7px;
  border-radius: 999px;
  background: var(--surface-soft);
  overflow: hidden;
}

.board-bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  min-width: 8px;
}

.board-bars small {
  display: block;
  margin-top: 4px;
  color: var(--text-sub);
  font-size: 11px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  padding: 12px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(145deg, var(--surface) 0%, var(--surface-soft) 130%);
  box-shadow: 0 8px 20px rgba(20, 20, 19, 0.06);
}

.stat-card.tone-primary {
  --tone-color: var(--primary);
}

.stat-card.tone-success {
  --tone-color: var(--success);
}

.stat-card.tone-accent {
  --tone-color: var(--text-accent-strong);
}

.stat-card.tone-warning {
  --tone-color: var(--warning);
}

.stat-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.stat-meta p {
  margin: 0;
  color: var(--text-sub);
  font-size: 11px;
}

.stat-pill {
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  padding: 2px 7px;
  font-size: 11px;
  color: var(--text-accent);
  background: var(--surface);
}

.stat-card h3 {
  margin: 6px 0 0;
  font-size: clamp(24px, 1.95vw, 30px);
  line-height: 1.2;
  color: var(--text-main);
  font-family: 'DIN Alternate', 'Bahnschrift', 'Segoe UI', sans-serif;
}

.stat-track {
  margin-top: 10px;
  height: 7px;
  width: 100%;
  border-radius: 999px;
  background: var(--surface);
  overflow: hidden;
}

.stat-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--tone-color, var(--primary));
  min-width: 8px;
}

.center-grid {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr) 308px;
  gap: 10px;
}

.category-panel,
.list-panel,
.action-panel {
  padding: 12px;
  min-width: 0;
}

.action-panel {
  border: 1px solid var(--border-soft);
  background: linear-gradient(165deg, var(--surface) 0%, var(--surface-soft) 100%);
  box-shadow: inset 0 1px 0 rgba(209, 207, 197, 0.22), 0 10px 24px rgba(74, 45, 31, 0.1);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
}

.panel-head h3,
.panel-head h4 {
  margin: 0;
  color: var(--primary);
}

.panel-head span {
  color: var(--text-sub);
  font-size: 12px;
}

.category-item {
  width: 100%;
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  background: var(--surface-soft);
  min-height: 52px;
  display: grid;
  gap: 6px;
  padding: 9px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-item:hover {
  transform: translateY(-1px);
  border-color: var(--primary);
}

.category-item.active {
  border-color: var(--primary);
  background: rgba(201, 100, 66, 0.12);
}

.category-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.category-progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: var(--surface);
  overflow: hidden;
}

.category-progress i {
  display: block;
  height: 100%;
  border-radius: 999px;
  min-width: 8px;
}

.list-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 8px;
}

.list-head h3 {
  margin: 0;
  color: var(--primary);
}

.list-head p {
  margin: 3px 0 0;
  font-size: 11px;
  color: var(--text-sub);
}

.list-head-meta {
  min-width: 110px;
  text-align: right;
}

.list-head-meta span {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
}

.list-head-meta strong {
  display: block;
  margin-top: 1px;
  color: var(--text-main);
  font-size: clamp(24px, 1.8vw, 28px);
  line-height: 1;
  font-family: 'DIN Alternate', 'Bahnschrift', 'Segoe UI', sans-serif;
}

.filter-row {
  margin-bottom: 8px;
  padding: 8px 8px 0;
  border-radius: 12px;
  background: var(--surface-soft);
  border: 1px solid var(--border-soft);
}

.list-panel :deep(.el-form--inline .el-form-item) {
  margin-right: 10px;
  margin-bottom: 8px;
}

.list-panel :deep(.el-table) {
  border-radius: 10px;
}

.table-empty {
  padding: 18px 0 22px;
  text-align: center;
}

.table-empty p {
  margin: 0;
  color: var(--text-sub);
}

.empty-graphic {
  width: 80px;
  height: 56px;
  margin: 0 auto 10px;
  position: relative;
}

.empty-graphic span {
  position: absolute;
  border-radius: 8px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
}

.empty-graphic span:nth-child(1) {
  width: 58px;
  height: 40px;
  left: 0;
  bottom: 0;
  transform: rotate(-8deg);
}

.empty-graphic span:nth-child(2) {
  width: 58px;
  height: 40px;
  right: 0;
  bottom: 2px;
}

.empty-graphic span:nth-child(3) {
  width: 24px;
  height: 6px;
  left: 50%;
  transform: translateX(-50%);
  bottom: -8px;
}

.pager {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.action-shell {
  margin-top: 2px;
  padding: 10px;
  border: 1px solid var(--border-soft);
  border-radius: 14px;
  background: linear-gradient(180deg, var(--surface) 0%, var(--surface-soft) 100%);
}

.action-shell.is-empty {
  min-height: 188px;
  display: flex;
}

.action-empty-state {
  width: 100%;
  border: 1px dashed rgba(201, 100, 66, 0.3);
  border-radius: 12px;
  background: radial-gradient(circle at 50% 0%, rgba(201, 100, 66, 0.14) 0%, var(--surface) 55%);
  display: grid;
  place-items: center;
  text-align: center;
  padding: 18px 14px;
}

.empty-spotlight {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, rgba(201, 100, 66, 0.34), rgba(201, 100, 66, 0.08));
  box-shadow: 0 8px 18px rgba(201, 100, 66, 0.22);
}

.action-empty-state h4 {
  margin: 10px 0 0;
  font-size: 16px;
  color: var(--primary);
}

.action-empty-state p {
  margin: 8px 0 0;
  max-width: 420px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-sub);
}

.empty-hints {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.empty-hints span {
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  font-size: 11px;
  color: var(--text-accent);
}

.selected-hero {
  border: 1px solid rgba(201, 100, 66, 0.24);
  border-radius: 12px;
  padding: 10px;
  background: linear-gradient(135deg, rgba(201, 100, 66, 0.16) 0%, rgba(176, 94, 63, 0.06) 100%);
}

.selected-caption {
  margin: 0;
  font-size: 11px;
  color: var(--text-sub);
  letter-spacing: 0.4px;
}

.selected-badges {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.selected-title {
  margin: 6px 0 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--text-main);
}

.selected-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.selected-item {
  position: relative;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  padding: 8px;
  background: linear-gradient(156deg, var(--surface) 0%, var(--surface-soft) 100%);
}

.selected-item::before {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  top: 0;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(201, 100, 66, 0.75), rgba(201, 100, 66, 0));
}

.selected-item small {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
}

.selected-item strong {
  display: block;
  margin-top: 3px;
  color: var(--text-main);
}

.action-group {
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.action-main-btn {
  width: 100%;
}

.action-sub-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.action-sub-grid :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

:deep(.action-group .el-button) {
  height: 34px;
  border-radius: 10px;
  font-weight: 600;
}

.weak-tag-block {
  margin-top: 10px;
  border: 1px solid rgba(249, 115, 22, 0.2);
  border-radius: 14px;
  padding: 10px;
  background: linear-gradient(180deg, var(--surface-soft) 0%, var(--surface) 100%);
}

.weak-head {
  margin-bottom: 8px;
}

.weak-tag-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.weak-tag-list li {
  border: 1px solid rgba(249, 115, 22, 0.18);
  border-radius: 12px;
  padding: 8px 9px;
  background: linear-gradient(110deg, var(--surface) 0%, var(--surface-soft) 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.weak-tag-list li:hover {
  transform: translateY(-1px);
  border-color: rgba(249, 115, 22, 0.3);
  box-shadow: 0 8px 16px rgba(249, 115, 22, 0.1);
}

.weak-tag-list li:first-child {
  border-color: rgba(239, 68, 68, 0.3);
  background: linear-gradient(116deg, var(--surface) 0%, var(--surface-soft) 100%);
}

.weak-tag-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.weak-tag-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.weak-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.12);
  color: var(--danger);
  font-size: 11px;
  font-weight: 700;
}

.weak-name {
  font-weight: 600;
  color: var(--text-main);
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
}

.weak-tag-line strong {
  color: var(--danger);
  font-size: 20px;
  line-height: 1;
  font-family: 'DIN Alternate', 'Bahnschrift', 'Segoe UI', sans-serif;
}

.weak-tag-track {
  width: 100%;
  height: 7px;
  border-radius: 999px;
  background: rgba(209, 207, 197, 0.32);
  overflow: hidden;
}

.weak-tag-track i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--danger), #c27a46);
  min-width: 8px;
  box-shadow: 0 0 14px rgba(239, 68, 68, 0.32);
}

:deep(.row-overdue td.el-table__cell) {
  background: rgba(239, 68, 68, 0.06);
}

:deep(.row-selected td.el-table__cell) {
  background: rgba(201, 100, 66, 0.08) !important;
}

.overdue {
  color: var(--danger);
  font-weight: 600;
}

@media (max-width: 1680px) {
  .center-grid {
    grid-template-columns: 232px minmax(0, 1fr);
  }

  .action-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1180px) {
  h2 {
    font-size: 34px;
  }

  .insight-board {
    grid-template-columns: 1fr;
  }

  .board-bars {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .center-grid {
    grid-template-columns: 1fr;
  }

  .action-panel {
    grid-column: auto;
  }
}

@media (max-width: 760px) {
  .top-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .board-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .selected-grid {
    grid-template-columns: 1fr;
  }

  .action-sub-grid {
    grid-template-columns: 1fr;
  }

  .action-empty-state {
    padding: 14px 10px;
  }

  .list-head {
    flex-direction: column;
  }

  .list-head-meta {
    text-align: left;
  }
}
</style>
