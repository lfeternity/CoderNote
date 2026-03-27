<template>
  <el-dialog
    :model-value="modelValue"
    title="更换头像"
    width="760px"
    top="8vh"
    @close="closeDialog"
  >
    <div class="dialog-body">
      <section class="left-panel">
        <div class="picker-row">
          <el-button type="primary" plain @click="chooseFile">选择图片</el-button>
          <input
            ref="fileInputRef"
            class="hidden-input"
            type="file"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
            @change="onFileChange"
          >
        </div>
        <p class="hint">仅支持 JPG/PNG/WEBP 格式，大小不超过 2MB</p>
        <p v-if="fileError" class="error-text">{{ fileError }}</p>

        <div
          ref="stageRef"
          class="crop-stage"
          @mousedown="startDrag"
          @touchstart.prevent="startDrag"
        >
          <img
            v-if="loadedImage"
            :src="imageUrl"
            class="crop-image"
            :style="imageStyle"
            draggable="false"
            alt="头像裁剪图"
          >
          <div v-else class="stage-empty">请先选择图片</div>
        </div>

        <div class="zoom-row">
          <span>缩放</span>
          <el-slider
            v-model="scale"
            :min="minScale"
            :max="maxScale"
            :step="0.01"
            :disabled="!loadedImage"
            @input="onScaleChange"
          />
        </div>
      </section>

      <section class="right-panel">
        <h4>预览效果</h4>
        <div class="preview-box">
          <img v-if="previewUrl" :src="previewUrl" alt="头像预览">
          <div v-else class="preview-empty">暂无预览</div>
        </div>
      </section>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button :disabled="saving" @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!loadedImage" @click="saveAvatarFile">
          确认保存
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAvatar } from '../../api/user'

const CROP_SIZE = 320
const EXPORT_SIZE = 512
const PREVIEW_SIZE = 192
const MAX_FILE_SIZE = 2 * 1024 * 1024
const ACCEPT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const fileInputRef = ref(null)
const stageRef = ref(null)

const fileError = ref('')
const imageUrl = ref('')
const loadedImage = ref(null)
const naturalWidth = ref(0)
const naturalHeight = ref(0)

const scale = ref(1)
const minScale = ref(1)
const maxScale = ref(4)
const offsetX = ref(0)
const offsetY = ref(0)
const previewUrl = ref('')
const saving = ref(false)

const dragging = ref(false)
const lastPoint = ref({ x: 0, y: 0 })

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      cleanupDialogState()
    }
  }
)

const imageStyle = computed(() => {
  return {
    width: `${naturalWidth.value}px`,
    height: `${naturalHeight.value}px`,
    transform: `translate(calc(-50% + ${offsetX.value}px), calc(-50% + ${offsetY.value}px)) scale(${scale.value})`
  }
})

function chooseFile() {
  fileInputRef.value?.click()
}

function closeDialog() {
  emit('update:modelValue', false)
}

function onFileChange(event) {
  const [file] = event?.target?.files || []
  if (!file) {
    return
  }

  fileError.value = ''

  const type = (file.type || '').toLowerCase()
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  const extAllowed = ext === 'jpg' || ext === 'jpeg' || ext === 'png' || ext === 'webp'
  const typeAllowed = ACCEPT_TYPES.has(type)

  if (!extAllowed || !typeAllowed) {
    fileError.value = '仅支持 JPG/PNG/WEBP 格式，大小不超过 2MB'
    clearFileInput()
    return
  }

  if (file.size > MAX_FILE_SIZE) {
    fileError.value = '仅支持 JPG/PNG/WEBP 格式，大小不超过 2MB'
    clearFileInput()
    return
  }

  loadImageFile(file)
}

function loadImageFile(file) {
  const objectUrl = URL.createObjectURL(file)
  const img = new Image()

  img.onload = () => {
    revokeImageUrl()
    imageUrl.value = objectUrl
    loadedImage.value = img
    naturalWidth.value = img.naturalWidth
    naturalHeight.value = img.naturalHeight

    const coverScale = Math.max(CROP_SIZE / naturalWidth.value, CROP_SIZE / naturalHeight.value)
    minScale.value = coverScale
    maxScale.value = Math.max(coverScale * 4, coverScale + 0.5)
    scale.value = coverScale
    offsetX.value = 0
    offsetY.value = 0

    renderPreview()
  }

  img.onerror = () => {
    URL.revokeObjectURL(objectUrl)
    fileError.value = '图片加载失败，请重新选择'
  }

  img.src = objectUrl
  clearFileInput()
}

function onScaleChange() {
  clampOffset()
  renderPreview()
}

function startDrag(event) {
  if (!loadedImage.value) {
    return
  }

  const point = readPoint(event)
  dragging.value = true
  lastPoint.value = point

  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', stopDrag)
  window.addEventListener('touchmove', onDragMove, { passive: false })
  window.addEventListener('touchend', stopDrag)
}

function onDragMove(event) {
  if (!dragging.value) {
    return
  }

  if (event.cancelable) {
    event.preventDefault()
  }

  const point = readPoint(event)
  const dx = point.x - lastPoint.value.x
  const dy = point.y - lastPoint.value.y

  lastPoint.value = point
  offsetX.value += dx
  offsetY.value += dy

  clampOffset()
  renderPreview()
}

