
<template>
  <div class="note-compose" :class="{ 'is-fullscreen': isFullscreen }">
    <header class="compose-toolbar">
      <div class="toolbar-main">
        <div class="toolbar-group">
          <el-tooltip :content="isFullscreen ? '退出全屏' : '进入全屏'" placement="bottom">
            <button class="tool-icon-btn" type="button" @click="onFullscreenAction">
              <el-icon v-if="isFullscreen"><ScaleToOriginal /></el-icon>
              <el-icon v-else><FullScreen /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="撤销" placement="bottom">
            <button class="tool-icon-btn" type="button" :disabled="!editor?.can().chain().focus().undo().run()" @click="editor?.chain().focus().undo().run()">
              <el-icon><RefreshLeft /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="重做" placement="bottom">
            <button class="tool-icon-btn" type="button" :disabled="!editor?.can().chain().focus().redo().run()" @click="editor?.chain().focus().redo().run()">
              <el-icon><RefreshRight /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="清除格式" placement="bottom">
            <button class="tool-icon-btn" type="button" @click="clearFormat">
              <el-icon><Brush /></el-icon>
            </button>
          </el-tooltip>
        </div>

        <div class="toolbar-group">
          <el-select
            v-model="textTypeValue"
            class="tool-select"
            size="small"
            :teleported="true"
            @change="setTextType"
          >
            <el-option v-for="item in textTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <el-select
            v-model="fontSizeValue"
            class="tool-select font-size-select"
            size="small"
            :teleported="true"
            @change="setFontSize"
          >
            <el-option v-for="item in fontSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <div class="toolbar-group">
          <el-tooltip content="加粗" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('bold') }" @click="editor?.chain().focus().toggleBold().run()">
              <span class="glyph-icon">B</span>
            </button>
          </el-tooltip>
          <el-tooltip content="斜体" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('italic') }" @click="editor?.chain().focus().toggleItalic().run()">
              <span class="glyph-icon">I</span>
            </button>
          </el-tooltip>
          <el-tooltip content="下划线" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('underline') }" @click="editor?.chain().focus().toggleUnderline().run()">
              <span class="glyph-icon glyph-under">U</span>
            </button>
          </el-tooltip>
          <el-tooltip content="删除线" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('strike') }" @click="editor?.chain().focus().toggleStrike().run()">
              <span class="glyph-icon glyph-strike">S</span>
            </button>
          </el-tooltip>
        </div>

        <div class="toolbar-group color-group">
          <el-color-picker
            v-model="textColorValue"
            size="small"
            :show-alpha="false"
            :predefine="presetColors"
            @change="setTextColor"
          />
          <el-color-picker
            v-model="highlightColorValue"
            size="small"
            :show-alpha="false"
            :predefine="presetHighlightColors"
            @change="setHighlightColor"
          />
        </div>

        <div class="toolbar-group">
          <el-tooltip content="左对齐" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive({ textAlign: 'left' }) }" @click="editor?.chain().focus().setTextAlign('left').run()">
              <span class="glyph-icon">L</span>
            </button>
          </el-tooltip>
          <el-tooltip content="居中" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive({ textAlign: 'center' }) }" @click="editor?.chain().focus().setTextAlign('center').run()">
              <span class="glyph-icon">C</span>
            </button>
          </el-tooltip>
          <el-tooltip content="右对齐" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive({ textAlign: 'right' }) }" @click="editor?.chain().focus().setTextAlign('right').run()">
              <span class="glyph-icon">R</span>
            </button>
          </el-tooltip>
          <el-tooltip content="两端对齐" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive({ textAlign: 'justify' }) }" @click="editor?.chain().focus().setTextAlign('justify').run()">
              <span class="glyph-icon">J</span>
            </button>
          </el-tooltip>
        </div>

        <div class="toolbar-group">
          <el-tooltip content="无序列表" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('bulletList') }" @click="editor?.chain().focus().toggleBulletList().run()">
              <el-icon><List /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="有序列表" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('orderedList') }" @click="editor?.chain().focus().toggleOrderedList().run()">
              <span class="glyph-icon">1.</span>
            </button>
          </el-tooltip>
          <el-tooltip content="任务列表" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('taskList') }" @click="editor?.chain().focus().toggleTaskList().run()">
              <el-icon><Checked /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="缩进" placement="bottom">
            <button class="tool-icon-btn" type="button" @click="sinkListItem">
              <el-icon><DArrowRight /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="减少缩进" placement="bottom">
            <button class="tool-icon-btn" type="button" @click="liftListItem">
              <el-icon><DArrowLeft /></el-icon>
            </button>
          </el-tooltip>
        </div>

        <div class="toolbar-group">
          <el-tooltip content="引用" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('blockquote') }" @click="editor?.chain().focus().toggleBlockquote().run()">
              <el-icon><ChatLineSquare /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="代码块" placement="bottom">
            <button class="tool-icon-btn" type="button" :class="{ active: editor?.isActive('codeBlock') }" @click="toggleCodeBlock">
              <el-icon><Cpu /></el-icon>
            </button>
          </el-tooltip>
          <el-select
            v-if="!isFullscreen"
            v-model="codeLanguageValue"
            class="tool-select code-language-select"
            size="small"
            :teleported="true"
            @change="setCodeLanguage"
          >
            <el-option v-for="item in codeModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select
            v-else
            v-model="moreTextStyleValue"
            class="tool-select more-text-style-select"
            size="small"
            placeholder="更多文本样式"
            :teleported="true"
            @change="setMoreTextStyle"
          >
            <el-option v-for="item in moreTextStyleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <div class="toolbar-group">
          <el-tooltip content="插入链接" placement="bottom">
            <button class="tool-icon-btn" type="button" @click="insertLink">
              <el-icon><LinkIcon /></el-icon>
            </button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="onImageCommand">
            <button class="tool-icon-btn" type="button">
              <el-icon><Picture /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="local">本地图片</el-dropdown-item>
                <el-dropdown-item command="url">图片 URL</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click" @command="onFragmentTypeCommand">
            <button class="tool-icon-btn" type="button">
              <el-icon><DocumentAdd /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="question">导入错题片段</el-dropdown-item>
                <el-dropdown-item command="material">导入资料片段</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-tooltip content="更多设置" placement="bottom">
            <button class="tool-icon-btn" type="button" @click="advancedDialogVisible = true">
              <el-icon><Setting /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>

      <div class="toolbar-actions">
        <el-dropdown v-if="enableDraft" trigger="click" @command="onMoreCommand">
          <button class="tool-more-btn" type="button">更多</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="clear-draft">清理草稿</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-tooltip content="返回" placement="bottom">
          <button class="tool-icon-btn" type="button" @click="$emit('cancel')">
            <el-icon><Back /></el-icon>
          </button>
        </el-tooltip>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ submitText }}</el-button>
      </div>
    </header>
    <input
      ref="localImageInputRef"
      class="local-image-input"
      type="file"
      accept="image/png,image/jpeg,image/jpg,image/gif,image/webp,image/bmp"
      @change="onLocalImageSelected"
    />

    <main class="compose-main" @dblclick="onEditorDoubleClick">
      <div class="compose-inner">
        <input
          v-model="form.title"
          class="title-input"
          placeholder="请输入标题"
          maxlength="120"
          @focusout="onEditorFocusOut"
        />

        <EditorContent
          v-if="editor"
          :editor="editor"
          class="editor-surface"
          @focusout="onEditorFocusOut"
        />
      </div>
    </main>

    <footer class="compose-footer">
      <div class="immersive-meta">
        <el-select
          v-model="form.language"
          class="footer-select language-select"
          clearable
          filterable
          placeholder="选择编程语言"
          :teleported="false"
          @change="onEditorFocusOut"
        >
          <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select
          v-model="form.tagNames"
          class="footer-select tag-select"
          multiple
          collapse-tags
          collapse-tags-tooltip
          filterable
          allow-create
          default-first-option
          placeholder="选择知识点标签"
          :teleported="false"
          @change="onEditorFocusOut"
        >
          <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.name" :value="tag.name" />
        </el-select>
      </div>

      <div class="word-count-wrap">
        <span class="word-count">{{ wordCount }}字</span>
      </div>
    </footer>
    <transition name="draft-popup-fade">
      <div v-if="restorePromptVisible" ref="restorePromptRef" class="draft-restore-card surface-card">
        <h4>检测到草稿</h4>
        <p>发现未保存草稿（{{ restorePromptSavedAt }}），是否恢复？</p>
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

    <el-dialog
      v-model="fragmentPickerVisible"
      :title="fragmentPickerType === 'question' ? '导入错题片段' : '导入资料片段'"
      width="560px"
    >
      <el-input
        v-model="fragmentPickerKeyword"
        clearable
        :placeholder="fragmentPickerType === 'question' ? '按错题标题或 ID 搜索' : '按资料标题或 ID 搜索'"
      />
      <el-scrollbar max-height="320px" class="fragment-picker-scrollbar">
        <el-empty
          v-if="!filteredFragmentOptions.length"
          :description="fragmentPickerType === 'question' ? '暂无可导入错题' : '暂无可导入资料'"
          :image-size="64"
        />
        <div v-else class="fragment-picker-list">
          <button
            v-for="item in filteredFragmentOptions"
            :key="item.id"
            class="fragment-picker-item"
            type="button"
            :class="{ active: Number(fragmentPickerSelectedId) === Number(item.id) }"
            @click="fragmentPickerSelectedId = Number(item.id)"
          >
            <span class="fragment-picker-title">
              {{ item.title || (fragmentPickerType === 'question' ? `错题 #${item.id}` : `资料 #${item.id}`) }}
            </span>
            <span class="fragment-picker-meta">
              #{{ item.id }}
              <span v-if="item.language"> · {{ item.language }}</span>
            </span>
          </button>
        </div>
      </el-scrollbar>
      <template #footer>
        <el-button @click="closeFragmentPicker">取消</el-button>
        <el-button type="primary" :disabled="!fragmentPickerSelectedId" @click="confirmFragmentPicker">
          插入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="advancedDialogVisible" title="高级设置" width="620px">
      <el-form label-width="96px">
        <el-form-item label="版本摘要">
          <el-input
            v-model="form.versionSummary"
            maxlength="120"
            show-word-limit
            placeholder="选填，不填会自动截取内容"
          />
        </el-form-item>

        <el-form-item label="笔记封面">
          <CoverPicker
            v-model="form.coverPath"
            biz-type="note"
            preview-label="笔记封面预览"
          />
        </el-form-item>

        <el-form-item label="关联错题">
          <el-select
            v-model="form.manualQuestionIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="可多选"
            style="width: 100%"
          >
            <el-option
              v-for="question in questionOptions"
              :key="question.id"
              :label="question.title || `错题 #${question.id}`"
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
            placeholder="可多选"
            style="width: 100%"
          >
            <el-option
              v-for="material in materialOptions"
              :key="material.id"
              :label="material.title || `资料 #${material.id}`"
              :value="material.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advancedDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  Brush,
  ChatLineSquare,
  Checked,
  Cpu,
  DArrowLeft,
  DArrowRight,
  DocumentAdd,
  FullScreen,
  Link as LinkIcon,
  List,
  Picture,
  RefreshLeft,
  RefreshRight,
  ScaleToOriginal,
  Setting
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { Extension, Mark, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import { TextStyle } from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import TextAlign from '@tiptap/extension-text-align'
import TiptapLink from '@tiptap/extension-link'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import Placeholder from '@tiptap/extension-placeholder'
import Image from '@tiptap/extension-image'
import CharacterCount from '@tiptap/extension-character-count'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { createLowlight } from 'lowlight'
import { common } from 'lowlight'
import { useAuthStore } from '../../store/auth'
import {
  buildDraftStorageKey,
  cleanupExpiredDraftsForUser,
  formatDraftSavedAt,
  readDraftRecord,
  removeDraftRecord,
  saveDraftRecord
} from '../../utils/draft'
import { uploadCoverImage } from '../../api/file'
import { renderMarkdownSafe } from '../../utils/markdown'
import { toZhMaterialTypeLabel } from '../../utils/material'
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
  },
  forceFullscreen: {
    type: Boolean,
    default: false
  },
  fullscreenTargetPath: {
    type: String,
    default: ''
  },
  fullscreenExitPath: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['submit', 'cancel'])

