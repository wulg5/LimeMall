package com.limemall.service;

import com.limemall.common.BusinessException;
import com.limemall.config.AlipayProperties;
import com.limemall.dto.PayConfigSaveDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.vo.PayConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付宝网页支付（alipay.trade.page.pay）服务。
 *
 * 配置优先级：**后台「系统管理 → 支付配置」(sys_config) 优先，环境变量兜底**——
 * 保存后立即生效，无需重启；密钥仅在后台页面以脱敏预览展示，不提供明文回读。
 *
 * 支付结果通过**支付宝异步通知**（/portal/pay/alipay/notify）验签入账；
 * 本地开发使用 cloudflared 等内网穿透把本机映射为公网 HTTPS 地址填写 notify_url。
 *
 * 不依赖支付宝 SDK：手工 RSA2（SHA256withRSA）签名与异步通知验签，便于答辩讲解。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    public static final String KEY_ENABLED = "alipay.enabled";
    public static final String KEY_APP_ID = "alipay.app-id";
    public static final String KEY_PRIVATE_KEY = "alipay.merchant-private-key";
    public static final String KEY_PUBLIC_KEY = "alipay.alipay-public-key";
    public static final String KEY_GATEWAY = "alipay.gateway-url";
    public static final String KEY_NOTIFY_URL = "alipay.notify-url";
    public static final String KEY_RETURN_URL = "alipay.return-url";

    private final AlipayProperties envProperties;
    private final SysConfigService configService;

    private volatile String cachedPrivateRaw;
    private volatile PrivateKey cachedPrivateKey;
    private volatile String cachedPublicRaw;
    private volatile PublicKey cachedPublicKey;

    /** 生效配置（后台配置优先，环境变量兜底；密钥齐全且开关打开才启用） */
    private ResolvedConfig resolve() {
        Map<String, String> db = configService.getAll();
        String appId = firstNonBlank(db.get(KEY_APP_ID), envProperties.getAppId());
        String privateKey = firstNonBlank(db.get(KEY_PRIVATE_KEY), envProperties.getMerchantPrivateKey());
        String publicKey = firstNonBlank(db.get(KEY_PUBLIC_KEY), envProperties.getAlipayPublicKey());
        String gatewayUrl = firstNonBlank(db.get(KEY_GATEWAY), envProperties.getGatewayUrl());
        String notifyUrl = firstNonBlank(db.get(KEY_NOTIFY_URL), envProperties.getNotifyUrl());
        String returnUrl = firstNonBlank(db.get(KEY_RETURN_URL), envProperties.getReturnUrl());
        String enabledFlag = db.get(KEY_ENABLED);
        boolean keysReady = StringUtils.hasText(appId)
                && StringUtils.hasText(privateKey) && StringUtils.hasText(publicKey);
        boolean enabled = keysReady
                && (StringUtils.hasText(enabledFlag) ? "1".equals(enabledFlag.trim()) : true);
        String mode = db.keySet().stream().anyMatch(key -> key.startsWith("alipay."))
                ? "db" : (keysReady ? "env" : "none");
        return new ResolvedConfig(enabled, appId, privateKey, publicKey,
                gatewayUrl, notifyUrl, returnUrl, mode);
    }

    /** 支付是否可用（未配置时前台发起支付将明确报错，不提供模拟入账） */
    public boolean isEnabled() {
        return resolve().enabled();
    }

    public String getAppId() {
        return resolve().appId();
    }

    /** 后台配置页视图（密钥脱敏） */
    public PayConfigVO describe() {
        ResolvedConfig config = resolve();
        PayConfigVO vo = new PayConfigVO();
        vo.setEnabled(config.enabled());
        vo.setAppId(config.appId());
        vo.setGatewayUrl(config.gatewayUrl());
        vo.setNotifyUrl(config.notifyUrl());
        vo.setReturnUrl(config.returnUrl());
        vo.setPrivateKeyConfigured(StringUtils.hasText(config.privateKey()));
        vo.setPrivateKeyPreview(mask(config.privateKey()));
        vo.setPublicKeyConfigured(StringUtils.hasText(config.publicKey()));
        vo.setPublicKeyPreview(mask(config.publicKey()));
        vo.setMode(config.mode());
        return vo;
    }

    /** 保存后台配置（密钥留空 = 保持不变） */
    public void saveConfig(PayConfigSaveDTO dto) {
        configService.put(KEY_ENABLED, Boolean.TRUE.equals(dto.getEnabled()) ? "1" : "0", "支付宝启用开关");
        if (StringUtils.hasText(dto.getAppId())) {
            configService.put(KEY_APP_ID, dto.getAppId().trim(), "支付宝沙箱 APPID");
        }
        if (StringUtils.hasText(dto.getMerchantPrivateKey())) {
            configService.put(KEY_PRIVATE_KEY, normalizeKey(dto.getMerchantPrivateKey()), "应用私钥（脱敏存储）");
        }
        if (StringUtils.hasText(dto.getAlipayPublicKey())) {
            configService.put(KEY_PUBLIC_KEY, normalizeKey(dto.getAlipayPublicKey()), "支付宝公钥");
        }
        if (StringUtils.hasText(dto.getGatewayUrl())) {
            configService.put(KEY_GATEWAY, dto.getGatewayUrl().trim(), "支付网关地址");
        }
        if (StringUtils.hasText(dto.getNotifyUrl())) {
            configService.put(KEY_NOTIFY_URL, dto.getNotifyUrl().trim(), "异步通知地址（公网HTTPS）");
        }
        if (StringUtils.hasText(dto.getReturnUrl())) {
            configService.put(KEY_RETURN_URL, dto.getReturnUrl().trim(), "同步跳转地址");
        }
    }

    /**
     * 构建电脑网站支付跳转 URL（跳转支付宝收银台）。
     */
    public String buildPagePayUrl(OmsOrder order) {
        ResolvedConfig config = resolve();
        String bizContent = "{"
                + "\"out_trade_no\":\"" + order.getOrderSn() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                + "\"total_amount\":\"" + order.getPayAmount().toPlainString() + "\","
                + "\"subject\":\"青柠商城订单 " + order.getOrderSn() + "\""
                + "}";
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", config.appId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        if (StringUtils.hasText(config.notifyUrl())) {
            params.put("notify_url", config.notifyUrl());
        }
        if (StringUtils.hasText(config.returnUrl())) {
            params.put("return_url", config.returnUrl());
        }
        String content = buildContent(params);
        params.put("sign", rsaSign(content, parsePrivateKey(config.privateKey())));
        return config.gatewayUrl() + "?" + params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 校验异步通知签名（剔除 sign、sign_type 后按参数名排序拼接验签）。
     * 注意：验签只看密钥是否可用，不受「启用开关」影响（避免关单前在途回调丢失）。
     */
    public boolean verifyNotify(Map<String, String> rawParams) {
        ResolvedConfig config = resolve();
        if (!StringUtils.hasText(config.publicKey())) {
            return false;
        }
        TreeMap<String, String> params = new TreeMap<>(rawParams);
        String sign = params.remove("sign");
        params.remove("sign_type");
        if (!StringUtils.hasText(sign)) {
            return false;
        }
        String content = buildContent(params);
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(parsePublicKey(config.publicKey()));
            verifier.update(content.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.warn("支付宝通知验签异常: {}", e.getMessage());
            return false;
        }
    }

    // ==================================================================
    // 私有工具
    // ==================================================================

    private record ResolvedConfig(boolean enabled, String appId, String privateKey, String publicKey,
                                  String gatewayUrl, String notifyUrl, String returnUrl, String mode) {
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : fallback;
    }

    /** 容忍粘贴带 BEGIN/END 头尾与换行的密钥，统一规范化 */
    private String normalizeKey(String raw) {
        return raw.replaceAll("-----(BEGIN|END)[^-]*-----", "")
                .replaceAll("\\s", "");
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= 10) {
            return "****";
        }
        return value.substring(0, 6) + "****" + value.substring(value.length() - 4);
    }

    private String buildContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String rsaSign(String content, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new BusinessException("支付签名失败，请检查应用私钥配置");
        }
    }

    private PrivateKey parsePrivateKey(String raw) {
        try {
            if (!raw.equals(cachedPrivateRaw)) {
                byte[] bytes = Base64.getDecoder().decode(normalizeKey(raw));
                cachedPrivateKey = KeyFactory.getInstance("RSA")
                        .generatePrivate(new PKCS8EncodedKeySpec(bytes));
                cachedPrivateRaw = raw;
            }
            return cachedPrivateKey;
        } catch (Exception e) {
            throw new BusinessException("应用私钥格式不正确（需 PKCS8 Base64）");
        }
    }

    private PublicKey parsePublicKey(String raw) {
        try {
            if (!raw.equals(cachedPublicRaw)) {
                byte[] bytes = Base64.getDecoder().decode(normalizeKey(raw));
                cachedPublicKey = KeyFactory.getInstance("RSA")
                        .generatePublic(new X509EncodedKeySpec(bytes));
                cachedPublicRaw = raw;
            }
            return cachedPublicKey;
        } catch (Exception e) {
            throw new BusinessException("支付宝公钥格式不正确（需 X509 Base64）");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
