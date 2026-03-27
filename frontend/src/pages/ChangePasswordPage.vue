<template>
  <section class="fade-in">
    <div class="head">
      <div class="page-title-back">
        <el-button class="back-chevron-btn" plain aria-label="返回个人中心" @click="$router.push('/user/center')">&lt;</el-button>
        <h2>修改密码</h2>
      </div>
    </div>
    <div class="surface-card card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 560px;">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">提交修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { changePassword } from '../api/user'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,16}$/, message: '新密码需为6-16位字母+数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    await changePassword(form)
    ElMessage.success('密码修改成功，请重新登录')
    authStore.clearProfile()
    router.push('/login')
  })
}
</script>

<style scoped>
.head {
  margin-bottom: 12px;
}

.head h2 {
  margin: 0;
  color: var(--primary);
}

.card {
  padding: 18px;
}
</style>
