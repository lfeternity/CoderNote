<template>
  <section class="fade-in bookshelf-page">
    <div class="toolbar">
      <h2>资料列表</h2>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/study-material/add')">新增资料</el-button>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="onBatchDelete">批量删除</el-button>
      </div>
    </div>

    <div class="surface-card block">
      <el-form inline class="bookshelf-filter-form">
        <el-form-item label="资料类型">
          <el-select v-model="query.materialType" clearable placeholder="全部" style="width: 170px" @change="loadList">
            <el-option v-for="item in materialTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
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
          <span>已选 {{ selectedIds.length }} 条</span>
        </div>
        <span>筛选后共 {{ total }} 条资料</span>
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
                <span class="bookshelf-time">{{ formatBookshelfTime(row.createdAt) }}</span>
              </div>
            </div>

            <div class="bookshelf-hover-actions" @click.stop>
              <el-button class="bookshelf-action" text type="warning" @click="onToggleFavorite(row)">
                <el-icon><Star /></el-icon>
                {{ row.favorite ? '取消收藏' : '收藏' }}
              </el-button>
              <el-button class="bookshelf-action" text type="primary" @click="$router.push(`/study-material/update/${row.id}`)">
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
            <p class="bookshelf-empty-text">你的书架还空着哦～快去添加学习资料吧</p>
            <el-button type="primary" @click="$router.push('/study-material/add')">新增资料</el-button>
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
import { Delete, EditPen, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import defaultCoverImage from '../assets/default-bookshelf-cover.svg'
import {
  getMaterialList,
  deleteMaterial,
  batchDeleteMaterial,
  favoriteMaterial,
  unfavoriteMaterial
} from '../api/material'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import {
  toZhLanguageLabel,
  toZhLanguageOptions,
  toZhMaterialTypeLabel,
  toZhMaterialTypeOptions
} from '../utils/material'
import {
  buildCoverPreviewUrl,
  formatBookshelfTime,
  getBookshelfSeed,
  resolveBookshelfTheme
} from '../utils/bookshelf'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const selectedIds = ref([])
const languageOptions = ref([])
const materialTypeOptions = ref([])
const tagOptions = ref([])
const router = useRouter()

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  materialType: '',
  language: '',
  tag: '',
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
  return getBookshelfSeed(row, '资料')
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

function goDetail(materialId) {
  router.push(`/study-material/detail/${materialId}`)
}

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = toZhLanguageOptions(data.languages)
  materialTypeOptions.value = toZhMaterialTypeOptions(data.materialTypes)
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadList() {
  loading.value = true
  try {
    const data = await getMaterialList(query)
    rows.value = (data.records || []).map((item) => ({
      ...item,
      favorite: !!item.favorite,
      materialTypeZh: toZhMaterialTypeLabel(item.materialType),
      languageZh: toZhLanguageLabel(item.language)
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

async function onDelete(id) {
  await ElMessageBox.confirm('确认删除该资料？删除后不可恢复', '删除确认', { type: 'warning' })
  await deleteMaterial(id)
  ElMessage.success('删除成功')
  selectedIds.value = selectedIds.value.filter((item) => item !== id)
  await loadList()
}

async function onBatchDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条资料？`, '批量删除确认', { type: 'warning' })
  await batchDeleteMaterial(selectedIds.value)
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  await loadList()
}

async function onToggleFavorite(row) {
  if (row.favorite) {
    await unfavoriteMaterial(row.id)
    row.favorite = false
    ElMessage.success('已取消收藏')
    return
  }
  await favoriteMaterial(row.id)
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
