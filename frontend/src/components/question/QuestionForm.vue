<template>
  <div class="question-form-wrap">
    <div v-if="enableDraft" class="form-head-actions">
      <el-dropdown trigger="click" @command="onMoreCommand">
        <el-button text type="primary">更多</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="clear-draft">清理草稿</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="112px" @focusout="onEditorFocusOut">
      <el-form-item label="错题标题" prop="title">
        <el-input v-model="form.title" placeholder="例如：Java泛型报错：类型不匹配" />
      </el-form-item>

      <el-form-item label="编程语言" prop="language">
        <el-select v-model="form.language" placeholder="请选择" style="width: 100%">
          <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="错题封面">
        <CoverPicker
          v-model="form.coverPath"
          biz-type="question"
          preview-label="错题封面预览"
        />
      </el-form-item>

      <el-form-item label="知识点标签" prop="tagNames">
        <el-select
          v-model="form.tagNames"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="可多选，可手动新增"
          style="width: 100%"
        >
          <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.name" :value="tag.name" />
        </el-select>
      </el-form-item>

      <el-form-item label="错误题目" prop="errorCode" required>
        <el-input v-model="form.errorCode" type="textarea" :rows="5" placeholder="可填写代码或文字" />
        <div class="upload-wrap">
          <el-upload
            :show-file-list="false"
            :accept="uploadAccept"
            :http-request="(options) => handleUpload(options, 'errorQuestionAttachments', 'errorCode')"
          >
            <el-button size="small" plain>上传错误题目附件</el-button>
          </el-upload>
        </div>
        <div v-if="form.errorQuestionAttachments.length" class="attachment-list">
          <div v-for="(file, index) in form.errorQuestionAttachments" :key="file.path || index" class="attachment-item">
            <el-link :href="attachmentDownloadUrl(file)" target="_blank">{{ file.fileName || '附件' }}</el-link>
            <el-tag v-if="file.image" size="small" type="success">图片</el-tag>
            <el-button link type="danger" @click="removeAttachment('errorQuestionAttachments', index, 'errorCode')">移除</el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="错误原因" prop="errorReason">
        <el-input v-model="form.errorReason" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="正确方案" prop="correctCode" required>
        <el-input v-model="form.correctCode" type="textarea" :rows="5" placeholder="可填写代码或文字" />
        <div class="upload-wrap">
          <el-upload
            :show-file-list="false"
            :accept="uploadAccept"
            :http-request="(options) => handleUpload(options, 'correctSolutionAttachments', 'correctCode')"
          >
            <el-button size="small" plain>上传正确方案附件</el-button>
          </el-upload>
        </div>
        <div v-if="form.correctSolutionAttachments.length" class="attachment-list">
          <div v-for="(file, index) in form.correctSolutionAttachments" :key="file.path || index" class="attachment-item">
            <el-link :href="attachmentDownloadUrl(file)" target="_blank">{{ file.fileName || '附件' }}</el-link>
            <el-tag v-if="file.image" size="small" type="success">图片</el-tag>
            <el-button link type="danger" @click="removeAttachment('correctSolutionAttachments', index, 'correctCode')">移除</el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="解决方案" prop="solution">
        <el-input v-model="form.solution" type="textarea" :rows="3" />
      </el-form-item>

      <el-form-item label="错题来源">
        <el-input v-model="form.source" placeholder="LeetCode / 课程作业 / 面试题" />
      </el-form-item>

      <el-form-item label="手动关联资料">
        <el-select
          v-model="form.manualMaterialIds"
          multiple
          filterable
          placeholder="可补充自动标签联动"
          style="width: 100%"
        >
          <el-option
            v-for="material in materialOptions"
            :key="material.id"
            :label="formatMaterialOption(material)"
            :value="material.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="onSubmit">提交</el-button>
        <el-button @click="$emit('cancel')">取消</el-button>
      </el-form-item>
    </el-form>

    <button class="ai-float-btn" type="button" @click="aiDrawerVisible = true">AI 分析</button>

    <el-drawer v-model="aiDrawerVisible" title="AI 分析助手" size="460px" append-to-body>
      <QuestionAnalysisPanel
        compact
        :question-id="props.modelValue?.id || null"
        :error-code="form.errorCode"
        :exception-message="form.errorReason"
        :context-description="[form.source, form.remark].filter(Boolean).join('；')"
        :language="form.language"
        :tag-names="form.tagNames"
        @apply="applyAiResult"
        @save="saveAiNote"
      />
    </el-drawer>

    <transition name="draft-popup-fade">
      <div v-if="restorePromptVisible" ref="restorePromptRef" class="draft-restore-card surface-card">
        <h4>未保存草稿</h4>
        <p>发现未保存的草稿，保存于：{{ restorePromptSavedAt }}，是否恢复？</p>
        <div class="draft-restore-actions">
          <el-button size="small" @click="ignoreRestorePrompt">忽略</el-button>
          <el-button size="small" type="primary" @click="restoreDraftContent">恢复</el-button>
        </div>
      </div>
    </transition>

    <transition name="draft-status-fade">
      <div
        v-if="draftStatusVisible"
        class="draft-status-tip"
        @mouseenter="onStatusHover(true)"
        @mouseleave="onStatusHover(false)"
      >
        {{ draftStatusText }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../store/auth'
import { toZhMaterialTypeLabel } from '../../utils/material'
import { buildDraftStorageKey, cleanupExpiredDraftsForUser, formatDraftSavedAt, readDraftRecord, removeDraftRecord, saveDraftRecord } from '../../utils/draft'
import { addNote } from '../../api/note'
import { uploadQuestionAttachment } from '../../api/question'
import QuestionAnalysisPanel from '../ai/QuestionAnalysisPanel.vue'
import CoverPicker from '../common/CoverPicker.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  languageOptions: {
    type: Array,
    default: () => []
  },
  tagOptions: {
    type: Array,
    default: () => []
  },
  materialOptions: {
    type: Array,
    default: () => []
  },
  draftKey: {
    type: String,
    default: 'new'
  },
  enableDraft: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['submit', 'cancel'])

