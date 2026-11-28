<template>
  <div class="page-container">
    <load-state v-if="loading || configError" :loading="loading" :error="configError" @retry="load" />
    <div v-else class="page-card">
      <div class="table-toolbar">
        <span class="title">支付配置（支付宝沙箱）</span>
      </div>

      <!-- 当前生效状态 -->
      <el-alert v-if="config" :type="config.enabled ? 'success' : 'info'" :closable="false" class="status-alert">
        <template #title>
          当前状态：{{ config.enabled ? '已启用支付宝沙箱支付' : '未启用（前台发起支付将提示未配置）' }}
          <span class="mode-tag">
            配置来源：{{ modeText }}
          </span>
        </template>
      </el-alert>

      <el-form v-if="config" ref="formRef" :model="form" label-width="120px" class="pay-form">
        <el-form-item label="启用支付">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" inline-prompt />
          <span class="form-tip">启用支付需要完整密钥和公网可访问的异步通知地址</span>
        </el-form-item>

        <el-form-item label="APPID">
          <el-input v-model="form.appId" placeholder="开放平台 → 沙箱应用 → APPID" maxlength="64" />
        </el-form-item>

        <el-form-item label="应用私钥">
          <el-input v-model="form.merchantPrivateKey" type="textarea" :rows="3"
                    :placeholder="config.privateKeyConfigured
                      ? `已配置（${config.privateKeyPreview}），留空保持不变，重新填写即覆盖`
                      : '粘贴应用私钥（PKCS8 Base64，可带 BEGIN/END 头尾）'" />
        </el-form-item>

        <el-form-item label="支付宝公钥">
          <el-input v-model="form.alipayPublicKey" type="textarea" :rows="3"
                    :placeholder="config.publicKeyConfigured
                      ? `已配置（${config.publicKeyPreview}），留空保持不变，重新填写即覆盖`
                      : '粘贴支付宝公钥（X509 Base64，可带 BEGIN/END 头尾）'" />
        </el-form-item>

        <el-form-item label="网关地址">
          <el-input v-model="form.gatewayUrl" placeholder="https://openapi-sandbox.dl.alipaydev.com/gateway.do" />
        </el-form-item>

        <el-form-item label="异步通知地址">
          <el-input v-model="form.notifyUrl" placeholder="https://xxxx.trycloudflare.com/portal/pay/alipay/notify" />
          <span class="form-tip">必须公网可访问；本地开发用 cloudflared 穿透本机 18080，路径为 /portal/pay/alipay/notify（详见 README）</span>
        </el-form-item>

        <el-form-item label="同步跳转地址">
          <el-input v-model="form.returnUrl" placeholder="https://你的域名/shop/orders" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置（立即生效）</el-button>
          <el-button @click="load">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-alert type="warning" :closable="false" class="tips-alert">
        <template #title>接入步骤</template>
        <ol class="tips-list">
          <li>开放平台控制台 → 沙箱应用 → 获取 APPID；接口加签方式选择「自定义密钥（RSA2）」，按工具生成密钥对。</li>
          <li>上传「应用公钥」到开放平台，将返回的「支付宝公钥」与应用私钥分别贴到上方保存。</li>
          <li>异步通知地址需公网可访问：本地开发运行 <code>cloudflared tunnel --url http://127.0.0.1:18080</code> 获取 HTTPS 地址填入（路径 /portal/pay/alipay/notify，详见 README）；回调经验签 + 金额校验 + 幂等入账，失败一律拒绝。</li>
          <li>保存后立即生效，无需重启；服务启动时可用环境变量兜底（后台配置优先）。</li>
        </ol>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import LoadState from '@/components/LoadState.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPayConfig, savePayConfig } from '@/api/config'

const config = ref(null)
const configError = ref(false)
const loading = ref(true)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  enabled: false,
  appId: '',
  merchantPrivateKey: '',
  alipayPublicKey: '',
  gatewayUrl: '',
  notifyUrl: '',
  returnUrl: ''
})

const modeText = computed(() => ({
  db: '后台配置',
  env: '环境变量',
  none: '未配置'
}[config.value?.mode] || '未配置'))

onMounted(load)

async function load() {
  loading.value = true
  configError.value = false
  try {
    config.value = await getPayConfig()
    form.enabled = !!config.value.enabled
    form.appId = config.value.appId || ''
    form.gatewayUrl = config.value.gatewayUrl || ''
    form.notifyUrl = config.value.notifyUrl || ''
    form.returnUrl = config.value.returnUrl || ''
    form.merchantPrivateKey = ''
    form.alipayPublicKey = ''
  } catch {
    configError.value = true
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await savePayConfig({ ...form })
    ElMessage.success('配置已保存并立即生效')
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.status-alert {
  margin-bottom: 20px;
}

.mode-tag {
  margin-left: 16px;
  font-weight: 400;
  opacity: 0.8;
}

.pay-form {
  max-width: 720px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.tips-alert {
  margin-top: 10px;
  max-width: 860px;
}

.tips-list {
  margin: 6px 0 0;
  padding-left: 18px;
  line-height: 1.9;
  font-size: 13px;
}
</style>
