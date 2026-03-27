<template>
  <section class="fade-in" v-loading="loading">
    <div class="top-row">
      <div class="page-title-back">
        <el-button class="back-chevron-btn" plain aria-label="返回列表" @click="goBackList">&lt;</el-button>
        <h2>{{ detail.title || '资料详情' }}</h2>
      </div>
      <div class="top-actions">
        <el-button type="warning" plain @click="onToggleFavorite">{{ detail.favorite ? '取消收藏' : '收藏' }}</el-button>
        <template v-if="canUseAiSummary">
          <el-select v-model="summaryModel" style="width: 170px">
            <el-option v-for="item in summaryModelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="summaryType" style="width: 132px">
            <el-option v-for="item in summaryTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" plain :loading="summaryLoading" @click="runAiSummary">AI 总结</el-button>
        </template>

        <el-dropdown trigger="click" @command="onNoteCommand">
          <el-button type="primary" plain>
            关联笔记
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="existing">选择已有笔记</el-dropdown-item>
              <el-dropdown-item command="new">新建笔记并关联</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button type="primary" @click="$router.push(`/study-material/update/${route.params.materialId}`)">编辑</el-button>
        <el-button type="danger" plain @click="onDelete">删除</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <article class="surface-card main-block">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="资料类型">{{ displayMaterialType }}</el-descriptions-item>
          <el-descriptions-item label="编程语言">{{ displayLanguage }}</el-descriptions-item>
          <el-descriptions-item label="新增时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <div class="chip-list">
              <span class="tag-chip" v-for="tag in detail.tagNames || []" :key="tag">{{ tag }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="资料来源">{{ displaySource || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3>资料内容 / 链接</h3>
        <template v-if="detail.content">
          <template v-if="canTryOpenLink">
            <div class="link-panel">
              <el-link class="link-url" type="primary" @click="tryOpenLink">{{ detail.content }}</el-link>
              <el-button size="small" type="primary" plain @click="tryOpenLink">尝试访问链接</el-button>
            </div>
          </template>
          <pre v-else class="code-block">{{ detail.content }}</pre>
        </template>
        <el-empty v-else description="暂无文字内容，可查看下方附件" :image-size="70" />

        <div v-if="canUseAiSummary && (summaryResult || summaryError || summaryLoading)" class="ai-summary-card">
          <div class="ai-summary-head">
            <h4>AI 总结卡片</h4>
            <el-button link type="primary" @click="summaryExpanded = !summaryExpanded">{{ summaryExpanded ? '收起' : '展开' }}</el-button>
          </div>

          <el-collapse-transition>
            <div v-show="summaryExpanded" class="ai-summary-body">
              <el-skeleton v-if="summaryLoading" animated :rows="5" />

              <div v-else-if="summaryError" class="ai-summary-error">
                <span>{{ summaryError }}</span>
                <el-button link type="primary" @click="retrySummary">重试</el-button>
              </div>

              <template v-else-if="summaryResult">
                <AiMarkdownContent :content="summaryResult.markdown" />
                <div v-if="summaryResult.mindMapImageData" class="mind-map-box">
                  <img :src="summaryResult.mindMapImageData" alt="思维导图" class="mind-map-image" />
                  <el-button size="small" type="primary" plain @click="downloadMindMap">下载导图</el-button>
                </div>
                <div class="ai-summary-actions">
                  <el-button plain @click="copySummary">一键复制</el-button>
                  <el-button type="primary" plain @click="saveSummaryAsNote">一键生成新笔记</el-button>
                </div>
              </template>
            </div>
          </el-collapse-transition>
        </div>

        <template v-if="allAttachments.length">
          <h3>资料附件</h3>

          <div v-if="imageAttachments.length" class="image-grid">
            <div v-for="(file, index) in imageAttachments" :key="file.path || index" class="image-card">
              <el-image
                class="attachment-image"
                :src="attachmentPreviewUrl(file)"
                :preview-src-list="imagePreviewUrls"
                :initial-index="index"
                fit="cover"
                preview-teleported
              />
              <p class="attachment-name">{{ file.fileName || `图片${index + 1}` }}</p>
            </div>
          </div>

          <div v-if="docAttachments.length" class="doc-list">
            <div v-for="(file, index) in docAttachments" :key="file.path || index" class="doc-item">
              <a
                class="doc-link"
                :href="attachmentDownloadUrl(file)"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ file.fileName || `附件${index + 1}` }}
              </a>
              <span class="doc-meta">{{ formatSize(file.size) }}</span>
            </div>
          </div>
        </template>
      </article>

      <aside class="surface-card side-block">
        <h3>关联错题</h3>
        <el-empty v-if="!(detail.relatedQuestions || []).length" description="暂无关联错题" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="question in detail.relatedQuestions"
            :key="question.id"
            :timestamp="statusLabel(question.masteryStatus)"
            placement="top"
          >
            <el-link type="primary" @click="$router.push(`/error-question/detail/${question.id}`)">
              {{ question.title }}
            </el-link>
            <div class="chip-list" style="margin-top: 6px;">
              <span class="tag-chip" v-for="tag in question.tagNames || []" :key="tag">{{ tag }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>

        <h3 style="margin-top: 14px;">关联笔记</h3>
        <el-empty v-if="!(detail.relatedNotes || []).length" description="暂无关联笔记" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="note in detail.relatedNotes"
            :key="note.id"
            :timestamp="toZhLanguageLabel(note.language)"
            placement="top"
          >
            <el-link type="primary" @click="$router.push(`/note/detail/${note.id}`)">
              {{ note.title }}
            </el-link>
            <div class="chip-list" style="margin-top: 6px;">
              <span class="tag-chip" v-for="tag in note.tagNames || []" :key="tag">{{ tag }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </aside>
    </div>

    <el-dialog v-model="noteDialogVisible" title="关联已有笔记" width="640px">
      <div class="dialog-toolbar">
        <el-input v-model="noteKeyword" placeholder="按标题/内容搜索笔记" clearable @keyup.enter="loadNoteOptions">
          <template #append>
            <el-button @click="loadNoteOptions">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-scrollbar max-height="320px" v-loading="noteLoading">
        <el-empty v-if="!noteOptions.length" description="暂无可选笔记" :image-size="60" />
        <el-checkbox-group v-else v-model="selectedNoteIds" class="note-check-group">
          <label v-for="item in noteOptions" :key="item.id" class="note-option">
            <el-checkbox :label="item.id">
              {{ item.title }}
              <span class="note-option-sub">（{{ item.languageZh }}）</span>
            </el-checkbox>
          </label>
        </el-checkbox-group>
      </el-scrollbar>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="noteDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="noteSubmitting" @click="confirmLinkNotes">确认关联</el-button>
        </span>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getAiModels, summarizeByAi } from '../api/ai'
import {
  deleteMaterial,
  favoriteMaterial,
  getMaterialDetail,
  unfavoriteMaterial
} from '../api/material'
import { addNote, getNoteList, linkMaterialNotes } from '../api/note'
import { toZhLanguageLabel, toZhMaterialSourceLabel, toZhMaterialTypeLabel } from '../utils/material'
import { AI_BUILTIN_MODEL, AI_PROVIDER_MODELS, getAiRuntimeConfig, setAiRuntimeConfig } from '../utils/aiRuntimeConfig'
import AiMarkdownContent from '../components/ai/AiMarkdownContent.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const fromFavorite = computed(() => route.query.from === 'favorite')

const detail = reactive({
  title: '',
  content: '',
  contentAttachments: [],
  tagNames: [],
  relatedQuestions: [],
  relatedNotes: [],
  favorite: false
})

const noteDialogVisible = ref(false)
const noteLoading = ref(false)
const noteSubmitting = ref(false)
const noteOptions = ref([])
const selectedNoteIds = ref([])
const noteKeyword = ref('')

const summaryType = ref('CORE_EXTRACT')
const summaryTypeOptions = [
  { value: 'CORE_EXTRACT', label: '核心提炼' },
  { value: 'MIND_MAP', label: '思维导图' },
  { value: 'INTERVIEW', label: '面试考点' },
  { value: 'FLASH_CARD', label: '速记卡片' }
]
const summaryLoading = ref(false)
const summaryError = ref('')
const summaryResult = ref(null)
const summaryExpanded = ref(false)
const summaryModel = ref(getAiRuntimeConfig().model || AI_BUILTIN_MODEL)
const summaryModelOptions = ref([{ value: 'SAFE_GPT_SIM', label: '安全增强模型' }])
const summaryModelLoaded = ref(false)
const lastSummaryPayload = ref(null)

function resolveAiErrorMessage(err) {
  const raw = err?.message || err?.response?.data?.message || ''
  const text = String(raw || '').trim()
  if (!text) {
    return 'AI 服务繁忙，请重试'
  }
  if (text.includes('timeout') || text.includes('超时')) {
    return 'AI 请求超时，请稍后重试'
  }
  return text
}

const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'])

const displayMaterialType = computed(() => toZhMaterialTypeLabel(detail.materialType) || '-')
const displayLanguage = computed(() => toZhLanguageLabel(detail.language) || '-')
const displaySource = computed(() => toZhMaterialSourceLabel(detail.source))
const canUseAiSummary = computed(() => ['KNOWLEDGE_NOTE', 'SOLUTION_TUTORIAL'].includes(detail.materialType))

const allAttachments = computed(() => (Array.isArray(detail.contentAttachments) ? detail.contentAttachments : []))
const imageAttachments = computed(() => allAttachments.value.filter((item) => isImageAttachment(item)))
const docAttachments = computed(() => allAttachments.value.filter((item) => !isImageAttachment(item)))
const imagePreviewUrls = computed(() => imageAttachments.value.map((item) => attachmentPreviewUrl(item)))

function goBackList() {
  if (fromFavorite.value) {
    router.push('/study-material/favorite')
    return
  }
  router.push('/study-material/list')
}

function statusLabel(value) {
  if (value === 'MASTERED') return '已掌握'
  if (value === 'REVIEWING') return '复习中'
  return '未复习'
}

function normalizeLink(content) {
  const raw = (content || '').trim()
  if (!raw) return null

  if (/^https?:\/\//i.test(raw)) {
    return raw
  }

  if (/^www\./i.test(raw)) {
    return `https://${raw}`
  }

  if (/^[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+(\/[\S]*)?$/i.test(raw)) {
    return `https://${raw}`
  }

  return null
}

const normalizedLink = computed(() => normalizeLink(detail.content))
const canTryOpenLink = computed(() => !!normalizedLink.value)

function tryOpenLink() {
  if (!normalizedLink.value) {
    ElMessage.warning('当前内容不是可访问链接')
    return
  }

  const win = window.open(normalizedLink.value, '_blank', 'noopener,noreferrer')
  if (!win) {
    ElMessage.warning('浏览器拦截了新窗口，请允许弹窗后重试')
  }
}

function fileExtension(fileName) {
  if (!fileName) return ''
  const index = fileName.lastIndexOf('.')
  if (index < 0 || index === fileName.length - 1) return ''
  return fileName.slice(index + 1).toLowerCase()
}

function isImageAttachment(file) {
  if (!file) return false
  if (file.image === true) return true
  if (typeof file.contentType === 'string' && file.contentType.toLowerCase().startsWith('image/')) {
    return true
  }
  const ext = fileExtension(file.fileName || file.path)
  return IMAGE_EXTENSIONS.has(ext)
}

function attachmentPreviewUrl(file) {
  if (file?.previewUrl) return file.previewUrl
  const path = encodeURIComponent(file?.path || '')
  const name = encodeURIComponent(file?.fileName || 'file')
  return `/api/v1/file/download?path=${path}&name=${name}`
}

function attachmentDownloadUrl(file) {
  if (file?.downloadUrl) return file.downloadUrl
  return `${attachmentPreviewUrl(file)}&download=true`
}

function formatSize(size) {
  const bytes = Number(size)
  if (!Number.isFinite(bytes) || bytes <= 0) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getMaterialDetail(route.params.materialId)
    Object.assign(detail, data)
    detail.favorite = !!data?.favorite
  } finally {
    loading.value = false
  }
}

async function loadNoteOptions() {
  noteLoading.value = true
  try {
    const data = await getNoteList({
      pageNo: 1,
      pageSize: 200,
      keyword: noteKeyword.value?.trim() || '',
      sortBy: 'updated_at',
      sortOrder: 'desc'
    })
    noteOptions.value = (data.records || []).map((item) => ({
      ...item,
      languageZh: toZhLanguageLabel(item.language)
    }))
  } finally {
    noteLoading.value = false
  }
}

function openNoteDialog() {
  selectedNoteIds.value = (detail.relatedNotes || []).map((item) => item.id)
  noteDialogVisible.value = true
  loadNoteOptions()
}

function goCreateNote() {
  router.push({
    path: '/note/add',
    query: {
      materialId: route.params.materialId,
      tagNames: (detail.tagNames || []).join(',')
    }
  })
}

function onNoteCommand(command) {
  if (command === 'existing') {
    openNoteDialog()
    return
  }
  if (command === 'new') {
    goCreateNote()
  }
}

async function confirmLinkNotes() {
  if (!selectedNoteIds.value.length) {
    ElMessage.warning('请至少选择一条笔记')
    return
  }

  noteSubmitting.value = true
  try {
    await linkMaterialNotes(route.params.materialId, selectedNoteIds.value)
    ElMessage.success('关联成功')
    noteDialogVisible.value = false
    await loadDetail()
  } finally {
    noteSubmitting.value = false
  }
}

async function ensureSummaryModel() {
  if (summaryModelLoaded.value) return
  try {
    const data = await getAiModels()
    const options = Array.isArray(data?.models)
      ? data.models
        .map((item) => ({
          value: item?.value ? String(item.value).trim() : '',
          label: item?.label ? String(item.label).trim() : ''
        }))
        .filter((item) => item.value && item.label)
      : []
    if (options.length) {
      summaryModelOptions.value = options
    }
    const runtimeModel = getAiRuntimeConfig().model
    const hasRuntimeModel = runtimeModel && summaryModelOptions.value.some((item) => item.value === runtimeModel)
    summaryModel.value = hasRuntimeModel
      ? runtimeModel
      : (data?.defaultModel || summaryModelOptions.value[0]?.value || summaryModel.value)
    summaryModelLoaded.value = true
  } catch (_) {
    // ignore model loading failure
  }
}

async function runAiSummary() {
  if (!canUseAiSummary.value) {
    ElMessage.warning('当前资料类型暂不支持 AI 总结')
    return
  }

  await ensureSummaryModel()
  const payload = {
    targetType: 'MATERIAL',
    targetId: Number(route.params.materialId),
    title: detail.title,
    content: detail.content,
    language: detail.language,
    tagNames: detail.tagNames || [],
    summaryType: summaryType.value,
    model: summaryModel.value
  }
  lastSummaryPayload.value = payload
  summaryLoading.value = true
  summaryError.value = ''
  try {
    summaryResult.value = await summarizeByAi(payload)
  } catch (err) {
    summaryError.value = resolveAiErrorMessage(err)
  } finally {
    summaryLoading.value = false
  }
}

async function retrySummary() {
  if (!lastSummaryPayload.value || summaryLoading.value) return
  summaryLoading.value = true
  summaryError.value = ''
  try {
    summaryResult.value = await summarizeByAi(lastSummaryPayload.value)
  } catch (err) {
    summaryError.value = resolveAiErrorMessage(err)
  } finally {
    summaryLoading.value = false
  }
}

async function copySummary() {
  const text = summaryResult.value?.summaryText || summaryResult.value?.markdown || ''
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('总结内容已复制')
  } catch (_) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function downloadMindMap() {
  const imageData = summaryResult.value?.mindMapImageData
  if (!imageData) return
  const link = document.createElement('a')
  link.href = imageData
  link.download = summaryResult.value?.mindMapFileName || 'mind_map.svg'
  link.click()
}

async function saveSummaryAsNote() {
  if (!summaryResult.value) {
    ElMessage.warning('请先生成 AI 总结')
    return
  }

  const noteId = await addNote({
    title: `${detail.title || '资料'} - AI总结`,
    content: summaryResult.value.markdown || summaryResult.value.summaryText,
    language: detail.language || 'Java',
    tagNames: (detail.tagNames || []).length ? detail.tagNames : ['AI辅助'],
    manualQuestionIds: (detail.relatedQuestions || []).map((item) => item.id),
    manualMaterialIds: [Number(route.params.materialId)]
  })
  ElMessage.success('AI 总结已生成新笔记')
  if (noteId) {
    router.push(`/note/detail/${noteId}`)
  }
}
async function onToggleFavorite() {
  if (detail.favorite) {
    await unfavoriteMaterial(route.params.materialId)
    detail.favorite = false
    ElMessage.success('已取消收藏')
    return
  }
  await favoriteMaterial(route.params.materialId)
  detail.favorite = true
  ElMessage.success('收藏成功')
}

async function onDelete() {
  await ElMessageBox.confirm('确认删除该资料？删除后不可恢复', '删除确认', { type: 'warning' })
  await deleteMaterial(route.params.materialId)
  ElMessage.success('删除成功')
  router.push('/study-material/list')
}

onMounted(() => {
  loadDetail()
  ensureSummaryModel()
})

watch(summaryModel, (value) => {
  const previous = getAiRuntimeConfig()
  const model = String(value || AI_BUILTIN_MODEL).toUpperCase()
  setAiRuntimeConfig({
    ...previous,
    model,
    provider: AI_PROVIDER_MODELS.includes(model) ? model : previous.provider,
    useCustomProvider: AI_PROVIDER_MODELS.includes(model) ? previous.useCustomProvider : false,
    baseUrl: previous.baseUrl,
    apiKey: previous.apiKey,
    modelName: previous.modelName
  }, { silent: true })
})
</script>

<style scoped>
.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.top-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.top-row h2 {
  margin: 0;
  color: var(--primary);
}

.detail-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
}

.main-block,
.side-block {
  padding: 16px;
}

.side-block {
  position: sticky;
  top: 14px;
  height: fit-content;
  max-height: calc(100vh - 40px);
  overflow: auto;
}

.main-block h3 {
  margin: 18px 0 8px;
  color: var(--primary);
}

.code-block {
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 8px;
  background: var(--surface-soft);
  border: 1px solid var(--border-soft);
  color: var(--text-main);
}

.link-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.link-url {
  max-width: 100%;
  word-break: break-all;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  margin-top: 8px;
}

.image-card {
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  padding: 8px;
  background: var(--surface);
}

.attachment-image {
  width: 100%;
  height: 120px;
  border-radius: 8px;
}

.attachment-name {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--text-sub);
  word-break: break-all;
}

