import request from '../utils/request'

export function getStatisticsOverview() {
  return request.get('/statistics/overview')
}
