<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
    <el-form-item label="资料标题" prop="title">
      <el-input v-model="form.title" placeholder="例如：Java 泛型详解笔记" />
    </el-form-item>

    <el-form-item label="资料类型" prop="materialType">
      <el-select v-model="form.materialType" placeholder="请选择" style="width: 100%">
        <el-option v-for="item in materialTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </el-form-item>

    <el-form-item label="资料封面">
      <CoverPicker
        v-model="form.coverPath"
        biz-type="material"
        preview-label="资料封面预览"
      />
    </el-form-item>

    <el-form-item label="编程语言" prop="language">
      <el-select v-model="form.language" placeholder="请选择" style="width: 100%">
        <el-option v-for="item in languageOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </el-form-item>

    <el-form-item label="知识点标签" prop="tagNames">
      <el-select
        v-model="form.tagNames"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="可多选，可手动新增"
        style="width: 100%"
      >
        <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.name" :value="tag.name" />
      </el-select>
    </el-form-item>

    <el-form-item label="内容/链接" prop="content">
      <el-input v-model="form.content" type="textarea" :rows="5" placeholder="可填写文字、代码或链接" />
      <div class="upload-wrap">
        <el-upload
          :show-file-list="false"
          :accept="uploadAccept"
          :http-request="handleUpload"
        >
          <el-button size="small" plain>上传文档/图片</el-button>
        </el-upload>
      </div>
      <div v-if="form.contentAttachments.length" class="attachment-list">
        <div v-for="(file, index) in form.contentAttachments" :key="file.path || index" class="attachment-item">
          <el-link :href="attachmentDownloadUrl(file)" target="_blank">{{ file.fileName || '附件' }}</el-link>
          <el-tag v-if="file.image" size="small" type="success">图片</el-tag>
          <el-button link type="danger" @click="removeAttachment(index)">移除</el-button>
        </div>
      </div>
    </el-form-item>

    <el-form-item label="手动关联错题">
      <el-select
        v-model="form.manualQuestionIds"
        multiple
        filterable
        placeholder="可补充自动标签联动"
        style="width: 100%"
      >
        <el-option
          v-for="question in questionOptions"
          :key="question.id"
          :label="`${question.title}（${question.masteryStatus || '未标记'}）`"
          :value="question.id"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="资料来源">
      <el-input v-model="form.source" placeholder="B站 / CSDN / 自己整理" />
    </el-form-item>

    <el-form-item label="备注">
      <el-input v-model="form.remark" type="textarea" :rows="2" />
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="onSubmit">提交</el-button>
      <el-button @click="$emit('cancel')">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadMaterialAttachment } from '../../api/material'
import CoverPicker from '../common/CoverPicker.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  materialTypeOptions: {
    type: Array,
    default: () => []
  },
  languageOptions: {
    type: Array,
    default: () => []
  },
  tagOptions: {
    type: Array,
    default: () => []
  },
  questionOptions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['submit', 'cancel'])
const formRef = ref(null)

const uploadAccept = '.png,.jpg,.jpeg,.gif,.webp,.bmp,.pdf,.doc,.docx,.md,.markdown,.txt,.rtf,.ppt,.pptx,.xls,.xlsx'

const form = reactive({
  title: '',
  materialType: '',
  language: '',
  coverPath: '',
  tagNames: [],
  content: '',
  source: '',
  remark: '',
  manualQuestionIds: [],
  contentAttachments: []
})

function validateContent(_, value, callback) {
  const hasText = !!value && value.trim().length > 0
  const hasAttachment = form.contentAttachments.length > 0
  if (hasText || hasAttachment) {
    callback()
    return
  }
  callback(new Error('请填写内容/链接或上传附件'))
}

const rules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  materialType: [{ required: true, message: '请选择资料类型', trigger: 'change' }],
  language: [{ required: true, message: '请选择语言', trigger: 'change' }],
  tagNames: [{ type: 'array', required: true, min: 1, message: '至少选择一个标签', trigger: 'change' }],
  content: [{ validator: validateContent, trigger: ['blur', 'change'] }]
}

watch(
  () => props.modelValue,
  (value) => {
    Object.assign(form, {
      title: value?.title || '',
      materialType: value?.materialType || '',
      language: value?.language || '',
      coverPath: value?.coverPath || '',
      tagNames: value?.tagNames ? [...value.tagNames] : [],
      content: value?.content || '',
      source: value?.source || '',
      remark: value?.remark || '',
      manualQuestionIds: value?.manualQuestionIds ? [...value.manualQuestionIds] : [],
      contentAttachments: value?.contentAttachments ? [...value.contentAttachments] : []
    })
  },
  { immediate: true, deep: true }
)

async function handleUpload(options) {
  try {
    const data = await uploadMaterialAttachment(options.file)
    form.contentAttachments.push(data)
    options.onSuccess?.(data)
    ElMessage.success('附件上传成功')
    formRef.value?.validateField('content')
  } catch (error) {
    options.onError?.(error)
  }
}

function removeAttachment(index) {
  form.contentAttachments.splice(index, 1)
  formRef.value?.validateField('content')
}

function attachmentDownloadUrl(file) {
  if (file?.downloadUrl) return file.downloadUrl
  const path = encodeURIComponent(file?.path || '')
  const name = encodeURIComponent(file?.fileName || 'file')
  return `/api/v1/file/download?path=${path}&name=${name}&download=true`
}

function normalizeAttachmentList(list) {
  return (list || [])
    .filter((item) => item && item.path)
    .map((item) => ({
      fileName: item.fileName,
      path: item.path,
      contentType: item.contentType,
      size: item.size,
      image: item.image
    }))
}

function onSubmit() {
  formRef.value.validate((valid) => {
    if (!valid) return
    emit('submit', {
      ...form,
      title: form.title.trim(),
      materialType: form.materialType.trim(),
      language: form.language.trim(),
      coverPath: form.coverPath || '',
      content: form.content?.trim() || '',
      tagNames: form.tagNames.map((item) => item.trim()).filter(Boolean),
      contentAttachments: normalizeAttachmentList(form.contentAttachments)
    })
  })
}
</script>

<style scoped>
.upload-wrap {
  margin-top: 8px;
}

.attachment-list {
  margin-top: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
  background: #fafbff;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.8;
}
</style>
