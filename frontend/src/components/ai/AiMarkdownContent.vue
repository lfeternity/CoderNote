<template>
  <div ref="containerRef" class="ai-markdown-body" v-html="htmlContent"></div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { renderMarkdownSafe } from '../../utils/markdown'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const containerRef = ref(null)
const htmlContent = computed(() => renderMarkdownSafe(props.content || ''))

watch(
  htmlContent,
  async () => {
    await nextTick()
    mountCopyButtons()
  },
  { immediate: true }
)

async function copyCode(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('代码已复制')
  } catch (_) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function mountCopyButtons() {
  const container = containerRef.value
  if (!container) return

  const codeBlocks = container.querySelectorAll('pre.md-code')
  codeBlocks.forEach((block) => {
    block.classList.add('ai-code-block')
    const oldBtn = block.querySelector('.ai-copy-btn')
    if (oldBtn) {
      oldBtn.remove()
    }

    const codeText = block.textContent || ''
    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'ai-copy-btn'
    button.textContent = '复制'
    button.onclick = () => copyCode(codeText)
    block.appendChild(button)
  })
}
</script>

<style scoped>
.ai-markdown-body {
  line-height: 1.72;
}

.ai-markdown-body :deep(h1),
.ai-markdown-body :deep(h2),
.ai-markdown-body :deep(h3),
.ai-markdown-body :deep(h4),
.ai-markdown-body :deep(h5),
.ai-markdown-body :deep(h6) {
  margin: 10px 0 8px;
  color: #1f3b7a;
}

.ai-markdown-body :deep(p) {
  margin: 8px 0;
}

.ai-markdown-body :deep(ul),
.ai-markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.ai-markdown-body :deep(code) {
  background: #eef2ff;
  border-radius: 4px;
  padding: 2px 6px;
  font-family: 'Consolas', 'Courier New', monospace;
}

.ai-markdown-body :deep(.ai-code-block),
.ai-markdown-body :deep(.md-code) {
  position: relative;
  margin: 10px 0;
  padding: 12px;
  background: #1f2937;
  color: #f8fafc;
  border-radius: 10px;
  overflow-x: auto;
  border: 1px solid #334155;
}

.ai-markdown-body :deep(.md-code code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.ai-markdown-body :deep(.ai-copy-btn) {
  position: absolute;
  top: 8px;
  right: 8px;
  border: 1px solid #64748b;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.78);
  color: #e2e8f0;
  font-size: 12px;
  padding: 2px 8px;
  cursor: pointer;
}

.ai-markdown-body :deep(.ai-copy-btn:hover) {
  background: rgba(30, 41, 59, 0.92);
}

@media (prefers-color-scheme: dark) {
  .ai-markdown-body :deep(h1),
  .ai-markdown-body :deep(h2),
  .ai-markdown-body :deep(h3),
  .ai-markdown-body :deep(h4),
  .ai-markdown-body :deep(h5),
  .ai-markdown-body :deep(h6) {
    color: #dbeafe;
  }

  .ai-markdown-body :deep(code) {
    background: #1e293b;
    color: #e2e8f0;
  }
}
</style>
