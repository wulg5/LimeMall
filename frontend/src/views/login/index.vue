<template>
  <div class="login-page">
    <section class="brand-panel" aria-label="青柠商城后台">
      <div class="brand-content">
        <router-link class="brand-lockup" to="/shop">
          <span class="brand-mark" aria-hidden="true"><el-icon><Orange /></el-icon></span>
          <span><strong>青柠商城</strong><small>LimeMall</small></span>
        </router-link>
        <p class="brand-title">商城管理工作台</p>
        <p>管理商品、订单、售后和会员服务。</p>
        <router-link class="back-to-store" to="/shop">返回商城</router-link>
      </div>
    </section>


    <div class="form-panel">
      <div class="form-card">
        <h1>管理员登录</h1>
        <p class="tip">请使用管理员账号登录系统</p>

        <el-form scroll-to-error ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号" aria-label="账号" autocomplete="username" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码" aria-label="密码" autocomplete="current-password" :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item v-if="needCaptcha" prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="图形验证码" aria-label="图形验证码" autocomplete="off" :prefix-icon="Key" maxlength="4" />
              <button type="button" class="captcha-button" aria-label="刷新图形验证码" @click="loadCaptcha"><img :src="captchaImage" class="captcha-img" alt="图形验证码" /></button>
            </div>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">
            登 录
          </el-button>
          <div class="login-security-tip">连续失败 2 次将要求图形验证码，连续失败 5 次将锁定 10 分钟</div>
        </el-form>


      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const needCaptcha = ref(false)
const captchaImage = ref('')
const form = reactive({ username: '', password: '', captchaId: '', captchaCode: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function loadCaptcha() {
  const data = await getCaptcha()
  form.captchaId = data.captchaId
  form.captchaCode = ''
  captchaImage.value = data.image
}

async function handleLogin() {
  if (!await formRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    await userStore.login(form)
    await userStore.fetchInfo()
    ElMessage.success('登录成功，欢迎回来')
    router.push(route.query.redirect || '/admin/dashboard')
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
</script>

<style scoped>
.login-page { min-height: 100dvh; display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
.brand-panel { display: grid; align-items: center; background: var(--pine); color: var(--paper); padding: clamp(26px, 7vw, 98px); }
.brand-content { width: min(100%, 430px); justify-self: end; }
.brand-lockup small, .back-to-store { color: var(--lime); }
.brand-title { font-size: clamp(31px, 5vw, 67px); font-weight: 750; color: var(--lime); letter-spacing: -.06em; line-height: 1.1; margin: 30px 0 9px; }
.back-to-store { display: inline-block; margin-top: 16px; }
.form-panel { display: grid; align-items: center; padding: clamp(24px, 6vw, 80px); }
.form-card { width: min(100%, 390px); }
.form-card h1 { color: var(--pine); font-size: clamp(28px, 4vw, 44px); margin: 0; }
.tip { color: var(--muted); margin: 8px 0 22px; }
.login-btn { width: 100%; }
.login-security-tip { font-size: 12px; margin-top: 14px; color: var(--muted); }
.captcha-row { display: flex; width: 100%; align-items: center; gap: 6px; }
.captcha-button { flex: 0 0 90px; padding: 0; background: transparent; border: 0; }
.captcha-img { display: block; max-width: 100%; }
@media (max-width: 680px) {
  .login-page { grid-template-columns: 1fr; align-content: start; }
  .brand-content { justify-self: start; }
  .brand-title { margin-top: 17px; }
  .form-card { margin-inline: auto; }
}
</style>