const DRAFT_BIZ_TYPE = 'NOTE'
const AUTO_SAVE_DELAY_MS = 30000
const DRAFT_PROMPT_AUTO_HIDE_MS = 30000
const DRAFT_STATUS_AUTO_HIDE_MS = 3000
const STORAGE_ERROR_MESSAGE_INTERVAL_MS = 5000
const BODY_FULLSCREEN_CLASS = 'note-editor-fullscreen-lock'

const DEFAULT_TEXT_TYPE = 'paragraph'
const DEFAULT_FONT_SIZE = '16px'
const DEFAULT_TEXT_COLOR = '#141413'
const DEFAULT_HIGHLIGHT_COLOR = '#ffe89a'
const DEFAULT_CODE_MODE = 'paragraph'
const MORE_TEXT_STYLE_PLACEHOLDER = ''

const textTypeOptions = [
  { label: '正文', value: 'paragraph' },
  { label: '标题1', value: 'heading-1' },
  { label: '标题2', value: 'heading-2' },
  { label: '标题3', value: 'heading-3' },
  { label: '标题4', value: 'heading-4' },
  { label: '标题5', value: 'heading-5' },
  { label: '标题6', value: 'heading-6' }
]

const fontSizeOptions = [
  { label: '12px', value: '12px' },
  { label: '13px', value: '13px' },
  { label: '14px', value: '14px' },
  { label: '15px', value: '15px' },
  { label: '16px', value: '16px' },
  { label: '19px', value: '19px' },
  { label: '22px', value: '22px' },
  { label: '24px', value: '24px' },
  { label: '29px', value: '29px' },
  { label: '32px', value: '32px' },
  { label: '40px', value: '40px' },
  { label: '48px', value: '48px' }
]

