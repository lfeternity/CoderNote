<template>
  <section class="ai-analysis-wrap" :class="{ compact }">
    <div class="ai-analysis-toolbar">
      <el-button type="primary" :loading="loading" @click="runAnalysis">AI 分析</el-button>
      <span class="runtime-config-tip">已使用 AI 助手模型配置</span>
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
import { ref } from 'vue'
import { analyzeQuestionByAi } from '../../api/ai'
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

const loading = ref(false)
const error = ref('')
const result = ref(null)
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

function buildPayload() {
  return {
    questionId: props.questionId ? Number(props.questionId) : undefined,
    errorCode: props.errorCode || '',
    exceptionMessage: props.exceptionMessage || '',
    contextDescription: props.contextDescription || '',
    language: props.language || '',
    tagNames: Array.isArray(props.tagNames) ? props.tagNames : []
  }
}

async function runAnalysis() {
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

.runtime-config-tip {
  color: var(--text-sub);
  font-size: 12px;
}

.quota-tip {
  color: var(--text-sub);
  font-size: 12px;
}

.ai-analysis-card {
  margin-top: 10px;
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  padding: 12px;
  background: color-mix(in srgb, var(--surface) 70%, var(--surface-soft));
}

.ai-analysis-error {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid color-mix(in srgb, var(--danger) 38%, var(--border));
  background: color-mix(in srgb, var(--danger) 11%, var(--surface));
  color: var(--danger);
  border-radius: 10px;
  padding: 8px 10px;
}

.loading-text {
  margin: 8px 0 0;
  color: var(--text-sub);
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
</style>
