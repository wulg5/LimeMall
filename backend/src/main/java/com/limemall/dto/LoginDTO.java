package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class LoginDTO {

    @NotBlank(message = "请输入账号")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 密码是否为前端 RSA 加密后的密文（true 时 password 为 Base64 密文） */
    private Boolean encrypted;

    /** 图形验证码ID（连续失败 2 次后必填） */
    private String captchaId;

    /** 图形验证码内容（连续失败 2 次后必填） */
    private String captchaCode;
}
