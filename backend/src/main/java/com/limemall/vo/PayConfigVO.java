package com.limemall.vo;

import lombok.Data;

/**
 * 支付配置视图（密钥仅返回脱敏预览，不回传明文）。
 */
@Data
public class PayConfigVO {

    /** 当前是否启用支付宝支付 */
    private Boolean enabled;

    private String appId;

    private String gatewayUrl;

    private String notifyUrl;

    private String returnUrl;

    /** 应用私钥是否已配置 */
    private Boolean privateKeyConfigured;

    /** 应用私钥脱敏预览（如 ABC123****WXYZ） */
    private String privateKeyPreview;

    /** 支付宝公钥是否已配置 */
    private Boolean publicKeyConfigured;

    private String publicKeyPreview;

    /** 配置来源：db 后台配置 / env 环境变量 / none 未配置 */
    private String mode;
}
