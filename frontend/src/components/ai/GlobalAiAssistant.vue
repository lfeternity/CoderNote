<template>
  <div ref="shellRef" class="float-shell">
    <button
      class="float-fab"
      :class="{ 'is-docked-left': dockState === 'left', 'is-docked-right': dockState === 'right' }"
      type="button"
      aria-label="快捷功能"
      :style="fabStyle"
      @mousedown="onFabMouseDown"
      @mouseenter="onFabMouseEnter"
      @mouseleave="onFabMouseLeave"
      @click="onFabClick"
      @contextmenu.prevent="onFabContextMenu"
    >
      <span class="fab-core">⚡</span>
      <span v-if="authStore.isLoggedIn && reviewBadgeCount > 0" class="fab-badge">
        {{ reviewBadgeCount > 99 ? '99+' : reviewBadgeCount }}
      </span>
    </button>

    <transition name="float-panel-pop">
      <section
        v-if="panelVisible"
        class="float-panel surface-card"
        :style="panelStyle"
        @mouseenter="onPanelMouseEnter"
        @mouseleave="onPanelMouseLeave"
      >
        <header class="float-panel-head">
          <h4>快捷功能面板</h4>
          <el-button link type="primary" @click="closePanel">收起</el-button>
        </header>

        <div class="float-panel-body">
          <section
            v-for="group in FLOAT_BUTTON_GROUPS"
            :key="group.key"
            class="float-group"
          >
            <h5>{{ group.label }}</h5>
            <div class="float-grid">
              <button
                v-for="item in groupedItems[group.key] || []"
                :key="item.key"
                class="float-item"
                type="button"
                @click="handleItemClick(item)"
              >
                <span class="float-item-icon">
                  <el-icon>
                    <component :is="resolveItemIcon(item.icon)" />
                  </el-icon>
                </span>
                <span class="float-item-label">{{ item.label }}</span>
              </button>
            </div>
          </section>
        </div>
      </section>
    </transition>

    <div
      v-if="contextMenuVisible"
      class="fab-context surface-card"
      :style="contextMenuStyle"
      @mousedown.stop
    >
      <button type="button" class="fab-context-item" @click="resetPosition">复位位置</button>
    </div>

    <el-dialog
      v-model="promptDialogVisible"
      :title="promptMode === 'solve' ? 'AI 解题输入' : 'AI 总结输入'"
      width="560px"
      append-to-body
    >
      <el-form label-width="92px">
        <el-form-item label="模型选择">
          <div class="model-select-row">
            <el-select v-model="selectedModel" style="width: 220px">
              <el-option v-for="item in modelOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button plain @click="openModelConfigDialog">模型配置</el-button>
          </div>
        </el-form-item>
        <el-form-item :label="promptMode === 'solve' ? '题目信息' : '总结内容'">
          <el-input
            v-model="promptText"
            type="textarea"
            :rows="8"
            resize="vertical"
            :placeholder="promptMode === 'solve'
              ? '输入题目、报错信息、上下文等内容'
              : '输入需要总结的笔记、资料或代码内容'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="promptDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="aiProcessing" @click="submitAiPrompt">开始生成</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resultDialogVisible"
      :title="promptMode === 'solve' ? 'AI 解题结果' : 'AI 总结结果'"
      width="720px"
      append-to-body
    >
      <div v-loading="aiProcessing" class="ai-result-wrap">
        <div v-if="aiError" class="ai-result-error">
          <span>{{ aiError }}</span>
        </div>
        <AiMarkdownContent v-else-if="aiMarkdown" :content="aiMarkdown" />
        <el-empty v-else description="暂无结果" :image-size="66" />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="copyAiResult">复制结果</el-button>
          <el-button type="primary" @click="resultDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="exportDialogVisible" title="批量导出" width="420px" append-to-body>
      <el-radio-group v-model="exportTarget" class="export-target-group">
        <el-radio-button label="QUESTION">错题 PDF</el-radio-button>
        <el-radio-button label="NOTE">笔记 PDF</el-radio-button>
      </el-radio-group>
      <p class="export-tip">将按当前账号数据导出整库 PDF，可在新窗口下载。</p>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="exportDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmBatchExport">立即导出</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="chatDialogVisible" title="AI 助手" width="760px" append-to-body>
      <div class="chat-toolbar">
        <div class="chat-toolbar-left">
          <el-select v-model="selectedModel" style="width: 190px">
            <el-option v-for="item in modelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button plain @click="openModelConfigDialog">模型配置</el-button>
        </div>
        <el-button @click="clearHistory" :disabled="sending">清空对话</el-button>
      </div>

      <div class="chat-quick-actions">
        <el-button
          v-for="command in quickCommands"
          :key="command"
          size="small"
          plain
          @click="sendMessage(command)"
        >
          {{ command }}
        </el-button>
      </div>

      <div ref="messageListRef" class="chat-message-list">
        <div
          v-for="(item, index) in messages"
          :key="`${item.role}-${index}`"
          class="chat-message-row"
          :class="item.role === 'user' ? 'is-user' : 'is-assistant'"
        >
          <div class="chat-message-bubble">
            <AiMarkdownContent :content="item.content" />
          </div>
        </div>

        <div v-if="sending" class="chat-loading-row">
          <el-skeleton animated :rows="2" />
        </div>
      </div>

      <div v-if="chatError" class="ai-result-error">
        <span>{{ chatError }}</span>
      </div>

      <footer class="chat-input-area">
        <el-input
          v-model="chatDraft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入你的问题，支持 Enter 发送，Ctrl+Enter 换行"
          @keydown.enter="handleInputEnter"
        />
        <div class="chat-input-actions">
          <el-button :disabled="sending" @click="retryLast">重试上次</el-button>
          <el-button type="primary" :loading="sending" @click="sendMessage()">发送</el-button>
        </div>
      </footer>
    </el-dialog>

    <el-dialog v-model="modelConfigDialogVisible" title="AI 模型配置" width="620px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="当前模型">
          <el-tag type="primary">{{ selectedModelLabel }}</el-tag>
        </el-form-item>

        <el-form-item v-if="isSelectedProviderModel" label="自定义配置">
          <el-switch
            v-model="runtimeModelConfig.useCustomProvider"
            active-text="启用"
            inactive-text="关闭"
          />
        </el-form-item>

        <template v-if="isSelectedProviderModel">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            class="model-config-tip"
            title="启用自定义配置后，将优先使用你填写的 Base URL / API Key / 模型型号；AI 解题、AI 总结、AI 助手都会复用这套配置。"
          />
          <el-form-item label="Base URL">
            <el-input
              v-model="runtimeModelConfig.baseUrl"
              :disabled="!runtimeModelConfig.useCustomProvider"
              placeholder="例如：https://api.openai.com/v1 或兼容网关地址"
            />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input
              v-model="runtimeModelConfig.apiKey"
              :disabled="!runtimeModelConfig.useCustomProvider"
              type="password"
              show-password
              placeholder="填写兼容格式 API Key"
            />
          </el-form-item>
          <el-form-item label="模型型号">
            <el-input
              v-model="runtimeModelConfig.modelName"
              :disabled="!runtimeModelConfig.useCustomProvider"
              placeholder="例如：gpt-5.2 / qwen-plus / moonshot-v1-8k"
            />
          </el-form-item>
        </template>

        <el-alert
          v-else
          type="success"
          show-icon
          :closable="false"
          title="当前是内置安全模型，无需额外配置。"
        />
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="modelConfigDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveModelConfig">保存配置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, ChatDotRound, CirclePlus, Collection, Cpu, Document, Download, EditPen, Fold, Grid, MoonNight, RefreshRight } from '@element-plus/icons-vue'
import { analyzeQuestionByAi, chatWithAi, getAiModels, summarizeByAi } from '../../api/ai'
import { getReviewSummary } from '../../api/review'
import { useAuthStore } from '../../store/auth'
import { FLOAT_BUTTON_GROUPS, FLOAT_BUTTON_ITEMS } from '../../config/floatButtonConfig'
import { AI_BUILTIN_MODEL, AI_PROVIDER_MODELS, AI_RUNTIME_CONFIG_EVENT, getAiRuntimeConfig, setAiRuntimeConfig } from '../../utils/aiRuntimeConfig'
import AiMarkdownContent from './AiMarkdownContent.vue'