const DRAFT_BIZ_TYPE = 'ERROR_QUESTION'
const AUTO_SAVE_DELAY_MS = 300
const DRAFT_PROMPT_AUTO_HIDE_MS = 30000
const DRAFT_STATUS_AUTO_HIDE_MS = 3000
const STORAGE_ERROR_MESSAGE_INTERVAL_MS = 5000

const authStore = useAuthStore()

const formRef = ref(null)
const aiDrawerVisible = ref(false)
const restorePromptRef = ref(null)

const restorePromptVisible = ref(false)
const restorePromptSavedAt = ref('')
const pendingRestoreDraft = ref(null)
const ignoredRestoreToken = ref('')

const draftStatus = ref('idle')
const draftStatusVisible = ref(false)
const draftStatusHovering = ref(false)
const mutatingForm = ref(false)

const modelSnapshot = ref('')
const lastDraftSnapshot = ref('')

let storageErrorNotifiedAt = 0
let draftSaveTimer = null
let restoreCheckTimer = null
let restoreAutoHideTimer = null
let draftStatusHideTimer = null

const uploadAccept = '.png,.jpg,.jpeg,.gif,.webp,.bmp,.pdf,.doc,.docx,.md,.markdown,.txt,.rtf,.ppt,.pptx,.xls,.xlsx'

const form = reactive({
  title: '',
  language: '',
  coverPath: '',
  tagNames: [],
  errorCode: '',
  errorReason: '',
  correctCode: '',
  solution: '',
  source: '',
  remark: '',
  manualMaterialIds: [],
  errorQuestionAttachments: [],
  correctSolutionAttachments: []
})

function validateErrorQuestion(_, value, callback) {
  const hasText = !!value && value.trim().length > 0
  const hasAttachment = form.errorQuestionAttachments.length > 0
  if (hasText || hasAttachment) {
    callback()
    return
  }
  callback(new Error('请填写错误题目或上传附件'))
}

function validateCorrectSolution(_, value, callback) {
  const hasText = !!value && value.trim().length > 0
  const hasAttachment = form.correctSolutionAttachments.length > 0
  if (hasText || hasAttachment) {
    callback()
    return
  }
  callback(new Error('请填写正确方案或上传附件'))
}

const rules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  language: [{ required: true, message: '请选择语言', trigger: 'change' }],
  tagNames: [{ type: 'array', required: true, min: 1, message: '至少选择一个标签', trigger: 'change' }],
  errorCode: [{ validator: validateErrorQuestion, trigger: ['blur', 'change'] }],
  errorReason: [{ required: true, message: '请填写错误原因', trigger: 'blur' }],
  correctCode: [{ validator: validateCorrectSolution, trigger: ['blur', 'change'] }],
  solution: [{ required: true, message: '请填写解决方案', trigger: 'blur' }]
}

