<template>
  <section class="fade-in bookshelf-page">
    <div class="toolbar">
      <h2>错题列表</h2>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/error-question/add')">新增错题</el-button>
        <el-button type="warning" plain :disabled="!selectedIds.length" @click="onBatchJoinReview">批量加入复习</el-button>
        <el-button type="success" plain :disabled="!selectedIds.length" @click="onExportSelectedPdf">导出选中 PDF</el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="onBatchDelete">批量删除</el-button>
      </div>
    </div>

    <div class="surface-card block">
      <el-form inline class="bookshelf-filter-form">
        <el-form-item label="编程语言">
          <el-select v-model="query.language" clearable placeholder="全部" style="width: 160px" @change="loadList">
            <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点标签">
          <el-select v-model="query.tag" clearable filterable placeholder="全部" style="width: 180px" @change="loadList">
            <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="掌握状态">
          <el-select v-model="query.masteryStatus" clearable placeholder="全部" style="width: 160px" @change="loadList">
            <el-option v-for="item in masteryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="搜索标题/标签" clearable @keyup.enter="loadList">
            <template #append>
              <el-button @click="loadList">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <div class="bookshelf-grid-meta">
        <div class="bookshelf-select-all">
          <el-checkbox
            :model-value="isAllSelectedOnPage"
            :indeterminate="isPartiallySelected"
            @change="onToggleSelectAll"
          >
            全选本页
          </el-checkbox>
          <span>已选 {{ selectedIds.length }} 题</span>
        </div>
        <span>筛选后共 {{ total }} 道错题</span>
      </div>

      <div class="bookshelf-body" v-loading="loading">
        <div v-if="rows.length" class="bookshelf-grid">
          <article
            v-for="row in rows"
            :key="row.id"
            class="bookshelf-card"
            :class="{ 'is-selected': isSelected(row.id) }"
            :style="coverVars(row)"
            @click="goDetail(row.id)"
          >
            <div class="bookshelf-cover-bg"></div>

            <div class="bookshelf-select" @click.stop>
              <el-checkbox :model-value="isSelected(row.id)" @change="(checked) => onToggleSelect(row.id, checked)" />
            </div>

            <div class="bookshelf-flag">
              <el-tag size="small" effect="dark" :type="reviewStatusTagType(row.masteryStatus)">
                {{ reviewStatusLabel(row.masteryStatus) }}
              </el-tag>
            </div>

            <div class="bookshelf-content">
              <span class="bookshelf-cover-name">{{ coverSeed(row) }}</span>
              <el-tooltip :content="row.title" placement="top">
                <p class="bookshelf-title-link">{{ row.title }}</p>
              </el-tooltip>

              <div class="bookshelf-bottom-meta">
                <span class="bookshelf-language">{{ row.language || '-' }}</span>
                <span class="bookshelf-time">{{ formatBookshelfTime(row.createdAt) }}</span>
              </div>
            </div>

            <div class="bookshelf-hover-actions" @click.stop>
              <el-button
                class="bookshelf-action"
                text
                type="warning"
                :disabled="isInReviewPlan(row)"
                @click="onJoinReview(row)"
              >
                <el-icon><AlarmClock /></el-icon>
                {{ isInReviewPlan(row) ? '已加入复习' : '加入复习' }}
              </el-button>
              <el-button class="bookshelf-action" text type="primary" @click="$router.push(`/error-question/update/${row.id}`)">
                <el-icon><EditPen /></el-icon>
                编辑
              </el-button>
              <el-button class="bookshelf-action" text type="danger" @click="onDelete(row.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </article>
        </div>

        <div v-else class="bookshelf-empty-wrap">
          <div class="bookshelf-empty">
            <div class="bookshelf-empty-illustration"></div>
            <p class="bookshelf-empty-text">你的错题书架还空着哦～快去新增错题吧</p>
            <el-button type="primary" @click="$router.push('/error-question/add')">新增错题</el-button>
          </div>
        </div>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :current-page="query.pageNo"
          :page-size="query.pageSize"
          @current-change="onCurrentChange"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AlarmClock, Delete, EditPen } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import defaultCoverImage from '../assets/default-bookshelf-cover.svg'
