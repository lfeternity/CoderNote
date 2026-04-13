<template>
  <section class="fade-in" v-loading="loading">
    <div class="top-row">
      <div class="page-title-back">
        <el-button
          class="back-chevron-btn"
          plain
          aria-label="返回列表"
          @click="$router.push('/error-question/list')"
        >&lt;</el-button>
        <h2>{{ detail.title || '错题详情' }}</h2>
      </div>
      <div class="top-actions">
        <el-button type="primary" @click="$router.push(`/error-question/update/${route.params.questionId}`)">编辑</el-button>
        <el-button type="warning" plain :disabled="inReviewPlan" @click="quickJoinReview">
          {{ inReviewPlan ? '已加入复习' : '加入复习' }}
        </el-button>
        <el-button plain @click="manualPlanDialogVisible = true">手动计划</el-button>

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

        <el-button type="success" plain @click="onExportPdf">导出PDF</el-button>
        <el-button type="danger" plain @click="onDelete">删除</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <article class="surface-card main-block">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="编程语言">{{ detail.language }}</el-descriptions-item>
          <el-descriptions-item label="新增时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="掌握状态">
            <el-select v-model="detail.masteryStatus" style="width: 140px" @change="changeStatus">
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
          <el-descriptions-item label="错题来源">{{ detail.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3>错误题目</h3>
        <pre v-if="detail.errorCode" class="code-block">{{ detail.errorCode }}</pre>
        <p v-else class="empty-text">-</p>
        <div v-if="(detail.errorQuestionAttachments || []).length" class="attachment-grid">
          <div v-for="file in detail.errorQuestionAttachments" :key="file.path" class="attachment-card">
            <template v-if="file.image">
              <el-image
                class="image-thumb"
                :src="attachmentPreviewUrl(file)"
                :preview-src-list="[attachmentPreviewUrl(file)]"
                fit="cover"
                preview-teleported
              />
              <div class="attachment-actions">
                <span class="file-name">{{ file.fileName }}</span>
                <el-link :href="attachmentDownloadUrl(file)" target="_blank">下载原图</el-link>
              </div>
            </template>
            <template v-else>
              <div class="attachment-actions">
                <span class="file-name">{{ file.fileName }}</span>
                <el-link :href="attachmentDownloadUrl(file)" target="_blank">下载文档</el-link>
              </div>
            </template>
          </div>
        </div>

        <h3>错误原因</h3>
        <p>{{ detail.errorReason }}</p>

        <h3>正确方案</h3>
        <pre v-if="detail.correctCode" class="code-block">{{ detail.correctCode }}</pre>
        <p v-else class="empty-text">-</p>
        <div v-if="(detail.correctSolutionAttachments || []).length" class="attachment-grid">
          <div v-for="file in detail.correctSolutionAttachments" :key="file.path" class="attachment-card">
            <template v-if="file.image">
              <el-image
                class="image-thumb"
                :src="attachmentPreviewUrl(file)"
                :preview-src-list="[attachmentPreviewUrl(file)]"
                fit="cover"
                preview-teleported
              />
              <div class="attachment-actions">
                <span class="file-name">{{ file.fileName }}</span>
                <el-link :href="attachmentDownloadUrl(file)" target="_blank">下载原图</el-link>
              </div>
            </template>
            <template v-else>
              <div class="attachment-actions">
                <span class="file-name">{{ file.fileName }}</span>
                <el-link :href="attachmentDownloadUrl(file)" target="_blank">下载文档</el-link>
              </div>
            </template>
          </div>
        </div>

        <QuestionAnalysisPanel
          :question-id="route.params.questionId"
          :error-code="detail.errorCode"
          :exception-message="detail.errorReason"
          :context-description="aiContextDescription"
          :language="detail.language"
          :tag-names="detail.tagNames || []"
          @apply="applyAiSolution"
          @save="saveAiNote"
        />

        <h3>解决方案</h3>
        <p>{{ detail.solution }}</p>
      </article>

      <aside class="surface-card side-block">
        <h3>关联学习资料</h3>
        <el-empty v-if="!(detail.relatedMaterials || []).length" description="暂无关联资料" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="material in detail.relatedMaterials"
            :key="material.id"
            :timestamp="toZhMaterialTypeLabel(material.materialType)"
            placement="top"
          >
            <el-link type="primary" @click="$router.push(`/study-material/detail/${material.id}`)">
              {{ material.title }}
            </el-link>
            <div class="chip-list" style="margin-top: 6px">
              <span class="tag-chip" v-for="tag in material.tagNames || []" :key="tag">{{ tag }}</span>
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
            <div class="chip-list" style="margin-top: 6px">
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
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { deleteQuestion, getQuestionDetail, updateQuestion, updateQuestionMastery, buildQuestionSingleExportPdfUrl } from '../api/question'
import { addNote, getNoteList, linkQuestionNotes } from '../api/note'
import { upsertReviewPlan } from '../api/review'
import { toZhLanguageLabel, toZhMaterialTypeLabel } from '../utils/material'
import { normalizeReviewDates } from '../utils/review'
import QuestionAnalysisPanel from '../components/ai/QuestionAnalysisPanel.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = reactive({
  title: '',
  masteryStatus: 'NOT_MASTERED',
  inReviewPlan: false,
  tagNames: [],
  relatedMaterials: [],
  relatedNotes: [],
  errorQuestionAttachments: [],
  correctSolutionAttachments: []
})

