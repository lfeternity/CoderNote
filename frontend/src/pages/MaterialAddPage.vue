<template>
  <section class="fade-in">
    <h2>新增资料</h2>
    <div class="surface-card block">
      <MaterialForm
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
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import MaterialForm from '../components/material/MaterialForm.vue'
import { addMaterial } from '../api/material'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { getQuestionList } from '../api/question'
import { toZhLanguageOptions, toZhMaterialTypeOptions } from '../utils/material'

const router = useRouter()
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

async function onSubmit(payload) {
  await addMaterial(payload)
  ElMessage.success('资料新增成功')
  router.push('/study-material/list')
}

loadOptions()
loadTags()
loadQuestionOptions()
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