const draftStorageKey = computed(() => buildDraftStorageKey({
  userId: authStore.profile?.id ?? 'anonymous',
  bizType: DRAFT_BIZ_TYPE,
  bizId: props.draftKey || 'new'
}))

const draftStatusText = computed(() => {
  if (draftStatus.value === 'saving') return '自动保存中...'
  if (draftStatus.value === 'saved') return '已自动保存'
  if (draftStatus.value === 'failed') return '草稿保存失败，请手动保存内容'
  return ''
})

function normalizeAttachmentList(list) {
  return (list || [])
    .filter((item) => item && item.path)
    .map((item) => ({
      fileName: item.fileName,
      path: item.path,
      contentType: item.contentType,
      size: item.size,
      image: item.image
    }))
}

function normalizeFormValue(value) {
  return {
    title: value?.title || '',
    language: value?.language || '',
    coverPath: value?.coverPath || '',
    tagNames: Array.isArray(value?.tagNames) ? [...value.tagNames] : [],
    errorCode: value?.errorCode || '',
    errorReason: value?.errorReason || '',
    correctCode: value?.correctCode || '',
    solution: value?.solution || '',
    source: value?.source || '',
    remark: value?.remark || '',
    manualMaterialIds: Array.isArray(value?.manualMaterialIds) ? [...value.manualMaterialIds] : [],
    errorQuestionAttachments: normalizeAttachmentList(value?.errorQuestionAttachments),
    correctSolutionAttachments: normalizeAttachmentList(value?.correctSolutionAttachments)
  }
}

function toSnapshot(value) {
  return JSON.stringify(normalizeFormValue(value))
}

function resetStatusHideTimer() {
  if (draftStatusHideTimer) {
    clearTimeout(draftStatusHideTimer)
    draftStatusHideTimer = null
  }
}

function resetRestoreAutoHideTimer() {
  if (restoreAutoHideTimer) {
    clearTimeout(restoreAutoHideTimer)
    restoreAutoHideTimer = null
  }
}

function hideRestorePrompt() {
  restorePromptVisible.value = false
  pendingRestoreDraft.value = null
  resetRestoreAutoHideTimer()
}

function scheduleStatusHide() {
  resetStatusHideTimer()
  draftStatusHideTimer = setTimeout(() => {
    if (draftStatusHovering.value) {
      scheduleStatusHide()
      return
    }
    draftStatus.value = 'idle'
    draftStatusVisible.value = false
  }, DRAFT_STATUS_AUTO_HIDE_MS)
}

function setDraftStatus(status) {
  draftStatus.value = status
  draftStatusVisible.value = status === 'saving' || status === 'saved' || status === 'failed'
  if (status === 'saved' || status === 'failed') {
    scheduleStatusHide()
    return
  }
  resetStatusHideTimer()
}

function currentDraftPayload() {
  return normalizeFormValue(form)
}

function currentDraftToken(record) {
  return `${draftStorageKey.value}:${record.savedAt}`
}

function saveDraftNow({ force = false, payload } = {}) {
  if (!props.enableDraft) return false

  const normalized = normalizeFormValue(payload || currentDraftPayload())
  const snapshot = toSnapshot(normalized)

  if (!force && snapshot === lastDraftSnapshot.value) {
    return false
  }

  setDraftStatus('saving')
  const saveResult = saveDraftRecord(draftStorageKey.value, normalized)

  if (!saveResult.ok) {
    setDraftStatus('failed')
    if (Date.now() - storageErrorNotifiedAt > STORAGE_ERROR_MESSAGE_INTERVAL_MS) {
      storageErrorNotifiedAt = Date.now()
      ElMessage.error('草稿保存失败，请手动保存内容')
    }
    return false
  }

  lastDraftSnapshot.value = snapshot
  ignoredRestoreToken.value = ''
  setDraftStatus('saved')
  return true
}

function scheduleDraftSave() {
  if (!props.enableDraft) return
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer)
  }
  draftSaveTimer = setTimeout(() => {
    draftSaveTimer = null
    saveDraftNow()
  }, AUTO_SAVE_DELAY_MS)
}

function flushDraftSave() {
  if (!props.enableDraft) return
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer)
    draftSaveTimer = null
  }
  saveDraftNow()
}

function scheduleRestorePromptCheck() {
  if (!props.enableDraft) return
  if (restoreCheckTimer) {
    clearTimeout(restoreCheckTimer)
  }
  restoreCheckTimer = setTimeout(() => {
    restoreCheckTimer = null
    checkRestorePrompt()
  }, 120)
}

