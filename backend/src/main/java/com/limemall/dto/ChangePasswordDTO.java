package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求参数（密码字段均为前端 RSA 加密密文，明文长度校验在解密后进行）。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    private String newPassword;

    /** 密码是否为前端 RSA 加密后的密文（服务端强制要求 true） */
    private Boolean encrypted;
}
