<template>
  <div class="note-form-wrap">
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

    <el-form ref="formRef" :model="form" :rules="rules" label-width="112px" @submit.prevent @focusout="onEditorFocusOut">
      <el-form-item label="笔记标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入笔记标题" />
      </el-form-item>

      <el-form-item label="笔记内容" prop="content">
        <el-tabs v-model="editorTab" class="editor-tabs">
          <el-tab-pane label="编辑" name="write">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="16"
              placeholder="支持 Markdown：代码块、标题、列表、链接"
            />
          </el-tab-pane>
          <el-tab-pane label="预览" name="preview">
            <div class="md-preview" v-html="previewHtml"></div>
            <el-empty v-if="!form.content?.trim()" description="暂无内容" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
        <p class="helper">快捷键：按 <code>Ctrl+S</code> 可直接保存。</p>
      </el-form-item>

      <el-form-item label="版本摘要">
        <el-input
          v-model="form.versionSummary"
          maxlength="120"
          show-word-limit
          placeholder="可选，不填则默认提取内容前50字"
        />
      </el-form-item>

      <el-form-item label="编程语言" prop="language">
        <el-select v-model="form.language" placeholder="请选择" style="width: 100%">
          <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="笔记封面">
        <CoverPicker
          v-model="form.coverPath"
          biz-type="note"
          preview-label="笔记封面预览"
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

      <el-form-item label="关联错题">
        <el-select
          v-model="form.manualQuestionIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="可选"
          style="width: 100%"
        >
          <el-option
            v-for="question in questionOptions"
            :key="question.id"
            :label="question.title"
            :value="question.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="关联资料">
        <el-select
          v-model="form.manualMaterialIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="可选"
          style="width: 100%"
        >
          <el-option
            v-for="material in materialOptions"
            :key="material.id"
            :label="material.title"
            :value="material.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ submitText }}</el-button>
        <el-button @click="$emit('cancel')">取消</el-button>
      </el-form-item>
    </el-form>

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
import { buildDraftStorageKey, cleanupExpiredDraftsForUser, formatDraftSavedAt, readDraftRecord, removeDraftRecord, saveDraftRecord } from '../../utils/draft'
import { renderMarkdownSafe } from '../../utils/markdown'
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
  questionOptions: {
    type: Array,
    default: () => []
  },
  materialOptions: {
    type: Array,
    default: () => []
  },
  submitText: {
    type: String,
    default: '保存'
  },
  submitting: {
    type: Boolean,
    default: false
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

const DRAFT_BIZ_TYPE = 'NOTE'
const AUTO_SAVE_DELAY_MS = 300
const DRAFT_PROMPT_AUTO_HIDE_MS = 30000
const DRAFT_STATUS_AUTO_HIDE_MS = 3000
const STORAGE_ERROR_MESSAGE_INTERVAL_MS = 5000

const authStore = useAuthStore()

const formRef = ref(null)
const editorTab = ref('write')
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

const form = reactive({
  title: '',
  content: '',
  versionSummary: '',
  language: '',
  coverPath: '',
  tagNames: [],
  manualQuestionIds: [],
  manualMaterialIds: []
})

const rules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写笔记内容', trigger: 'blur' }],
  language: [{ required: true, message: '请选择语言', trigger: 'change' }],
  tagNames: [{ type: 'array', required: true, min: 1, message: '至少选择一个标签', trigger: 'change' }]
}

const previewHtml = computed(() => renderMarkdownSafe(form.content))
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

function normalizeFormValue(value) {
  return {
    title: value?.title || '',
    content: value?.content || '',
    versionSummary: value?.versionSummary || '',
    language: value?.language || '',
    coverPath: value?.coverPath || '',
    tagNames: Array.isArray(value?.tagNames) ? [...value.tagNames] : [],
    manualQuestionIds: Array.isArray(value?.manualQuestionIds) ? [...value.manualQuestionIds] : [],
    manualMaterialIds: Array.isArray(value?.manualMaterialIds) ? [...value.manualMaterialIds] : []
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

function normalizeSubmitPayload() {
  return {
    title: form.title.trim(),
    content: form.content,
    versionSummary: (form.versionSummary || '').trim(),
    language: form.language,
    coverPath: form.coverPath || '',
    tagNames: form.tagNames.map((item) => item.trim()).filter(Boolean),
    manualQuestionIds: (form.manualQuestionIds || []).filter((id) => id !== null && id !== undefined),
    manualMaterialIds: (form.manualMaterialIds || []).filter((id) => id !== null && id !== undefined)
  }
}

function onSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    const payload = normalizeSubmitPayload()
    emit('submit', payload, {
      clearDraft,
      syncDraft: (nextPayload) => syncDraft(nextPayload || payload),
      promoteDraft: (nextBizId, nextPayload) => promoteDraft(nextBizId, nextPayload || payload)
    })
  })
}

function onKeydown(event) {
  const isSave = (event.ctrlKey || event.metaKey) && String(event.key).toLowerCase() === 's'
  if (!isSave) return
  event.preventDefault()
  onSubmit()
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
    content: form.content,
    versionSummary: form.versionSummary,
    language: form.language,
    coverPath: form.coverPath,
    tagNames: form.tagNames,
    manualQuestionIds: form.manualQuestionIds,
    manualMaterialIds: form.manualMaterialIds
  }),
  () => {
    if (!props.enableDraft || mutatingForm.value) return
    scheduleDraftSave()
  },
  { deep: true }
)

watch(editorTab, () => {
  if (!props.enableDraft) return
  flushDraftSave()
})

watch(
  () => draftStorageKey.value,
  () => {
    ignoredRestoreToken.value = ''
    scheduleRestorePromptCheck()
  }
)

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  if (!props.enableDraft) return
  cleanupExpiredDraftsForUser(authStore.profile?.id ?? 'anonymous')
  scheduleRestorePromptCheck()
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', onWindowBlur)
  window.addEventListener('beforeunload', onBeforeUnload)
  document.addEventListener('mousedown', onDocumentPointerDown, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
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
.note-form-wrap {
  position: relative;
}

.form-head-actions {
  display: flex;
  justify-content: flex-end;
  margin: 0 0 8px;
}

.editor-tabs {
  width: 100%;
}

.md-preview {
  min-height: 280px;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  padding: 12px;
  background: var(--surface-soft);
  line-height: 1.7;
}

.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3),
.md-preview :deep(h4),
.md-preview :deep(h5),
.md-preview :deep(h6) {
  margin: 10px 0 8px;
  color: var(--primary);
}

.md-preview :deep(p) {
  margin: 8px 0;
}

.md-preview :deep(ul),
.md-preview :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.md-preview :deep(code) {
  background: rgba(30, 64, 175, 0.12);
  border-radius: 4px;
  padding: 2px 6px;
  font-family: "Consolas", "Courier New", monospace;
}

.md-preview :deep(.md-code) {
  margin: 10px 0;
  background: #1f2937;
  color: #f9fafb;
  border-radius: 8px;
  padding: 10px;
  overflow-x: auto;
}

.md-preview :deep(.md-code code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.helper {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.helper code {
  background: rgba(30, 64, 175, 0.12);
  border-radius: 4px;
  padding: 1px 4px;
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
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(255, 255, 255, 0.94);
  color: #94a3b8;
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
  background: rgba(15, 23, 42, 0.94);
  border-color: rgba(100, 116, 139, 0.5);
  color: #94a3b8;
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
