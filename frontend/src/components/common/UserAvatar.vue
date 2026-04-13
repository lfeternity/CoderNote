<template>
  <div class="user-avatar" :style="avatarStyle">
    <img
      v-if="showImage"
      class="avatar-image"
      :src="src"
      :alt="`${nickname || '用户'}头像`"
      @error="onImageError"
    >
    <span v-else class="avatar-fallback">{{ initialLetter }}</span>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  nickname: {
    type: String,
    default: ''
  },
  size: {
    type: [Number, String],
    default: 40
  }
})

const imageFailed = ref(false)

watch(
  () => props.src,
  () => {
    imageFailed.value = false
  }
)

const showImage = computed(() => {
  return !!props.src && !imageFailed.value
})

const initialLetter = computed(() => {
  const source = (props.nickname || '').trim()
  if (!source) {
    return 'CN'
  }
  return source.slice(0, 1).toUpperCase()
})

const avatarStyle = computed(() => {
  const side = typeof props.size === 'number' ? `${props.size}px` : props.size
  return {
    width: side,
    height: side,
    '--avatar-size': side
  }
})

function onImageError() {
  imageFailed.value = true
}
</script>

<style scoped>
.user-avatar {
  border-radius: 50%;
  border: 2px solid rgba(250, 249, 245, 0.96);
  box-shadow: 0 8px 20px rgba(20, 20, 19, 0.15);
  overflow: hidden;
  background: linear-gradient(135deg, #d07a59 0%, #b85e3e 100%);
  color: #fffdf9;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  font-size: calc(var(--avatar-size, 40px) * 0.38);
  font-weight: 700;
  letter-spacing: 0.02em;
  user-select: none;
}
</style>
