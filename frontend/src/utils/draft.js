const DRAFT_PREFIX = 'draft'
const DRAFT_SCHEMA_VERSION = 1
const DEFAULT_EXPIRE_DAYS = 7
const DAY_MS = 24 * 60 * 60 * 1000

function safeGetItem(key) {
  try {
    return localStorage.getItem(key)
  } catch (_) {
    return null
  }
}

function safeSetItem(key, value) {
  try {
    localStorage.setItem(key, value)
    return { ok: true }
  } catch (error) {
    return { ok: false, error }
  }
}

function safeRemoveItem(key) {
  try {
    localStorage.removeItem(key)
  } catch (_) {
    // ignore remove failure
  }
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function normalizeSegment(value, fallback) {
  const text = String(value ?? '').trim()
  return text || fallback
}

function normalizeTimestamp(value, fallback) {
  const timestamp = Number(value)
  return Number.isFinite(timestamp) && timestamp > 0 ? timestamp : fallback
}

function createRecord(payload, now, expireDays) {
  const safeDays = Number.isFinite(Number(expireDays)) && Number(expireDays) > 0
    ? Number(expireDays)
    : DEFAULT_EXPIRE_DAYS
  const savedAt = now
  const expireAt = savedAt + safeDays * DAY_MS
  return {
    schemaVersion: DRAFT_SCHEMA_VERSION,
    savedAt,
    expireAt,
    payload
  }
}

function parseRecord(raw, now) {
  if (!raw) return null

  let parsed
  try {
    parsed = JSON.parse(raw)
  } catch (_) {
    return { corrupted: true }
  }

  if (!isPlainObject(parsed)) {
    return { corrupted: true }
  }

  if (parsed.schemaVersion !== DRAFT_SCHEMA_VERSION) {
    // Backward compatibility: previous versions may save plain payload directly.
    if (parsed.payload === undefined) {
      return {
        payload: parsed,
        savedAt: now,
        expireAt: now + DEFAULT_EXPIRE_DAYS * DAY_MS,
        legacy: true
      }
    }
    return { corrupted: true }
  }

  if (!isPlainObject(parsed.payload)) {
    return { corrupted: true }
  }

  const savedAt = normalizeTimestamp(parsed.savedAt, now)
  const expireAt = normalizeTimestamp(parsed.expireAt, savedAt + DEFAULT_EXPIRE_DAYS * DAY_MS)

  return {
    payload: parsed.payload,
    savedAt,
    expireAt
  }
}

export function buildDraftStorageKey({ userId, bizType, bizId }) {
  const safeUserId = normalizeSegment(userId, 'anonymous')
  const safeBizType = normalizeSegment(bizType, 'UNKNOWN')
  const safeBizId = normalizeSegment(bizId, 'new')
  return `${DRAFT_PREFIX}:${safeUserId}:${safeBizType}:${safeBizId}`
}

export function formatDraftSavedAt(value) {
  const timestamp = normalizeTimestamp(value, Date.now())
  const date = new Date(timestamp)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

export function readDraftRecord(storageKey, { now = Date.now() } = {}) {
  const raw = safeGetItem(storageKey)
  if (!raw) return null

  const parsed = parseRecord(raw, now)
  if (!parsed || parsed.corrupted) {
    safeRemoveItem(storageKey)
    return null
  }

  if (parsed.expireAt <= now) {
    safeRemoveItem(storageKey)
    return null
  }

  if (parsed.legacy) {
    const rewritten = saveDraftRecord(storageKey, parsed.payload, { now: parsed.savedAt })
    if (!rewritten.ok) {
      return null
    }
    return {
      payload: parsed.payload,
      savedAt: rewritten.savedAt,
      expireAt: rewritten.expireAt
    }
  }

  return parsed
}

export function saveDraftRecord(storageKey, payload, { now = Date.now(), expireDays = DEFAULT_EXPIRE_DAYS } = {}) {
  if (!isPlainObject(payload)) {
    return {
      ok: false,
      error: new Error('Invalid draft payload')
    }
  }

  const record = createRecord(payload, now, expireDays)
  const writeResult = safeSetItem(storageKey, JSON.stringify(record))
  if (!writeResult.ok) {
    return {
      ok: false,
      error: writeResult.error
    }
  }

  return {
    ok: true,
    savedAt: record.savedAt,
    expireAt: record.expireAt
  }
}

export function removeDraftRecord(storageKey) {
  safeRemoveItem(storageKey)
}

export function cleanupExpiredDraftsForUser(userId, { now = Date.now() } = {}) {
  const safeUserId = normalizeSegment(userId, 'anonymous')
  const prefix = `${DRAFT_PREFIX}:${safeUserId}:`

  let total = 0
  let cleaned = 0

  try {
    for (let index = localStorage.length - 1; index >= 0; index -= 1) {
      const key = localStorage.key(index)
      if (!key || !key.startsWith(prefix)) continue

      total += 1
      const parsed = parseRecord(localStorage.getItem(key), now)
      if (!parsed || parsed.corrupted || parsed.expireAt <= now) {
        safeRemoveItem(key)
        cleaned += 1
      }
    }
  } catch (_) {
    return { total: 0, cleaned: 0 }
  }

  return { total, cleaned }
}