function stopDrag() {
  dragging.value = false
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', stopDrag)
  window.removeEventListener('touchmove', onDragMove)
  window.removeEventListener('touchend', stopDrag)
}

function readPoint(event) {
  if (event.touches?.length) {
    return {
      x: event.touches[0].clientX,
      y: event.touches[0].clientY
    }
  }
  return {
    x: event.clientX,
    y: event.clientY
  }
}

function clampOffset() {
  if (!loadedImage.value) {
    return
  }

  const displayWidth = naturalWidth.value * scale.value
  const displayHeight = naturalHeight.value * scale.value

  const maxOffsetX = Math.max(0, (displayWidth - CROP_SIZE) / 2)
  const maxOffsetY = Math.max(0, (displayHeight - CROP_SIZE) / 2)

  offsetX.value = Math.min(maxOffsetX, Math.max(-maxOffsetX, offsetX.value))
  offsetY.value = Math.min(maxOffsetY, Math.max(-maxOffsetY, offsetY.value))
}

function drawCropToCanvas(size) {
  const image = loadedImage.value
  if (!image) {
    return null
  }

  const displayWidth = naturalWidth.value * scale.value
  const displayHeight = naturalHeight.value * scale.value

  const imageLeft = (CROP_SIZE - displayWidth) / 2 + offsetX.value
  const imageTop = (CROP_SIZE - displayHeight) / 2 + offsetY.value

  const sx = (0 - imageLeft) / scale.value
  const sy = (0 - imageTop) / scale.value
  const sw = CROP_SIZE / scale.value
  const sh = CROP_SIZE / scale.value

  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size

  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, size, size)
  ctx.imageSmoothingEnabled = true
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(image, sx, sy, sw, sh, 0, 0, size, size)

  return canvas
}

function renderPreview() {
  const canvas = drawCropToCanvas(PREVIEW_SIZE)
  previewUrl.value = canvas ? canvas.toDataURL('image/jpeg', 0.9) : ''
}

async function saveAvatarFile() {
  if (!loadedImage.value) {
    return
  }

  saving.value = true
  try {
    const exportCanvas = drawCropToCanvas(EXPORT_SIZE)
    if (!exportCanvas) {
      throw new Error('图片加载失败，请重新选择')
    }

    const blob = await canvasToBlob(exportCanvas, 'image/jpeg', 0.86)
    const file = new File([blob], `avatar-${Date.now()}.jpg`, { type: 'image/jpeg' })
    const profile = await uploadAvatar(file)

    emit('saved', profile)
    ElMessage.success('头像已更新')
    closeDialog()
  } catch (error) {
    if (!error?.message) {
      ElMessage.error('上传失败，请重试')
    }
  } finally {
    saving.value = false
  }
}

function canvasToBlob(canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('图片处理失败'))
        return
      }
      resolve(blob)
    }, type, quality)
  })
}

function clearFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function revokeImageUrl() {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
}

function cleanupDialogState() {
  stopDrag()
  revokeImageUrl()
  imageUrl.value = ''
  loadedImage.value = null
  naturalWidth.value = 0
  naturalHeight.value = 0
  scale.value = 1
  minScale.value = 1
  maxScale.value = 4
  offsetX.value = 0
  offsetY.value = 0
  previewUrl.value = ''
  fileError.value = ''
  saving.value = false
  clearFileInput()
}

onBeforeUnmount(() => {
  cleanupDialogState()
})
</script>

<style scoped>
.dialog-body {
  display: grid;
  grid-template-columns: 1fr 220px;
  gap: 16px;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hidden-input {
  display: none;
}

.hint {
  margin: 0;
  color: #5f6f95;
  font-size: 12px;
}

.error-text {
  margin: 0;
  color: #d73a49;
  font-size: 13px;
}

.crop-stage {
  width: 320px;
  height: 320px;
  border-radius: 14px;
  border: 1px solid #d4def2;
  background: linear-gradient(145deg, #f8fbff 0%, #edf3ff 100%);
  overflow: hidden;
  position: relative;
  cursor: grab;
  user-select: none;
}

.crop-stage:active {
  cursor: grabbing;
}

.crop-image {
  position: absolute;
  left: 50%;
  top: 50%;
  transform-origin: center center;
  max-width: none;
  pointer-events: none;
}

.stage-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a94ab;
  font-size: 14px;
}

.zoom-row {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 12px;
  width: 320px;
}

.zoom-row span {
  color: #40557f;
  font-size: 13px;
}

.right-panel {
  border: 1px solid #d4def2;
  border-radius: 14px;
  background: #f7faff;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.right-panel h4 {
  margin: 0;
  color: #2f4b8d;
  font-size: 14px;
}

.preview-box {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #ffffff;
  box-shadow: 0 8px 20px rgba(30, 64, 175, 0.15);
  background: linear-gradient(135deg, #2f73ea 0%, #1e40af 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-empty {
  color: #d9e4ff;
  font-size: 12px;
}

@media (max-width: 860px) {
  .dialog-body {
    grid-template-columns: 1fr;
  }

  .crop-stage,
  .zoom-row {
    width: 100%;
    max-width: 320px;
  }

  .right-panel {
    align-items: flex-start;
  }
}
</style>