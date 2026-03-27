<template>
  <section class="fade-in">
    <h2>新建笔记</h2>
    <div class="surface-card block">
      <NoteForm
        :model-value="formModel"
        :language-options="languageOptions"
        :tag-options="tagOptions"
        :question-options="questionOptions"
        :material-options="materialOptions"
        submit-text="保存笔记"
        :submitting="submitting"
        :draft-key="draftKey"
        @submit="onSubmit"
        @cancel="$router.push('/note/list')"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NoteForm from '../components/note/NoteForm.vue'
import { addNote } from '../api/note'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { getQuestionList } from '../api/question'
import { getMaterialList } from '../api/material'
import { toZhLanguageOptions } from '../utils/material'

const route = useRoute()
const router = useRouter()
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

const draftKey = computed(() => {
  const questionId = route.query.questionId || ''
  const materialId = route.query.materialId || ''
  return `add_q${questionId}_m${materialId}`
})

function parsePrefillTags() {
  const raw = String(route.query.tagNames || '').trim()
  if (!raw) return []
  return raw
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function applyPrefillFromRoute() {
  const questionId = Number(route.query.questionId)
  const materialId = Number(route.query.materialId)
  const tagNames = parsePrefillTags()

  if (Number.isInteger(questionId) && questionId > 0) {
    formModel.manualQuestionIds = [questionId]
  }
  if (Number.isInteger(materialId) && materialId > 0) {
    formModel.manualMaterialIds = [materialId]
  }
  if (tagNames.length) {
    formModel.tagNames = tagNames
  }
}

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

async function onSubmit(payload, helpers) {
  submitting.value = true
  try {
    const noteId = await addNote(payload)
    helpers?.promoteDraft?.(noteId, payload)
    ElMessage.success('笔记新增成功')
    router.push('/note/list')
  } finally {
    submitting.value = false
  }
}

applyPrefillFromRoute()
loadOptions()
loadTags()
loadQuestionOptions()
loadMaterialOptions()
</script>

<style scoped>
h2 {
  margin: 0 0 12px;
  color: var(--primary);
}

.block {
  padding: 16px;
}
</style>
