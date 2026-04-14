import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const apiBaseURL = (import.meta.env.VITE_API_BASE_URL || '/api/v1').trim()

const request = axios.create({
  baseURL: apiBaseURL || '/api/v1',
  timeout: 10000,
  withCredentials: true
})

function getCookie(name) {
  if (!name || typeof document === 'undefined') return ''
  const encodedName = encodeURIComponent(name)
  const parts = document.cookie ? document.cookie.split('; ') : []
  for (const part of parts) {
    const index = part.indexOf('=')
    if (index <= 0) continue
    const key = part.slice(0, index)
    if (key === encodedName || key === name) {
      return decodeURIComponent(part.slice(index + 1))
    }
  }
  return ''
}

function isMutatingMethod(method) {
  const normalized = String(method || 'GET').trim().toUpperCase()
  return normalized !== 'GET' && normalized !== 'HEAD' && normalized !== 'OPTIONS'
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function toSnakeCase(key) {
  return key.replace(/([A-Z])/g, '_$1').toLowerCase()
}

function toCamelCase(key) {
  return key.replace(/_([a-z])/g, (_, c) => c.toUpperCase())
}

function transformKeysDeep(value, keyTransformer) {
  if (Array.isArray(value)) {
    return value.map((item) => transformKeysDeep(item, keyTransformer))
  }
  if (isPlainObject(value)) {
    return Object.keys(value).reduce((acc, key) => {
      acc[keyTransformer(key)] = transformKeysDeep(value[key], keyTransformer)
      return acc
    }, {})
  }
  return value
}

request.interceptors.request.use((config) => {
  if (isMutatingMethod(config.method)) {
    const csrfToken = getCookie('XSRF-TOKEN')
    if (csrfToken) {
      config.headers = config.headers || {}
      config.headers['X-CSRF-Token'] = csrfToken
    }
  }

  const hasTransformableBody =
    config.data &&
    !(config.data instanceof FormData) &&
    !(config.data instanceof URLSearchParams)

  if (hasTransformableBody) {
    config.data = transformKeysDeep(config.data, toSnakeCase)
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data || {}

    if (body.code === 200) {
      return transformKeysDeep(body.data, toCamelCase)
    }

    if (body.code === 401) {
      localStorage.removeItem('eb_logged_in')
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
      ElMessage.error(body.message || 'Please login first')
      return Promise.reject(new Error(body.message || 'Unauthorized'))
    }

    ElMessage.error(body.message || 'Request failed')
    return Promise.reject(new Error(body.message || 'Request failed'))
  },
  (error) => {
    ElMessage.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default request
