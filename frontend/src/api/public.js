import request from '../utils/request'

export function getOptions() {
  return request.get('/public/options')
}