const STORAGE_POSITION_KEY = 'eb_float_button_position'
const STORAGE_PANEL_OPEN_KEY = 'eb_float_button_panel_open'
const STORAGE_THEME_KEY = 'eb_theme_mode'
const REVIEW_PROMPT_STORAGE_PREFIX = 'eb_review_prompt'
const SIDEBAR_TOGGLE_EVENT = 'eb:sidebar-toggle-request'
const EDGE_DOCK_TRIGGER = 22
const EDGE_VISIBLE_RATIO = 0.5
const HOVER_OPEN_DELAY = 280
const HOVER_CLOSE_DELAY = 140
const ITEM_ICON_MAP = {
  CirclePlus,
  EditPen,
  Collection,
  Cpu,
  Document,
  ChatDotRound,
  RefreshRight,
  Download,
  Aim,
  MoonNight,
  Fold
}
const FALLBACK_ICON = Grid

const quickCommands = [
  '帮我写一段 Java 泛型的 demo',
  '解释一下 Spring IOC 原理',
  '帮我复习一下 Redis 过期策略'
]

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const shellRef = ref(null)
const messageListRef = ref(null)

const panelVisible = ref(false)
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })

const viewport = reactive({ width: 1200, height: 800 })
const buttonPosition = reactive({ x: 0, y: 0 })
const panelPosition = reactive({ left: 0, top: 0, width: 332, maxHeight: 460 })

const dragState = reactive({
  active: false,
  moved: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0
})
const ignoreClick = ref(false)
const dockState = ref('none')
const hoverEnabled = ref(false)
const hoverState = reactive({ overFab: false, overPanel: false })
let hoverCloseTimer = null
let hoverOpenTimer = null

const promptDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const exportDialogVisible = ref(false)
const promptMode = ref('solve')
const promptText = ref('')
const aiProcessing = ref(false)
const aiError = ref('')
const aiMarkdown = ref('')
const exportTarget = ref('QUESTION')

const chatDialogVisible = ref(false)
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是 AI 助手。你可以直接粘贴报错信息、代码片段或学习问题，我会给你结构化建议。'
  }
])
const chatDraft = ref('')
const sending = ref(false)
const chatError = ref('')
const lastChatPayload = ref(null)

const modelOptions = ref([{ value: 'SAFE_GPT_SIM', label: '安全增强模型' }])
const selectedModel = ref('SAFE_GPT_SIM')
const modelLoaded = ref(false)
const modelConfigDialogVisible = ref(false)
const runtimeModelConfig = reactive({
  provider: 'OPENAI',
  useCustomProvider: false,
  baseUrl: '',
  apiKey: '',
  modelName: ''
})

