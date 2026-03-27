<template>
  <section class="fade-in">
    <h2>编辑错题</h2>
    <div class="surface-card block" v-loading="loading">
      <QuestionForm
        :model-value="formModel"
        :language-options="languageOptions"
        :tag-options="tagOptions"
        :material-options="materialOptions"
        :draft-key="String(route.params.questionId || 'edit')"
        @submit="onSubmit"
        @cancel="$router.push('/error-question/list')"
      />
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QuestionForm from '../components/question/QuestionForm.vue'
import { getQuestionDetail, updateQuestion } from '../api/question'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { getMaterialList } from '../api/material'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const formModel = reactive({
  title: '',
  language: '',
  coverPath: '',
  tagNames: [],
  errorCode: '',
  errorReason: '',
  correctCode: '',
  solution: '',
  source: '',
  remark: '',
  manualMaterialIds: [],
  errorQuestionAttachments: [],
  correctSolutionAttachments: []
})

const languageOptions = ref([])
const tagOptions = ref([])
const materialOptions = ref([])

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = data.languages || []
}

async function loadTagOptions() {
  tagOptions.value = await getTagList()
}

async function loadMaterialOptions() {
  const data = await getMaterialList({ pageNo: 1, pageSize: 200 })
  materialOptions.value = data.records || []
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getQuestionDetail(route.params.questionId)
    Object.assign(formModel, {
      title: data.title,
      language: data.language,
      coverPath: data.coverPath || '',
      tagNames: data.tagNames || [],
      errorCode: data.errorCode,
      errorReason: data.errorReason,
      correctCode: data.correctCode,
      solution: data.solution,
      source: data.source,
      remark: data.remark,
      manualMaterialIds: data.manualMaterialIds || [],
      errorQuestionAttachments: data.errorQuestionAttachments || [],
      correctSolutionAttachments: data.correctSolutionAttachments || []
    })
  } finally {
    loading.value = false
  }
}

async function onSubmit(payload, helpers) {
  await updateQuestion(route.params.questionId, payload)
  helpers?.syncDraft?.(payload)
  ElMessage.success('错题编辑成功')
  router.push('/error-question/list')
}

loadOptions()
loadTagOptions()
loadMaterialOptions()
loadDetail()
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
