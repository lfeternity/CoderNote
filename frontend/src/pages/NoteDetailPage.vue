<template>
  <section class="fade-in" v-loading="loading">
    <div class="top-row">
      <div class="page-title-back">
        <el-button
          class="back-chevron-btn"
          plain
          aria-label="返回列表"
          @click="$router.push('/note/list')"
        >&lt;</el-button>
        <h2>{{ detail.title || '笔记详情' }}</h2>
      </div>
      <div class="actions-wrap">
        <div class="actions">
          <el-select v-model="summaryType" style="width: 132px">
            <el-option v-for="item in summaryTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" plain :loading="summaryLoading" @click="runAiSummary">AI 总结</el-button>
          <el-button type="warning" plain :disabled="inReviewPlan" @click="quickJoinReview">
            {{ inReviewPlan ? '已加入复习' : '加入复习' }}
          </el-button>
          <el-button plain @click="manualPlanDialogVisible = true">手动计划</el-button>
          <el-button type="warning" plain @click="onToggleFavorite">{{ detail.favorite ? '取消收藏' : '收藏' }}</el-button>
          <el-button type="success" plain @click="onExportPdf">导出PDF</el-button>
          <el-button type="primary" plain @click="openVersionHistory">
            <el-icon><Clock /></el-icon>
            <span>版本历史</span>
          </el-button>
          <el-button type="primary" @click="$router.push(`/note/update/${route.params.noteId}`)">编辑</el-button>
          <el-button type="danger" plain @click="onDelete">删除</el-button>
        </div>
      </div>
    </div>

    <div class="detail-grid">
      <article class="surface-card main-block">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="笔记标题">{{ detail.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="编程语言">{{ languageLabel }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updatedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="复习状态">
            <el-select v-model="detail.masteryStatus" style="width: 140px" @change="changeMasteryStatus">
              <el-option label="未复习" value="NOT_MASTERED" />
              <el-option label="复习中" value="REVIEWING" />
              <el-option label="已掌握" value="MASTERED" />
            </el-select>
          </el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <div class="chip-list">
              <span class="tag-chip" v-for="tag in detail.tagNames || []" :key="tag">{{ tag }}</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <h3>笔记内容</h3>
        <div v-if="detail.content?.trim()" class="markdown-body" v-html="contentHtml"></div>
        <el-empty v-else description="暂无内容" :image-size="70" />

        <div v-if="summaryResult || summaryError || summaryLoading" class="ai-summary-card">
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

        <h3 style="margin-top: 14px;">关联资料</h3>
        <el-empty v-if="!(detail.relatedMaterials || []).length" description="暂无关联资料" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="material in detail.relatedMaterials"
            :key="material.id"
            :timestamp="materialTypeLabel(material.materialType)"
            placement="top"
          >
            <el-link type="primary" @click="$router.push(`/study-material/detail/${material.id}`)">
              {{ material.title }}
            </el-link>
            <div class="chip-list" style="margin-top: 6px;">
              <span class="tag-chip" v-for="tag in material.tagNames || []" :key="tag">{{ tag }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </aside>
    </div>

    <el-drawer
      v-model="historyDrawerVisible"
      class="note-version-drawer"
      size="82%"
      :with-header="false"
      append-to-body
      destroy-on-close
    >
      <div class="version-head">
        <div>
          <h3>{{ detail.title || '笔记' }} · 版本历史</h3>
          <p>仅手动保存会生成版本，自动草稿不入历史。</p>
        </div>
        <div class="version-head-actions">
          <el-button link type="primary" :loading="versionListLoading" @click="reloadVersionList">刷新</el-button>
          <el-button @click="historyDrawerVisible = false">关闭</el-button>
        </div>
      </div>

      <div class="version-layout">
        <aside class="version-list">
          <el-skeleton v-if="versionListLoading && !versionList.length" animated :rows="6" />
          <el-empty
            v-else-if="!versionList.length"
            description="暂无版本记录，首次保存后将生成 v1 版本"
            :image-size="72"
          />
          <el-scrollbar v-else max-height="560px">
            <div
              v-for="item in versionList"
              :key="item.id"
              class="version-item"
              :class="{ active: selectedVersionId === item.id }"
              @click="onSelectVersion(item.id)"
            >
              <div class="version-item-main">
                <div class="version-item-title">
                  <strong>{{ item.versionLabel || `v${item.versionNo}` }}</strong>
                  <span>{{ item.createdAt || '-' }}</span>
                </div>
                <p>{{ item.summary || '内容更新' }}</p>
                <small>编辑人：{{ item.editorName || '本人' }}</small>
              </div>
              <div @click.stop>
                <el-checkbox
                  :model-value="compareVersionIds.includes(item.id)"
                  @change="(checked) => onVersionCompareToggle(item.id, checked)"
                >
                  对比
                </el-checkbox>
              </div>
            </div>
          </el-scrollbar>
          <p class="version-tip">可勾选两个版本直接对比</p>
        </aside>

        <section class="version-main">
          <template v-if="compareMode !== 'NONE'">
            <div class="compare-top">
              <div>
                <h4>版本差异</h4>
                <p>{{ comparePair.left?.versionLabel || '-' }} ↔ {{ comparePair.right?.versionLabel || '-' }}</p>
              </div>
              <div class="compare-actions">
                <el-button
                  v-if="comparePair.left?.id"
                  type="warning"
                  plain
                  size="small"
                  @click="onRestoreVersion(comparePair.left)"
                >
                  恢复基础版本
                </el-button>
                <el-button
                  v-if="comparePair.right?.id"
                  type="warning"
                  plain
                  size="small"
                  @click="onRestoreVersion(comparePair.right)"
                >
                  恢复对比版本
                </el-button>
                <el-button size="small" @click="closeCompare">关闭对比</el-button>
              </div>
            </div>

            <div class="diff-tags" v-if="diffFragments.length">
              <el-tag
                v-for="fragment in diffFragments"
                :key="fragment.label"
                effect="plain"
                @click="scrollToDiffRow(fragment.rowIndex)"
              >
                {{ fragment.label }}
              </el-tag>
            </div>

            <div class="diff-table" ref="diffContainerRef" v-loading="comparePanelLoading">
              <div v-if="!comparePanelLoading && !diffRows.length" class="diff-empty">当前两个版本暂无差异</div>
              <div
                v-for="(row, rowIndex) in diffRows"
                :key="`${rowIndex}_${row.type}`"
                class="diff-row"
                :class="`diff-row--${row.type}`"
                :data-row-index="rowIndex"
              >
                <div class="diff-cell">
                  <span class="line-no">{{ row.leftLineNo ?? '' }}</span>
                  <pre :class="{ strike: row.type === 'removed' }">{{ row.leftText }}</pre>
                </div>
                <div class="diff-cell">
                  <span class="line-no">{{ row.rightLineNo ?? '' }}</span>
                  <pre>{{ row.rightText }}</pre>
                </div>
              </div>
            </div>
          </template>

          <template v-else>
            <el-skeleton v-if="versionDetailLoading" animated :rows="8" />
            <el-empty v-else-if="!selectedVersionDetail" description="请选择左侧版本进行预览" :image-size="70" />
            <div v-else class="preview-pane">
              <div class="preview-top">
                <div>
                  <h4>{{ selectedVersionDetail.versionLabel || '-' }}</h4>
                  <p>{{ selectedVersionDetail.createdAt || '-' }}</p>
                </div>
                <div class="preview-actions">
                  <el-button type="warning" plain size="small" @click="onRestoreVersion(selectedVersionDetail)">恢复此版本</el-button>
                  <el-button type="primary" plain size="small" @click="onCompareCurrent">对比当前版本</el-button>
                  <el-button type="danger" plain size="small" @click="onDeleteVersion(selectedVersionDetail)">删除版本</el-button>
                </div>
              </div>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="标题">{{ selectedVersionDetail.title || '-' }}</el-descriptions-item>
                <el-descriptions-item label="编程语言">{{ toZhLanguageLabel(selectedVersionDetail.language) || '-' }}</el-descriptions-item>
                <el-descriptions-item label="版本摘要" :span="2">{{ selectedVersionDetail.summary || '-' }}</el-descriptions-item>
                <el-descriptions-item label="编辑人">{{ selectedVersionDetail.editorName || '本人' }}</el-descriptions-item>
                <el-descriptions-item label="保存时间">{{ selectedVersionDetail.createdAt || '-' }}</el-descriptions-item>
                <el-descriptions-item label="标签" :span="2">
                  <div class="chip-list">
                    <span class="tag-chip" v-for="tag in selectedVersionDetail.tagNames || []" :key="tag">{{ tag }}</span>
                  </div>
                </el-descriptions-item>
              </el-descriptions>
              <h5>版本内容预览</h5>
              <div
                v-if="selectedVersionDetail.content?.trim()"
                class="markdown-body preview-content"
                v-html="selectedVersionContentHtml"
              ></div>
              <el-empty v-else description="该版本暂无内容" :image-size="62" />
            </div>
          </template>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="manualPlanDialogVisible" title="手动复习计划" width="460px">
      <el-form label-width="100px">
        <el-form-item label="复习日期">
          <el-date-picker
            v-model="manualReviewDates"
            type="dates"
            placeholder="选择多个复习日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="manualPlanDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmManualPlan">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import { summarizeByAi } from '../api/ai'
import {
  addNote,
  buildNoteSingleExportPdfUrl,
  deleteNote,
  deleteNoteVersion,
  favoriteNote,
  getNoteDetail,
  getNoteVersionDetail,
  getNoteVersionList,
  restoreNoteVersion,
  unfavoriteNote,
  updateNoteMastery
} from '../api/note'
import { upsertReviewPlan } from '../api/review'
import { toZhLanguageLabel, toZhMaterialTypeLabel } from '../utils/material'
import { renderNoteContent } from '../utils/noteContent'
import { normalizeReviewDates, reviewStatusLabel } from '../utils/review'
import { buildSideBySideDiff } from '../utils/textDiff'
import AiMarkdownContent from '../components/ai/AiMarkdownContent.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const detail = reactive({
  title: '',
  language: '',
  masteryStatus: 'NOT_MASTERED',
  inReviewPlan: false,
  content: '',
  tagNames: [],
  favorite: false,
  relatedQuestions: [],
  relatedMaterials: [],
  createdAt: '',
  updatedAt: ''
})

const languageLabel = computed(() => toZhLanguageLabel(detail.language) || '-')
const contentHtml = computed(() => renderNoteContent(detail.content))
const inReviewPlan = computed(() => !!detail.inReviewPlan)

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
const lastSummaryPayload = ref(null)

const manualPlanDialogVisible = ref(false)
const manualReviewDates = ref([])

const historyDrawerVisible = ref(false)
const versionListLoading = ref(false)
const versionDetailLoading = ref(false)
const versionList = ref([])
const selectedVersionId = ref(null)
const selectedVersionDetail = ref(null)
const selectedVersionContentHtml = computed(() => renderNoteContent(selectedVersionDetail.value?.content || ''))
const compareVersionIds = ref([])
const compareMode = ref('NONE')
const comparePair = reactive({ left: null, right: null })
const comparePanelLoading = ref(false)
const diffRows = ref([])
const diffFragments = ref([])
const diffContainerRef = ref(null)

function resolveAiErrorMessage(err) {
  const raw = err?.message || err?.response?.data?.message || ''
  const text = String(raw || '').trim()
  if (!text) return 'AI 服务繁忙，请重试'
  if (text.includes('timeout') || text.includes('超时')) return 'AI 请求超时，请稍后重试'
  return text
}

function statusLabel(value) {
  return reviewStatusLabel(value)
}

function materialTypeLabel(value) {
  return toZhMaterialTypeLabel(value) || value || '资料'
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getNoteDetail(route.params.noteId)
    Object.assign(detail, { ...data, favorite: !!data.favorite })
  } finally {
    loading.value = false
  }
}

async function runAiSummary() {
  const payload = {
    targetType: 'NOTE',
    targetId: Number(route.params.noteId),
    title: detail.title,
    content: detail.content,
    language: detail.language,
    tagNames: detail.tagNames || [],
    summaryType: summaryType.value
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
  if (!text) return ElMessage.warning('暂无可复制内容')
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
  if (!summaryResult.value) return ElMessage.warning('请先生成 AI 总结')
  const noteId = await addNote({
    title: `${detail.title || '笔记'} - AI总结`,
    content: summaryResult.value.markdown || summaryResult.value.summaryText,
    language: detail.language || 'Java',
    tagNames: (detail.tagNames || []).length ? detail.tagNames : ['AI辅助'],
    manualQuestionIds: (detail.relatedQuestions || []).map((item) => item.id),
    manualMaterialIds: (detail.relatedMaterials || []).map((item) => item.id)
  })
  ElMessage.success('AI 总结已生成新笔记')
  if (noteId) router.push(`/note/detail/${noteId}`)
}

async function onToggleFavorite() {
  if (detail.favorite) {
    await unfavoriteNote(route.params.noteId)
    detail.favorite = false
    return ElMessage.success('已取消收藏')
  }
  await favoriteNote(route.params.noteId)
  detail.favorite = true
  ElMessage.success('收藏成功')
}

async function changeMasteryStatus(value) {
  await updateNoteMastery(route.params.noteId, value)
  ElMessage.success('复习状态已更新')
}

async function quickJoinReview() {
  await upsertReviewPlan({ contentType: 'NOTE', contentId: Number(route.params.noteId), planMode: 'AUTO' })
  detail.inReviewPlan = true
  detail.masteryStatus = 'REVIEWING'
  ElMessage.success('已加入复习计划')
}

async function confirmManualPlan() {
  const dates = normalizeReviewDates(manualReviewDates.value)
  if (!dates.length) return ElMessage.warning('请至少选择一个复习日期')
  await upsertReviewPlan({
    contentType: 'NOTE',
    contentId: Number(route.params.noteId),
    planMode: 'MANUAL',
    manualReviewDates: dates
  })
  detail.inReviewPlan = true
  detail.masteryStatus = 'REVIEWING'
  manualPlanDialogVisible.value = false
  ElMessage.success('手动复习计划已保存')
}

async function onDelete() {
  await ElMessageBox.confirm('确认删除该笔记？删除后不可恢复', '删除确认', { type: 'warning' })
  await deleteNote(route.params.noteId)
  ElMessage.success('删除成功')
  router.push('/note/list')
}

function onExportPdf() {
  window.open(buildNoteSingleExportPdfUrl(route.params.noteId), '_blank')
}

function closeCompare() {
  compareMode.value = 'NONE'
  comparePair.left = null
  comparePair.right = null
  diffRows.value = []
  diffFragments.value = []
}

function rebuildDiff(leftContent, rightContent) {
  const diff = buildSideBySideDiff(leftContent, rightContent)
  diffRows.value = diff.rows || []
  diffFragments.value = diff.fragments || []
}

async function loadComparedVersions() {
  const noteId = Number(route.params.noteId)
  if (!Number.isInteger(noteId) || noteId <= 0) {
    ElMessage.error('笔记ID无效，无法加载版本对比')
    return
  }
  if (compareVersionIds.value.length !== 2) return
  comparePanelLoading.value = true
  try {
    const [a, b] = await Promise.all([
      getNoteVersionDetail(noteId, compareVersionIds.value[0]),
      getNoteVersionDetail(noteId, compareVersionIds.value[1])
    ])
    const sorted = [a, b].sort((x, y) => Number(x.versionNo || 0) - Number(y.versionNo || 0))
    comparePair.left = sorted[0]
    comparePair.right = sorted[1]
    compareMode.value = 'TWO_VERSION'
    rebuildDiff(sorted[0]?.content || '', sorted[1]?.content || '')
  } finally {
    comparePanelLoading.value = false
  }
}

function onVersionCompareToggle(versionId, checked) {
  if (checked) {
    if (compareVersionIds.value.includes(versionId)) return
    if (compareVersionIds.value.length >= 2) return ElMessage.warning('最多勾选两个版本进行对比')
    compareVersionIds.value = [...compareVersionIds.value, versionId]
  } else {
    compareVersionIds.value = compareVersionIds.value.filter((id) => id !== versionId)
  }
  if (compareVersionIds.value.length === 2) return loadComparedVersions()
  if (compareMode.value === 'TWO_VERSION') closeCompare()
}

async function onSelectVersion(versionId) {
  const noteId = Number(route.params.noteId)
  if (!Number.isInteger(noteId) || noteId <= 0) {
    ElMessage.error('笔记ID无效，无法加载版本详情')
    return
  }
  selectedVersionId.value = versionId
  versionDetailLoading.value = true
  try {
    selectedVersionDetail.value = await getNoteVersionDetail(noteId, versionId)
  } finally {
    versionDetailLoading.value = false
  }
}

async function reloadVersionList() {
  const noteId = Number(route.params.noteId)
  if (!Number.isInteger(noteId) || noteId <= 0) {
    ElMessage.error('笔记ID无效，无法加载版本历史')
    return
  }
  versionListLoading.value = true
  try {
    try {
      versionList.value = (await getNoteVersionList(noteId)) || []
    } catch (error) {
      if (error?.response?.status === 404) {
        ElMessage.error('版本历史接口未找到，请重启后端并确认已部署最新代码')
        return
      }
      throw error
    }
    if (!versionList.value.length) {
      selectedVersionId.value = null
      selectedVersionDetail.value = null
      compareVersionIds.value = []
      closeCompare()
      return
    }
    const id = versionList.value.some((item) => item.id === selectedVersionId.value)
      ? selectedVersionId.value
      : versionList.value[0].id
    await onSelectVersion(id)
  } finally {
    versionListLoading.value = false
  }
}

async function openVersionHistory() {
  historyDrawerVisible.value = true
  compareVersionIds.value = []
  closeCompare()
  await reloadVersionList()
}

function compareContainsVersion(versionId) {
  return comparePair.left?.id === versionId || comparePair.right?.id === versionId
}

async function onDeleteVersion(version) {
  const noteId = Number(route.params.noteId)
  if (!Number.isInteger(noteId) || noteId <= 0) {
    ElMessage.error('笔记ID无效，无法删除版本')
    return
  }
  if (!version?.id) return
  await ElMessageBox.confirm(`确定删除 ${version.versionLabel || '该版本'} 吗？删除后不可恢复。`, '删除版本', { type: 'warning' })
  await deleteNoteVersion(noteId, version.id)
  ElMessage.success('版本已删除')
  compareVersionIds.value = compareVersionIds.value.filter((id) => id !== version.id)
  if (compareContainsVersion(version.id)) closeCompare()
  await reloadVersionList()
}

async function onRestoreVersion(version) {
  const noteId = Number(route.params.noteId)
  if (!Number.isInteger(noteId) || noteId <= 0) {
    ElMessage.error('笔记ID无效，无法恢复版本')
    return
  }
  if (!version?.id) return
  await ElMessageBox.confirm(
    `确定要恢复至 ${version.versionLabel || '该版本'} 吗？当前内容将被覆盖并生成新版本。`,
    '恢复确认',
    { type: 'warning' }
  )
  await restoreNoteVersion(noteId, version.id)
  ElMessage.success('版本恢复成功，已自动生成新版本')
  closeCompare()
  compareVersionIds.value = []
  selectedVersionId.value = null
  await loadDetail()
  await reloadVersionList()
}

function onCompareCurrent() {
  if (!selectedVersionDetail.value) return
  comparePair.left = selectedVersionDetail.value
  comparePair.right = {
    id: null,
    versionLabel: '当前版本',
    content: detail.content || '',
    createdAt: detail.updatedAt || detail.createdAt || ''
  }
  compareMode.value = 'CURRENT'
  compareVersionIds.value = []
  rebuildDiff(comparePair.left?.content || '', comparePair.right?.content || '')
}

function scrollToDiffRow(rowIndex) {
  nextTick(() => {
    const target = diffContainerRef.value?.querySelector(`[data-row-index="${rowIndex}"]`)
    if (target) target.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

onMounted(() => {
  loadDetail()
})

watch(historyDrawerVisible, (visible) => {
  if (visible) return
  compareVersionIds.value = []
  closeCompare()
})

</script>

<style scoped>
.top-row { display: flex; flex-direction: column; align-items: flex-start; margin-bottom: 12px; }
.top-row h2 { margin: 0; color: var(--primary); }
.actions-wrap {
  width: 100%;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  min-width: max-content;
}
.actions :deep(.el-button),.actions :deep(.el-select) { flex-shrink: 0; }
.detail-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 12px; }
.main-block,.side-block { padding: 16px; }
.side-block { position: sticky; top: 14px; height: fit-content; max-height: calc(100vh - 40px); overflow: auto; }
.main-block h3,.side-block h3 { margin: 16px 0 8px; color: var(--primary); }

.markdown-body {
  line-height: 1.72;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface-soft);
  color: var(--text-main);
  padding: 12px;
}
.markdown-body :deep(h1),.markdown-body :deep(h2),.markdown-body :deep(h3),.markdown-body :deep(h4),.markdown-body :deep(h5),.markdown-body :deep(h6) {
  margin: 10px 0 8px;
  color: var(--primary);
}
.markdown-body :deep(p) { margin: 8px 0; }
.markdown-body :deep(ul),.markdown-body :deep(ol) { margin: 8px 0; padding-left: 20px; }
.markdown-body :deep(code) {
  background: rgba(201, 100, 66, 0.15);
  border-radius: 4px;
  padding: 2px 6px;
  color: var(--text-main);
  font-family: 'Consolas', 'Courier New', monospace;
}
.markdown-body :deep(.md-code) {
  margin: 10px 0;
  padding: 10px;
  background: #232320;
  color: #faf9f5;
  border-radius: 8px;
  overflow-x: auto;
}
.markdown-body :deep(.md-code code) { background: transparent; padding: 0; color: inherit; }

.ai-summary-card { margin-top: 14px; border: 1px solid var(--border-soft); border-radius: 12px; background: var(--surface); }
.ai-summary-head { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; border-bottom: 1px solid var(--border-soft); }
.ai-summary-head h4 { margin: 0; color: var(--text-accent-strong); }
.ai-summary-body { padding: 12px; }
.ai-summary-error {
  border: 1px solid rgba(181, 51, 51, 0.28);
  border-radius: 10px;
  background: rgba(181, 51, 51, 0.1);
  color: var(--danger);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
}
.mind-map-box { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.mind-map-image { width: 100%; border: 1px solid var(--border-soft); border-radius: 10px; background: var(--surface); }
.ai-summary-actions { margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; }

:deep(.note-version-drawer .el-drawer__body) { padding: 14px 16px; }
.version-head { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.version-head h3 { margin: 0; color: var(--primary); }
.version-head p { margin: 6px 0 0; color: var(--text-sub); font-size: 13px; }
.version-head-actions { display: flex; gap: 8px; }
.version-layout { display: grid; grid-template-columns: 320px 1fr; gap: 12px; min-height: 580px; }

.version-list {
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  background: var(--surface);
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.version-item {
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface-soft);
  padding: 10px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.version-item + .version-item { margin-top: 8px; }
.version-item:hover { border-color: rgba(201, 100, 66, 0.42); }
.version-item.active { border-color: rgba(201, 100, 66, 0.58); box-shadow: 0 6px 20px rgba(20, 20, 19, 0.12); }
.version-item-title { display: flex; flex-direction: column; gap: 2px; }
.version-item-title strong { color: var(--text-accent-strong); }
.version-item-title span,.version-item-main small { color: var(--text-sub); font-size: 12px; }
.version-item-main p {
  margin: 6px 0;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.version-tip { margin: 0; font-size: 12px; color: var(--text-sub); }
.version-main { border: 1px solid var(--border-soft); border-radius: 12px; background: var(--surface); padding: 12px; }

.compare-top,.preview-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; margin-bottom: 10px; }
.compare-top h4,.preview-top h4 { margin: 0; color: var(--text-accent-strong); }
.compare-top p,.preview-top p { margin: 4px 0 0; color: var(--text-sub); font-size: 12px; }
.compare-actions,.preview-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.diff-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.diff-table { border: 1px solid var(--border-soft); border-radius: 10px; max-height: 430px; overflow: auto; background: var(--surface); }
.diff-empty { text-align: center; padding: 28px 10px; color: var(--text-sub); }
.diff-row { display: grid; grid-template-columns: 1fr 1fr; }
.diff-row + .diff-row { border-top: 1px solid var(--border-soft); }
.diff-cell { min-width: 0; padding: 6px 8px; display: flex; gap: 8px; }
.diff-cell + .diff-cell { border-left: 1px solid var(--border-soft); }
.line-no { width: 38px; text-align: right; color: var(--text-sub); font-size: 12px; }
.diff-cell pre {
  margin: 0;
  flex: 1;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-main);
}
.strike { text-decoration: line-through; }
.diff-row--added .diff-cell:nth-child(2) { background: rgba(16, 185, 129, 0.14); }
.diff-row--removed .diff-cell:nth-child(1) { background: rgba(239, 68, 68, 0.14); }
.diff-row--modified .diff-cell { background: rgba(245, 158, 11, 0.14); }

.preview-pane h5 { margin: 14px 0 8px; color: var(--text-accent-strong); }
.preview-content { max-height: 460px; overflow: auto; }

:global(:root[data-theme='dark']) .ai-summary-card { background: #232320; border-color: #3b3a35; }
:global(:root[data-theme='dark']) .ai-summary-head { border-bottom-color: #3b3a35; }
:global(:root[data-theme='dark']) .ai-summary-head h4 { color: #faf9f5; }
:global(:root[data-theme='dark']) .mind-map-image { border-color: #3b3a35; background: #1c1c1a; }

@media (max-width: 1100px) {
  .detail-grid { grid-template-columns: 1fr; }
  .side-block { position: static; max-height: none; }
  .actions-wrap { margin-top: 10px; }
  .version-head { flex-direction: column; }
  .version-layout { grid-template-columns: 1fr; }
}
</style>
