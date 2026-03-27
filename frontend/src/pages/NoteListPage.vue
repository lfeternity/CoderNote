<template>
  <section class="fade-in bookshelf-page">
    <div class="toolbar">
      <div class="toolbar-left">
        <h2>笔记列表</h2>
        <el-input
          v-model="query.keyword"
          class="toolbar-search"
          placeholder="搜索标题/内容/标签"
          clearable
          @keyup.enter="loadList"
        >
          <template #append>
            <el-button @click="loadList">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/note/add')">新建笔记</el-button>
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

        <el-form-item label="收藏状态">
          <el-select v-model="query.favoriteStatus" clearable placeholder="全部" style="width: 150px" @change="loadList">
            <el-option label="已收藏" value="FAVORITE" />
            <el-option label="未收藏" value="UNFAVORITE" />
          </el-select>
        </el-form-item>

        <el-form-item label="排序字段">
          <el-select v-model="query.sortBy" style="width: 160px" @change="loadList">
            <el-option label="更新时间" value="updated_at" />
            <el-option label="创建时间" value="created_at" />
          </el-select>
        </el-form-item>

        <el-form-item label="排序方式">
          <el-select v-model="query.sortOrder" style="width: 120px" @change="loadList">
            <el-option label="降序" value="desc" />
            <el-option label="升序" value="asc" />
          </el-select>
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
          <span>已选 {{ selectedIds.length }} 篇</span>
        </div>
        <span>筛选后共 {{ total }} 篇笔记</span>
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
              <el-tooltip :content="row.favorite ? '取消收藏' : '收藏'" placement="top">
                <button class="bookshelf-favorite-btn" type="button" @click.stop="onToggleFavorite(row)">
                  <el-icon>
                    <StarFilled v-if="row.favorite" />
                    <Star v-else />
                  </el-icon>
                </button>
              </el-tooltip>
            </div>

            <div class="bookshelf-content">
              <span class="bookshelf-cover-name">{{ coverSeed(row) }}</span>
              <el-tooltip :content="row.title" placement="top">
                <p class="bookshelf-title-link">{{ row.title }}</p>
              </el-tooltip>

              <div class="bookshelf-bottom-meta">
                <span class="bookshelf-language">{{ row.languageZh || row.language || '-' }}</span>
                <span class="bookshelf-time">{{ row.updatedAtText }}</span>
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
              <el-button class="bookshelf-action" text type="primary" @click="$router.push(`/note/update/${row.id}`)">
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
            <p class="bookshelf-empty-text">你的笔记书架还空着哦～快去新建笔记吧</p>
            <el-button type="primary" @click="$router.push('/note/add')">新建笔记</el-button>
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
import { useRouter } from 'vue-router'
import { AlarmClock, Delete, EditPen, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import defaultCoverImage from '../assets/default-bookshelf-cover.svg'
import {
  batchDeleteNote,
  deleteNote,
  downloadNoteBatchPdf,
  favoriteNote,
  getNoteList,
  unfavoriteNote
} from '../api/note'
import { batchUpsertReviewPlan, upsertReviewPlan } from '../api/review'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { toZhLanguageLabel, toZhLanguageOptions } from '../utils/material'
import {
  buildCoverPreviewUrl,
  formatBookshelfTime,
  getBookshelfSeed,
  resolveBookshelfTheme
} from '../utils/bookshelf'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const selectedIds = ref([])
const languageOptions = ref([])
const tagOptions = ref([])

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  language: '',
  tag: '',
  favoriteStatus: '',
  keyword: '',
  sortBy: 'updated_at',
  sortOrder: 'desc'
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
  return getBookshelfSeed(row, '笔记')
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

function goDetail(noteId) {
  router.push(`/note/detail/${noteId}`)
}

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = toZhLanguageOptions(data.languages)
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadList() {
  loading.value = true
  try {
    const data = await getNoteList(query)
    rows.value = (data.records || []).map((item) => ({
      ...item,
      favorite: !!item.favorite,
      languageZh: toZhLanguageLabel(item.language),
      updatedAtText: formatBookshelfTime(item.updatedAt || item.createdAt)
    }))
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

async function onDelete(noteId) {
  await ElMessageBox.confirm('确认删除该笔记？删除后不可恢复', '删除确认', { type: 'warning' })
  await deleteNote(noteId)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter((item) => item !== noteId)
  await loadList()
}

async function onExportSelectedPdf() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选笔记')
    return
  }

  const { blob, fileName } = await downloadNoteBatchPdf(selectedIds.value)
  downloadBlob(blob, fileName)
  ElMessage.success('导出成功')
}

function downloadBlob(blob, fileName) {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || `notes_${Date.now()}.pdf`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

async function onBatchDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条笔记？`, '批量删除确认', { type: 'warning' })
  await batchDeleteNote(selectedIds.value)
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  await loadList()
}

async function onJoinReview(row) {
  await upsertReviewPlan({
    contentType: 'NOTE',
    contentId: row.id,
    planMode: 'AUTO'
  })
  ElMessage.success('已加入复习计划')
  await loadList()
}

async function onBatchJoinReview() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选笔记')
    return
  }
  await batchUpsertReviewPlan({
    contentType: 'NOTE',
    contentIds: selectedIds.value,
    planMode: 'AUTO'
  })
  ElMessage.success('批量加入复习成功')
  await loadList()
}

async function onToggleFavorite(row) {
  if (row.favorite) {
    await unfavoriteNote(row.id)
    row.favorite = false
    ElMessage.success('已取消收藏')
    return
  }
  await favoriteNote(row.id)
  row.favorite = true
  ElMessage.success('收藏成功')
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
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-left {
  display: inline-flex;
  align-items: center;
  gap: 0;
  min-width: 0;
}

h2 {
  margin: 0;
  color: var(--primary);
}

.toolbar-search {
  width: 320px;
  max-width: 36vw;
  margin-left: 24px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.bookshelf-filter-form {
  flex-wrap: wrap;
  overflow-x: visible;
  overflow-y: visible;
  row-gap: 8px;
  padding-bottom: 0;
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

@media (max-width: 960px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .toolbar-left {
    width: 100%;
    flex-wrap: wrap;
    gap: 10px;
  }

  .toolbar-search {
    width: 100%;
    max-width: none;
    margin-left: 0;
  }
}
</style>