const codeModeOptions = [
  { label: '正文', value: 'paragraph' },
  { label: '纯文本', value: 'plaintext' },
  { label: 'Java', value: 'java' },
  { label: 'JavaScript', value: 'javascript' },
  { label: 'TypeScript', value: 'typescript' },
  { label: 'Python', value: 'python' },
  { label: 'C', value: 'c' },
  { label: 'C++', value: 'cpp' },
  { label: 'Go', value: 'go' },
  { label: 'SQL', value: 'sql' },
  { label: 'JSON', value: 'json' },
  { label: 'XML', value: 'xml' },
  { label: 'Bash', value: 'bash' }
]

const moreTextStyleOptions = [
  { label: '上标', value: 'superscript' },
  { label: '下标', value: 'subscript' },
  { label: '行内代码', value: 'inline-code' }
]

const presetColors = ['#2f2f2f', '#c96442', '#286f6c', '#385a8f', '#704f9f', '#b83280', '#8f3d3d']
const presetHighlightColors = ['#ffe89a', '#ffe0be', '#d9f6cb', '#cde9ff', '#ecd5ff', '#ffd7e9', '#ffffff']

const Superscript = Mark.create({
  name: 'superscript',
  excludes: 'subscript',

  parseHTML() {
    return [{ tag: 'sup' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['sup', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      toggleSuperscript:
        () =>
          ({ commands }) =>
            commands.toggleMark(this.name)
    }
  }
})

const Subscript = Mark.create({
  name: 'subscript',
  excludes: 'superscript',

  parseHTML() {
    return [{ tag: 'sub' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['sub', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      toggleSubscript:
        () =>
          ({ commands }) =>
            commands.toggleMark(this.name)
    }
  }
})

const FontSize = Extension.create({
  name: 'fontSize',

  addOptions() {
    return {
      types: ['textStyle']
    }
  },

  addGlobalAttributes() {
    return [
      {
        types: this.options.types,
        attributes: {
          fontSize: {
            default: null,
            parseHTML: (element) => element.style.fontSize || null,
            renderHTML: (attributes) => {
              if (!attributes.fontSize) {
                return {}
              }
              return {
                style: `font-size: ${attributes.fontSize}`
              }
            }
          }
        }
      }
    ]
  },

  addCommands() {
    return {
      setFontSize:
        (fontSize) =>
          ({ chain }) =>
            chain()
              .setMark('textStyle', { fontSize })
              .run(),
      unsetFontSize:
        () =>
          ({ chain }) =>
            chain()
              .setMark('textStyle', { fontSize: null })
              .removeEmptyTextStyle()
              .run()
    }
  }
})

const lowlight = createLowlight(common)
const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const restorePromptRef = ref(null)
const localImageInputRef = ref(null)
const advancedDialogVisible = ref(false)
const fragmentPickerVisible = ref(false)
const fragmentPickerType = ref('question')
const fragmentPickerKeyword = ref('')
const fragmentPickerSelectedId = ref(null)
const isFullscreen = ref(Boolean(props.forceFullscreen))

const restorePromptVisible = ref(false)
const restorePromptSavedAt = ref('')
const pendingRestoreDraft = ref(null)
const ignoredRestoreToken = ref('')

const draftStatus = ref('idle')
const draftStatusVisible = ref(false)
const draftStatusHovering = ref(false)
const mutatingForm = ref(false)
const syncingFromEditor = ref(false)
const syncingToEditor = ref(false)

const textTypeValue = ref(DEFAULT_TEXT_TYPE)
const fontSizeValue = ref(DEFAULT_FONT_SIZE)
const textColorValue = ref(DEFAULT_TEXT_COLOR)
const highlightColorValue = ref(DEFAULT_HIGHLIGHT_COLOR)
const codeLanguageValue = ref(DEFAULT_CODE_MODE)
const moreTextStyleValue = ref(MORE_TEXT_STYLE_PLACEHOLDER)

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

function looksLikeHtml(value) {
  return /<\/?[a-z][\s\S]*>/i.test(String(value || ''))
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

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

function contentToEditorHtml(content) {
  const raw = String(content || '').trim()
  if (!raw) {
    return '<p></p>'
  }
  if (looksLikeHtml(raw)) {
    return raw
  }
  return renderMarkdownSafe(raw)
}

function setBodyFullscreen(value) {
  if (typeof document === 'undefined') return
  document.body.classList.toggle(BODY_FULLSCREEN_CLASS, value)
}

watch(
  () => isFullscreen.value,
  (value) => {
    setBodyFullscreen(value)
  }
)

const currentFragmentOptions = computed(() =>
  fragmentPickerType.value === 'question' ? (props.questionOptions || []) : (props.materialOptions || [])
)

const filteredFragmentOptions = computed(() => {
  const keyword = String(fragmentPickerKeyword.value || '').trim().toLowerCase()
  if (!keyword) {
    return currentFragmentOptions.value.slice(0, 200)
  }
  return currentFragmentOptions.value
    .filter((item) => {
      const title = String(item?.title || '').toLowerCase()
      const idText = String(item?.id || '').toLowerCase()
      return title.includes(keyword) || idText.includes(keyword)
    })
    .slice(0, 200)
})

const draftStorageKey = computed(() => buildDraftStorageKey({
  userId: authStore.profile?.id ?? 'anonymous',
  bizType: DRAFT_BIZ_TYPE,
  bizId: props.draftKey || 'new'
}))

const draftStatusText = computed(() => {
  if (draftStatus.value === 'saving') return '自动保存草稿中...'
  if (draftStatus.value === 'saved') return '草稿已自动保存'
  if (draftStatus.value === 'failed') return '草稿保存失败，请手动保存'
  return ''
})

const wordCount = computed(() => {
  if (!editor.value) {
    return 0
  }
  const count = editor.value.storage.characterCount?.characters?.()
  return Number.isFinite(count) ? count : 0
})

function syncToolbarState() {
  if (!editor.value) return
  const instance = editor.value
  const textStyleAttrs = editor.value.getAttributes('textStyle') || {}
  const highlightAttrs = editor.value.getAttributes('highlight') || {}
  const codeAttrs = editor.value.getAttributes('codeBlock') || {}

  const activeHeading = [1, 2, 3, 4, 5, 6].find((level) => instance.isActive('heading', { level }))
  textTypeValue.value = activeHeading ? `heading-${activeHeading}` : DEFAULT_TEXT_TYPE
  fontSizeValue.value = textStyleAttrs.fontSize || DEFAULT_FONT_SIZE
  textColorValue.value = textStyleAttrs.color || DEFAULT_TEXT_COLOR
  highlightColorValue.value = highlightAttrs.color || DEFAULT_HIGHLIGHT_COLOR
  if (instance.isActive('codeBlock')) {
    codeLanguageValue.value = codeAttrs.language || 'plaintext'
  } else {
    codeLanguageValue.value = DEFAULT_CODE_MODE
  }
}

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      codeBlock: false
    }),
    CodeBlockLowlight.configure({ lowlight }),
    TextStyle,
    FontSize,
    Color,
    Highlight.configure({ multicolor: true }),
    Underline,
    Superscript,
    Subscript,
    TiptapLink.configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
      defaultProtocol: 'https'
    }),
    TextAlign.configure({
      types: ['heading', 'paragraph']
    }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Image,
    Placeholder.configure({
      placeholder: '开始记录你的错题思路、代码片段和总结...'
    }),
    CharacterCount
  ],
  content: contentToEditorHtml(form.content),
  editorProps: {
    attributes: {
      class: 'note-editor-content',
      spellcheck: 'false'
    }
  },
  onCreate: ({ editor: instance }) => {
    syncingToEditor.value = true
    instance.commands.setContent(contentToEditorHtml(form.content), false)
    syncingToEditor.value = false
    syncToolbarState()
  },
  onSelectionUpdate: () => {
    syncToolbarState()
  },
  onUpdate: ({ editor: instance }) => {
    if (syncingToEditor.value) {
      return
    }
    syncingFromEditor.value = true
    form.content = instance.getHTML()
    syncingFromEditor.value = false
    syncToolbarState()
  }
})

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
      ElMessage.error('草稿保存失败，请手动保存')
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

