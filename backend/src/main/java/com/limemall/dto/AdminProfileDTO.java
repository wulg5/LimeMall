package com.limemall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员个人资料编辑参数（账号/角色不可自改）。
 */
@Data
public class AdminProfileDTO {

    @NotBlank(message = "请输入昵称")
    @Size(max = 64, message = "昵称不能超过 64 字")
    private String nickname;

    @Size(max = 500, message = "头像地址过长")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱过长")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
