#!/usr/bin/env bash
# =====================================================================
# 登录/注册/改密调试助手：将明文密码加密为后端可解密的 Base64 密文
# 用法：
#   ./tools/password-encrypt.sh '明文密码' [后端地址(默认 http://127.0.0.1:18080)]
#
# 调试示例（服务端强制 encrypted=true，明文请求会被拒绝）：
#   CIPHER=$(./tools/password-encrypt.sh admin123)
#   curl -X POST http://127.0.0.1:18080/admin/auth/login \
#        -H 'Content-Type: application/json' \
#        -d "{\"username\":\"admin\",\"password\":\"$CIPHER\",\"encrypted\":true}"
# =====================================================================
set -e
PLAIN="${1:?用法: password-encrypt.sh <明文密码> [后端地址]}"
BASE="${2:-http://127.0.0.1:18080}"

PUB_B64=$(curl -s "$BASE/auth/public-key" | sed 's/.*"publicKey":"\([^"]*\)".*/\1/')
if [ -z "$PUB_B64" ]; then
  echo "获取公钥失败：$BASE/auth/public-key" >&2
  exit 1
fi

TMP_PEM=$(mktemp)
{
  echo "-----BEGIN PUBLIC KEY-----"
  echo "$PUB_B64" | fold -w 64
  echo "-----END PUBLIC KEY-----"
} > "$TMP_PEM"

printf '%s' "$PLAIN" \
  | openssl pkeyutl -encrypt -pubin -inkey "$TMP_PEM" \
      -pkeyopt rsa_padding_mode:oaep \
      -pkeyopt rsa_oaep_md:sha256 \
      -pkeyopt rsa_mgf1_md:sha256 \
  | base64 -w0
rm -f "$TMP_PEM"
echo