.doc-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--border-soft);
  border-radius: 8px;
  background: var(--surface-soft);
}

.doc-link {
  color: var(--primary);
  text-decoration: none;
  word-break: break-all;
}

.doc-link:hover {
  text-decoration: underline;
}

.doc-meta {
  flex-shrink: 0;
  color: var(--text-sub);
  font-size: 12px;
}

.ai-summary-card {
  margin-top: 14px;
  border: 1px solid #dbe3ee;
  border-radius: 12px;
  background: #fbfdff;
}

.ai-summary-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.ai-summary-head h4 {
  margin: 0;
  color: #1f3b7a;
}

.ai-summary-body {
  padding: 12px;
}

.ai-summary-error {
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
}

.mind-map-box {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mind-map-image {
  width: 100%;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
}

.ai-summary-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dialog-toolbar {
  margin-bottom: 12px;
}

.note-check-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-option {
  border: 1px solid var(--border-soft);
  border-radius: 8px;
  padding: 8px 10px;
  background: var(--surface-soft);
}

.note-option-sub {
  color: var(--text-sub);
}

:global(:root[data-theme='dark']) .ai-summary-card {
  background: #0f172a;
  border-color: #334155;
}

:global(:root[data-theme='dark']) .ai-summary-head {
  border-bottom-color: #334155;
}

:global(:root[data-theme='dark']) .ai-summary-head h4 {
  color: #dbeafe;
}

:global(:root[data-theme='dark']) .mind-map-image {
  border-color: #334155;
  background: #111827;
}

@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .side-block {
    position: static;
    max-height: none;
  }

  .top-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .top-actions {
    flex-wrap: wrap;
  }
}

</style>