function applyEditorContent(content) {
  if (!editor.value) return
  syncingToEditor.value = true
  editor.value.commands.setContent(contentToEditorHtml(content), false)
  syncingToEditor.value = false
  syncToolbarState()
}

function restoreDraftContent() {
  if (!pendingRestoreDraft.value) return
  const restored = normalizeFormValue(pendingRestoreDraft.value.payload)
  mutatingForm.value = true
  Object.assign(form, restored)
  mutatingForm.value = false
  applyEditorContent(restored.content)
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
      ElMessage.error('草稿保存失败，请手动保存')
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

function hasTextContent(htmlText) {
  return String(htmlText || '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .trim().length > 0
}

function validateSubmitPayload(payload) {
  if (!payload.title) {
    ElMessage.warning('请填写笔记标题')
    return false
  }
  if (!hasTextContent(payload.content)) {
    ElMessage.warning('请填写笔记内容')
    return false
  }
  if (!payload.language) {
    ElMessage.warning('请选择编程语言')
    return false
  }
  if (!payload.tagNames.length) {
    ElMessage.warning('请至少选择一个知识点标签')
    return false
  }
  return true
}

function onSubmit() {
  const payload = normalizeSubmitPayload()
  if (!validateSubmitPayload(payload)) {
    return
  }
  emit('submit', payload, {
    clearDraft,
    syncDraft: (nextPayload) => syncDraft(nextPayload || payload),
    promoteDraft: (nextBizId, nextPayload) => promoteDraft(nextBizId, nextPayload || payload)
  })
}

async function openFullscreenTarget() {
  const targetPath = String(props.fullscreenTargetPath || '').trim()
  if (!targetPath || isFullscreen.value) {
    return false
  }
  flushDraftSave()
  try {
    await router.push({
      path: targetPath,
      query: { ...route.query }
    })
    return true
  } catch (_) {
    return false
  }
}

async function closeFullscreenTarget() {
  const exitPath = String(props.fullscreenExitPath || '').trim()
  if (!exitPath) {
    return false
  }
  flushDraftSave()
  try {
    await router.push({
      path: exitPath,
      query: { ...route.query }
    })
    return true
  } catch (_) {
    return false
  }
}

function exitFullscreen() {
  closeFullscreenTarget().then((navigated) => {
    if (!navigated) {
      toggleFullscreen(false)
    }
  })
}

function onFullscreenAction() {
  if (isFullscreen.value) {
    exitFullscreen()
    return
  }
  openFullscreenTarget().then((navigated) => {
    if (!navigated) {
      toggleFullscreen(true)
    }
  })
}

function onEditorDoubleClick() {
  if (isFullscreen.value) {
    return
  }
  openFullscreenTarget().then((navigated) => {
    if (!navigated) {
      toggleFullscreen(true)
    }
  })
}

function toggleFullscreen(next) {
  if (typeof next === 'boolean') {
    isFullscreen.value = next
    return
  }
  isFullscreen.value = !isFullscreen.value
}

function clearFormat() {
  if (!editor.value) return
  editor.value.chain().focus().unsetAllMarks().clearNodes().run()
  syncToolbarState()
}

function setTextType(value) {
  if (!editor.value) return
  const rawValue = String(value || DEFAULT_TEXT_TYPE)
  if (rawValue === DEFAULT_TEXT_TYPE) {
    editor.value.chain().focus().setParagraph().run()
  } else {
    const matched = rawValue.match(/^heading-(\d)$/)
    const level = Number(matched?.[1] || 0)
    if (level >= 1 && level <= 6) {
      editor.value.chain().focus().setHeading({ level }).run()
    } else {
      editor.value.chain().focus().setParagraph().run()
    }
  }
  syncToolbarState()
}

function setFontSize(value) {
  if (!editor.value) return
  if (!value || value === DEFAULT_FONT_SIZE) {
    editor.value.chain().focus().unsetFontSize().run()
  } else {
    editor.value.chain().focus().setFontSize(value).run()
  }
  syncToolbarState()
}

function setTextColor(color) {
  if (!editor.value) return
  if (!color || color === DEFAULT_TEXT_COLOR) {
    editor.value.chain().focus().unsetColor().run()
  } else {
    editor.value.chain().focus().setColor(color).run()
  }
  syncToolbarState()
}

function setHighlightColor(color) {
  if (!editor.value) return
  if (!color || color === '#ffffff') {
    editor.value.chain().focus().unsetHighlight().run()
  } else {
    editor.value.chain().focus().setHighlight({ color }).run()
  }
  syncToolbarState()
}

function sinkListItem() {
  if (!editor.value) return
  editor.value.chain().focus().sinkListItem('listItem').run()
}

function liftListItem() {
  if (!editor.value) return
  editor.value.chain().focus().liftListItem('listItem').run()
}

function toggleCodeBlock() {
  if (!editor.value) return
  if (editor.value.isActive('codeBlock')) {
    editor.value.chain().focus().toggleCodeBlock().run()
  } else {
    editor.value.chain().focus().toggleCodeBlock({ language: 'plaintext' }).run()
  }
  syncToolbarState()
}

function setCodeLanguage(language) {
  if (!editor.value) return
  const nextMode = String(language || DEFAULT_CODE_MODE)
  if (nextMode === DEFAULT_CODE_MODE) {
    if (editor.value.isActive('codeBlock')) {
      editor.value.chain().focus().toggleCodeBlock().run()
    }
    syncToolbarState()
    return
  }

  if (editor.value.isActive('codeBlock')) {
    editor.value.chain().focus().setCodeBlock({ language: nextMode }).run()
  } else {
    editor.value.chain().focus().toggleCodeBlock({ language: nextMode }).run()
  }
  syncToolbarState()
}

function setMoreTextStyle(value) {
  if (!editor.value) {
    moreTextStyleValue.value = MORE_TEXT_STYLE_PLACEHOLDER
    return
  }

  const command = String(value || '').trim()
  if (!command) return

  if (command === 'superscript') {
    editor.value.chain().focus().toggleSuperscript().run()
  } else if (command === 'subscript') {
    editor.value.chain().focus().toggleSubscript().run()
  } else if (command === 'inline-code') {
    editor.value.chain().focus().toggleCode().run()
  }

  moreTextStyleValue.value = MORE_TEXT_STYLE_PLACEHOLDER
  syncToolbarState()
}

async function insertLink() {
  if (!editor.value) return
  const previousUrl = editor.value.getAttributes('link').href || ''
  try {
    const { value } = await ElMessageBox.prompt('请输入链接地址（http 或 https）', '插入链接', {
      inputValue: previousUrl,
      confirmButtonText: '插入',
      cancelButtonText: '取消'
    })

    const url = String(value || '').trim()
    if (!url) {
      editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
      return
    }

    if (!/^https?:\/\//i.test(url)) {
      ElMessage.warning('链接地址需以 http:// 或 https:// 开头')
      return
    }

    editor.value
      .chain()
      .focus()
      .extendMarkRange('link')
      .setLink({
        href: url,
        target: '_blank',
        rel: 'noopener noreferrer'
      })
      .run()
  } catch (_) {
    // canceled by user
  }
}

async function insertImageByUrl() {
  if (!editor.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入图片 URL（http 或 https）', '插入图片', {
      confirmButtonText: '插入',
      cancelButtonText: '取消'
    })
    const url = String(value || '').trim()
    if (!/^https?:\/\//i.test(url)) {
      ElMessage.warning('图片地址需以 http:// 或 https:// 开头')
      return
    }
    editor.value.chain().focus().setImage({ src: url, alt: '笔记插图' }).run()
  } catch (_) {
    // canceled by user
  }
}

