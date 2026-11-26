<template>
  <div class="shop-login-page">
    <div class="login-card">
      <div class="login-header">
        <img src="/img/logo.svg" alt="LimeMall" />
        <h1>{{ isRegister ? '注册青柠商城账号' : '欢迎回到青柠商城' }}</h1>
        <p>{{ isRegister ? '注册后可领券、下单、管理订单' : '登录后可领券、下单、管理订单' }}</p>
      </div>

      <el-form v-if="!isRegister" ref="loginFormRef" :model="loginForm" :rules="loginRules"
               size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item v-if="needCaptcha">
          <div class="captcha-row">
            <el-input v-model="loginForm.captchaCode" placeholder="图形验证码" maxlength="4" />
            <img :src="captchaImage" class="captcha-img" alt="验证码" title="看不清？点击刷新" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
        <div class="login-security-tip">连续失败 2 次将要求图形验证码，连续失败 5 次将锁定 10 分钟</div>
      </el-form>

      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules"
               size="large" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="账号（3-20 位字母数字下划线）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="密码（6-32 位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="确认密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" :prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号（选填）" :prefix-icon="Iphone" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
          注册并登录
        </el-button>
      </el-form>

      <div class="switch-row">
        <template v-if="!isRegister">
          还没有账号？<a @click="isRegister = true">立即注册</a>
          
        </template>
        <template v-else>
          已有账号？<a @click="isRegister = false">去登录</a>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, User, UserFilled } from '@element-plus/icons-vue'
import { portalRegister } from '@/api/portal'
import { getCaptcha } from '@/api/auth'
import { encryptRequired } from '@/utils/crypto'
import { useShopStore } from '@/stores/shop'

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()

const isRegister = ref(route.query.mode === 'register')
const loading = ref(false)
const needCaptcha = ref(false)
const captchaImage = ref('')

const loginFormRef = ref()
const loginForm = reactive({ username: '', password: '', captchaId: '', captchaCode: '' })
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerFormRef = ref()
const registerForm = reactive({ username: '', password: '', confirmPassword: '', nickname: '', phone: '' })
const registerRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function loadCaptcha() {
  const data = await getCaptcha()
  loginForm.captchaId = data.captchaId
  loginForm.captchaCode = ''
  captchaImage.value = data.image
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    await shopStore.login(loginForm)
    await shopStore.fetchMe()
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/shop')
  } catch (e) {
    // 连续失败 2 次后服务端要求验证码：展示并刷新验证码
    if ((e?.message || '').includes('验证码')) {
      needCaptcha.value = true
      await loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    const cipher = await encryptRequired(registerForm.password)
    await portalRegister({
      username: registerForm.username,
      password: cipher,
      encrypted: true,
      nickname: registerForm.nickname || undefined,
      phone: registerForm.phone || undefined
    })
    ElMessage.success('注册成功，正在登录...')
    await shopStore.login({ username: registerForm.username, password: registerForm.password })
    await shopStore.fetchMe()
    router.push(route.query.redirect || '/shop')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.shop-login-page {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.login-card {
  width: 440px;
  background: #ffffff;
  border-radius: 20px;
  padding: 36px;
  box-shadow: 0 10px 40px rgba(15, 46, 36, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 26px;
}

.login-header img {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  margin-bottom: 14px;
}

.login-header h1 {
  font-size: 20px;
  margin: 0 0 8px;
  color: #111827;
}

.login-header p {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.login-security-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 4px;
}

.switch-row {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}

.switch-row a {
  color: var(--el-color-primary);
  cursor: pointer;
  margin-left: 4px;
}

.demo-tip {
  display: block;
  margin-top: 10px;
  color: #9ca3af;
  font-size: 12px;
}
</style>
