package com.limemall.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 支付配置保存参数。
 * 密钥类字段留空表示「保持现有值不变」，重新填写即为覆盖。
 */
@Data
public class PayConfigSaveDTO {

    @NotNull(message = "请选择是否启用")
    private Boolean enabled;

    @Size(max = 64, message = "APPID 格式不正确")
    private String appId;

    @Size(max = 4000, message = "应用私钥过长")
    private String merchantPrivateKey;

    @Size(max = 4000, message = "支付宝公钥过长")
    private String alipayPublicKey;

    @Size(max = 255)
    private String gatewayUrl;

    @Size(max = 500)
    private String notifyUrl;

    @Size(max = 500)
    private String returnUrl;
}