function checkRestorePrompt() {
  if (!props.enableDraft) return

  const draftRecord = readDraftRecord(draftStorageKey.value)
  if (!draftRecord) {
    hideRestorePrompt()
    return
  }

  const draftToken = currentDraftToken(draftRecord)
  if (draftToken === ignoredRestoreToken.value) {
    return
  }

  const draftSnapshot = toSnapshot(draftRecord.payload)
  if (draftSnapshot === modelSnapshot.value) {
    hideRestorePrompt()
    return
  }

  pendingRestoreDraft.value = {
    ...draftRecord,
    snapshot: draftSnapshot,
    token: draftToken
  }
  restorePromptSavedAt.value = formatDraftSavedAt(draftRecord.savedAt)
  restorePromptVisible.value = true
  resetRestoreAutoHideTimer()
  restoreAutoHideTimer = setTimeout(() => {
    ignoreRestorePrompt()
  }, DRAFT_PROMPT_AUTO_HIDE_MS)
}

function ignoreRestorePrompt() {
  if (pendingRestoreDraft.value?.token) {
    ignoredRestoreToken.value = pendingRestoreDraft.value.token
  }
  hideRestorePrompt()
}

function restoreDraftContent() {
  if (!pendingRestoreDraft.value) return
  const restored = normalizeFormValue(pendingRestoreDraft.value.payload)
  mutatingForm.value = true
  Object.assign(form, restored)
  mutatingForm.value = false
  lastDraftSnapshot.value = pendingRestoreDraft.value.snapshot
  hideRestorePrompt()
  formRef.value?.validate?.()
  ElMessage.success('已恢复草稿内容')
}

function clearDraft() {
  removeDraftRecord(draftStorageKey.value)
  ignoredRestoreToken.value = ''
  hideRestorePrompt()
}

function syncDraft(payload) {
  return saveDraftNow({ force: true, payload })
}

function promoteDraft(nextBizId, payload) {
  const safeBizId = String(nextBizId ?? '').trim()
  if (!safeBizId) {
    return syncDraft(payload)
  }

  const targetKey = buildDraftStorageKey({
    userId: authStore.profile?.id ?? 'anonymous',
    bizType: DRAFT_BIZ_TYPE,
    bizId: safeBizId
  })
  const normalized = normalizeFormValue(payload || currentDraftPayload())
  const saveResult = saveDraftRecord(targetKey, normalized)

  if (!saveResult.ok) {
    if (Date.now() - storageErrorNotifiedAt > STORAGE_ERROR_MESSAGE_INTERVAL_MS) {
      storageErrorNotifiedAt = Date.now()
      ElMessage.error('草稿保存失败，请手动保存内容')
    }
    return false
  }

  if (targetKey !== draftStorageKey.value) {
    removeDraftRecord(draftStorageKey.value)
  }
  lastDraftSnapshot.value = toSnapshot(normalized)
  ignoredRestoreToken.value = ''
  return true
}

