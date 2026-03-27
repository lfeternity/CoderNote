<template>
  <section class="fade-in">
    <h2>编辑资料</h2>
    <div class="surface-card block" v-loading="loading">
      <MaterialForm
        :model-value="formModel"
        :material-type-options="materialTypeOptions"
        :language-options="languageOptions"
        :tag-options="tagOptions"
        :question-options="questionOptions"
        @submit="onSubmit"
        @cancel="$router.push('/study-material/list')"
      />
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MaterialForm from '../components/material/MaterialForm.vue'
import { getMaterialDetail, updateMaterial } from '../api/material'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { getQuestionList } from '../api/question'
import { toZhLanguageOptions, toZhMaterialTypeOptions } from '../utils/material'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const formModel = reactive({
  title: '',
  materialType: '',
  language: '',
  coverPath: '',
  tagNames: [],
  content: '',
  contentAttachments: [],
  source: '',
  remark: '',
  manualQuestionIds: []
})

const materialTypeOptions = ref([])
const languageOptions = ref([])
const tagOptions = ref([])
const questionOptions = ref([])

async function loadOptions() {
  const data = await getOptions()
  languageOptions.value = toZhLanguageOptions(data.languages)
  materialTypeOptions.value = toZhMaterialTypeOptions(data.materialTypes)
}

async function loadTags() {
  tagOptions.value = await getTagList()
}

async function loadQuestionOptions() {
  const data = await getQuestionList({ pageNo: 1, pageSize: 200 })
  questionOptions.value = data.records || []
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getMaterialDetail(route.params.materialId)
    Object.assign(formModel, {
      title: data.title,
      materialType: data.materialType,
      language: data.language,
      coverPath: data.coverPath || '',
      tagNames: data.tagNames || [],
      content: data.content,
      contentAttachments: data.contentAttachments || [],
      source: data.source,
      remark: data.remark,
      manualQuestionIds: data.manualQuestionIds || []
    })
  } finally {
    loading.value = false
  }
}

async function onSubmit(payload) {
  await updateMaterial(route.params.materialId, payload)
  ElMessage.success('资料编辑成功')
  router.push('/study-material/list')
}

loadOptions()
loadTags()
loadQuestionOptions()
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
