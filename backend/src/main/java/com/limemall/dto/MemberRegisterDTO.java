package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 会员注册请求参数。
 */
@Data
public class MemberRegisterDTO {

    @NotBlank(message = "请输入账号")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "账号为 3-20 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 密码是否为前端 RSA 加密后的密文（服务端强制要求 true；明文长度校验在解密后进行） */
    private Boolean encrypted;

    @Size(max = 64, message = "昵称不能超过 64 字")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