function onImageCommand(command) {
  if (command === 'url') {
    insertImageByUrl()
    return
  }
  if (command === 'local') {
    if (!localImageInputRef.value) return
    localImageInputRef.value.value = ''
    localImageInputRef.value.click()
  }
}

function openFragmentPicker(type) {
  const nextType = type === 'material' ? 'material' : 'question'
  fragmentPickerType.value = nextType
  fragmentPickerKeyword.value = ''
  const list = nextType === 'question' ? (props.questionOptions || []) : (props.materialOptions || [])
  const firstId = Number(list?.[0]?.id)
  fragmentPickerSelectedId.value = Number.isInteger(firstId) ? firstId : null
  fragmentPickerVisible.value = true
}

function closeFragmentPicker() {
  fragmentPickerVisible.value = false
  fragmentPickerKeyword.value = ''
  fragmentPickerSelectedId.value = null
}

function onFragmentTypeCommand(command) {
  if (command === 'material') {
    openFragmentPicker('material')
    return
  }
  openFragmentPicker('question')
}

function confirmFragmentPicker() {
  const selectedId = Number(fragmentPickerSelectedId.value)
  if (!Number.isInteger(selectedId)) {
    ElMessage.warning(fragmentPickerType.value === 'question' ? '请选择要导入的错题' : '请选择要导入的资料')
    return
  }
  if (fragmentPickerType.value === 'question') {
    insertQuestionByCommand(selectedId)
  } else {
    insertMaterialByCommand(selectedId)
  }
  closeFragmentPicker()
}

