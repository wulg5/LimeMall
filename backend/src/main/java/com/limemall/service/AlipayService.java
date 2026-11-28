package com.limemall.service;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limemall.common.BusinessException;
import com.limemall.config.AlipayProperties;
import com.limemall.dto.PayConfigSaveDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.mapper.AlipayChannelProfileMapper;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
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

    private static final String LEGACY_PROFILE = "legacy";
    private static final ZoneId ALIPAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final AlipayProperties envProperties;
    private final ObjectMapper objectMapper;
    private final SysConfigService configService;
    private final AlipayChannelProfileMapper profileMapper;

    private volatile CachedKey<PrivateKey> cachedPrivate;
    private volatile CachedKey<PublicKey> cachedPublic;

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
        ChannelConfig channel = new ChannelConfig(appId, privateKey, publicKey, gatewayUrl);
        boolean keysReady = channel.hasKeys();
        boolean enabled = keysReady && validGatewayUrl(gatewayUrl) && validNotifyUrl(notifyUrl)
                && (StringUtils.hasText(enabledFlag) ? "1".equals(enabledFlag.trim()) : true);
        String mode = db.keySet().stream().anyMatch(key -> key.startsWith("alipay."))
                ? "db" : (keysReady ? "env" : "none");
        return new ResolvedConfig(enabled, channel, notifyUrl, returnUrl, mode);
    }

    /** 支付是否可用（未配置时前台发起支付将明确报错，不提供模拟入账） */
    public boolean isEnabled() {
        return resolve().enabled();
    }

    /** 后台配置页视图（密钥脱敏） */
    public PayConfigVO describe() {
        ResolvedConfig config = resolve();
        ChannelConfig channel = config.channel();
        PayConfigVO vo = new PayConfigVO();
        vo.setEnabled(config.enabled());
        vo.setAppId(channel.appId());
        vo.setGatewayUrl(channel.gatewayUrl());
        vo.setNotifyUrl(config.notifyUrl());
        vo.setReturnUrl(config.returnUrl());
        vo.setPrivateKeyConfigured(StringUtils.hasText(channel.privateKey()));
        vo.setPrivateKeyPreview(mask(channel.privateKey()));
        vo.setPublicKeyConfigured(StringUtils.hasText(channel.publicKey()));
        vo.setPublicKeyPreview(mask(channel.publicKey()));
        vo.setMode(config.mode());
        return vo;
    }

    /** 保存后台配置（密钥留空 = 保持不变） */
    @Transactional
    public void saveConfig(PayConfigSaveDTO dto) {
        ResolvedConfig previous = resolve();
        boolean enabled = Boolean.TRUE.equals(dto.getEnabled());
        if (enabled && !validNotifyUrl(firstNonBlank(dto.getNotifyUrl(), previous.notifyUrl()))) {
            throw new BusinessException("启用支付宝支付前，请填写公网可访问的异步通知地址");
        }
        ChannelConfig original = previous.channel();
        String gatewayUrl = firstNonBlank(dto.getGatewayUrl(), original.gatewayUrl());
        if ((enabled || StringUtils.hasText(dto.getGatewayUrl())) && !validGatewayUrl(gatewayUrl)) {
            throw new BusinessException("支付宝网关须为完整的 HTTP 或 HTTPS 地址，且不能包含查询参数或片段");
        }
        if (enabled) {
            ChannelConfig candidate = new ChannelConfig(
                    firstNonBlank(dto.getAppId(), original.appId()),
                    firstNonBlank(dto.getMerchantPrivateKey(), original.privateKey()),
                    firstNonBlank(dto.getAlipayPublicKey(), original.publicKey()),
                    gatewayUrl);
            requireChannelKeys(candidate);
            parsePrivateKey(candidate.privateKey());
            parsePublicKey(candidate.publicKey());
        } else {
            if (StringUtils.hasText(dto.getMerchantPrivateKey())) {
                parsePrivateKey(dto.getMerchantPrivateKey());
            }
            if (StringUtils.hasText(dto.getAlipayPublicKey())) {
                parsePublicKey(dto.getAlipayPublicKey());
            }
        }
        configService.put(KEY_ENABLED, enabled ? "1" : "0", "支付宝启用开关");
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
        ChannelConfig updated = resolve().channel();
        if (!original.equals(updated)) {
            if (original.hasKeys()) {
                profileMapper.remember(LEGACY_PROFILE, encodeProfile(original));
                if (!original.sameChannel(updated) || !updated.hasKeys()) {
                    saveCredentials(original);
                }
            }
            if (updated.hasKeys()) {
                saveCredentials(updated);
            }
        }
    }

    /**
     * 构建电脑网站支付跳转 URL（跳转支付宝收银台）。
     */
    public PaymentPage buildPagePayUrl(OmsOrder order, LocalDateTime paymentDeadline, String returnHost) {
        ResolvedConfig config = resolve();
        if (!config.enabled()) {
            throw new BusinessException("支付功能尚未配置，请联系管理员");
        }
        String profileId = order.getPaymentProfileId();
        if (profileId == null && order.getPaymentCheckTime() != null
                && profileMapper.find(LEGACY_PROFILE) != null) {
            profileId = LEGACY_PROFILE;
        }
        ChannelConfig channel = profileId == null ? config.channel()
                : withLatestCredentials(requireProfile(profileId), config.channel());
        requireGatewayUrl(channel);
        String bizContent = "{"
                + "\"out_trade_no\":\"" + order.getOrderSn() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                + "\"total_amount\":\"" + order.getPayAmount().toPlainString() + "\","
                + "\"time_expire\":\"" + paymentDeadline.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ALIPAY_ZONE).format(ALIPAY_TIME) + "\","
                + "\"subject\":\"青柠商城订单 " + order.getOrderSn() + "\""
                + "}";
        TreeMap<String, String> params = requestParameters(channel, "alipay.trade.page.pay", bizContent);
        params.put("notify_url", config.notifyUrl().trim());
        if (StringUtils.hasText(config.returnUrl())) {
            params.put("return_url", paymentReturnUrl(config.returnUrl(), returnHost));
        }
        String url = channel.gatewayUrl() + "?" + signAndEncode(channel, params);
        return new PaymentPage(url, profileId == null ? rememberProfile(channel) : profileId);
    }

    public record PaymentPage(String url, String profileId) {}

    private String rememberProfile(ChannelConfig channel) {
        String payload = encodeProfile(channel);
        String id = fingerprint(payload);
        if (profileMapper.find(id) == null) {
            profileMapper.remember(id, payload);
        }
        return id;
    }

    private void saveCredentials(ChannelConfig channel) {
        profileMapper.saveCredentials(credentialsId(channel), encodeProfile(channel));
    }

    private String credentialsId(ChannelConfig channel) {
        return fingerprint("credentials:" + objectMapper.createArrayNode().add(channel.appId()).add(channel.gatewayUrl()));
    }

    private static String fingerprint(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String encodeProfile(ChannelConfig channel) {
        try {
            return objectMapper.writeValueAsString(channel);
        } catch (IOException e) {
            throw new IllegalStateException("无法保存支付宝渠道配置", e);
        }
    }

    private ChannelConfig requireProfile(String id) {
        String stored = profileMapper.find(id);
        if (stored == null) {
            throw new BusinessException("订单对应的支付宝渠道配置不存在");
        }
        return decodeProfile(stored);
    }

    private ChannelConfig channelFor(OmsOrder order) {
        ChannelConfig current = resolve().channel();
        return withLatestCredentials(originalChannel(order, current), current);
    }

    private ChannelConfig originalChannel(OmsOrder order, ChannelConfig current) {
        if (order.getPaymentProfileId() != null) {
            return requireProfile(order.getPaymentProfileId());
        }
        String legacy = profileMapper.find(LEGACY_PROFILE);
        return legacy == null ? current : decodeProfile(legacy);
    }

    private ChannelConfig withLatestCredentials(ChannelConfig original, ChannelConfig current) {
        if (original.sameChannel(current) && current.hasKeys()) {
            return current;
        }
        String stored = profileMapper.find(credentialsId(original));
        return stored == null ? original : decodeProfile(stored);
    }

    private ChannelConfig decodeProfile(String payload) {
        try {
            return objectMapper.readValue(payload, ChannelConfig.class);
        } catch (IOException e) {
            throw new BusinessException("订单对应的支付宝渠道配置无法读取");
        }
    }

    private static boolean validGatewayUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && uri.getPort() != 0 && uri.getPort() <= 65535;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void requireGatewayUrl(ChannelConfig channel) {
        if (!validGatewayUrl(channel.gatewayUrl())) {
            throw new BusinessException("支付宝网关地址无效，请检查渠道配置");
        }
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
        ChannelConfig channel = channelFor(order);
        requireChannelKeys(channel);
        var identity = objectMapper.createObjectNode().put("out_trade_no", order.getOrderSn());
        JsonNode result = gatewayCall(channel, "alipay.trade.query", identity);
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
                    && isSuccessfulTradeResponse(order, gatewayCall(channel, "alipay.trade.close", identity)));
            default -> throw new BusinessException("支付宝查单返回未知交易状态");
        };
    }

    private boolean isSuccessfulTradeResponse(OmsOrder order, JsonNode result) {
        return "10000".equals(result.path("code").asText())
                && order.getOrderSn().equals(result.path("out_trade_no").asText());
    }

    public record PaymentCheck(LocalDateTime paidAt, boolean finished) {}

    /** 全额原路退款。用商户订单号派生固定请求号，重试与查询均使用同一号码。 */
    public boolean refund(OmsOrder order, BigDecimal amount) {
        if (order == null) {
            throw new BusinessException("退款订单不存在");
        }
        String orderSn = order.getOrderSn();
        ChannelConfig channel = channelFor(order);
        // 即使暂停收新款，也必须能处理已付款订单的退款。
        requireChannelKeys(channel);
        String refundNo = "LMRF-" + orderSn;
        var identity = objectMapper.createObjectNode()
                .put("out_trade_no", orderSn).put("out_request_no", refundNo);
        JsonNode result = gatewayCall(channel, "alipay.trade.refund",
                identity.deepCopy().put("refund_amount", amount.toPlainString()));
        if ("10000".equals(result.path("code").asText())
                && "Y".equals(result.path("fund_change").asText())
                && orderSn.equals(result.path("out_trade_no").asText())
                && sameAmount(amount, result.path("refund_fee"))) {
            return true;
        }
        JsonNode query = gatewayCall(channel, "alipay.trade.fastpay.refund.query", identity);
        return "10000".equals(query.path("code").asText())
                && "REFUND_SUCCESS".equals(query.path("refund_status").asText())
                && orderSn.equals(query.path("out_trade_no").asText())
                && refundNo.equals(query.path("out_request_no").asText())
                && sameAmount(amount, query.path("refund_amount"));
    }

    private void requireChannelKeys(ChannelConfig channel) {
        if (!channel.hasKeys()) {
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

    private JsonNode gatewayCall(ChannelConfig channel, String method, JsonNode content) {
        requireGatewayUrl(channel);
        try {
            TreeMap<String, String> params = requestParameters(channel, method, objectMapper.writeValueAsString(content));
            HttpRequest request = HttpRequest.newBuilder(URI.create(channel.gatewayUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(signAndEncode(channel, params)))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new BusinessException("支付宝网关暂时不可用");
            }
            return verifiedResponse(channel, method.replace('.', '_') + "_response", response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("支付宝网关请求中断");
        } catch (IOException e) {
            throw new BusinessException("支付宝网关响应暂不可用");
        }
    }

    /** RSA2 验签必须使用原始 JSON 子串，不能重新序列化后验签。 */
    private JsonNode verifiedResponse(ChannelConfig channel, String responseName, String body) throws IOException {
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
            if (response == null || signature == null || !verifySignature(channel, response, signature)) {
                throw new BusinessException("支付宝网关响应验签失败");
            }
            return objectMapper.readTree(response);
        }
    }

    private TreeMap<String, String> requestParameters(ChannelConfig channel, String method, String bizContent) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", channel.appId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now(ALIPAY_ZONE).format(ALIPAY_TIME));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        return params;
    }

    private String signAndEncode(ChannelConfig channel, TreeMap<String, String> params) {
        params.put("sign", rsaSign(buildContent(params), parsePrivateKey(channel.privateKey())));
        return params.entrySet().stream().filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 校验异步通知签名（剔除 sign、sign_type 后按参数名排序拼接验签）。
     * 注意：验签只看密钥是否可用，不受「启用开关」影响（避免关单前在途回调丢失）。
     */
    public boolean verifyNotify(OmsOrder order, Map<String, String> rawParams) {
        ChannelConfig channel = channelFor(order);
        if (!StringUtils.hasText(channel.publicKey()) || !StringUtils.hasText(channel.appId())
                || !channel.appId().equals(rawParams.get("app_id"))) {
            return false;
        }
        TreeMap<String, String> params = new TreeMap<>(rawParams);
        String sign = params.remove("sign");
        params.remove("sign_type");
        if (!StringUtils.hasText(sign)) {
            return false;
        }
        return verifySignature(channel, buildContent(params), sign);
    }

    private boolean verifySignature(ChannelConfig channel, String content, String sign) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(parsePublicKey(channel.publicKey()));
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

    private record ResolvedConfig(boolean enabled, ChannelConfig channel, String notifyUrl, String returnUrl, String mode) {}

    private record ChannelConfig(String appId, String privateKey, String publicKey, String gatewayUrl) {
        boolean sameChannel(ChannelConfig other) {
            return Objects.equals(appId, other.appId) && Objects.equals(gatewayUrl, other.gatewayUrl);
        }

        boolean hasKeys() {
            return StringUtils.hasText(appId) && StringUtils.hasText(privateKey) && StringUtils.hasText(publicKey);
        }
    }

    private record CachedKey<T>(String raw, T key) {}

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
            CachedKey<PrivateKey> cached = cachedPrivate;
            if (cached != null && raw.equals(cached.raw())) {
                return cached.key();
            }
            byte[] bytes = Base64.getDecoder().decode(normalizeKey(raw));
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
            cachedPrivate = new CachedKey<>(raw, key);
            return key;
        } catch (Exception e) {
            throw new BusinessException("应用私钥格式不正确（需 PKCS8 Base64）");
        }
    }

    private PublicKey parsePublicKey(String raw) {
        try {
            CachedKey<PublicKey> cached = cachedPublic;
            if (cached != null && raw.equals(cached.raw())) {
                return cached.key();
            }
            byte[] bytes = Base64.getDecoder().decode(normalizeKey(raw));
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
            cachedPublic = new CachedKey<>(raw, key);
            return key;
        } catch (Exception e) {
            throw new BusinessException("支付宝公钥格式不正确（需 X509 Base64）");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
