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
          <div class="info-row">
            <span>邮箱</span>
            <b>{{ userStore.info?.email || '未设置' }}</b>
          </div>
          <div class="info-row">
            <span>手机号</span>
            <b>{{ userStore.info?.phone || '未设置' }}</b>
          </div>
          <el-button class="edit-btn" type="primary" plain @click="openProfileEditor">编辑资料</el-button>
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

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="profileVisible" title="编辑资料" width="460px" destroy-on-close>
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
        <el-form-item label="头像">
          <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar-preview" alt="头像" />
            <div v-else class="avatar-preview avatar-empty">点击上传</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="profileForm.nickname" maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="选填" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updatePassword, updateProfile } from '@/api/auth'
import request from '@/utils/request'
import { encryptRequired } from '@/utils/crypto'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const profileVisible = ref(false)
const savingProfile = ref(false)
const profileFormRef = ref()
const profileForm = reactive({ nickname: '', avatar: '', email: '', phone: '' })
const profileRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function openProfileEditor() {
  profileForm.nickname = userStore.info?.nickname || ''
  profileForm.avatar = userStore.info?.avatar || ''
  profileForm.email = userStore.info?.email || ''
  profileForm.phone = userStore.info?.phone || ''
  profileVisible.value = true
}

async function uploadAvatar(options) {
  const data = new FormData()
  data.append('file', options.file)
  profileForm.avatar = await request.post('/admin/upload/avatar', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

async function saveProfile() {
  await profileFormRef.value.validate()
  savingProfile.value = true
  try {
    await updateProfile({ ...profileForm })
    await userStore.fetchInfo()
    ElMessage.success('资料已更新')
    profileVisible.value = false
  } finally {
    savingProfile.value = false
  }
}

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
    // 新旧密码均加密传输（服务端强制校验 encrypted=true）
    const [oldCipher, newCipher] = await Promise.all([
      encryptRequired(form.oldPassword),
      encryptRequired(form.newPassword)
    ])
    await updatePassword({ oldPassword: oldCipher, newPassword: newCipher, encrypted: true })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/admin/login')
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

.edit-btn {
  margin-top: 14px;
  width: 160px;
}

.avatar-preview {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  cursor: pointer;
}

.avatar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 12px;
  background: #f9fafb;
}

.info-row b {
  color: var(--el-color-primary);
}
</style>
