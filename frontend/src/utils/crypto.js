import { ElMessage } from 'element-plus'
import request from '@/utils/request'

/**
 * 登录密码传输加密（双通道，任何环境都不发明文）：
 * 1. 首选 WebCrypto RSA-OAEP-SHA256（HTTPS / localhost 等安全上下文）；
 * 2. 安全上下文不可用（如局域网 HTTP 访问）时，自动加载纯 JS RSA 库
 *    （jsencrypt，PKCS#1 v1.5）加密，后端两种填充均可解密。
 *
 * 说明：应用层加密用于防「被动嗅探/日志泄露」的纵深防御；**权威防线仍是 HTTPS**，
 * 生产部署必须启用 TLS（Nginx/云端/cloudflared），以同时防御主动 MITM。
 */
let cachedKey = null
let cachedAt = 0
const KEY_TTL = 10 * 60 * 1000

async function getPublicKey() {
  if (cachedKey && Date.now() - cachedAt < KEY_TTL) {
    return cachedKey
  }
  const data = await request.get('/auth/public-key')
  cachedKey = data.publicKey
  cachedAt = Date.now()
  return cachedKey
}

async function webCryptoEncrypt(plain, base64Key) {
  const der = Uint8Array.from(atob(base64Key), (char) => char.charCodeAt(0))
  const key = await window.crypto.subtle.importKey(
    'spki', der, { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt']
  )
  const buffer = await window.crypto.subtle.encrypt(
    { name: 'RSA-OAEP' }, key, new TextEncoder().encode(plain)
  )
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
}

async function jsRsaEncrypt(plain, base64Key) {
  const { default: JSEncrypt } = await import('jsencrypt')
  const pem = '-----BEGIN PUBLIC KEY-----\n'
    + (base64Key.match(/.{1,64}/g) || []).join('\n')
    + '\n-----END PUBLIC KEY-----'
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(pem)
  return encryptor.encrypt(plain) || null
}

async function encryptPassword(plain) {
  const base64Key = await getPublicKey()
  if (window.crypto && window.crypto.subtle) {
    try {
      return await webCryptoEncrypt(plain, base64Key)
    } catch (e) {
      /* 回退到纯 JS 实现 */
    }
  }
  return jsRsaEncrypt(plain, base64Key)
}

/**
 * 构建登录请求载荷：任何情况下都发送密文（encrypted=true）。
 * 加密组件异常时终止提交并提示，绝不回退明文。验证码字段（失败 2 次后）原样透传。
 */
export async function buildLoginPayload(form) {
  const payload = { username: form.username }
  if (form.captchaId && form.captchaCode) {
    payload.captchaId = form.captchaId
    payload.captchaCode = form.captchaCode
  }
  const cipher = await encryptPassword(form.password)
  if (!cipher) {
    ElMessage.error('登录加密组件初始化失败，请刷新页面重试')
    throw new Error('ENCRYPT_FAILED')
  }
  payload.password = cipher
  payload.encrypted = true
  return payload
}
