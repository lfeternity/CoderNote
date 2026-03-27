const MAJOR_LABEL_MAP = {
  'Computer Science': '计算机科学',
  'Software Engineering': '软件工程',
  'Big Data': '大数据',
  AI: '人工智能',
  'Network Engineering': '网络工程'
}

export function toZhMajorLabel(value) {
  if (!value) return ''
  return MAJOR_LABEL_MAP[value] || value
}

export function toZhMajorOption(item) {
  if (typeof item === 'string') {
    return {
      label: toZhMajorLabel(item),
      value: item
    }
  }
  const value = item?.value || ''
  const originalLabel = item?.label || value
  return {
    ...item,
    label: toZhMajorLabel(value) || toZhMajorLabel(originalLabel) || originalLabel
  }
}

export function toZhMajorOptions(list) {
  return (list || []).map(toZhMajorOption)
}