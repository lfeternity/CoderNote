const COVER_THEME_BY_KEY = {
  java: { main: '#4EA4FF', soft: '#D8ECFF', gradient: 'linear-gradient(135deg, #0A3268 0%, #1F6BD7 48%, #56C8FF 100%)' },
  python: { main: '#49D87A', soft: '#DDFBEA', gradient: 'linear-gradient(135deg, #0F3F3C 0%, #1F8B77 52%, #63D5A3 100%)' },
  redis: { main: '#5FB5FF', soft: '#DDEEFF', gradient: 'linear-gradient(135deg, #0A3D7A 0%, #2867D6 55%, #61D2FF 100%)' },
  mysql: { main: '#6CC8FF', soft: '#DDF6FF', gradient: 'linear-gradient(135deg, #0A2F4C 0%, #145A9C 56%, #4DC9FF 100%)' },
  frontend: { main: '#7BE1F0', soft: '#E1FAFF', gradient: 'linear-gradient(135deg, #093B4D 0%, #0F7084 58%, #49D1EE 100%)' },
  javascript: { main: '#FFCF67', soft: '#FFF5DC', gradient: 'linear-gradient(135deg, #5D4511 0%, #A97610 57%, #FFD677 100%)' },
  vue: { main: '#66E3AD', soft: '#E3FAEF', gradient: 'linear-gradient(135deg, #0A4836 0%, #158066 56%, #6EE4B2 100%)' },
  spring: { main: '#84DD79', soft: '#E8FCE3', gradient: 'linear-gradient(135deg, #1E4B1F 0%, #3A7A34 56%, #92E37F 100%)' },
  algorithm: { main: '#7FB1FF', soft: '#E5EEFF', gradient: 'linear-gradient(135deg, #13224F 0%, #274AA6 56%, #7CB9FF 100%)' },
  network: { main: '#70E2E9', soft: '#E0FBFC', gradient: 'linear-gradient(135deg, #103B52 0%, #1B6788 52%, #5ED7E8 100%)' }
}

const COVER_FALLBACK_THEMES = [
  { main: '#5FB5FF', soft: '#DDEEFF', gradient: 'linear-gradient(135deg, #0A3D7A 0%, #2867D6 55%, #61D2FF 100%)' },
  { main: '#70E2E9', soft: '#E0FBFC', gradient: 'linear-gradient(135deg, #103B52 0%, #1B6788 52%, #5ED7E8 100%)' },
  { main: '#66E3AD', soft: '#E3FAEF', gradient: 'linear-gradient(135deg, #0A4836 0%, #158066 56%, #6EE4B2 100%)' },
  { main: '#FFCF67', soft: '#FFF5DC', gradient: 'linear-gradient(135deg, #5D4511 0%, #A97610 57%, #FFD677 100%)' },
  { main: '#FF9AB0', soft: '#FFE8EE', gradient: 'linear-gradient(135deg, #5A1E37 0%, #A83C68 57%, #FFA8C0 100%)' },
  { main: '#A5A1FF', soft: '#ECEAFF', gradient: 'linear-gradient(135deg, #252257 0%, #5348B8 57%, #AEA9FF 100%)' }
]

function normalizeKey(raw) {
  if (!raw) return ''
  return String(raw).trim().toLowerCase()
}

function tokenize(value) {
  return normalizeKey(value)
    .split(/[\s\-_/+#.]+/)
    .filter(Boolean)
}

function findKnownTheme(seed) {
  const keys = tokenize(seed)
  for (const key of keys) {
    if (COVER_THEME_BY_KEY[key]) {
      return COVER_THEME_BY_KEY[key]
    }
  }
  const normalized = normalizeKey(seed)
  if (normalized && COVER_THEME_BY_KEY[normalized]) {
    return COVER_THEME_BY_KEY[normalized]
  }
  return null
}

function hashText(value) {
  let hash = 0
  const text = String(value || '')
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash << 5) - hash + text.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

export function resolveBookshelfTheme(seed) {
  const knownTheme = findKnownTheme(seed)
  if (knownTheme) {
    return knownTheme
  }
  const index = hashText(seed || 'default') % COVER_FALLBACK_THEMES.length
  return COVER_FALLBACK_THEMES[index]
}

export function getBookshelfSeed(row, fallback = '') {
  if (!row) return fallback
  const firstTag = (row.tagNames || [])[0] || ''
  return firstTag || row.languageZh || row.language || row.materialTypeZh || row.materialType || fallback
}

export function getBookshelfInitial(seed) {
  const text = String(seed || '').trim()
  if (!text) return 'BK'
  const first = text[0]
  const isLatin = /^[a-zA-Z]$/.test(first)
  if (isLatin) {
    return first.toUpperCase()
  }
  return first
}

export function clampTags(tags, max = 2) {
  return (tags || []).slice(0, max)
}

export function formatBookshelfTime(value) {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

export function buildCoverPreviewUrl(coverPath, fileName = 'cover') {
  if (!coverPath) return ''
  const path = encodeURIComponent(String(coverPath).trim())
  const name = encodeURIComponent(fileName)
  return `/api/v1/file/download?path=${path}&name=${name}`
}
