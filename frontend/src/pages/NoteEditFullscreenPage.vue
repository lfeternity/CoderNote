<template>
  <section class="note-edit-fullscreen-page fade-in" v-loading="loading">
    <NoteForm
      :model-value="formModel"
      :language-options="languageOptions"
      :tag-options="tagOptions"
      :question-options="questionOptions"
      :material-options="materialOptions"
      submit-text="保存修改"
      :submitting="submitting"
      :draft-key="`edit_${route.params.noteId}`"
      :force-fullscreen="true"
      :fullscreen-exit-path="`/note/update/${route.params.noteId}`"
      @submit="onSubmit"
      @cancel="$router.push(`/note/update/${route.params.noteId}`)"
    />
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NoteForm from '../components/note/NoteForm.vue'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { getQuestionList } from '../api/question'
import { getMaterialList } from '../api/material'
import { getNoteDetail, updateNote } from '../api/note'
import { toZhLanguageOptions } from '../utils/material'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const languageOptions = ref([])
const tagOptions = ref([])
const questionOptions = ref([])
const materialOptions = ref([])

const formModel = reactive({
  title: '',
  content: '',
  versionSummary: '',
  language: '',
  coverPath: '',
  tagNames: [],
  manualQuestionIds: [],
  manualMaterialIds: []
})

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = toZhLanguageOptions(data.languages)
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadQuestionOptions() {
  const data = await getQuestionList({ pageNo: 1, pageSize: 200 })
  questionOptions.value = data.records || []
}

async function loadMaterialOptions() {
  const data = await getMaterialList({ pageNo: 1, pageSize: 200 })
  materialOptions.value = data.records || []
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getNoteDetail(route.params.noteId)
    Object.assign(formModel, {
      title: data.title || '',
      content: data.content || '',
      versionSummary: '',
      language: data.language || '',
      coverPath: data.coverPath || '',
      tagNames: data.tagNames || [],
      manualQuestionIds: data.manualQuestionIds || [],
      manualMaterialIds: data.manualMaterialIds || []
    })
  } finally {
    loading.value = false
  }
}

async function onSubmit(payload, helpers) {
  submitting.value = true
  try {
    await updateNote(route.params.noteId, payload)
    helpers?.syncDraft?.(payload)
    ElMessage.success('笔记编辑成功')
    router.push('/note/list')
  } finally {
    submitting.value = false
  }
}

loadOptions()
loadTags()
loadQuestionOptions()
loadMaterialOptions()
loadDetail()
</script>

<style scoped>
.note-edit-fullscreen-page {
  width: 100%;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: var(--bg);
}

.note-edit-fullscreen-page :deep(.note-compose) {
  height: 100vh;
}
</style>
