<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="9">
        <div class="page-card profile-card">
          <el-avatar :size="88" :src="userStore.info?.avatar || '/img/avatar.svg'" />
          <h3>{{ userStore.info?.nickname }}</h3>
          <p class="username">@{{ userStore.info?.username }}</p>
          <div class="role-tags">
            <el-tag v-for="role in userStore.roles" :key="role" type="success" effect="light">{{ role }}</el-tag>
          </div>
          <el-divider />
          <div class="info-row">
            <span>权限点数量</span>
            <b>{{ userStore.perms.length }}</b>
          </div>
          <div class="info-row">
            <span>可访问菜单</span>
            <b>{{ menuCount }}</b>
          </div>
        </div>
      </el-col>

      <el-col :span="15">
        <div class="page-card">
          <div class="table-toolbar">
            <span class="title">修改密码</span>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 460px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32 位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updatePassword } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const menuCount = computed(() => {
  const count = (nodes) => nodes.reduce(
    (sum, node) => sum + 1 + (node.children ? count(node.children) : 0), 0
  )
  return count(userStore.menus)
})

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-card {
  text-align: center;
  padding: 36px 24px;
}

.profile-card h3 {
  margin: 14px 0 4px;
  color: #111827;
}

.username {
  color: #9ca3af;
  margin: 0 0 14px;
  font-size: 13px;
}

.role-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 28px;
  color: #6b7280;
  font-size: 14px;
}

.info-row b {
  color: var(--el-color-primary);
}
</style>