const noteDialogVisible = ref(false)
const noteLoading = ref(false)
const noteSubmitting = ref(false)
const noteOptions = ref([])
const selectedNoteIds = ref([])
const noteKeyword = ref('')
const manualPlanDialogVisible = ref(false)
const manualReviewDates = ref([])
const aiContextDescription = computed(() => [detail.source, detail.remark].filter(Boolean).join('；'))
const inReviewPlan = computed(() => !!detail.inReviewPlan)

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

async function loadDetail() {
  loading.value = true
  try {
    const data = await getQuestionDetail(route.params.questionId)
    Object.assign(detail, data)
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
      questionId: route.params.questionId,
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
    await linkQuestionNotes(route.params.questionId, selectedNoteIds.value)
    ElMessage.success('关联成功')
    noteDialogVisible.value = false
    await loadDetail()
  } finally {
    noteSubmitting.value = false
  }
}

async function quickJoinReview() {
  await upsertReviewPlan({
    contentType: 'QUESTION',
    contentId: Number(route.params.questionId),
    planMode: 'AUTO'
  })
  detail.inReviewPlan = true
  detail.masteryStatus = 'REVIEWING'
  ElMessage.success('已加入复习计划')
}

async function confirmManualPlan() {
  const dates = normalizeReviewDates(manualReviewDates.value)
  if (!dates.length) {
    ElMessage.warning('请至少选择一个复习日期')
    return
  }
  await upsertReviewPlan({
    contentType: 'QUESTION',
    contentId: Number(route.params.questionId),
    planMode: 'MANUAL',
    manualReviewDates: dates
  })
  detail.inReviewPlan = true
  detail.masteryStatus = 'REVIEWING'
  manualPlanDialogVisible.value = false
  ElMessage.success('手动复习计划已保存')
}

function normalizeAttachmentList(list) {
  return (list || [])
    .map((item) => ({
      fileName: item.fileName,
      path: item.path,
      contentType: item.contentType,
      size: item.size,
      image: item.image
    }))
    .filter((item) => item.path)
}

function buildQuestionUpdatePayload(solutionText, fixedCode) {
  return {
    title: detail.title,
    language: detail.language,
    tagNames: (detail.tagNames || []).length ? detail.tagNames : ['AI辅助'],
    errorCode: detail.errorCode || '',
    errorReason: detail.errorReason || 'AI 分析补充说明',
    correctCode: fixedCode || detail.correctCode || '',
    solution: solutionText || detail.solution || 'AI 已生成修复建议',
    source: detail.source || '',
    remark: detail.remark || '',
    manualMaterialIds: detail.manualMaterialIds || [],
    errorQuestionAttachments: normalizeAttachmentList(detail.errorQuestionAttachments),
    correctSolutionAttachments: normalizeAttachmentList(detail.correctSolutionAttachments)
  }
}

async function applyAiSolution(aiResult) {
  const payload = buildQuestionUpdatePayload(aiResult?.solutionText || aiResult?.markdown || '', aiResult?.fixedCode || '')
  await updateQuestion(route.params.questionId, payload)
  detail.solution = payload.solution
  detail.correctCode = payload.correctCode
  ElMessage.success('已一键填入解决方案')
}

async function saveAiNote(aiResult) {
  const noteId = await addNote({
    title: `${detail.title || '错题'} - AI分析`,
    content: aiResult?.markdown || aiResult?.solutionText || 'AI 分析结果为空，请重试',
    language: detail.language || 'Java',
    tagNames: (detail.tagNames || []).length ? detail.tagNames : ['AI辅助'],
    manualQuestionIds: [Number(route.params.questionId)],
    manualMaterialIds: detail.manualMaterialIds || []
  })
  ElMessage.success('已保存为新笔记')
  if (noteId) {
    router.push(`/note/detail/${noteId}`)
  }
}
function onExportPdf() {
  const url = buildQuestionSingleExportPdfUrl(route.params.questionId)
  window.open(url, '_blank')
}

async function onDelete() {
  await ElMessageBox.confirm('确认删除该错题？删除后不可恢复', '删除确认', { type: 'warning' })
  await deleteQuestion(route.params.questionId)
  ElMessage.success('删除成功')
  router.push('/error-question/list')
}

async function changeStatus(value) {
  await updateQuestionMastery(route.params.questionId, value)
  ElMessage.success('掌握状态已更新')
}

loadDetail()
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
  color: var(--text-accent-strong);
}

.empty-text {
  color: var(--text-sub);
}

.attachment-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 8px;
}

.attachment-card {
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  padding: 10px;
  background: var(--surface-soft);
}

.image-thumb {
  width: 180px;
  height: 120px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.attachment-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.file-name {
  color: var(--text-main);
  word-break: break-all;
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
}
</style>
