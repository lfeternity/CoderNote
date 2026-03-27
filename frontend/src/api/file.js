import request from '../utils/request'

export function uploadCoverImage(file, bizType = 'question') {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/file/upload?bizType=${encodeURIComponent(bizType)}`, formData)
}

