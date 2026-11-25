import request from '@/utils/request'

/**
 * 登录密码传输加密（RSA-OAEP-SHA256，公钥由后端启动时生成，内存持有）。
 *
 * 说明：应用层加密用于防「被动嗅探/日志泄露」的纵深防御；权威防线仍是 HTTPS，
 * 生产部署必须启用 TLS（Nginx/云端/cloudflared）。
 * WebCrypto 仅在安全上下文（HTTPS 或 localhost）可用，不可用时自动回退明文
 * （服务端兼容），并通过 HTTPS 保证传输安全。
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

async function encryptPassword(plain) {
  try {
    if (!window.crypto || !window.crypto.subtle) {
      return null
    }
    const base64Key = await getPublicKey()
    const der = Uint8Array.from(atob(base64Key), (char) => char.charCodeAt(0))
    const key = await window.crypto.subtle.importKey(
      'spki', der, { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt']
    )
    const cipherBuffer = await window.crypto.subtle.encrypt(
      { name: 'RSA-OAEP' }, key, new TextEncoder().encode(plain)
    )
    return btoa(String.fromCharCode(...new Uint8Array(cipherBuffer)))
  } catch (e) {
    return null
  }
}

/**
 * 构建登录请求载荷：加密成功则携带 { password: 密文, encrypted: true }，
 * 否则回退 { password: 明文 }（服务端兼容，仅限 HTTPS 兜底场景）。
 */
export async function buildLoginPayload(form) {
  const cipher = await encryptPassword(form.password)
  if (cipher) {
    return { username: form.username, password: cipher, encrypted: true }
  }
  return { username: form.username, password: form.password }
}