async function onClearDraft() {
  await ElMessageBox.confirm(
    '确定要清理当前草稿吗？清理后将无法恢复。',
    '清理草稿',
    {
      confirmButtonText: '确认清理',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  clearDraft()
  ElMessage.success('草稿已清理')
}

function onMoreCommand(command) {
  if (command !== 'clear-draft') return
  onClearDraft().catch(() => {})
}

function formatMaterialOption(material) {
  const typeLabel = toZhMaterialTypeLabel(material?.materialType) || '未知类型'
  return `${material?.title || '未命名资料'}（${typeLabel}）`
}

async function handleUpload(options, attachmentField, validateField) {
  try {
    const data = await uploadQuestionAttachment(options.file)
    form[attachmentField].push(data)
    options.onSuccess?.(data)
    ElMessage.success('附件上传成功')
    formRef.value?.validateField(validateField)
  } catch (error) {
    options.onError?.(error)
  }
}

function removeAttachment(attachmentField, index, validateField) {
  form[attachmentField].splice(index, 1)
  formRef.value?.validateField(validateField)
}

function attachmentDownloadUrl(file) {
  if (file?.downloadUrl) return file.downloadUrl
  const path = encodeURIComponent(file?.path || '')
  const name = encodeURIComponent(file?.fileName || 'file')
  return `/api/v1/file/download?path=${path}&name=${name}&download=true`
}

function applyAiResult(aiResult) {
  if (aiResult?.fixedCode) {
    form.correctCode = aiResult.fixedCode
  }
  if (aiResult?.solutionText || aiResult?.markdown) {
    form.solution = aiResult.solutionText || aiResult.markdown
  }
  formRef.value?.validateField('correctCode')
  formRef.value?.validateField('solution')
  aiDrawerVisible.value = false
  ElMessage.success('AI 结果已填入表单')
}

async function saveAiNote(aiResult) {
  const questionId = Number(props.modelValue?.id)
  const manualQuestionIds = Number.isFinite(questionId) && questionId > 0 ? [questionId] : []
  await addNote({
    title: `${form.title || '错题'} - AI分析`,
    content: aiResult?.markdown || aiResult?.solutionText || 'AI 分析结果为空，请重试',
    language: form.language || 'Java',
    tagNames: (form.tagNames || []).length ? form.tagNames : ['AI辅助'],
    manualQuestionIds,
    manualMaterialIds: form.manualMaterialIds || []
  })
  ElMessage.success('AI 结果已保存为新笔记')
}

function onSubmit() {
  formRef.value.validate((valid) => {
    if (!valid) return
    const payload = {
      ...form,
      title: form.title.trim(),
      language: form.language.trim(),
      coverPath: form.coverPath || '',
      errorCode: form.errorCode?.trim() || '',
      correctCode: form.correctCode?.trim() || '',
      tagNames: form.tagNames.map((item) => item.trim()).filter(Boolean),
      errorQuestionAttachments: normalizeAttachmentList(form.errorQuestionAttachments),
      correctSolutionAttachments: normalizeAttachmentList(form.correctSolutionAttachments)
    }
    emit('submit', payload, {
      clearDraft,
      syncDraft: (nextPayload) => syncDraft(nextPayload || payload),
      promoteDraft: (nextBizId, nextPayload) => promoteDraft(nextBizId, nextPayload || payload)
    })
  })
}

function onEditorFocusOut() {
  flushDraftSave()
}

function onVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    flushDraftSave()
  }
}

function onWindowBlur() {
  flushDraftSave()
}

function onBeforeUnload() {
  flushDraftSave()
}

function onDocumentPointerDown(event) {
  if (!restorePromptVisible.value) return
  const target = event?.target
  if (!target) return
  if (restorePromptRef.value?.contains(target)) return
  ignoreRestorePrompt()
}

function onStatusHover(value) {
  draftStatusHovering.value = value
  if (!value && (draftStatus.value === 'saved' || draftStatus.value === 'failed')) {
    scheduleStatusHide()
  }
}

watch(
  () => props.modelValue,
  (value) => {
    const next = normalizeFormValue(value)
    const snapshot = toSnapshot(next)
    if (snapshot === modelSnapshot.value) {
      return
    }

    modelSnapshot.value = snapshot
    lastDraftSnapshot.value = snapshot

    mutatingForm.value = true
    Object.assign(form, next)
    mutatingForm.value = false

    scheduleRestorePromptCheck()
  },
  { immediate: true, deep: true }
)

watch(
  () => ({
    title: form.title,
    language: form.language,
    coverPath: form.coverPath,
    tagNames: form.tagNames,
    errorCode: form.errorCode,
    errorReason: form.errorReason,
    correctCode: form.correctCode,
    solution: form.solution,
    source: form.source,
    remark: form.remark,
    manualMaterialIds: form.manualMaterialIds,
    errorQuestionAttachments: form.errorQuestionAttachments,
    correctSolutionAttachments: form.correctSolutionAttachments
  }),
  () => {
    if (!props.enableDraft || mutatingForm.value) return
    scheduleDraftSave()
  },
  { deep: true }
)

watch(
  () => draftStorageKey.value,
  () => {
    ignoredRestoreToken.value = ''
    scheduleRestorePromptCheck()
  }
)

onMounted(() => {
  if (!props.enableDraft) return
  cleanupExpiredDraftsForUser(authStore.profile?.id ?? 'anonymous')
  scheduleRestorePromptCheck()
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', onWindowBlur)
  window.addEventListener('beforeunload', onBeforeUnload)
  document.addEventListener('mousedown', onDocumentPointerDown, true)
})

onBeforeUnmount(() => {
  if (!props.enableDraft) return
  flushDraftSave()
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('blur', onWindowBlur)
  window.removeEventListener('beforeunload', onBeforeUnload)
  document.removeEventListener('mousedown', onDocumentPointerDown, true)

  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer)
    draftSaveTimer = null
  }
  if (restoreCheckTimer) {
    clearTimeout(restoreCheckTimer)
    restoreCheckTimer = null
  }
  resetRestoreAutoHideTimer()
  resetStatusHideTimer()
})
</script>

