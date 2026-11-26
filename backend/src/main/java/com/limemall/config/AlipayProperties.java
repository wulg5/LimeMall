package com.limemall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置（环境变量注入）。
 * app-id / merchant-private-key / alipay-public-key 三项齐全时启用真实支付；未配置时前台支付明确报错。
 */
@Data
@Component
@ConfigurationProperties(prefix = "limemall.alipay")
public class AlipayProperties {

    /** 沙箱应用的 APPID */
    private String appId = "";

    /** 应用私钥（PKCS8，Base64，不含头尾） */
    private String merchantPrivateKey = "";

    /** 支付宝公钥（Base64，不含头尾） */
    private String alipayPublicKey = "";

    /** 网关地址（沙箱默认） */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 异步通知地址（必须公网可访问，如 https://域名/api/portal/pay/alipay/notify） */
    private String notifyUrl = "";

    /** 同步跳转地址（支付完成后浏览器回跳） */
    private String returnUrl = "";
}