const themeMode = ref('light')
const reviewSummary = reactive({
  todayTotalCount: 0,
  overdueCount: 0
})

const fabSize = computed(() => (viewport.width < 1200 ? 52 : 60))
const reviewBadgeCount = computed(() => {
  const total = Number(reviewSummary.todayTotalCount || 0)
  return Number.isFinite(total) ? Math.max(0, total) : 0
})

const fabStyle = computed(() => ({
  left: `${buttonPosition.x}px`,
  top: `${buttonPosition.y}px`,
  width: `${fabSize.value}px`,
  height: `${fabSize.value}px`
}))

const panelStyle = computed(() => ({
  left: `${panelPosition.left}px`,
  top: `${panelPosition.top}px`,
  width: `${panelPosition.width}px`,
  maxHeight: `${panelPosition.maxHeight}px`
}))

const contextMenuStyle = computed(() => ({
  left: `${contextMenuPos.x}px`,
  top: `${contextMenuPos.y}px`
}))

const selectedModelLabel = computed(() => {
  const matched = modelOptions.value.find((item) => item.value === selectedModel.value)
  return matched?.label || selectedModel.value || AI_BUILTIN_MODEL
})

const isSelectedProviderModel = computed(() => AI_PROVIDER_MODELS.includes(String(selectedModel.value || '').toUpperCase()))

const groupedItems = computed(() => {
  const grouped = {}
  FLOAT_BUTTON_GROUPS.forEach((group) => {
    grouped[group.key] = []
  })
  FLOAT_BUTTON_ITEMS
    .filter((item) => authStore.isLoggedIn || item.key !== 'my-review')
    .forEach((item) => {
    if (!grouped[item.group]) {
      grouped[item.group] = []
    }
    grouped[item.group].push(item)
    })
  return grouped
})

function resolveItemIcon(iconName) {
  if (!iconName) {
    return FALLBACK_ICON
  }
  return ITEM_ICON_MAP[iconName] || FALLBACK_ICON
}

function isProviderModelKey(model) {
  return AI_PROVIDER_MODELS.includes(String(model || '').toUpperCase())
}

function syncRuntimeConfigFromStorage({ syncModel = false } = {}) {
  const config = getAiRuntimeConfig()
  runtimeModelConfig.provider = config.provider
  runtimeModelConfig.useCustomProvider = !!config.useCustomProvider
  runtimeModelConfig.baseUrl = config.baseUrl || ''
  runtimeModelConfig.apiKey = config.apiKey || ''
  runtimeModelConfig.modelName = config.modelName || ''

  if (syncModel && String(config.model || '').trim()) {
    selectedModel.value = config.model
  }
}

function persistRuntimeConfig() {
  const previous = getAiRuntimeConfig()
  const next = {
    ...previous,
    model: selectedModel.value || AI_BUILTIN_MODEL,
    provider: isProviderModelKey(selectedModel.value) ? selectedModel.value : runtimeModelConfig.provider,
    useCustomProvider: isProviderModelKey(selectedModel.value) ? runtimeModelConfig.useCustomProvider : false,
    baseUrl: runtimeModelConfig.baseUrl,
    apiKey: runtimeModelConfig.apiKey,
    modelName: runtimeModelConfig.modelName
  }
  return setAiRuntimeConfig(next)
}

function onRuntimeConfigUpdated(event) {
  const detail = event?.detail || getAiRuntimeConfig()
  runtimeModelConfig.provider = detail.provider || 'OPENAI'
  runtimeModelConfig.useCustomProvider = !!detail.useCustomProvider
  runtimeModelConfig.baseUrl = detail.baseUrl || ''
  runtimeModelConfig.apiKey = detail.apiKey || ''
  runtimeModelConfig.modelName = detail.modelName || ''
  if (String(detail.model || '').trim() && detail.model !== selectedModel.value) {
    selectedModel.value = detail.model
  }
}

function safeGetStorage(key) {
  try {
    return localStorage.getItem(key)
  } catch (_) {
    return null
  }
}

function safeSetStorage(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch (_) {
    // ignore storage write failure
  }
}

function clamp(value, min, max) {
  if (!Number.isFinite(value)) return min
  if (value < min) return min
  if (value > max) return max
  return value
}

function updateViewport() {
  viewport.width = window.innerWidth
  viewport.height = window.innerHeight
}

function defaultPosition() {
  return {
    x: Math.max(12, viewport.width - fabSize.value - 24),
    y: Math.max(12, viewport.height - fabSize.value - 28)
  }
}

function getHorizontalBounds(allowHalfHidden = false) {
  if (allowHalfHidden) {
    const edgeOffset = Math.round(fabSize.value * EDGE_VISIBLE_RATIO)
    return {
      minX: -edgeOffset,
      maxX: Math.max(-edgeOffset, viewport.width - fabSize.value + edgeOffset)
    }
  }

  return {
    minX: 8,
    maxX: Math.max(8, viewport.width - fabSize.value - 8)
  }
}

function normalizePosition(position, allowHalfHidden = false) {
  const { minX, maxX } = getHorizontalBounds(allowHalfHidden)
  const maxY = Math.max(8, viewport.height - fabSize.value - 8)
  return {
    x: clamp(Number(position?.x), minX, maxX),
    y: clamp(Number(position?.y), 8, maxY)
  }
}

