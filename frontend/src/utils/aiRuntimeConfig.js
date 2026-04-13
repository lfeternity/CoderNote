const AI_RUNTIME_CONFIG_KEY = 'eb_ai_runtime_config'
export const AI_RUNTIME_CONFIG_EVENT = 'eb:ai-runtime-config-updated'

export const AI_BUILTIN_MODEL = 'SAFE_GPT_SIM'
export const AI_PROVIDER_MODELS = ['OPENAI', 'QWEN', 'KIMI', 'DEEPSEEK', 'GEMINI', 'CLAUDE']

function trim(value) {
  return value == null ? '' : String(value).trim()
}

function normalizeProvider(value) {
  const provider = trim(value).toUpperCase()
  return AI_PROVIDER_MODELS.includes(provider) ? provider : 'OPENAI'
}

function normalizeModel(value) {
  const model = trim(value).toUpperCase()
  if (model === AI_BUILTIN_MODEL || AI_PROVIDER_MODELS.includes(model)) {
    return model
  }
  return AI_BUILTIN_MODEL
}

function normalizeConfig(raw) {
  const model = normalizeModel(raw?.model)
  const provider = normalizeProvider(raw?.provider || model)
  return {
    model,
    provider,
    useCustomProvider: !!raw?.useCustomProvider && AI_PROVIDER_MODELS.includes(model),
    baseUrl: trim(raw?.baseUrl),
    apiKey: trim(raw?.apiKey),
    modelName: trim(raw?.modelName)
  }
}

function safeParse(raw) {
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (_) {
    return {}
  }
}

export function getAiRuntimeConfig() {
  try {
    const raw = localStorage.getItem(AI_RUNTIME_CONFIG_KEY)
    return normalizeConfig(safeParse(raw))
  } catch (_) {
    return normalizeConfig({})
  }
}

export function setAiRuntimeConfig(nextConfig, { silent = false } = {}) {
  const normalized = normalizeConfig(nextConfig || {})
  try {
    localStorage.setItem(AI_RUNTIME_CONFIG_KEY, JSON.stringify(normalized))
  } catch (_) {
    // ignore storage write failure
  }

  if (!silent) {
    window.dispatchEvent(new CustomEvent(AI_RUNTIME_CONFIG_EVENT, { detail: normalized }))
  }
  return normalized
}

export function buildAiPayloadWithRuntimeConfig(payload) {
  const nextPayload = {
    ...(payload || {})
  }
  const runtimeConfig = getAiRuntimeConfig()
  nextPayload.model = runtimeConfig.model

  if (runtimeConfig.useCustomProvider && AI_PROVIDER_MODELS.includes(runtimeConfig.model)) {
    nextPayload.modelConfig = {
      provider: runtimeConfig.model,
      baseUrl: runtimeConfig.baseUrl,
      apiKey: runtimeConfig.apiKey,
      modelName: runtimeConfig.modelName
    }
  } else {
    delete nextPayload.modelConfig
  }

  return nextPayload
}