import {
  getQuestionList,
  deleteQuestion,
  batchDeleteQuestion,
  downloadQuestionBatchPdf
} from '../api/question'
import { batchUpsertReviewPlan, upsertReviewPlan } from '../api/review'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { reviewStatusLabel, reviewStatusTagType } from '../utils/review'
import {
  buildCoverPreviewUrl,
  formatBookshelfTime,
  getBookshelfSeed,
  resolveBookshelfTheme
} from '../utils/bookshelf'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const selectedIds = ref([])
const languageOptions = ref([])
const masteryOptions = ref([])
const tagOptions = ref([])

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  language: '',
  tag: '',
  masteryStatus: String(route.query.masteryStatus || ''),
  keyword: ''
})

const selectedIdSet = computed(() => new Set(selectedIds.value))

const isAllSelectedOnPage = computed(() => {
  return rows.value.length > 0 && rows.value.every((item) => selectedIdSet.value.has(item.id))
})

const isPartiallySelected = computed(() => {
  return selectedIds.value.length > 0 && !isAllSelectedOnPage.value
})

function syncSelectionWithRows() {
  const rowIds = new Set(rows.value.map((item) => item.id))
  selectedIds.value = selectedIds.value.filter((id) => rowIds.has(id))
}

function isSelected(id) {
  return selectedIdSet.value.has(id)
}

function onToggleSelect(id, checked) {
  if (checked) {
    if (!selectedIdSet.value.has(id)) {
      selectedIds.value = [...selectedIds.value, id]
    }
    return
  }
  selectedIds.value = selectedIds.value.filter((item) => item !== id)
}

function onToggleSelectAll(checked) {
  if (checked) {
    selectedIds.value = rows.value.map((item) => item.id)
    return
  }
  selectedIds.value = []
}

function coverSeed(row) {
  return getBookshelfSeed(row, reviewStatusLabel(row.masteryStatus) || '错题')
}

function coverVars(row) {
  const theme = resolveBookshelfTheme(coverSeed(row))
  const coverImage = row.coverPath ? buildCoverPreviewUrl(row.coverPath) : defaultCoverImage
  return {
    '--book-main': theme.main,
    '--book-soft': theme.soft,
    '--cover-gradient': theme.gradient,
    '--cover-image': `url("${coverImage}")`
  }
}

function isInReviewPlan(row) {
  return !!row?.inReviewPlan
}

function goDetail(questionId) {
  router.push(`/error-question/detail/${questionId}`)
}

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = data.languages || []
  masteryOptions.value = (data.masteryStatuses || []).map((item) => ({
    ...item,
    label: reviewStatusLabel(item.value)
  }))
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadList() {
  loading.value = true
  try {
    const data = await getQuestionList(query)
    rows.value = data.records || []
    total.value = data.total || 0
    syncSelectionWithRows()
  } finally {
    loading.value = false
  }
}

function onCurrentChange(page) {
  query.pageNo = page
  loadList()
}

async function onJoinReview(row) {
  await upsertReviewPlan({
    contentType: 'QUESTION',
    contentId: row.id,
    planMode: 'AUTO'
  })
  ElMessage.success('已加入复习计划')
  await loadList()
}

async function onBatchJoinReview() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选错题')
    return
  }
  await batchUpsertReviewPlan({
    contentType: 'QUESTION',
    contentIds: selectedIds.value,
    planMode: 'AUTO'
  })
  ElMessage.success('批量加入复习成功')
  await loadList()
}

async function onExportSelectedPdf() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选错题')
    return
  }

  const { blob, fileName } = await downloadQuestionBatchPdf(selectedIds.value)
  downloadBlob(blob, fileName)
  ElMessage.success('导出成功')
}

function downloadBlob(blob, fileName) {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || `error_questions_${Date.now()}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

async function onDelete(id) {
  await ElMessageBox.confirm('确认删除该错题？删除后不可恢复', '删除确认', { type: 'warning' })
  await deleteQuestion(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter((item) => item !== id)
  await loadList()
}

async function onBatchDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条错题？`, '批量删除确认', { type: 'warning' })
  await batchDeleteQuestion(selectedIds.value)
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  await loadList()
}

loadOptions()
loadTags()
loadList()
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

h2 {
  margin: 0;
  color: var(--primary);
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.block {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.bookshelf-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
