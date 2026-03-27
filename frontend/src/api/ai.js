import request from '../utils/request'
import { buildAiPayloadWithRuntimeConfig } from '../utils/aiRuntimeConfig'

const AI_REQUEST_TIMEOUT = 60000

export function getAiModels() {
  return request.get('/ai/models', { timeout: AI_REQUEST_TIMEOUT })
}

export function analyzeQuestionByAi(payload) {
  return request.post('/ai/question-analysis', buildAiPayloadWithRuntimeConfig(payload), { timeout: AI_REQUEST_TIMEOUT })
}

export function summarizeByAi(payload) {
  return request.post('/ai/summary', buildAiPayloadWithRuntimeConfig(payload), { timeout: AI_REQUEST_TIMEOUT })
}

export function chatWithAi(payload) {
  return request.post('/ai/chat', buildAiPayloadWithRuntimeConfig(payload), { timeout: AI_REQUEST_TIMEOUT })
}
