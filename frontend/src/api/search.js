import request from '../utils/request'

export function searchResult(params) {
  return request.get('/search/result', { params })
}
