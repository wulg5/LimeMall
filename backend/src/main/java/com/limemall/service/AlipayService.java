package com.limemall.service;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limemall.common.BusinessException;
import com.limemall.config.AlipayProperties;
import com.limemall.dto.PayConfigSaveDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.vo.PayConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    private static final ZoneId ALIPAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final AlipayProperties envProperties;
    private final ObjectMapper objectMapper;
    private final SysConfigService configService;

    private volatile String cachedPrivateRaw;
    private volatile PrivateKey cachedPrivateKey;
    private volatile String cachedPublicRaw;
    private volatile PublicKey cachedPublicKey;

    /** 后台配置优先，环境变量兜底。收款需要完整密钥和异步通知地址。 */
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
        boolean enabled = keysReady && validNotifyUrl(notifyUrl)
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
    @Transactional
    public void saveConfig(PayConfigSaveDTO dto) {
        if (Boolean.TRUE.equals(dto.getEnabled())
                && !validNotifyUrl(firstNonBlank(dto.getNotifyUrl(), resolve().notifyUrl()))) {
            throw new BusinessException("启用支付宝支付前，请填写公网可访问的异步通知地址");
        }
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
    public String buildPagePayUrl(OmsOrder order, LocalDateTime paymentDeadline, String returnHost) {
        ResolvedConfig config = resolve();
        if (!config.enabled()) {
            throw new BusinessException("支付功能尚未配置，请联系管理员");
        }
        String bizContent = "{"
                + "\"out_trade_no\":\"" + order.getOrderSn() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                + "\"total_amount\":\"" + order.getPayAmount().toPlainString() + "\","
                + "\"time_expire\":\"" + paymentDeadline.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ALIPAY_ZONE).format(ALIPAY_TIME) + "\","
                + "\"subject\":\"青柠商城订单 " + order.getOrderSn() + "\""
                + "}";
        TreeMap<String, String> params = requestParameters(config, "alipay.trade.page.pay", bizContent);
        params.put("notify_url", config.notifyUrl().trim());
        if (StringUtils.hasText(config.returnUrl())) {
            params.put("return_url", paymentReturnUrl(config.returnUrl(), returnHost));
        }
        return config.gatewayUrl() + "?" + signAndEncode(config, params);
    }

    private static boolean validNotifyUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !isLoopbackHost(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String paymentReturnUrl(String configuredUrl, String returnHost) {
        if (isLoopbackHost(returnHost)) {
            URI uri = URI.create(configuredUrl);
            if (isLoopbackHost(uri.getHost())) {
                return UriComponentsBuilder.fromUri(uri).host(returnHost).build(true).toUriString();
            }
        }
        return configuredUrl;
    }

    private static boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
    }

    /** 查单响应验签后才可入账，超时仍在等待付款时先关闭支付宝交易。 */
    public PaymentCheck checkPayment(OmsOrder order, boolean expired) {
        ResolvedConfig config = resolve();
        requireChannelKeys(config);
        var identity = objectMapper.createObjectNode().put("out_trade_no", order.getOrderSn());
        JsonNode result = gatewayCall(config, "alipay.trade.query", identity);
        if ("40004".equals(result.path("code").asText())
                && "ACQ.TRADE_NOT_EXIST".equals(result.path("sub_code").asText())) {
            return new PaymentCheck(null, expired);
        }
        if (!isSuccessfulTradeResponse(order, result)) {
            throw new BusinessException("支付宝查单未确认订单状态");
        }
        return switch (result.path("trade_status").asText()) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" -> {
                if (!sameAmount(order.getPayAmount(), result.path("total_amount"))) {
                    throw new BusinessException("支付宝查单金额与订单金额不符");
                }
                String paidAt = result.path("send_pay_date").asText();
                try {
                    LocalDateTime paymentTime = paidAt.isBlank() ? LocalDateTime.now()
                            : LocalDateTime.parse(paidAt, ALIPAY_TIME).atZone(ALIPAY_ZONE)
                            .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                    yield new PaymentCheck(paymentTime, true);
                } catch (DateTimeParseException e) {
                    throw new BusinessException("支付宝查单付款时间格式不正确");
                }
            }
            case "TRADE_CLOSED" -> new PaymentCheck(null, true);
            case "WAIT_BUYER_PAY" -> new PaymentCheck(null, (expired || order.getStatus() == OrderService.STATUS_CLOSED)
                    && isSuccessfulTradeResponse(order, gatewayCall(config, "alipay.trade.close", identity)));
            default -> throw new BusinessException("支付宝查单返回未知交易状态");
        };
    }

    private boolean isSuccessfulTradeResponse(OmsOrder order, JsonNode result) {
        return "10000".equals(result.path("code").asText())
                && order.getOrderSn().equals(result.path("out_trade_no").asText());
    }

    public record PaymentCheck(LocalDateTime paidAt, boolean finished) {}

    /** 全额原路退款。用商户订单号派生固定请求号，重试与查询均使用同一号码。 */
    public boolean refund(String orderSn, BigDecimal amount) {
        ResolvedConfig config = resolve();
        // 即使暂停收新款，也必须能处理已付款订单的退款。
        requireChannelKeys(config);
        String refundNo = "LMRF-" + orderSn;
        var identity = objectMapper.createObjectNode()
                .put("out_trade_no", orderSn).put("out_request_no", refundNo);
        JsonNode result = gatewayCall(config, "alipay.trade.refund",
                identity.deepCopy().put("refund_amount", amount.toPlainString()));
        if ("10000".equals(result.path("code").asText())
                && "Y".equals(result.path("fund_change").asText())
                && orderSn.equals(result.path("out_trade_no").asText())
                && sameAmount(amount, result.path("refund_fee"))) {
            return true;
        }
        JsonNode query = gatewayCall(config, "alipay.trade.fastpay.refund.query", identity);
        return "10000".equals(query.path("code").asText())
                && "REFUND_SUCCESS".equals(query.path("refund_status").asText())
                && orderSn.equals(query.path("out_trade_no").asText())
                && refundNo.equals(query.path("out_request_no").asText())
                && sameAmount(amount, query.path("refund_amount"));
    }

    private void requireChannelKeys(ResolvedConfig config) {
        if (!StringUtils.hasText(config.appId()) || !StringUtils.hasText(config.privateKey())
                || !StringUtils.hasText(config.publicKey())) {
            throw new BusinessException("支付宝密钥未配置，等待渠道处理");
        }
    }

    private boolean sameAmount(BigDecimal expected, JsonNode amount) {
        try {
            return expected.compareTo(new BigDecimal(amount.asText())) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private JsonNode gatewayCall(ResolvedConfig config, String method, JsonNode content) {
        try {
            TreeMap<String, String> params = requestParameters(config, method, objectMapper.writeValueAsString(content));
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.gatewayUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(signAndEncode(config, params)))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new BusinessException("支付宝网关暂时不可用");
            }
            return verifiedResponse(config, method.replace('.', '_') + "_response", response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("支付宝网关请求中断");
        } catch (IOException e) {
            throw new BusinessException("支付宝网关响应暂不可用");
        }
    }

    /** RSA2 验签必须使用原始 JSON 子串，不能重新序列化后验签。 */
    private JsonNode verifiedResponse(ResolvedConfig config, String responseName, String body) throws IOException {
        try (var parser = objectMapper.createParser(body)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new BusinessException("支付宝网关响应格式不正确");
            }
            String response = null;
            String signature = null;
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String field = parser.currentName();
                JsonToken value = parser.nextToken();
                if (responseName.equals(field) && value == JsonToken.START_OBJECT) {
                    int start = Math.toIntExact(parser.currentTokenLocation().getCharOffset());
                    parser.skipChildren();
                    int end = Math.toIntExact(parser.currentLocation().getCharOffset());
                    response = body.substring(start, end);
                } else if ("sign".equals(field) && value == JsonToken.VALUE_STRING) {
                    signature = parser.getText();
                } else {
                    parser.skipChildren();
                }
            }
            if (response == null || signature == null || !verifySignature(config, response, signature)) {
                throw new BusinessException("支付宝网关响应验签失败");
            }
            return objectMapper.readTree(response);
        }
    }

    private TreeMap<String, String> requestParameters(ResolvedConfig config, String method, String bizContent) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", config.appId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now(ALIPAY_ZONE).format(ALIPAY_TIME));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        return params;
    }

    private String signAndEncode(ResolvedConfig config, TreeMap<String, String> params) {
        params.put("sign", rsaSign(buildContent(params), parsePrivateKey(config.privateKey())));
        return params.entrySet().stream().filter(entry -> StringUtils.hasText(entry.getValue()))
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
        return verifySignature(config, buildContent(params), sign);
    }

    private boolean verifySignature(ResolvedConfig config, String content, String sign) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(parsePublicKey(config.publicKey()));
            verifier.update(content.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.warn("支付宝签名校验失败", e);
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
