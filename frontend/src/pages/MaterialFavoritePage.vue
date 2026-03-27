<template>
  <section class="fade-in bookshelf-page">
    <div class="toolbar">
      <div class="page-title-back">
        <el-button
          class="back-chevron-btn"
          plain
          :aria-label="activeTab === 'material' ? '返回资料列表' : '返回笔记列表'"
          @click="$router.push(activeTab === 'material' ? '/study-material/list' : '/note/list')"
        >&lt;</el-button>
        <h2>我的收藏</h2>
      </div>

      <el-input
        v-if="activeTab === 'note'"
        v-model="noteQuery.keyword"
        class="toolbar-search toolbar-search-note"
        placeholder="搜索标题/内容/标签"
        clearable
        @keyup.enter="loadNoteList"
      >
        <template #append>
          <el-button @click="loadNoteList">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div class="surface-card block">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="资料收藏" name="material">
          <div class="favorite-tab-panel">
            <el-form inline class="bookshelf-filter-form">
              <el-form-item label="资料类型">
                <el-select v-model="materialQuery.materialType" clearable placeholder="全部" style="width: 170px" @change="loadMaterialList">
                  <el-option v-for="item in materialTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="编程语言">
                <el-select v-model="materialQuery.language" clearable placeholder="全部" style="width: 160px" @change="loadMaterialList">
                  <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="知识点标签">
                <el-select v-model="materialQuery.tag" clearable filterable placeholder="全部" style="width: 180px" @change="loadMaterialList">
                  <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-input v-model="materialQuery.keyword" placeholder="搜索标题/标签" clearable @keyup.enter="loadMaterialList">
                  <template #append>
                    <el-button @click="loadMaterialList">搜索</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>

            <div class="bookshelf-grid-meta">
              <span>筛选后共 {{ materialTotal }} 份收藏资料</span>
            </div>

            <div class="bookshelf-body" v-loading="materialLoading">
              <div v-if="materialRows.length" class="bookshelf-grid favorite-material-grid">
                <article
                  v-for="row in materialRows"
                  :key="row.id"
                  class="bookshelf-card"
                  :style="materialCoverVars(row)"
                  @click="goMaterialDetail(row.id)"
                >
                  <div class="bookshelf-cover-bg"></div>

                  <div class="bookshelf-content">
                    <el-tooltip :content="row.title" placement="top">
                      <p class="bookshelf-title-link">{{ row.title }}</p>
                    </el-tooltip>

                    <div class="bookshelf-bottom-meta">
                      <span class="bookshelf-language">{{ row.languageZh || row.language || '-' }}</span>
                      <span class="bookshelf-time">{{ row.createdAtText }}</span>
                    </div>
                  </div>

                  <div class="bookshelf-hover-actions favorite-hover-actions" @click.stop>
                    <el-button class="bookshelf-action" text type="warning" @click="onUnfavoriteMaterial(row.id)">取消收藏</el-button>
                  </div>
                </article>
              </div>

              <div v-else class="bookshelf-empty-wrap">
                <div class="bookshelf-empty">
                  <div class="bookshelf-empty-illustration"></div>
                  <p class="bookshelf-empty-text">暂无收藏资料，去资料页添加收藏吧</p>
                </div>
              </div>
            </div>

            <div class="pager">
              <el-pagination
                background
                layout="total, prev, pager, next"
                :total="materialTotal"
                :current-page="materialQuery.pageNo"
                :page-size="materialQuery.pageSize"
                @current-change="onMaterialCurrentChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="笔记收藏" name="note">
          <div class="favorite-tab-panel">
            <el-form inline class="bookshelf-filter-form favorite-note-filter-form">
              <el-form-item label="编程语言">
                <el-select v-model="noteQuery.language" clearable placeholder="全部" style="width: 160px" @change="loadNoteList">
                  <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="知识点标签">
                <el-select v-model="noteQuery.tag" clearable filterable placeholder="全部" style="width: 180px" @change="loadNoteList">
                  <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>

              <el-form-item label="排序字段">
                <el-select v-model="noteQuery.sortBy" style="width: 160px" @change="loadNoteList">
                  <el-option label="更新时间" value="updated_at" />
                  <el-option label="创建时间" value="created_at" />
                </el-select>
              </el-form-item>

              <el-form-item label="排序方式">
                <el-select v-model="noteQuery.sortOrder" style="width: 120px" @change="loadNoteList">
                  <el-option label="降序" value="desc" />
                  <el-option label="升序" value="asc" />
                </el-select>
              </el-form-item>
            </el-form>

            <div class="bookshelf-grid-meta">
              <span>筛选后共 {{ noteTotal }} 篇收藏笔记</span>
            </div>

            <div class="bookshelf-body" v-loading="noteLoading">
              <div v-if="noteRows.length" class="bookshelf-grid favorite-note-grid">
                <article
                  v-for="row in noteRows"
                  :key="row.id"
                  class="bookshelf-card"
                  :style="noteCoverVars(row)"
                  @click="goNoteDetail(row.id)"
                >
                  <div class="bookshelf-cover-bg"></div>

                  <div class="bookshelf-content">
                    <el-tooltip :content="row.title" placement="top">
                      <p class="bookshelf-title-link">{{ row.title }}</p>
                    </el-tooltip>

                    <div class="bookshelf-bottom-meta">
                      <span class="bookshelf-language">{{ row.languageZh || row.language || '-' }}</span>
                      <span class="bookshelf-time">{{ row.updatedAtText }}</span>
                    </div>
                  </div>

                  <div class="bookshelf-hover-actions favorite-hover-actions" @click.stop>
                    <el-button class="bookshelf-action" text type="warning" @click="onUnfavoriteNote(row.id)">取消收藏</el-button>
                  </div>
                </article>
              </div>

              <div v-else class="bookshelf-empty-wrap">
                <div class="bookshelf-empty">
                  <div class="bookshelf-empty-illustration"></div>
                  <p class="bookshelf-empty-text">暂无收藏笔记，去笔记页添加收藏吧</p>
                </div>
              </div>
            </div>

            <div class="pager">
              <el-pagination
                background
                layout="total, prev, pager, next"
                :total="noteTotal"
                :current-page="noteQuery.pageNo"
                :page-size="noteQuery.pageSize"
                @current-change="onNoteCurrentChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import defaultCoverImage from '../assets/default-bookshelf-cover.svg'
