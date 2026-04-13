import DOMPurify from 'dompurify'
import { renderMarkdownSafe } from './markdown'

const HTML_PATTERN = /<\/?[a-z][\s\S]*>/i

const SANITIZE_OPTIONS = {
  ALLOWED_TAGS: [
    'a', 'p', 'div', 'span', 'br', 'strong', 'em', 'u', 's', 'del', 'mark',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li',
    'blockquote',
    'pre', 'code',
    'img', 'hr',
    'input'
  ],
  ALLOWED_ATTR: [
    'href', 'target', 'rel',
    'src', 'alt', 'title',
    'class', 'style',
    'type', 'checked',
    'data-type'
  ],
  FORBID_TAGS: ['script', 'iframe', 'object'],
  FORBID_ATTR: ['onerror', 'onclick', 'onload'],
  ALLOW_DATA_ATTR: true
}

function looksLikeHtml(value) {
  return HTML_PATTERN.test(String(value || ''))
}

export function renderNoteContent(content) {
  const raw = String(content || '').trim()
  if (!raw) return ''
  if (!looksLikeHtml(raw)) {
    return renderMarkdownSafe(raw)
  }
  return DOMPurify.sanitize(raw, SANITIZE_OPTIONS)
}
