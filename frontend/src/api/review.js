import request from '../utils/request'

export function getReviewSummary() {
  return request.get('/review/summary')
}

export function getReviewOverview() {
  return request.get('/review/overview')
}

export function getReviewList(params) {
  return request.get('/review/list', { params })
}

export function getReviewSessionItems(params) {
  return request.get('/review/session-items', { params })
}

export function upsertReviewPlan(payload) {
  return request.post('/review/plan', payload)
}

export function batchUpsertReviewPlan(payload) {
  return request.post('/review/plan/batch', payload)
}

export function updateReviewStatus(payload) {
  return request.put('/review/status', payload)
}

export function executeReview(payload) {
  return request.post('/review/execute', payload)
}

export function removeReviewPlan(contentType, contentId) {
  return request.delete(`/review/plan/${contentType}/${contentId}`)
}

export function clearAllReviewPlans() {
  return request.delete('/review/plan/clear')
}