import { getFavoriteMaterialList, unfavoriteMaterial } from '../api/material'
import { getNoteList, unfavoriteNote } from '../api/note'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import {
  toZhLanguageLabel,
  toZhLanguageOptions,
  toZhMaterialTypeOptions
} from '../utils/material'
import {
  buildCoverPreviewUrl,
  formatBookshelfTime,
  getBookshelfSeed,
  resolveBookshelfTheme
} from '../utils/bookshelf'

const router = useRouter()
const activeTab = ref('material')

const languageOptions = ref([])
const materialTypeOptions = ref([])
const tagOptions = ref([])

const materialLoading = ref(false)
const materialRows = ref([])
const materialTotal = ref(0)

const noteLoading = ref(false)
const noteRows = ref([])
const noteTotal = ref(0)

const materialQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  materialType: '',
  language: '',
  tag: '',
  keyword: ''
})

const noteQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  language: '',
  tag: '',
  keyword: '',
  favoriteStatus: 'FAVORITE',
  sortBy: 'updated_at',
  sortOrder: 'desc'
})

function buildCoverVars(row, seedText) {
  const seed = getBookshelfSeed(row, seedText)
  const theme = resolveBookshelfTheme(seed)
  const coverImage = row.coverPath ? buildCoverPreviewUrl(row.coverPath) : defaultCoverImage
  return {
    '--book-main': theme.main,
    '--book-soft': theme.soft,
    '--cover-gradient': theme.gradient,
    '--cover-image': `url("${coverImage}")`
  }
}

function materialCoverVars(row) {
  return buildCoverVars(row, '资料')
}

function noteCoverVars(row) {
  return buildCoverVars(row, '笔记')
}

function goMaterialDetail(materialId) {
  router.push({ path: `/study-material/detail/${materialId}`, query: { from: 'favorite' } })
}

function goNoteDetail(noteId) {
  router.push(`/note/detail/${noteId}`)
}

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = toZhLanguageOptions(data.languages)
  materialTypeOptions.value = toZhMaterialTypeOptions(data.materialTypes)
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadMaterialList() {
  materialLoading.value = true
  try {
    const data = await getFavoriteMaterialList(materialQuery)
    materialRows.value = (data.records || []).map((item) => ({
      ...item,
      languageZh: toZhLanguageLabel(item.language),
      createdAtText: formatBookshelfTime(item.createdAt)
    }))
    materialTotal.value = data.total || 0
  } finally {
    materialLoading.value = false
  }
}

async function loadNoteList() {
  noteLoading.value = true
  try {
    const data = await getNoteList(noteQuery)
    noteRows.value = (data.records || []).map((item) => ({
      ...item,
      languageZh: toZhLanguageLabel(item.language),
      updatedAtText: formatBookshelfTime(item.updatedAt || item.createdAt)
    }))
    noteTotal.value = data.total || 0
  } finally {
    noteLoading.value = false
  }
}

function onTabChange(name) {
  if (name === 'material') {
    loadMaterialList()
    return
  }
  loadNoteList()
}

function onMaterialCurrentChange(page) {
  materialQuery.pageNo = page
  loadMaterialList()
}

function onNoteCurrentChange(page) {
  noteQuery.pageNo = page
  loadNoteList()
}

async function onUnfavoriteMaterial(materialId) {
  await unfavoriteMaterial(materialId)
  ElMessage.success('已取消收藏')
  await loadMaterialList()
}

async function onUnfavoriteNote(noteId) {
  await unfavoriteNote(noteId)
  ElMessage.success('已取消收藏')
  await loadNoteList()
}

loadOptions()
loadTags()
loadMaterialList()
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.toolbar h2 {
  margin: 0;
  color: var(--primary);
}

.toolbar-search-note {
  width: 320px;
  margin-left: 24px;
}

.bookshelf-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.block {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.favorite-tab-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.favorite-note-filter-form {
  flex-wrap: wrap;
  overflow-x: visible;
  overflow-y: visible;
  row-gap: 8px;
  padding-bottom: 0;
}

.favorite-material-grid {
  padding-left: 38px;
}

.favorite-hover-actions {
  justify-content: flex-end;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 10px;
  }

  .toolbar-search-note {
    width: 100%;
    margin-left: 0;
  }

  .favorite-material-grid {
    padding-left: 6px;
  }
}
</style>
