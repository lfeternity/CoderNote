<template>
  <section class="fade-in">
    <div class="toolbar">
      <h2>搜索结果</h2>
      <div class="search-inline">
        <el-input v-model="keyword" placeholder="重新输入关键词" clearable @keyup.enter="doSearch" style="width: 320px" />
        <el-select v-model="resultType" style="width: 120px" @change="loadData">
          <el-option label="全部" value="all" />
          <el-option label="错题" value="question" />
          <el-option label="资料" value="material" />
          <el-option label="笔记" value="note" />
        </el-select>
        <el-button type="primary" @click="doSearch">搜索</el-button>
      </div>
    </div>

    <div class="surface-card block" v-loading="loading">
      <p class="hint">当前关键词：<strong>{{ keyword || '-' }}</strong></p>

      <el-empty v-if="isEmpty" description="未找到相关结果，请更换关键词" />

      <template v-else>
        <template v-if="showQuestions">
          <h3>错题结果</h3>
          <el-table :data="questionRows" style="width: 100%; margin-bottom: 16px;">
            <el-table-column prop="title" label="标题" min-width="220">
              <template #default="{ row }">
                <el-link :underline="false" type="primary" @click="$router.push(`/error-question/detail/${row.id}`)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="language" label="语言" width="120" />
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }">
                <div class="chip-list">
                  <span class="tag-chip" v-for="tag in row.tagNames || []" :key="tag">{{ tag }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <template v-if="showMaterials">
          <h3>资料结果</h3>
          <el-table :data="materialRows" style="width: 100%; margin-bottom: 16px;">
            <el-table-column prop="title" label="标题" min-width="220">
              <template #default="{ row }">
                <el-link :underline="false" type="primary" @click="$router.push(`/study-material/detail/${row.id}`)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="materialTypeZh" label="类型" width="130" />
            <el-table-column prop="languageZh" label="语言" width="120" />
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }">
                <div class="chip-list">
                  <span class="tag-chip" v-for="tag in row.tagNames || []" :key="tag">{{ tag }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <template v-if="showNotes">
          <h3>笔记结果</h3>
          <el-table :data="noteRows" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="220">
              <template #default="{ row }">
                <el-link :underline="false" type="primary" @click="$router.push(`/note/detail/${row.id}`)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="languageZh" label="语言" width="120" />
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }">
                <div class="chip-list">
                  <span class="tag-chip" v-for="tag in row.tagNames || []" :key="tag">{{ tag }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div class="pager">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="currentTotal"
            :current-page="pageNo"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            @current-change="onCurrentChange"
            @size-change="onSizeChange"
          />
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchResult } from '../api/search'
import { toZhLanguageLabel, toZhMaterialTypeLabel } from '../utils/material'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const keyword = ref(route.query.keyword || '')
const resultType = ref('all')
const pageNo = ref(1)
const pageSize = ref(10)

const questionRows = ref([])
const materialRows = ref([])
const noteRows = ref([])

const questionTotal = ref(0)
const materialTotal = ref(0)
const noteTotal = ref(0)

const showQuestions = computed(() => resultType.value === 'all' || resultType.value === 'question')
const showMaterials = computed(() => resultType.value === 'all' || resultType.value === 'material')
const showNotes = computed(() => resultType.value === 'all' || resultType.value === 'note')

const isEmpty = computed(() => {
  const emptyQuestion = !showQuestions.value || !questionRows.value.length
  const emptyMaterial = !showMaterials.value || !materialRows.value.length
  const emptyNote = !showNotes.value || !noteRows.value.length
  return emptyQuestion && emptyMaterial && emptyNote
})

const currentTotal = computed(() => {
  if (resultType.value === 'question') return questionTotal.value
  if (resultType.value === 'material') return materialTotal.value
  if (resultType.value === 'note') return noteTotal.value
  return Math.max(questionTotal.value, materialTotal.value, noteTotal.value)
})

async function loadData() {
  if (!keyword.value?.trim()) {
    questionRows.value = []
    materialRows.value = []
    noteRows.value = []
    questionTotal.value = 0
    materialTotal.value = 0
    noteTotal.value = 0
    return
  }

  loading.value = true
  try {
    const data = await searchResult({ keyword: keyword.value.trim(), pageNo: pageNo.value, pageSize: pageSize.value })
    questionRows.value = data.questionPage?.records || []
    materialRows.value = (data.materialPage?.records || []).map((item) => ({
      ...item,
      materialTypeZh: toZhMaterialTypeLabel(item.materialType),
      languageZh: toZhLanguageLabel(item.language)
    }))
    noteRows.value = (data.notePage?.records || []).map((item) => ({
      ...item,
      languageZh: toZhLanguageLabel(item.language)
    }))

    questionTotal.value = data.questionPage?.total || 0
    materialTotal.value = data.materialPage?.total || 0
    noteTotal.value = data.notePage?.total || 0
  } finally {
    loading.value = false
  }
}

function doSearch() {
  pageNo.value = 1
  router.push({ path: '/search/result', query: { keyword: keyword.value.trim() } })
}

function onCurrentChange(value) {
  pageNo.value = value
  loadData()
}

function onSizeChange(value) {
  pageSize.value = value
  pageNo.value = 1
  loadData()
}

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = value || ''
    pageNo.value = 1
    loadData()
  },
  { immediate: true }
)
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

h3 {
  color: var(--primary);
}

.search-inline {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.block {
  padding: 14px;
}

.hint {
  color: var(--text-sub);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