function resolveDockStateByX(x) {
  const edgeOffset = Math.round(fabSize.value * EDGE_VISIBLE_RATIO)
  const leftDockX = -edgeOffset
  const rightDockX = Math.max(-edgeOffset, viewport.width - fabSize.value + edgeOffset)

  if (Math.abs(x - leftDockX) <= 1) {
    return 'left'
  }
  if (Math.abs(x - rightDockX) <= 1) {
    return 'right'
  }
  return 'none'
}

function dockToEdge(side) {
  if (side === 'left') {
    const leftDockX = -Math.round(fabSize.value * EDGE_VISIBLE_RATIO)
    buttonPosition.x = leftDockX
    dockState.value = 'left'
    return
  }

  if (side === 'right') {
    const edgeOffset = Math.round(fabSize.value * EDGE_VISIBLE_RATIO)
    buttonPosition.x = Math.max(-edgeOffset, viewport.width - fabSize.value + edgeOffset)
    dockState.value = 'right'
    return
  }

  dockState.value = 'none'
}

function applyPosition(position, allowHalfHidden = false) {
  const next = normalizePosition(position, allowHalfHidden)
  buttonPosition.x = next.x
  buttonPosition.y = next.y
  dockState.value = resolveDockStateByX(buttonPosition.x)
}

function savePosition() {
  const payload = {
    x: Math.round(buttonPosition.x),
    y: Math.round(buttonPosition.y)
  }
  safeSetStorage(STORAGE_POSITION_KEY, JSON.stringify(payload))
}

function restorePosition() {
  const raw = safeGetStorage(STORAGE_POSITION_KEY)
  if (!raw) {
    applyPosition(defaultPosition())
    return
  }

  try {
    const parsed = JSON.parse(raw)
    applyPosition(parsed, true)
  } catch (_) {
    applyPosition(defaultPosition())
  }
}

function persistPanelOpenState() {
  safeSetStorage(STORAGE_PANEL_OPEN_KEY, panelVisible.value ? '1' : '0')
}

function restorePanelOpenState() {
  panelVisible.value = safeGetStorage(STORAGE_PANEL_OPEN_KEY) === '1'
}

function refreshPanelPosition() {
  const width = viewport.width < 768 ? Math.max(260, viewport.width - 20) : 332
  const maxHeight = clamp(viewport.height - 24, 260, 540)

  const preferLeft = buttonPosition.x + fabSize.value / 2 - width / 2
  const preferTop = buttonPosition.y - maxHeight - 12
  const fallbackTop = buttonPosition.y + fabSize.value + 12

  panelPosition.width = width
  panelPosition.maxHeight = maxHeight
  panelPosition.left = clamp(preferLeft, 12, Math.max(12, viewport.width - width - 12))
  panelPosition.top = preferTop < 12
    ? clamp(fallbackTop, 12, Math.max(12, viewport.height - maxHeight - 12))
    : clamp(preferTop, 12, Math.max(12, viewport.height - maxHeight - 12))
}

function closePanel() {
  panelVisible.value = false
  persistPanelOpenState()
}

function closeContextMenu() {
  contextMenuVisible.value = false
}

function clearHoverCloseTimer() {
  if (hoverCloseTimer) {
    clearTimeout(hoverCloseTimer)
    hoverCloseTimer = null
  }
}

function clearHoverOpenTimer() {
  if (hoverOpenTimer) {
    clearTimeout(hoverOpenTimer)
    hoverOpenTimer = null
  }
}

function canUseHoverAutoPanel() {
  return hoverEnabled.value && !dragState.active
}

function openPanelByHover() {
  if (!canUseHoverAutoPanel()) {
    return
  }
  clearHoverCloseTimer()
  if (!panelVisible.value) {
    panelVisible.value = true
    persistPanelOpenState()
  }
  nextTick(() => {
    refreshPanelPosition()
  })
}

function schedulePanelOpenByHover() {
  if (!canUseHoverAutoPanel()) {
    return
  }
  clearHoverCloseTimer()
  clearHoverOpenTimer()
  if (panelVisible.value) {
    return
  }
  hoverOpenTimer = setTimeout(() => {
    hoverOpenTimer = null
    if (!canUseHoverAutoPanel() || !hoverState.overFab || hoverState.overPanel) {
      return
    }
    openPanelByHover()
  }, HOVER_OPEN_DELAY)
}

function schedulePanelCloseByHover() {
  if (!hoverEnabled.value) {
    return
  }
  clearHoverCloseTimer()
  hoverCloseTimer = setTimeout(() => {
    hoverCloseTimer = null
    if (dragState.active || hoverState.overFab || hoverState.overPanel) {
      return
    }
    if (panelVisible.value) {
      closePanel()
    }
  }, HOVER_CLOSE_DELAY)
}

function onFabMouseEnter() {
  hoverState.overFab = true
  schedulePanelOpenByHover()
}

function onFabMouseLeave() {
  hoverState.overFab = false
  clearHoverOpenTimer()
  schedulePanelCloseByHover()
}

function onPanelMouseEnter() {
  hoverState.overPanel = true
  clearHoverOpenTimer()
  clearHoverCloseTimer()
}

function onPanelMouseLeave() {
  hoverState.overPanel = false
  schedulePanelCloseByHover()
}

