<template>
  <section class="fade-in">
    <h2>个人中心</h2>

    <div class="panel-grid">
      <div class="surface-card block">
        <h3>基础信息</h3>

        <div class="avatar-section">
          <UserAvatar :src="profile.avatarUrl" :nickname="profile.nickname" :size="108" />
          <div class="avatar-action-group">
            <el-button type="primary" @click="avatarDialogVisible = true">更换头像</el-button>
            <el-button plain :disabled="!profile.avatarUrl" @click="onResetAvatar">恢复默认头像</el-button>
          </div>
        </div>

        <el-form label-width="98px">
          <el-form-item label="昵称">
            <el-input :model-value="profile.nickname" disabled />
          </el-form-item>
          <el-form-item label="学号">
            <el-input v-model="form.studentNo" :disabled="!isEditing" />
          </el-form-item>
          <el-form-item label="专业">
            <el-select
              v-model="form.major"
              style="width: 100%"
              :disabled="!isEditing"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入专业"
            >
              <el-option v-for="item in majorSelectOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" :disabled="!isEditing" />
          </el-form-item>
          <el-form-item>
            <el-button v-if="!isEditing" type="primary" plain @click="startEdit">编辑资料</el-button>
            <el-button v-else type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="surface-card block">
        <h3>快捷入口</h3>
        <div class="quick-actions">
          <el-button type="primary" plain @click="$router.push('/error-question/list')">我的错题</el-button>
          <el-button type="success" plain @click="$router.push('/study-material/list')">我的资料</el-button>
          <el-button type="warning" plain @click="$router.push('/study-material/favorite')">我的收藏</el-button>
          <el-button type="danger" plain @click="$router.push('/user/change-password')">修改密码</el-button>
        </div>

        <el-descriptions title="账户信息" :column="1" border style="margin-top: 14px">
          <el-descriptions-item label="注册时间">{{ profile.registerTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前专业">{{ displayMajor }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <AvatarCropDialog v-model="avatarDialogVisible" @saved="onAvatarSaved" />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AvatarCropDialog from '../components/user/AvatarCropDialog.vue'
import UserAvatar from '../components/common/UserAvatar.vue'
import { getOptions } from '../api/public'
import { getProfile, resetAvatar as resetUserAvatar, updateProfile } from '../api/user'
import { useAuthStore } from '../store/auth'
import { toZhMajorLabel, toZhMajorOptions } from '../utils/major'

const authStore = useAuthStore()
const profile = reactive({
  nickname: '',
  studentNo: '',
  major: '',
  remark: '',
  registerTime: '',
  avatarUrl: ''
})
const form = reactive({
  studentNo: '',
  major: '',
  remark: ''
})
const majorOptions = ref([])
const isEditing = ref(false)
const avatarDialogVisible = ref(false)

const majorSelectOptions = computed(() => {
  const base = Array.isArray(majorOptions.value) ? majorOptions.value.slice() : []
  const current = String(form.major || '').trim()
  if (!current) {
    return base
  }
  const exists = base.some((item) => String(item?.value || '').trim() === current)
  if (exists) {
    return base
  }
  return [{ label: toZhMajorLabel(current) || current, value: current }, ...base]
})

const displayMajor = computed(() => {
  return profile.major ? toZhMajorLabel(profile.major) : '-'
})

function syncProfile(data) {
  Object.assign(profile, {
    ...data,
    avatarUrl: data.avatarUrl || ''
  })
  Object.assign(form, {
    studentNo: data.studentNo || '',
    major: data.major || '',
    remark: data.remark || ''
  })
  authStore.setProfile(data)
}

async function loadOptions() {
  const data = await getOptions()
  majorOptions.value = toZhMajorOptions(data.majors)
}

async function loadProfile() {
  const data = await getProfile()
  syncProfile(data)
}

function startEdit() {
  isEditing.value = true
}

async function saveProfile() {
  const payload = {
    ...form,
    major: String(form.major || '').trim()
  }
  await updateProfile(payload)
  ElMessage.success('资料更新成功')
  await loadProfile()
  isEditing.value = false
}

async function onResetAvatar() {
  const data = await resetUserAvatar()
  syncProfile(data)
  ElMessage.success('已恢复默认头像')
}

function onAvatarSaved(profileData) {
  if (profileData) {
    syncProfile(profileData)
    return
  }
  loadProfile()
}

onMounted(async () => {
  await loadOptions()
  await loadProfile()
})
</script>

<style scoped>
h2 {
  margin: 0 0 12px;
  color: var(--primary);
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.block {
  padding: 16px;
}

.block h3 {
  margin-top: 0;
}

.avatar-section {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px dashed var(--border-soft);
  border-radius: 12px;
  background: linear-gradient(145deg, var(--surface-soft) 0%, var(--surface) 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.avatar-action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }

  .avatar-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar-action-group {
    justify-content: flex-start;
  }
}
</style>
