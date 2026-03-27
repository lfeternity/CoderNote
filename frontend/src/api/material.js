import request from '../utils/request'

export function getMaterialList(params) {
  return request.get('/study-material/list', { params })
}

export function getFavoriteMaterialList(params) {
  return request.get('/study-material/favorite/list', { params })
}

export function addMaterial(payload) {
  return request.post('/study-material/add', payload)
}

export function updateMaterial(materialId, payload) {
  return request.put(`/study-material/update/${materialId}`, payload)
}

export function getMaterialDetail(materialId) {
  return request.get(`/study-material/detail/${materialId}`)
}

export function favoriteMaterial(materialId) {
  return request.post(`/study-material/favorite/${materialId}`)
}

export function unfavoriteMaterial(materialId) {
  return request.delete(`/study-material/favorite/${materialId}`)
}

export function deleteMaterial(materialId) {
  return request.delete(`/study-material/delete/${materialId}`)
}

export function batchDeleteMaterial(ids) {
  return request.post('/study-material/batch-delete', { ids })
}

export function uploadMaterialAttachment(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload?bizType=material', formData)
}