function onFabClick() {
  if (ignoreClick.value) {
    ignoreClick.value = false
    return
  }

  if (canUseHoverAutoPanel()) {
    clearHoverOpenTimer()
    clearHoverCloseTimer()
    closeContextMenu()
    if (!panelVisible.value) {
      panelVisible.value = true
      persistPanelOpenState()
      nextTick(() => {
        refreshPanelPosition()
      })
    }
    return
  }

  panelVisible.value = !panelVisible.value
  closeContextMenu()
  persistPanelOpenState()

  if (panelVisible.value) {
    nextTick(() => {
      refreshPanelPosition()
    })
  }
}

function onDragMove(event) {
  if (!dragState.active) {
    return
  }

  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY
  if (!dragState.moved && (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3)) {
    dragState.moved = true

    if (dockState.value === 'left') {
      dragState.originX = 8
      dockState.value = 'none'
    } else if (dockState.value === 'right') {
      dragState.originX = Math.max(8, viewport.width - fabSize.value - 8)
      dockState.value = 'none'
    }
  }

  const next = normalizePosition({
    x: dragState.originX + deltaX,
    y: dragState.originY + deltaY
  })

  buttonPosition.x = next.x
  buttonPosition.y = next.y

  if (panelVisible.value) {
    refreshPanelPosition()
  }
}

function onDragEnd() {
  if (!dragState.active) {
    return
  }

  dragState.active = false
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)

  if (dragState.moved) {
    const gapLeft = buttonPosition.x
    const gapRight = viewport.width - buttonPosition.x - fabSize.value

    if (gapLeft <= EDGE_DOCK_TRIGGER) {
      dockToEdge('left')
    } else if (gapRight <= EDGE_DOCK_TRIGGER) {
      dockToEdge('right')
    } else {
      dockState.value = 'none'
    }

    savePosition()
    ignoreClick.value = true

    if (panelVisible.value) {
      refreshPanelPosition()
    }
  }

  dragState.moved = false
}

function onFabMouseDown(event) {
  if (event.button !== 0) {
    return
  }

  clearHoverOpenTimer()
  dragState.active = true
  dragState.moved = false
  dragState.startX = event.clientX
  dragState.startY = event.clientY
  dragState.originX = buttonPosition.x
  dragState.originY = buttonPosition.y

  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

function onFabContextMenu(event) {
  closePanel()
  contextMenuVisible.value = true

  const menuWidth = 126
  const menuHeight = 44
  contextMenuPos.x = clamp(event.clientX, 8, Math.max(8, viewport.width - menuWidth - 8))
  contextMenuPos.y = clamp(event.clientY, 8, Math.max(8, viewport.height - menuHeight - 8))
}

function resetPosition() {
  applyPosition(defaultPosition())
  savePosition()
  closeContextMenu()
  if (panelVisible.value) {
    refreshPanelPosition()
  }
}

function onGlobalPointerDown(event) {
  const shell = shellRef.value
  if (!shell) {
    return
  }
  if (shell.contains(event.target)) {
    return
  }

  if (panelVisible.value) {
    closePanel()
  }
  if (contextMenuVisible.value) {
    closeContextMenu()
  }
}

function onWindowResize() {
  updateViewport()

  if (dockState.value === 'left' || dockState.value === 'right') {
    dockToEdge(dockState.value)
    buttonPosition.y = normalizePosition(buttonPosition).y
  } else {
    applyPosition(buttonPosition)
  }

  if (panelVisible.value) {
    refreshPanelPosition()
  }
}

function applyTheme(mode) {
  document.documentElement.setAttribute('data-theme', mode)
  document.documentElement.classList.toggle('dark', mode === 'dark')
}

function restoreTheme() {
  const stored = safeGetStorage(STORAGE_THEME_KEY)
  themeMode.value = stored === 'dark' ? 'dark' : 'light'
  applyTheme(themeMode.value)
}

function toggleThemeMode() {
  themeMode.value = themeMode.value === 'light' ? 'dark' : 'light'
  applyTheme(themeMode.value)
  safeSetStorage(STORAGE_THEME_KEY, themeMode.value)
  ElMessage.success(themeMode.value === 'dark' ? '已切换为暗色主题' : '已切换为浅色主题')
  closePanel()
}

function openModelConfigDialog() {
  syncRuntimeConfigFromStorage()
  if (isProviderModelKey(selectedModel.value)) {
    runtimeModelConfig.provider = selectedModel.value
  }
  modelConfigDialogVisible.value = true
}

function saveModelConfig() {
  if (isProviderModelKey(selectedModel.value) && runtimeModelConfig.useCustomProvider) {
    if (!String(runtimeModelConfig.apiKey || '').trim()) {
      ElMessage.warning('请填写 API Key')
      return
    }
    if (!String(runtimeModelConfig.modelName || '').trim()) {
      ElMessage.warning('请填写模型型号')
      return
    }
  }

  persistRuntimeConfig()
  modelConfigDialogVisible.value = false
  ElMessage.success('AI 模型配置已保存')
}

async function ensureModels() {
  if (modelLoaded.value) {
    return
  }

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
      modelOptions.value = options
    }
    const runtimeConfig = getAiRuntimeConfig()
    const runtimeModel = String(runtimeConfig?.model || '').trim()
    const hasRuntimeModel = runtimeModel && modelOptions.value.some((item) => item.value === runtimeModel)
    selectedModel.value = hasRuntimeModel
      ? runtimeModel
      : (data?.defaultModel || modelOptions.value[0]?.value || selectedModel.value)
  } catch (_) {
    // keep default model option
  } finally {
    modelLoaded.value = true
  }
}