<style scoped>
.question-form-wrap {
  position: relative;
}

.form-head-actions {
  display: flex;
  justify-content: flex-end;
  margin: 0 0 8px;
}

.ai-float-btn {
  position: fixed;
  right: 96px;
  top: 52%;
  transform: translateY(-50%);
  border: 1px solid rgba(255, 236, 222, 0.78);
  border-radius: 999px;
  padding: 10px 18px;
  background:
    radial-gradient(circle at 28% 22%, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.4) 34%, rgba(255, 255, 255, 0) 60%),
    linear-gradient(145deg, #df9b7f 0%, #c9704f 48%, #b65a3b 100%);
  color: #fffaf6;
  font-weight: 600;
  letter-spacing: 0.3px;
  text-shadow: 0 1px 2px rgba(67, 33, 20, 0.38);
  cursor: pointer;
  box-shadow:
    inset 0 2px 6px rgba(255, 255, 255, 0.55),
    inset 0 -10px 14px rgba(146, 79, 51, 0.24),
    0 12px 24px rgba(57, 33, 22, 0.26);
  overflow: hidden;
  isolation: isolate;
  z-index: 310;
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.ai-float-btn::before {
  content: '';
  position: absolute;
  top: 11%;
  left: 12%;
  width: 46%;
  height: 36%;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86) 0%, rgba(255, 255, 255, 0.08) 100%);
  transform: rotate(-11deg);
  pointer-events: none;
}

.ai-float-btn:hover {
  transform: translateY(calc(-50% - 1px));
  box-shadow:
    inset 0 2px 7px rgba(255, 255, 255, 0.62),
    inset 0 -10px 16px rgba(126, 68, 43, 0.3),
    0 16px 30px rgba(44, 25, 17, 0.3);
  filter: saturate(1.05);
}

:global(:root[data-theme='dark']) .ai-float-btn {
  border-color: rgba(241, 196, 172, 0.42);
  background:
    radial-gradient(circle at 30% 20%, rgba(247, 224, 210, 0.36) 0%, rgba(247, 224, 210, 0.08) 38%, rgba(247, 224, 210, 0) 60%),
    linear-gradient(145deg, #73412d 0%, #6a3a29 48%, #572f20 100%);
  box-shadow:
    inset 0 2px 5px rgba(245, 222, 207, 0.24),
    inset 0 -8px 12px rgba(41, 21, 14, 0.48),
    0 12px 26px rgba(0, 0, 0, 0.42);
}

.upload-wrap {
  margin-top: 8px;
}

.attachment-list {
  margin-top: 8px;
  border: 1px solid var(--border-soft);
  border-radius: 8px;
  padding: 8px 10px;
  background: var(--surface-soft);
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.8;
}

.draft-restore-card {
  position: fixed;
  top: 110px;
  left: 50%;
  width: 300px;
  transform: translateX(-50%);
  border-radius: 8px;
  border: 1px solid var(--border-soft);
  padding: 12px;
  z-index: 320;
}

.draft-restore-card h4 {
  margin: 0;
  font-size: 15px;
  color: var(--primary);
}

.draft-restore-card p {
  margin: 8px 0 0;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.6;
}

.draft-restore-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.draft-status-tip {
  position: fixed;
  right: 24px;
  bottom: 16px;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid rgba(193, 185, 170, 0.5);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.2;
  z-index: 320;
}

.draft-popup-fade-enter-active,
.draft-popup-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.draft-popup-fade-enter-from,
.draft-popup-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -3px);
}

.draft-status-fade-enter-active,
.draft-status-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.draft-status-fade-enter-from,
.draft-status-fade-leave-to {
  opacity: 0;
  transform: translateY(3px);
}

:global(:root[data-theme='dark']) .draft-status-tip {
  background: rgba(30, 30, 27, 0.94);
  border-color: rgba(97, 97, 90, 0.5);
  color: #b0aea5;
}

@media (max-width: 1200px) {
  .ai-float-btn {
    right: 16px;
    top: auto;
    bottom: 110px;
    transform: none;
  }

  .ai-float-btn:hover {
    transform: translateY(-1px);
  }
}

@media (max-width: 768px) {
  .draft-restore-card {
    width: calc(100% - 24px);
    top: 96px;
  }

  .draft-status-tip {
    right: 12px;
    bottom: 12px;
  }
}
</style>