function resolveUploadedImageUrl(data, fallbackFileName) {
  if (!data) return ''
  const directUrl = String(data.previewUrl || data.downloadUrl || '').trim()
  if (directUrl) {
    return directUrl
  }

  const path = String(data.path || '').trim()
  if (!path) return ''

  const encodedPath = encodeURIComponent(path)
  const encodedName = encodeURIComponent(String(data.fileName || fallbackFileName || 'image'))
  return `/api/v1/file/download?path=${encodedPath}&name=${encodedName}`
}

async function onLocalImageSelected(event) {
  if (!editor.value) return
  const input = event?.target
  const file = input?.files?.[0]
  if (!file) return

  if (!String(file.type || '').toLowerCase().startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    if (input) input.value = ''
    return
  }

  try {
    const data = await uploadCoverImage(file, 'note')
    const imageSrc = resolveUploadedImageUrl(data, file.name)
    if (!imageSrc) {
      ElMessage.warning('图片上传成功，但未返回可用地址')
      return
    }
    editor.value.chain().focus().setImage({ src: imageSrc, alt: file.name || '本地图片' }).run()
    ElMessage.success('本地图片已插入')
  } catch (_) {
    // upload failed, error handled by interceptor
  } finally {
    if (input) input.value = ''
  }
}

function insertQuestionByCommand(command) {
  const questionId = Number(command)
  if (!Number.isInteger(questionId)) return
  const question = (props.questionOptions || []).find((item) => Number(item.id) === questionId)
  if (!question || !editor.value) return

  const title = String(question.title || `错题 #${question.id}`).trim()
  const language = String(question.language || '').trim()
  const lines = [
    `<h3>错题记录：${escapeHtml(title)}</h3>`,
    `<p><strong>错题ID：</strong>${question.id}</p>`,
    language ? `<p><strong>语言：</strong>${escapeHtml(language)}</p>` : '',
    '<blockquote><p>错误原因：</p><p>修复思路：</p></blockquote>',
    '<p></p>'
  ].filter(Boolean)

  editor.value.chain().focus().insertContent(lines.join('')).run()

  if (!form.manualQuestionIds.includes(questionId)) {
    form.manualQuestionIds = [...form.manualQuestionIds, questionId]
  }
  ElMessage.success('已插入错题片段')
}

function insertMaterialByCommand(command) {
  const materialId = Number(command)
  if (!Number.isInteger(materialId)) return
  const material = (props.materialOptions || []).find((item) => Number(item.id) === materialId)
  if (!material || !editor.value) return

  const title = String(material.title || `资料 #${material.id}`).trim()
  const materialTypeText = toZhMaterialTypeLabel(material.materialType) || String(material.materialType || '').trim()
  const language = String(material.language || '').trim()
  const lines = [
    `<h3>资料摘录：${escapeHtml(title)}</h3>`,
    `<p><strong>资料ID：</strong>${material.id}</p>`,
    materialTypeText ? `<p><strong>资料类型：</strong>${escapeHtml(materialTypeText)}</p>` : '',
    language ? `<p><strong>语言：</strong>${escapeHtml(language)}</p>` : '',
    '<blockquote><p>关键知识点：</p><p>可复用代码/结论：</p></blockquote>',
    '<p></p>'
  ].filter(Boolean)

  editor.value.chain().focus().insertContent(lines.join('')).run()

  if (!form.manualMaterialIds.includes(materialId)) {
    form.manualMaterialIds = [...form.manualMaterialIds, materialId]
  }
  ElMessage.success('已插入资料片段')
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

function shouldIgnoreShortcutTarget(target) {
  if (!target) return false
  const tagName = target.tagName?.toLowerCase?.()
  return tagName === 'input' || tagName === 'select'
}

function onGlobalKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && String(event.key).toLowerCase() === 's') {
    event.preventDefault()
    onSubmit()
    return
  }

  if (event.key === 'Escape' && isFullscreen.value) {
    event.preventDefault()
    exitFullscreen()
    return
  }

  if (shouldIgnoreShortcutTarget(event.target)) {
    return
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

    if (editor.value && !syncingFromEditor.value) {
      applyEditorContent(next.content)
    }

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

watch(
  () => draftStorageKey.value,
  () => {
    ignoredRestoreToken.value = ''
    scheduleRestorePromptCheck()
  }
)

watch(
  () => props.forceFullscreen,
  (value) => {
    if (value) {
      isFullscreen.value = true
    }
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)

  if (!props.enableDraft) return
  cleanupExpiredDraftsForUser(authStore.profile?.id ?? 'anonymous')
  scheduleRestorePromptCheck()
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', onWindowBlur)
  window.addEventListener('beforeunload', onBeforeUnload)
  document.addEventListener('mousedown', onDocumentPointerDown, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
  setBodyFullscreen(false)
  editor.value?.destroy()

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
.note-compose {
  --toolbar-height: 58px;
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100%;
  height: 100%;
  border-radius: 14px;
  background: var(--surface);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow), 0 0 0 1px color-mix(in srgb, var(--color-ring-warm) 62%, transparent);
  overflow: hidden;
}

.note-compose.is-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 420;
  width: 100vw;
  height: 100vh;
  border-radius: 0;
  border: none;
  box-shadow: none;
  background: var(--bg);
}

.compose-toolbar {
  z-index: 30;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: var(--toolbar-height);
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 94%, var(--surface-soft));
  backdrop-filter: blur(8px);
}

