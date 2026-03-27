<template>
  <section class="fade-in bookshelf-page">
    <div class="toolbar">
      <h2>标签库</h2>
      <el-button type="primary" @click="openAddDialog">新增标签</el-button>
    </div>

    <div class="surface-card block">
      <div class="bookshelf-grid-meta">
        <span>共 {{ rows.length }} 个标签</span>
      </div>

      <div class="bookshelf-body" v-loading="loading">
        <div v-if="rows.length" class="bookshelf-grid tag-bookshelf-grid">
          <article
            v-for="row in rows"
            :key="row.id"
            class="bookshelf-card tag-bookshelf-card"
            :style="coverVars(row)"
            @click="goRelation(row.id)"
          >
            <div class="bookshelf-cover-bg"></div>

            <div class="bookshelf-content tag-bookshelf-content">
              <el-tooltip :content="row.name" placement="top">
                <p class="bookshelf-title-link tag-title">{{ row.name }}</p>
              </el-tooltip>

              <div class="tag-usage-wrap">
                <span class="tag-usage-label">使用次数</span>
                <span class="tag-usage-count">{{ row.usageCount || 0 }}</span>
              </div>
            </div>

            <div class="bookshelf-hover-actions tag-hover-actions" @click.stop>
              <el-button class="bookshelf-action" text type="primary" :disabled="!row.canEdit" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button class="bookshelf-action" text type="danger" :disabled="!row.canDelete" @click="onDelete(row)">
                删除
              </el-button>
            </div>
          </article>
        </div>

        <div v-else class="bookshelf-empty-wrap">
          <div class="bookshelf-empty">
            <div class="bookshelf-empty-illustration"></div>
            <p class="bookshelf-empty-text">暂无标签，先新增一个标签吧</p>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增标签' : '编辑标签'" width="420px">
      <el-form :model="dialogForm" label-width="90px">
        <el-form-item label="标签名称">
          <el-input v-model="dialogForm.name" placeholder="请输入标签名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDialog">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addTag, deleteTag, getTagList, updateTag } from '../api/tag'
import { getBookshelfSeed, resolveBookshelfTheme } from '../utils/bookshelf'
import defaultCoverImage from '../assets/default-bookshelf-cover.svg'

const router = useRouter()
const loading = ref(false)
const rows = ref([])

const dialogVisible = ref(false)
const dialogMode = ref('add')
const currentTagId = ref(null)
const dialogForm = reactive({ name: '' })

function coverVars(row) {
  const seed = getBookshelfSeed(row, row.name || '标签')
  const theme = resolveBookshelfTheme(seed)
  return {
    '--book-main': theme.main,
    '--book-soft': theme.soft,
    '--cover-gradient': theme.gradient,
    '--cover-image': `url("${defaultCoverImage}")`
  }
}

function goRelation(tagId) {
  router.push(`/tag/relation/${tagId}`)
}

async function loadTags() {
  loading.value = true
  try {
    rows.value = await getTagList()
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  dialogMode.value = 'add'
  currentTagId.value = null
  dialogForm.name = ''
  dialogVisible.value = true
}

function openEditDialog(row) {
  if (!row.canEdit) return
  dialogMode.value = 'edit'
  currentTagId.value = row.id
  dialogForm.name = row.name
  dialogVisible.value = true
}

async function submitDialog() {
  const name = dialogForm.name.trim()
  if (!name) {
    ElMessage.warning('标签名称不能为空')
    return
  }

  if (dialogMode.value === 'add') {
    await addTag({ name })
    ElMessage.success('标签新增成功')
  } else {
    await updateTag(currentTagId.value, { name })
    ElMessage.success('标签编辑成功')
  }

  dialogVisible.value = false
  await loadTags()
}

async function onDelete(row) {
  if (!row.canDelete) return
  await ElMessageBox.confirm(`确认删除标签“${row.name}”？`, '删除确认', { type: 'warning' })
  await deleteTag(row.id)
  ElMessage.success('标签删除成功')
  await loadTags()
}

loadTags()
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

h2 {
  margin: 0;
  color: var(--primary);
}

.bookshelf-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.block {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.tag-bookshelf-grid {
  padding-left: 36px;
}

.tag-bookshelf-card {
  cursor: pointer;
}

.tag-bookshelf-content {
  gap: 6px;
}

.tag-title {
  -webkit-line-clamp: 2;
  min-height: 34px;
}

.tag-usage-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.tag-usage-label {
  border-radius: 999px;
  border: 1px solid rgba(219, 240, 255, 0.55);
  background: rgba(6, 18, 38, 0.32);
  color: #dff2ff;
  padding: 1px 7px;
  font-size: 11px;
  line-height: 1.2;
  white-space: nowrap;
}

.tag-usage-count {
  color: rgba(220, 237, 255, 0.95);
  font-size: 13px;
  line-height: 1.2;
  font-weight: 600;
  text-shadow: 0 2px 8px rgba(8, 22, 48, 0.54);
}

.tag-hover-actions {
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .toolbar {
    margin-bottom: 10px;
  }

  .tag-bookshelf-grid {
    padding-left: 6px;
  }
}
</style>
