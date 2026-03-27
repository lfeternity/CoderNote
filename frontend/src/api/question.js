import axios from 'axios'
import request from '../utils/request'

export function getQuestionList(params) {
  return request.get('/error-question/list', { params })
}

export function addQuestion(payload) {
  return request.post('/error-question/add', payload)
}

export function updateQuestion(questionId, payload) {
  return request.put(`/error-question/update/${questionId}`, payload)
}

export function getQuestionDetail(questionId) {
  return request.get(`/error-question/detail/${questionId}`)
}

export function deleteQuestion(questionId) {
  return request.delete(`/error-question/delete/${questionId}`)
}

export function batchDeleteQuestion(ids) {
  return request.post('/error-question/batch-delete', { ids })
}

export function updateQuestionMastery(questionId, masteryStatus) {
  return request.put(`/error-question/mastery-status/${questionId}`, { masteryStatus })
}

export function uploadQuestionAttachment(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData)
}

export function buildQuestionExportPdfUrl(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === null || value === undefined) return
    const normalized = String(value).trim()
    if (!normalized) return
    search.append(key, normalized)
  })
  const query = search.toString()
  return `/api/v1/error-question/export-pdf${query ? `?${query}` : ''}`
}

export function buildQuestionSingleExportPdfUrl(questionId) {
  return `/api/v1/error-question/export-pdf/${questionId}`
}

function resolveFileName(contentDisposition, defaultName) {
  if (!contentDisposition) return defaultName

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (_) {
      return utf8Match[1]
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || defaultName
}

async function normalizePdfResponseError(response) {
  const contentType = response?.headers?.['content-type'] || ''
  if (contentType.includes('application/pdf')) {
    return null
  }

  const blob = response?.data
  if (!blob) {
    return '导出失败'
  }

  try {
    const text = await blob.text()
    const parsed = JSON.parse(text)
    return parsed?.message || '导出失败'
  } catch (_) {
    return '导出失败'
  }
}

export async function downloadQuestionBatchPdf(ids) {
  const response = await axios.post(
    '/api/v1/error-question/export-pdf/batch',
    { ids },
    {
      responseType: 'blob',
      withCredentials: true,
      timeout: 30000
    }
  )

  const errorMessage = await normalizePdfResponseError(response)
  if (errorMessage) {
    throw new Error(errorMessage)
  }

  return {
    blob: response.data,
    fileName: resolveFileName(response.headers?.['content-disposition'], 'error_questions_selected.pdf')
  }
}
