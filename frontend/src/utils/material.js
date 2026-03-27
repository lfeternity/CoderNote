const MATERIAL_TYPE_LABEL_MAP = {
  KNOWLEDGE_NOTE: '\u77E5\u8BC6\u70B9\u7B14\u8BB0',
  SOLUTION_TUTORIAL: '\u89E3\u9898\u6559\u7A0B',
  VIDEO_LINK: '\u89C6\u9891\u94FE\u63A5',
  CODE_TEMPLATE: '\u4EE3\u7801\u6A21\u677F',
  DOC_LINK: '\u6587\u6863\u94FE\u63A5'
}

const LANGUAGE_LABEL_MAP = {
  Java: 'Java',
  Python: 'Python',
  FrontEnd: '\u524D\u7AEF',
  MySQL: 'MySQL',
  'C++': 'C++'
}

const MATERIAL_SOURCE_LABEL_MAP = {
  BiliBili: 'B\u7AD9',
  CSDN: 'CSDN',
  Self: '\u81EA\u5DF1\u6574\u7406',
  Official: '\u5B98\u65B9',
  Other: '\u5176\u4ED6'
}

function toZhByMap(value, map) {
  if (!value) return ''
  return map[value] || value
}

function mapOptionLabel(item, labelMapper) {
  if (typeof item === 'string') {
    return {
      label: labelMapper(item) || item,
      value: item
    }
  }
  const value = item?.value || ''
  const originalLabel = item?.label || value
  return {
    ...item,
    label: labelMapper(value) || labelMapper(originalLabel) || originalLabel
  }
}

export function toZhMaterialTypeLabel(value) {
  return toZhByMap(value, MATERIAL_TYPE_LABEL_MAP)
}

export function toZhLanguageLabel(value) {
  return toZhByMap(value, LANGUAGE_LABEL_MAP)
}

export function toZhMaterialSourceLabel(value) {
  return toZhByMap(value, MATERIAL_SOURCE_LABEL_MAP)
}

export function toZhMaterialTypeOptions(list) {
  return (list || []).map((item) => mapOptionLabel(item, toZhMaterialTypeLabel))
}

export function toZhLanguageOptions(list) {
  return (list || []).map((item) => mapOptionLabel(item, toZhLanguageLabel))
}