.toolbar-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.toolbar-main::-webkit-scrollbar {
  display: none;
}

.toolbar-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding-right: 8px;
  margin-right: 2px;
  border-right: 1px solid var(--border-soft);
}

.toolbar-group:last-child {
  border-right: none;
  padding-right: 0;
  margin-right: 0;
}

.tool-icon-btn {
  border: 1px solid var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-main);
  border-radius: 8px;
  width: 30px;
  min-width: 30px;
  height: 30px;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--color-ring-warm) 58%, transparent);
  padding: 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tool-icon-btn:hover {
  border-color: color-mix(in srgb, var(--primary) 68%, var(--border-soft));
  background: color-mix(in srgb, var(--primary) 13%, var(--surface-soft));
  color: var(--primary);
}

.tool-icon-btn.active {
  border-color: color-mix(in srgb, var(--primary) 72%, var(--border-soft));
  background: color-mix(in srgb, var(--primary) 18%, var(--surface));
  color: color-mix(in srgb, var(--primary) 78%, var(--text-main));
}

.tool-icon-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.tool-icon-btn :deep(.el-icon) {
  font-size: 15px;
}

.glyph-icon {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.glyph-under {
  text-decoration: underline;
}

.glyph-strike {
  text-decoration: line-through;
}

.color-group :deep(.el-color-picker__trigger) {
  border: 1px solid var(--border-soft);
  border-radius: 8px;
}

.tool-select {
  width: 118px;
}

.font-size-select {
  width: 84px;
}

.code-language-select {
  width: 110px;
}

.more-text-style-select {
  width: 126px;
}

.toolbar-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-left: 8px;
  flex-shrink: 0;
}

.tool-more-btn {
  height: 30px;
  border: 1px solid var(--border-soft);
  border-radius: 8px;
  padding: 0 12px;
  background: var(--surface-soft);
  color: var(--text-accent);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--color-ring-warm) 58%, transparent);
  cursor: pointer;
  line-height: 1;
}

.tool-more-btn:hover {
  border-color: color-mix(in srgb, var(--primary) 68%, var(--border-soft));
  background: color-mix(in srgb, var(--primary) 12%, var(--surface-soft));
  color: var(--primary);
}

.compose-main {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
  background: color-mix(in srgb, var(--bg) 80%, var(--surface));
}

.compose-inner {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(1120px, calc(100% - 30px));
  margin: 12px auto;
  padding: 0 0 6px;
  overflow: hidden;
}

.title-input {
  width: 100%;
  border: none;
  border-radius: 8px;
  padding: 4px 8px;
  margin: 2px 0 4px;
  font-size: clamp(24px, 2.6vw, 34px);
  line-height: 1.2;
  font-family: var(--font-serif);
  font-weight: 500;
  color: var(--text-main);
  background: transparent;
  outline: none;
  caret-color: var(--primary);
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.title-input:focus {
  background: color-mix(in srgb, var(--primary) 7%, var(--surface));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary) 52%, transparent);
}

.title-input::placeholder {
  color: color-mix(in srgb, var(--text-sub) 82%, var(--surface));
}

.title-input:not(:placeholder-shown) {
  color: var(--text-main);
}

.editor-surface {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid var(--border-soft);
  background: var(--surface);
  box-shadow: var(--shadow), 0 0 0 1px color-mix(in srgb, var(--color-ring-warm) 58%, transparent);
}

.editor-surface :deep(.note-editor-content) {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 18px 22px;
  outline: none;
  color: var(--text-main);
  font-size: 16px;
  line-height: 1.72;
}

.editor-surface :deep(.note-editor-content::-webkit-scrollbar) {
  display: none;
}

.editor-surface :deep(.note-editor-content p.is-editor-empty:first-child::before) {
  color: color-mix(in srgb, var(--text-sub) 82%, var(--surface));
  content: attr(data-placeholder);
  pointer-events: none;
  float: left;
  height: 0;
}

.editor-surface :deep(h1),
.editor-surface :deep(h2),
.editor-surface :deep(h3),
.editor-surface :deep(h4) {
  margin: 14px 0 8px;
  color: var(--text-main);
}

.editor-surface :deep(p) {
  margin: 8px 0;
}

.editor-surface :deep(ul),
.editor-surface :deep(ol) {
  margin: 10px 0;
  padding-left: 22px;
}

.editor-surface :deep(blockquote) {
  margin: 12px 0;
  border-left: 3px solid color-mix(in srgb, var(--primary) 72%, var(--border-soft));
  padding: 4px 10px;
  color: var(--text-sub);
  background: color-mix(in srgb, var(--surface-soft) 62%, var(--surface));
  border-radius: 8px;
}

