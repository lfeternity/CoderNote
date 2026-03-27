import request from '../utils/request'

export function getTagList() {
  return request.get('/tag/list')
}

export function addTag(payload) {
  return request.post('/tag/add', payload)
}

export function updateTag(tagId, payload) {
  return request.put(`/tag/update/${tagId}`, payload)
}

export function deleteTag(tagId) {
  return request.delete(`/tag/delete/${tagId}`)
}

export function getTagRelation(tagId) {
  return request.get(`/tag/relation/${tagId}`)
}
