<template>
  <section class="ai-analysis-wrap" :class="{ compact }">
    <div class="ai-analysis-toolbar">
      <el-button type="primary" :loading="loading" @click="runAnalysis">AI 分析</el-button>
      <el-select v-model="selectedModel" size="small" style="width: 170px">
        <el-option v-for="item in models" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <span v-if="result" class="quota-tip">剩余次数：{{ result.remainingCount ?? '-' }}</span>
    </div>

    <div v-if="loading" class="ai-analysis-card">
      <el-skeleton animated :rows="5" />
      <p class="loading-text">正在分析你的代码...</p>
    </div>

    <div v-else-if="error" class="ai-analysis-error">
      <span>{{ error }}</span>
      <el-button link type="primary" @click="retry">重试</el-button>
    </div>

    <div v-else-if="result" class="ai-analysis-card">
      <AiMarkdownContent :content="result.markdown" />
      <div class="ai-analysis-actions">
        <el-button type="primary" plain @click="emit('apply', result)">一键填入解决方案</el-button>
        <el-button v-if="showSaveButton" plain @click="emit('save', result)">保存为新笔记</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { analyzeQuestionByAi, getAiModels } from '../../api/ai'
import { AI_BUILTIN_MODEL, AI_PROVIDER_MODELS, getAiRuntimeConfig, setAiRuntimeConfig } from '../../utils/aiRuntimeConfig'
import AiMarkdownContent from './AiMarkdownContent.vue'

const props = defineProps({
  questionId: {
    type: [Number, String],
    default: null
  },
  errorCode: {
    type: String,
    default: ''
  },
  exceptionMessage: {
    type: String,
    default: ''
  },
  contextDescription: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: ''
  },
  tagNames: {
    type: Array,
    default: () => []
  },
  compact: {
    type: Boolean,
    default: false
  },
  showSaveButton: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['apply', 'save'])

const defaultModelOptions = [{ value: 'SAFE_GPT_SIM', label: '安全增强模型' }]
const loading = ref(false)
const error = ref('')
const result = ref(null)
const models = ref([...defaultModelOptions])
const selectedModel = ref(getAiRuntimeConfig().model || AI_BUILTIN_MODEL)
const modelsLoaded = ref(false)
const modelLoading = ref(false)
const lastPayload = ref(null)

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

async function ensureModels() {
  if (modelsLoaded.value || modelLoading.value) return
  modelLoading.value = true
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
      models.value = options
    }
    const runtimeModel = getAiRuntimeConfig().model
    const hasRuntimeModel = runtimeModel && models.value.some((item) => item.value === runtimeModel)
    selectedModel.value = hasRuntimeModel
      ? runtimeModel
      : (data?.defaultModel || models.value[0]?.value || selectedModel.value)
    modelsLoaded.value = true
  } catch (_) {
    models.value = [...defaultModelOptions]
  }
  modelLoading.value = false
}

function buildPayload() {
  return {
    questionId: props.questionId ? Number(props.questionId) : undefined,
    errorCode: props.errorCode || '',
    exceptionMessage: props.exceptionMessage || '',
    contextDescription: props.contextDescription || '',
    language: props.language || '',
    tagNames: Array.isArray(props.tagNames) ? props.tagNames : [],
    model: selectedModel.value
  }
}

async function runAnalysis() {
  await ensureModels()
  const payload = buildPayload()
  lastPayload.value = payload
  loading.value = true
  error.value = ''
  try {
    result.value = await analyzeQuestionByAi(payload)
  } catch (err) {
    error.value = resolveAiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function retry() {
  if (!lastPayload.value) {
    await runAnalysis()
    return
  }
  loading.value = true
  error.value = ''
  try {
    result.value = await analyzeQuestionByAi(lastPayload.value)
  } catch (err) {
    error.value = resolveAiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  ensureModels()
})

watch(selectedModel, (value) => {
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
.ai-analysis-wrap {
  margin: 8px 0 12px;
}

.ai-analysis-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.quota-tip {
  color: #64748b;
  font-size: 12px;
}

.ai-analysis-card {
  margin-top: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 12px;
  padding: 12px;
  background: #fbfdff;
}

.ai-analysis-error {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
  border-radius: 10px;
  padding: 8px 10px;
}

.loading-text {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
}

.ai-analysis-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-analysis-wrap.compact .ai-analysis-card {
  max-height: 52vh;
  overflow: auto;
}

@media (prefers-color-scheme: dark) {
  .ai-analysis-card {
    background: #0f172a;
    border-color: #334155;
  }

  .quota-tip,
  .loading-text {
    color: #94a3b8;
  }
}
</style>

