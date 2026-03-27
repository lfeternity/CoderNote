import axios from 'axios'
import request from '../utils/request'

export function getNoteList(params) {
  return request.get('/note/list', { params })
}

export function addNote(payload) {
  return request.post('/note/add', payload)
}

export function updateNote(noteId, payload) {
  return request.put(`/note/update/${noteId}`, payload)
}

export function getNoteDetail(noteId) {
  return request.get(`/note/detail/${noteId}`)
}

export function getNoteVersionList(noteId) {
  return request.get(`/note/version/list/${noteId}`)
}

export function getNoteVersionDetail(noteId, versionId) {
  return request.get(`/note/version/detail/${noteId}/${versionId}`)
}

export function restoreNoteVersion(noteId, versionId) {
  return request.post(`/note/version/restore/${noteId}/${versionId}`)
}

export function deleteNoteVersion(noteId, versionId) {
  return request.delete(`/note/version/delete/${noteId}/${versionId}`)
}

export function deleteNote(noteId) {
  return request.delete(`/note/delete/${noteId}`)
}

export function batchDeleteNote(ids) {
  return request.post('/note/batch-delete', { ids })
}

export function favoriteNote(noteId) {
  return request.post(`/note/favorite/${noteId}`)
}

export function unfavoriteNote(noteId) {
  return request.delete(`/note/favorite/${noteId}`)
}

export function updateNoteMastery(noteId, masteryStatus) {
  return request.put(`/note/mastery-status/${noteId}`, { masteryStatus })
}

export function linkQuestionNotes(questionId, ids) {
  return request.post(`/note/link-question/${questionId}`, { ids })
}

export function linkMaterialNotes(materialId, ids) {
  return request.post(`/note/link-material/${materialId}`, { ids })
}

export function buildNoteSingleExportPdfUrl(noteId) {
  return `/api/v1/note/export-pdf/${noteId}`
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

export async function downloadNoteBatchPdf(ids) {
  const response = await axios.post(
    '/api/v1/note/export-pdf/batch',
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
    fileName: resolveFileName(response.headers?.['content-disposition'], 'notes_selected.pdf')
  }
}
