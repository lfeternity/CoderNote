<template>
  <section class="fade-in">
    <h2>新增错题</h2>
    <div class="surface-card block">
      <QuestionForm
        :language-options="languageOptions"
        :tag-options="tagOptions"
        :material-options="materialOptions"
        draft-key="new"
        @submit="onSubmit"
        @cancel="$router.push('/error-question/list')"
      />
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import QuestionForm from '../components/question/QuestionForm.vue'
import { addQuestion } from '../api/question'
import { getOptions } from '../api/public'
import { getTagList } from '../api/tag'
import { getMaterialList } from '../api/material'
import { useRouter } from 'vue-router'

const router = useRouter()
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

async function onSubmit(payload, helpers) {
  const questionId = await addQuestion(payload)
  helpers?.promoteDraft?.(questionId, payload)
  ElMessage.success('错题新增成功')
  router.push('/error-question/list')
}

loadOptions()
loadTagOptions()
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
