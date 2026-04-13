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
  --ai-code-bg: #232320;
  --ai-code-border: #3b3a35;
  --ai-code-text: #faf9f5;
  --ai-copy-border: #6e6c66;
  --ai-copy-bg: rgba(20, 20, 19, 0.74);
  --ai-copy-bg-hover: rgba(20, 20, 19, 0.88);
}

:global(:root[data-theme='dark']) .ai-markdown-body {
  --ai-code-bg: #1c1c1a;
  --ai-code-border: #3f3f3a;
  --ai-copy-border: #7b776f;
  --ai-copy-bg: rgba(20, 20, 19, 0.78);
  --ai-copy-bg-hover: rgba(20, 20, 19, 0.9);
}

.ai-markdown-body :deep(h1),
.ai-markdown-body :deep(h2),
.ai-markdown-body :deep(h3),
.ai-markdown-body :deep(h4),
.ai-markdown-body :deep(h5),
.ai-markdown-body :deep(h6) {
  margin: 10px 0 8px;
  color: var(--text-main);
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
  background: var(--code-bg);
  border: 1px solid var(--code-border);
  border-radius: 4px;
  padding: 2px 6px;
  color: var(--text-accent);
  font-family: var(--font-mono);
}

.ai-markdown-body :deep(.ai-code-block),
.ai-markdown-body :deep(.md-code) {
  position: relative;
  margin: 10px 0;
  padding: 12px;
  background: var(--ai-code-bg);
  color: var(--ai-code-text);
  border-radius: 10px;
  overflow-x: auto;
  border: 1px solid var(--ai-code-border);
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
  border: 1px solid var(--ai-copy-border);
  border-radius: 6px;
  background: var(--ai-copy-bg);
  color: var(--ai-code-text);
  font-size: 12px;
  padding: 2px 8px;
  cursor: pointer;
}

.ai-markdown-body :deep(.ai-copy-btn:hover) {
  background: var(--ai-copy-bg-hover);
}
</style>