.editor-surface :deep(pre) {
  margin: 12px 0;
  border-radius: 10px;
  overflow: auto;
  background: #232320;
  color: #f7f5ee;
  border: 1px solid #3b3a35;
}

.editor-surface :deep(pre code) {
  display: block;
  padding: 14px 16px;
  font-family: 'Cascadia Mono', 'Consolas', monospace;
  line-height: 1.55;
}

.editor-surface :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 10px;
  border: 1px solid var(--border-soft);
}

.editor-surface :deep(a) {
  color: var(--primary);
  text-decoration: underline;
}

.editor-surface :deep(.hljs-comment),
.editor-surface :deep(.hljs-quote) {
  color: #94969f;
}

.editor-surface :deep(.hljs-keyword),
.editor-surface :deep(.hljs-selector-tag),
.editor-surface :deep(.hljs-subst) {
  color: #f47067;
}

.editor-surface :deep(.hljs-number),
.editor-surface :deep(.hljs-literal),
.editor-surface :deep(.hljs-variable),
.editor-surface :deep(.hljs-template-variable),
.editor-surface :deep(.hljs-tag .hljs-attr) {
  color: #d29922;
}

.editor-surface :deep(.hljs-string),
.editor-surface :deep(.hljs-doctag) {
  color: #7ee787;
}

.editor-surface :deep(.hljs-title),
.editor-surface :deep(.hljs-section),
.editor-surface :deep(.hljs-selector-id) {
  color: #79c0ff;
}

.editor-surface :deep(.hljs-type),
.editor-surface :deep(.hljs-class .hljs-title) {
  color: #ffa657;
}

.compose-footer {
  z-index: 25;
  display: grid;
  gap: 8px;
  padding: 8px 12px 10px;
  border-top: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--surface) 95%, var(--surface-soft));
  backdrop-filter: blur(8px);
}

.immersive-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-select {
  min-width: 0;
}

.language-select {
  width: 170px;
}

.tag-select {
  flex: 1;
}

.word-count-wrap {
  display: flex;
  align-items: center;
}

.word-count {
  color: var(--text-sub);
  font-size: 13px;
}

.fragment-picker-scrollbar {
  margin-top: 10px;
}

.fragment-picker-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 6px 4px 2px;
}

.fragment-picker-item {
  width: 100%;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  background: var(--surface);
  color: var(--text-main);
  text-align: left;
  padding: 10px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fragment-picker-item:hover {
  border-color: color-mix(in srgb, var(--primary) 65%, var(--border-soft));
  background: color-mix(in srgb, var(--primary) 8%, var(--surface));
}

.fragment-picker-item.active {
  border-color: color-mix(in srgb, var(--primary) 72%, var(--border-soft));
  background: color-mix(in srgb, var(--primary) 12%, var(--surface));
}

.fragment-picker-title {
  font-size: 14px;
  line-height: 1.4;
  font-weight: 600;
}

.fragment-picker-meta {
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.3;
}

.local-image-input {
  display: none;
}

.draft-restore-card {
  position: fixed;
  top: 92px;
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

:global(body.note-editor-fullscreen-lock) {
  overflow: hidden;
}

:global(:root[data-theme='dark']) .note-compose {
  background: var(--surface);
  border-color: var(--border-soft);
}

:global(:root[data-theme='dark']) .note-compose.is-fullscreen {
  background: var(--bg);
}

:global(:root[data-theme='dark']) .compose-toolbar,
:global(:root[data-theme='dark']) .compose-footer {
  background: color-mix(in srgb, var(--surface) 90%, var(--surface-soft));
  border-color: var(--border-soft);
}

:global(:root[data-theme='dark']) .compose-main {
  background: color-mix(in srgb, var(--bg) 82%, var(--surface));
}

:global(:root[data-theme='dark']) .title-input {
  color: var(--text-main);
}

:global(:root[data-theme='dark']) .title-input::placeholder {
  color: color-mix(in srgb, var(--text-sub) 80%, var(--surface));
}

:global(:root[data-theme='dark']) .editor-surface {
  border-color: var(--border-soft);
  background: var(--surface);
}

:global(:root[data-theme='dark']) .editor-surface :deep(.note-editor-content) {
  color: var(--text-main);
}

:global(:root[data-theme='dark']) .editor-surface :deep(.note-editor-content p.is-editor-empty:first-child::before) {
  color: color-mix(in srgb, var(--text-sub) 80%, var(--surface));
}

:global(:root[data-theme='dark']) .tool-icon-btn {
  border-color: var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-main);
}

:global(:root[data-theme='dark']) .tool-icon-btn.active {
  border-color: color-mix(in srgb, var(--primary) 72%, var(--border-soft));
  background: color-mix(in srgb, var(--primary) 20%, var(--surface));
  color: color-mix(in srgb, var(--primary) 78%, var(--text-main));
}

:global(:root[data-theme='dark']) .tool-more-btn {
  border-color: var(--border-soft);
  background: var(--surface-soft);
  color: var(--text-main);
}

:global(:root[data-theme='dark']) .word-count {
  color: var(--text-sub);
}

:global(:root[data-theme='dark']) .draft-status-tip {
  background: color-mix(in srgb, var(--surface) 94%, transparent);
  border-color: color-mix(in srgb, var(--border-soft) 72%, transparent);
  color: var(--text-sub);
}

@media (max-width: 980px) {
  .compose-toolbar {
    grid-template-columns: minmax(0, 1fr);
    align-items: stretch;
  }

  .toolbar-main {
    width: 100%;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }

  .compose-inner {
    width: calc(100% - 16px);
    margin: 10px auto;
  }

  .immersive-meta {
    flex-direction: column;
    align-items: stretch;
  }

  .language-select,
  .tag-select {
    width: 100%;
  }
}
</style>