function resolveAiErrorMessage(error) {
  const raw = error?.message || error?.response?.data?.message || ''
  const text = String(raw || '').trim()
  if (!text) {
    return 'AI 服务繁忙，请重试'
  }
  if (text.includes('timeout') || text.includes('超时')) {
    return 'AI 请求超时，请稍后重试'
  }
  return text
}

function requireLogin() {
  if (authStore.isLoggedIn) {
    return true
  }
  ElMessage.warning('请先登录后使用该功能')
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  closePanel()
  return false
}

function openAiPrompt(mode) {
  if (!requireLogin()) {
    return
  }
  promptMode.value = mode
  promptText.value = ''
  aiError.value = ''
  aiMarkdown.value = ''
  promptDialogVisible.value = true
  ensureModels()
  closePanel()
}

async function submitAiPrompt() {
  const text = promptText.value.trim()
  if (!text) {
    ElMessage.warning('请输入内容')
    return
  }

  await ensureModels()

  promptDialogVisible.value = false
  resultDialogVisible.value = true
  aiProcessing.value = true
  aiError.value = ''
  aiMarkdown.value = ''

  try {
    if (promptMode.value === 'solve') {
      const data = await analyzeQuestionByAi({
        contextDescription: text,
        language: 'Java',
        model: selectedModel.value
      })
      aiMarkdown.value = data?.markdown || data?.solutionText || '未生成可用结果，请补充上下文后重试。'
      return
    }

    const data = await summarizeByAi({
      title: '悬浮按钮输入',
      content: text,
      language: 'Java',
      summaryType: 'CORE_EXTRACT',
      model: selectedModel.value
    })
    aiMarkdown.value = data?.markdown || data?.summaryText || '未生成可用结果，请补充内容后重试。'
  } catch (error) {
    aiError.value = resolveAiErrorMessage(error)
  } finally {
    aiProcessing.value = false
  }
}

async function copyAiResult() {
  const text = String(aiMarkdown.value || '').trim()
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }

  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (_) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function openExportDialog() {
  if (!requireLogin()) {
    return
  }
  exportDialogVisible.value = true
  closePanel()
}

function confirmBatchExport() {
  const url = exportTarget.value === 'NOTE'
    ? '/api/v1/note/export-pdf?sort_by=updated_at&sort_order=desc'
    : '/api/v1/error-question/export-pdf'

  window.open(url, '_blank')
  exportDialogVisible.value = false
}

async function toggleFocusMode() {
  if (!document.fullscreenElement) {
    try {
      await document.documentElement.requestFullscreen()
      ElMessage.success('已进入专注模式')
    } catch (_) {
      ElMessage.warning('当前浏览器不支持进入专注模式')
    }
  } else {
    try {
      await document.exitFullscreen()
      ElMessage.success('已退出专注模式')
    } catch (_) {
      ElMessage.warning('退出专注模式失败，请重试')
    }
  }
  closePanel()
}

function toggleSidebar() {
  const layoutExists = !!document.querySelector('.layout-shell')
  if (!layoutExists) {
    ElMessage.warning('当前页面不支持侧边栏折叠')
    closePanel()
    return
  }

  window.dispatchEvent(new CustomEvent(SIDEBAR_TOGGLE_EVENT))
  closePanel()
}

function handleInputEnter(event) {
  if (!event || event.isComposing) {
    return
  }
  if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
    return
  }
  event.preventDefault()
  sendMessage()
}

async function openChatDialog() {
  if (!requireLogin()) {
    return
  }
  await ensureModels()
  chatDialogVisible.value = true
  closePanel()
  await scrollToBottom()
}

async function sendMessage(quickMessage = '') {
  if (sending.value) {
    return
  }

  const content = String(quickMessage || chatDraft.value || '').trim()
  if (!content) {
    ElMessage.warning('请输入问题')
    return
  }

  if (!quickMessage) {
    chatDraft.value = ''
  }
  chatError.value = ''

  const userMessage = { role: 'user', content }
  messages.value.push(userMessage)
  await scrollToBottom()

  const payload = {
    message: content,
    model: selectedModel.value,
    history: messages.value.slice(-12).map((item) => ({ role: item.role, content: item.content }))
  }
  lastChatPayload.value = payload
  await requestAnswer(payload)
}

async function requestAnswer(payload) {
  sending.value = true
  chatError.value = ''
  try {
    const data = await chatWithAi(payload)
    messages.value.push({
      role: 'assistant',
      content: data?.answerMarkdown || '当前没有生成可用回答，请补充更多上下文后重试。'
    })
    await scrollToBottom()
  } catch (error) {
    chatError.value = resolveAiErrorMessage(error)
  } finally {
    sending.value = false
  }
}

async function retryLast() {
  if (!lastChatPayload.value || sending.value) {
    return
  }
  await requestAnswer(lastChatPayload.value)
}

async function clearHistory() {
  if (sending.value) {
    return
  }

  await ElMessageBox.confirm('确认清空当前对话历史？', '提示', { type: 'warning' })
  messages.value = [
    {
      role: 'assistant',
      content: '对话已清空。你可以继续提问，我会结合当前输入提供建议。'
    }
  ]
  chatError.value = ''
  lastChatPayload.value = null
  await scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (!el) {
    return
  }
  el.scrollTop = el.scrollHeight
}

function reviewPromptStorageKey() {
  const userId = authStore.profile?.id
  if (!userId) {
    return ''
  }
  const today = new Date().toISOString().slice(0, 10)
  return `${REVIEW_PROMPT_STORAGE_PREFIX}_${userId}_${today}`
}

