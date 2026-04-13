<template>
  <div class="cover-picker">
    <div class="cover-preview" :class="{ 'is-default': !previewUrl }" :style="previewStyle">
      <div class="cover-preview-mask">
        <span class="preview-label">{{ previewLabel }}</span>
      </div>
    </div>

    <div class="cover-actions">
      <el-upload
        :show-file-list="false"
        :accept="coverAccept"
        :http-request="handleUpload"
      >
        <el-button size="small" plain>上传封面</el-button>
      </el-upload>
      <el-button size="small" text :disabled="!innerPath" @click="resetDefaultCover">恢复默认封面</el-button>
      <span class="cover-tip">建议尺寸 1200x760，支持 JPG/PNG/WEBP。</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadCoverImage } from '../../api/file'
import { buildCoverPreviewUrl } from '../../utils/bookshelf'
import builtInDefaultCoverImage from '../../assets/default-bookshelf-cover.svg'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  bizType: {
    type: String,
    default: 'question'
  },
  previewLabel: {
    type: String,
    default: '封面预览'
  },
  defaultCoverImage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const coverAccept = '.png,.jpg,.jpeg,.webp'
const innerPath = ref('')
const previewUrl = ref('')

const previewStyle = computed(() => {
  if (previewUrl.value) {
    return {
      backgroundImage: `url("${previewUrl.value}")`
    }
  }
  return {
    backgroundImage: `url("${props.defaultCoverImage || builtInDefaultCoverImage}")`
  }
})

function syncFromValue(value) {
  innerPath.value = value || ''
  previewUrl.value = innerPath.value ? buildCoverPreviewUrl(innerPath.value, 'cover') : ''
}

watch(
  () => props.modelValue,
  (value) => {
    syncFromValue(value)
  },
  { immediate: true }
)

async function handleUpload(options) {
  try {
    const data = await uploadCoverImage(options.file, props.bizType)
    const coverPath = data?.path || ''
    innerPath.value = coverPath
    previewUrl.value = coverPath ? buildCoverPreviewUrl(coverPath, data?.fileName || 'cover') : ''
    emit('update:modelValue', coverPath)
    options.onSuccess?.(data)
    ElMessage.success('封面上传成功')
  } catch (error) {
    options.onError?.(error)
  }
}

function resetDefaultCover() {
  innerPath.value = ''
  previewUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped>
.cover-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.cover-preview {
  position: relative;
  width: min(100%, 220px);
  aspect-ratio: 7 / 10;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(240, 216, 197, 0.6);
  box-shadow: 0 10px 20px rgba(20, 20, 19, 0.12);
  background-size: cover;
  background-position: center;
}

.cover-preview.is-default {
  background-size: cover;
  background-position: center;
}

.cover-preview-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(180deg, rgba(28, 20, 15, 0) 0%, rgba(28, 20, 15, 0.76) 100%);
}

.preview-label {
  font-size: 12px;
  color: rgba(255, 243, 234, 0.94);
}

.cover-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.cover-tip {
  color: var(--text-sub);
  font-size: 12px;
}
</style>
