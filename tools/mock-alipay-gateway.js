/**
 * 本地模拟支付宝网关（仅用于开发联调 / 答辩演示「主动查单」链路，不参与生产）。
 *
 * 用法：
 *   MOCK_AMOUNT=399.00 MOCK_PRIV_KEY_PATH=/tmp/am_priv8.pem node tools/mock-alipay-gateway.js
 *
 * 然后在后台「支付配置」把网关地址指到 http://127.0.0.1:18099/gateway.do，
 * 支付公钥填与该私钥配对的公钥，即可验证：下单 → 耐心等待 → 订单页「刷新支付状态」→ 验签入账。
 */
const http = require('http')
const crypto = require('crypto')
const fs = require('fs')

const PORT = process.env.MOCK_PORT || 18099
const AMOUNT = process.env.MOCK_AMOUNT || '0.01'
const KEY_PATH = process.env.MOCK_PRIV_KEY_PATH

if (!KEY_PATH) {
  console.error('请通过 MOCK_PRIV_KEY_PATH 指定 PKCS8 私钥文件（PEM）')
  process.exit(1)
}
const privateKey = fs.readFileSync(KEY_PATH)

const server = http.createServer((req, res) => {
  if (req.method !== 'POST') {
    res.writeHead(405)
    return res.end()
  }
  let body = ''
  req.on('data', (chunk) => { body += chunk })
  req.on('end', () => {
    const params = new URLSearchParams(body)
    const method = params.get('method')
    if (method !== 'alipay.trade.query') {
      res.writeHead(200, { 'Content-Type': 'text/plain;charset=utf-8' })
      return res.end('unsupported method')
    }
    let outTradeNo = ''
    try {
      outTradeNo = JSON.parse(params.get('biz_content') || '{}').out_trade_no || ''
    } catch (e) { /* ignore */ }
    const inner = JSON.stringify({
      code: '10000',
      msg: 'Success',
      out_trade_no: outTradeNo,
      trade_no: 'MOCK' + Date.now(),
      trade_status: 'TRADE_SUCCESS',
      total_amount: AMOUNT
    })
    const sign = crypto.sign('RSA-SHA256', Buffer.from(inner, 'utf8'), privateKey).toString('base64')
    const responseBody = '{"alipay_trade_query_response":' + inner + ',"sign":"' + sign + '"}'
    console.log('[mock-alipay] trade.query', outTradeNo, '-> TRADE_SUCCESS', AMOUNT)
    res.writeHead(200, { 'Content-Type': 'application/json;charset=utf-8' })
    res.end(responseBody)
  })
})

server.listen(PORT, '127.0.0.1', () => {
  console.log(`[mock-alipay] listening on http://127.0.0.1:${PORT}/gateway.do amount=${AMOUNT}`)
})
