export function reviewStatusLabel(value) {
  if (value === 'MASTERED') return '已掌握'
  if (value === 'REVIEWING') return '复习中'
  return '未复习'
}

export function reviewStatusTagType(value) {
  if (value === 'MASTERED') return 'success'
  if (value === 'REVIEWING') return 'warning'
  return 'info'
}

export function reviewStatusOrder(value) {
  if (value === 'MASTERED') return 3
  if (value === 'REVIEWING') return 2
  return 1
}

export function reviewContentTypeLabel(value) {
  if (value === 'QUESTION') return '错题'
  if (value === 'NOTE') return '笔记'
  return value || '-'
}

export function normalizeReviewDates(dates) {
  return (dates || [])
    .map((item) => {
      if (!item) return ''
      if (typeof item === 'string') return item.slice(0, 10)
      const date = new Date(item)
      if (Number.isNaN(date.getTime())) return ''
      return date.toISOString().slice(0, 10)
    })
    .filter(Boolean)
}
