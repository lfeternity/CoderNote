function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeLink(raw) {
  const url = String(raw || '').trim()
  if (!/^https?:\/\//i.test(url)) {
    return ''
  }
  return url
}

function parseInline(value) {
  let text = escapeHtml(value)

  const codeTokens = []
  text = text.replace(/`([^`]+)`/g, (_, code) => {
    const token = `__CODE_TOKEN_${codeTokens.length}__`
    codeTokens.push(`<code>${escapeHtml(code)}</code>`)
    return token
  })

  text = text.replace(/\[([^\]]+)\]\(([^\s)]+)\)/g, (_, label, link) => {
    const safeLink = normalizeLink(link)
    if (!safeLink) {
      return escapeHtml(`${label}(${link})`)
    }
    return `<a href="${escapeHtml(safeLink)}" target="_blank" rel="noopener noreferrer">${escapeHtml(label)}</a>`
  })

  text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  text = text.replace(/(^|\s)\*([^*]+)\*(?=\s|$)/g, '$1<em>$2</em>')

  text = text.replace(/(https?:\/\/[^\s<]+)/g, (match) => {
    const safeLink = normalizeLink(match)
    if (!safeLink) return match
    return `<a href="${escapeHtml(safeLink)}" target="_blank" rel="noopener noreferrer">${escapeHtml(safeLink)}</a>`
  })

  text = text.replace(/__CODE_TOKEN_(\d+)__/g, (_, index) => codeTokens[Number(index)] || '')
  return text
}

function closeListIfNeed(state, html) {
  if (state.inUl) {
    html.push('</ul>')
    state.inUl = false
  }
  if (state.inOl) {
    html.push('</ol>')
    state.inOl = false
  }
}

export function renderMarkdownSafe(markdownText) {
  const source = String(markdownText || '').replace(/\r\n/g, '\n')
  if (!source.trim()) {
    return ''
  }

  const lines = source.split('\n')
  const html = []
  const state = {
    inCode: false,
    codeLang: '',
    codeBuffer: [],
    inUl: false,
    inOl: false
  }

  for (const line of lines) {
    const codeFenceMatch = line.match(/^```\s*([\w+-]*)\s*$/)
    if (codeFenceMatch) {
      if (!state.inCode) {
        closeListIfNeed(state, html)
        state.inCode = true
        state.codeLang = codeFenceMatch[1] || ''
        state.codeBuffer = []
      } else {
        const langClass = state.codeLang ? ` class="lang-${escapeHtml(state.codeLang)}"` : ''
        html.push(`<pre class="md-code"><code${langClass}>${escapeHtml(state.codeBuffer.join('\n'))}</code></pre>`)
        state.inCode = false
        state.codeLang = ''
        state.codeBuffer = []
      }
      continue
    }

    if (state.inCode) {
      state.codeBuffer.push(line)
      continue
    }

    if (!line.trim()) {
      closeListIfNeed(state, html)
      continue
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      closeListIfNeed(state, html)
      const level = heading[1].length
      html.push(`<h${level}>${parseInline(heading[2].trim())}</h${level}>`)
      continue
    }

    const unordered = line.match(/^[-*+]\s+(.+)$/)
    if (unordered) {
      if (!state.inUl) {
        if (state.inOl) {
          html.push('</ol>')
          state.inOl = false
        }
        html.push('<ul>')
        state.inUl = true
      }
      html.push(`<li>${parseInline(unordered[1].trim())}</li>`)
      continue
    }

    const ordered = line.match(/^\d+\.\s+(.+)$/)
    if (ordered) {
      if (!state.inOl) {
        if (state.inUl) {
          html.push('</ul>')
          state.inUl = false
        }
        html.push('<ol>')
        state.inOl = true
      }
      html.push(`<li>${parseInline(ordered[1].trim())}</li>`)
      continue
    }

    closeListIfNeed(state, html)
    html.push(`<p>${parseInline(line.trim())}</p>`)
  }

  if (state.inCode) {
    const langClass = state.codeLang ? ` class="lang-${escapeHtml(state.codeLang)}"` : ''
    html.push(`<pre class="md-code"><code${langClass}>${escapeHtml(state.codeBuffer.join('\n'))}</code></pre>`)
  }

  closeListIfNeed(state, html)
  return html.join('\n')
}