function tryShowReviewPrompt(totalCount) {
  if (!authStore.isLoggedIn || totalCount <= 0) {
    return
  }
  const storageKey = reviewPromptStorageKey()
  if (!storageKey) {
    return
  }
  if (safeGetStorage(storageKey) === '1') {
    return
  }
  safeSetStorage(storageKey, '1')
  ElMessageBox.alert(`今日有 ${totalCount} 项内容待复习`, '复习提醒', {
    confirmButtonText: '去看看'
  }).then(() => {
    router.push('/review/center')
  }).catch(() => {})
}

async function refreshReviewSummary({ withPrompt = false } = {}) {
  if (!authStore.isLoggedIn) {
    reviewSummary.todayTotalCount = 0
    reviewSummary.overdueCount = 0
    return
  }

  try {
    const data = await getReviewSummary()
    reviewSummary.todayTotalCount = Number(data?.todayTotalCount || 0)
    reviewSummary.overdueCount = Number(data?.overdueCount || 0)
    if (withPrompt) {
      tryShowReviewPrompt(reviewSummary.todayTotalCount)
    }
  } catch (_) {
    reviewSummary.todayTotalCount = 0
    reviewSummary.overdueCount = 0
  }
}

const actionHandlers = {
  route: async (item) => {
    const payload = item.payload || {}
    if (!payload.path) {
      ElMessage.warning('路由未配置')
      closePanel()
      return
    }
    await router.push({ path: payload.path, query: payload.query || {} })
    closePanel()
  },
  'ai-solve': async () => {
    openAiPrompt('solve')
  },
  'ai-summary': async () => {
    openAiPrompt('summary')
  },
  'ai-chat': async () => {
    await openChatDialog()
  },
  'batch-export': async () => {
    openExportDialog()
  },
  'focus-mode': async () => {
    await toggleFocusMode()
  },
  'toggle-theme': async () => {
    toggleThemeMode()
  },
  'toggle-sidebar': async () => {
    toggleSidebar()
  },
  event: async (item) => {
    const payload = item.payload || {}
    const eventName = String(payload.eventName || '').trim()
    if (!eventName) {
      ElMessage.warning('事件名未配置')
      closePanel()
      return
    }
    window.dispatchEvent(new CustomEvent(eventName, { detail: payload.detail || null }))
    closePanel()
  }
}

function shouldRequireLoginForItem(item) {
  if (!item) {
    return false
  }
  if (item.action === 'toggle-theme') {
    return true
  }
  return item.group === 'create'
}

async function handleItemClick(item) {
  if (!item) {
    return
  }

  if (shouldRequireLoginForItem(item) && !requireLogin()) {
    return
  }

  const handler = actionHandlers[item.action]
  if (!handler) {
    ElMessage.warning('该功能尚未接入')
    closePanel()
    return
  }

  try {
    await handler(item)
  } catch (_) {
    ElMessage.error('功能执行失败，请稍后重试')
  }
}

watch(panelVisible, (value) => {
  persistPanelOpenState()
  if (value) {
    nextTick(() => {
      refreshPanelPosition()
    })
  }
})

watch(chatDialogVisible, (value) => {
  if (value) {
    nextTick(() => {
      scrollToBottom()
    })
  }
})

watch(selectedModel, (value) => {
  const previous = getAiRuntimeConfig()
  const next = {
    ...previous,
    model: value || AI_BUILTIN_MODEL,
    provider: isProviderModelKey(value) ? value : previous.provider,
    useCustomProvider: isProviderModelKey(value) ? previous.useCustomProvider : false,
    baseUrl: previous.baseUrl,
    apiKey: previous.apiKey,
    modelName: previous.modelName
  }
  setAiRuntimeConfig(next, { silent: true })
  if (isProviderModelKey(value)) {
    runtimeModelConfig.provider = value
  }
})

watch(
  () => authStore.isLoggedIn,
  async (loggedIn, prev) => {
    if (!loggedIn) {
      reviewSummary.todayTotalCount = 0
      reviewSummary.overdueCount = 0
      return
    }
    await refreshReviewSummary({ withPrompt: loggedIn && !prev })
  }
)

watch(
  () => route.fullPath,
  async () => {
    if (!authStore.isLoggedIn) return
    await refreshReviewSummary()
  }
)

onMounted(async () => {
  await authStore.bootstrap()
  await refreshReviewSummary({ withPrompt: true })
  updateViewport()
  syncRuntimeConfigFromStorage({ syncModel: true })
  const hoverMedia = typeof window.matchMedia === 'function'
    ? window.matchMedia('(hover: hover) and (pointer: fine)')
    : null
  hoverEnabled.value = !!hoverMedia?.matches
  restoreTheme()
  restorePosition()
  restorePanelOpenState()

  if (panelVisible.value) {
    await nextTick()
    refreshPanelPosition()
  }

  document.addEventListener('mousedown', onGlobalPointerDown, true)
  window.addEventListener('resize', onWindowResize)
  window.addEventListener(AI_RUNTIME_CONFIG_EVENT, onRuntimeConfigUpdated)
})

onBeforeUnmount(() => {
  clearHoverOpenTimer()
  clearHoverCloseTimer()
  document.removeEventListener('mousedown', onGlobalPointerDown, true)
  window.removeEventListener('resize', onWindowResize)
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  window.removeEventListener(AI_RUNTIME_CONFIG_EVENT, onRuntimeConfigUpdated)
})
</script>

<style scoped>
.float-shell {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
}

