package com.limemall.security;

import com.limemall.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * 登录密码传输加密服务（应用层，补充 TLS 之外的防护）。
 *
 * 登录时前端用本服务下发的 RSA 公钥（RSA-OAEP-SHA256）加密密码，后端私钥解密；
 * 私钥仅存在于进程内存（每次启动重新生成），历史抓包无法重放解密。
 * 注意：应用层加密主要防「被动嗅探/日志泄露」，**权威防线仍是 HTTPS（TLS）**——
 * 生产环境必须启用 TLS（Nginx/云负载均衡/cloudflared），本机制为纵深防御。
 */
@Slf4j
@Service
public class CryptoService {

    private final KeyPair keyPair;

    public CryptoService() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("初始化登录传输加密密钥失败", e);
        }
    }

    /** 公钥（X509，Base64）供前端 importKey 使用 */
    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * 强制加密场景（登录/注册/改密/创建管理员）：非加密请求一律拒绝，杜绝明文通道。
     */
    public String requireDecrypted(String password, Boolean encrypted) {
        if (!Boolean.TRUE.equals(encrypted)) {
            throw new BusinessException("安全要求：密码必须加密传输，明文请求已被拒绝");
        }
        return decrypt(password);
    }

    /**
     * 解密前端加密后的密码（Base64 密文）。
     * 支持两种填充：WebCrypto 的 RSA-OAEP-SHA256（首选）与纯 JS 库的 PKCS#1 v1.5
     * （浏览器非安全上下文 —— 如局域网 HTTP——无法使用 WebCrypto 时的回退）。
     */
    public String decrypt(String base64Cipher) {
        byte[] cipherBytes;
        try {
            cipherBytes = Base64.getDecoder().decode(base64Cipher);
        } catch (Exception e) {
            throw new BusinessException("登录加密参数无效，请刷新页面后重试");
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec spec = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), spec);
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception oaepFailure) {
            // 回退：PKCS#1 v1.5（纯 JS RSA 库填充方式）
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
            } catch (Exception pkcs1Failure) {
                // 不区分具体原因，避免侧信道信息；要求前端刷新获取最新公钥后重试
                throw new BusinessException("登录加密参数无效，请刷新页面后重试");
            }
        }
    }
}
