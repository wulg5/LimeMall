package com.limemall.service;

import com.limemall.common.BusinessException;
import com.limemall.config.AlipayProperties;
import com.limemall.entity.OmsOrder;
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
 * 不依赖支付宝 SDK：按照开放平台文档手工完成 RSA2（SHA256withRSA）签名与异步通知验签，
 * 便于课程答辩讲解原理。生产接入建议直接使用官方 SDK（alipay-sdk-java）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private final AlipayProperties properties;

    private volatile PrivateKey merchantPrivateKey;
    private volatile PublicKey alipayPublicKey;

    /** 密钥配置齐全时启用真实支付 */
    public boolean isEnabled() {
        return StringUtils.hasText(properties.getAppId())
                && StringUtils.hasText(properties.getMerchantPrivateKey())
                && StringUtils.hasText(properties.getAlipayPublicKey());
    }

    public String getAppId() {
        return properties.getAppId();
    }

    /**
     * 构建电脑网站支付跳转 URL（跳转支付宝收银台）。
     */
    public String buildPagePayUrl(OmsOrder order) {
        String bizContent = "{"
                + "\"out_trade_no\":\"" + order.getOrderSn() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                + "\"total_amount\":\"" + order.getPayAmount().toPlainString() + "\","
                + "\"subject\":\"青柠商城订单 " + order.getOrderSn() + "\""
                + "}";
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", properties.getAppId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        if (StringUtils.hasText(properties.getNotifyUrl())) {
            params.put("notify_url", properties.getNotifyUrl());
        }
        if (StringUtils.hasText(properties.getReturnUrl())) {
            params.put("return_url", properties.getReturnUrl());
        }
        String content = buildContent(params);
        params.put("sign", rsaSign(content));
        return properties.getGatewayUrl() + "?" + params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 校验异步通知签名（剔除 sign、sign_type 后按参数名排序拼接验签）。
     */
    public boolean verifyNotify(Map<String, String> rawParams) {
        if (!isEnabled()) {
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
            verifier.initVerify(resolveAlipayPublicKey());
            verifier.update(content.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.warn("支付宝通知验签异常: {}", e.getMessage());
            return false;
        }
    }

    private String buildContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String rsaSign(String content) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(resolveMerchantPrivateKey());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new BusinessException("支付签名失败，请检查应用私钥配置");
        }
    }

    private PrivateKey resolveMerchantPrivateKey() throws Exception {
        if (merchantPrivateKey == null) {
            byte[] bytes = Base64.getDecoder()
                    .decode(properties.getMerchantPrivateKey().replaceAll("\\s", ""));
            merchantPrivateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(bytes));
        }
        return merchantPrivateKey;
    }

    private PublicKey resolveAlipayPublicKey() throws Exception {
        if (alipayPublicKey == null) {
            byte[] bytes = Base64.getDecoder()
                    .decode(properties.getAlipayPublicKey().replaceAll("\\s", ""));
            alipayPublicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(bytes));
        }
        return alipayPublicKey;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