.float-fab {
  position: fixed;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  background:
    radial-gradient(circle at 28% 24%, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.38) 30%, rgba(255, 255, 255, 0) 56%),
    linear-gradient(160deg, #9adfff 0%, #67c4ff 44%, #4297ee 100%);
  color: #f8fdff;
  box-shadow:
    inset 0 2px 6px rgba(255, 255, 255, 0.55),
    inset 0 -10px 16px rgba(49, 124, 199, 0.24),
    0 14px 30px rgba(52, 137, 218, 0.28);
  overflow: visible;
  isolation: isolate;
  backdrop-filter: blur(1.5px);
  cursor: grab;
  user-select: none;
  pointer-events: auto;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-radius 0.2s ease, filter 0.2s ease;
}

.float-fab::before {
  content: '';
  position: absolute;
  top: 10%;
  left: 14%;
  width: 56%;
  height: 34%;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.12) 100%);
  transform: rotate(-14deg);
  pointer-events: none;
}

.float-fab.is-docked-left {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.float-fab.is-docked-right {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.float-fab:hover {
  transform: translateY(-1px) scale(1.05);
  box-shadow:
    inset 0 2px 7px rgba(255, 255, 255, 0.62),
    inset 0 -10px 18px rgba(45, 118, 190, 0.3),
    0 18px 36px rgba(45, 126, 209, 0.32);
  filter: saturate(1.05);
}

.float-fab:active {
  cursor: grabbing;
  transform: scale(0.98);
}

.fab-core {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.fab-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  z-index: 2;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  border: 2px solid #fff;
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  pointer-events: none;
}


:global(:root[data-theme='dark']) .float-fab {
  border-color: rgba(177, 219, 255, 0.48);
  background:
    radial-gradient(circle at 26% 22%, rgba(224, 241, 255, 0.46) 0%, rgba(224, 241, 255, 0.08) 34%, rgba(224, 241, 255, 0) 58%),
    linear-gradient(160deg, #3d669d 0%, #305796 46%, #29467f 100%);
  box-shadow:
    inset 0 2px 5px rgba(211, 233, 255, 0.24),
    inset 0 -8px 14px rgba(9, 26, 49, 0.5),
    0 14px 32px rgba(0, 0, 0, 0.45);
}

:global(:root[data-theme='dark']) .float-item {
  background: rgba(122, 162, 255, 0.08);
}

:global(:root[data-theme='dark']) .float-item:hover {
  border-color: rgba(122, 162, 255, 0.65);
  background: rgba(122, 162, 255, 0.16);
}

:global(:root[data-theme='dark']) .float-item-icon {
  background: rgba(122, 162, 255, 0.2);
}
.float-panel {
  position: fixed;
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  backdrop-filter: blur(8px);
  overflow: hidden;
  pointer-events: auto;
}

.float-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.float-panel-head h4 {
  margin: 0;
  font-size: 15px;
  color: var(--text-main);
}

.float-panel-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-y;
}

.float-group + .float-group {
  margin-top: 10px;
}

.float-group h5 {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text-sub);
  font-weight: 700;
}

.float-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.float-item {
  min-height: 80px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(30, 64, 175, 0.04);
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.float-item:hover {
  transform: translateY(-1px) scale(1.03);
  border-color: #96b5ff;
  background: rgba(30, 64, 175, 0.1);
}

.float-item-icon {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: rgba(30, 64, 175, 0.12);
  color: var(--primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.float-item-icon .el-icon {
  font-size: 19px;
}

.float-item-label {
  font-size: 12px;
  color: var(--text-main);
  font-weight: 600;
}

.fab-context {
  position: fixed;
  min-width: 120px;
  padding: 6px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  pointer-events: auto;
}

.fab-context-item {
  width: 100%;
  border: none;
  border-radius: 8px;
  background: rgba(30, 64, 175, 0.08);
  color: var(--text-main);
  min-height: 32px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.fab-context-item:hover {
  background: rgba(30, 64, 175, 0.16);
}

.ai-result-wrap {
  min-height: 180px;
}

.ai-result-error {
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px;
}

.export-target-group {
  display: inline-flex;
  margin-bottom: 8px;
}

.export-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748b;
}

.model-select-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.model-config-tip {
  margin-bottom: 12px;
}

.chat-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.chat-toolbar-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chat-quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.chat-message-list {
  max-height: 46vh;
  overflow-y: auto;
  border: 1px solid #dbe3ee;
  border-radius: 12px;
  padding: 10px;
  background: #f8fbff;
}

.chat-message-row {
  display: flex;
  margin-bottom: 10px;
}

.chat-message-row.is-user {
  justify-content: flex-end;
}

.chat-message-row.is-assistant {
  justify-content: flex-start;
}

.chat-message-bubble {
  max-width: 88%;
  border-radius: 12px;
  padding: 8px 10px;
  border: 1px solid #dbe3ee;
  background: #ffffff;
}

.chat-message-row.is-user .chat-message-bubble {
  background: #eaf2ff;
  border-color: #bdd2ff;
}

.chat-loading-row {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  padding: 10px;
  background: #ffffff;
}

.chat-input-area {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-input-actions {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.float-panel-pop-enter-active,
.float-panel-pop-leave-active {
  transition: all 0.3s ease;
}

.float-panel-pop-enter-from,
.float-panel-pop-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

@media (max-width: 1199px) {
  .fab-core {
    font-size: 14px;
  }

  .float-item {
    min-height: 74px;
  }
}
</style>
























