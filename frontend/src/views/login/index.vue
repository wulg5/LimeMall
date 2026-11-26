<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">
          <img src="/img/logo.svg" alt="LimeMall" />
          <span>青柠商城</span>
        </div>
        <h1>LimeMall 电商管理系统</h1>
        <p class="slogan">商品 · 订单 · 会员 · 营销 · 数据看板，一站式电商运营后台</p>
        <ul class="feature-list">
          <li>RBAC 权限体系，菜单到按钮级控制</li>
          <li>SPU / SKU 商品模型，库存实时联动</li>
          <li>订单状态机与退款审核闭环</li>
          <li>实时经营看板，核心指标一目了然</li>
        </ul>
      </div>
      <div class="circle c1"></div>
      <div class="circle c2"></div>
      <div class="circle c3"></div>
    </div>

    <!-- 右侧登录区 -->
    <div class="form-panel">
      <div class="form-card">
        <h2>欢迎回来</h2>
        <p class="tip">请使用管理员账号登录系统</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item v-if="needCaptcha" prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="图形验证码" :prefix-icon="Key" maxlength="4" />
              <img :src="captchaImage" class="captcha-img" alt="验证码" title="看不清？点击刷新" @click="loadCaptcha" />
            </div>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
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
  await formRef.value.validate()
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
.login-page {
  display: flex;
  height: 100%;
  background: #ffffff;
}

/* 左侧品牌区 */
.brand-panel {
  flex: 1.15;
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #064e3b 0%, #059669 55%, #10b981 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  padding: 0 72px;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 520px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: 48px;
}

.brand-logo img {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.brand-panel h1 {
  font-size: 36px;
  line-height: 1.3;
  margin: 0 0 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.slogan {
  font-size: 15px;
  opacity: 0.85;
  margin-bottom: 40px;
  line-height: 1.8;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  position: relative;
  padding-left: 26px;
  margin-bottom: 16px;
  font-size: 14px;
  opacity: 0.92;
}

.feature-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  box-shadow: inset 0 0 0 3px rgba(255, 255, 255, 0.15);
}

.feature-list li::after {
  content: '';
  position: absolute;
  left: 4.5px;
  top: 8px;
  width: 5px;
  height: 2.5px;
  border-left: 2px solid #ffffff;
  border-bottom: 2px solid #ffffff;
  transform: rotate(-45deg);
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.c1 {
  width: 420px;
  height: 420px;
  right: -140px;
  top: -120px;
}

.c2 {
  width: 260px;
  height: 260px;
  left: -80px;
  bottom: -60px;
  background: rgba(255, 255, 255, 0.06);
}

.c3 {
  width: 120px;
  height: 120px;
  right: 18%;
  bottom: 14%;
  background: rgba(255, 255, 255, 0.1);
}

/* 右侧表单区 */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

.form-card {
  width: 400px;
  padding: 20px;
}

.form-card h2 {
  font-size: 26px;
  margin: 0 0 8px;
  color: #111827;
}

.tip {
  color: #9ca3af;
  font-size: 14px;
  margin: 0 0 32px;
}

.login-security-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  letter-spacing: 6px;
  border-radius: 10px;
  margin-top: 6px;
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

.demo-accounts {
  margin-top: 36px;
  padding: 16px;
  background: #f8faf9;
  border-radius: 12px;
}

.demo-title {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 10px;
}

.demo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.demo-row:hover {
  background: #eef7f2;
}

.account-text {
  font-size: 13px;
  color: #4b5563;
  font-family: Consolas, monospace;
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
