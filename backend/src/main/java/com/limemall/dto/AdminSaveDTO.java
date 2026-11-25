package com.limemall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员新增/编辑请求参数。
 */
@Data
public class AdminSaveDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "请输入登录账号")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "账号为 3-20 位字母、数字或下划线")
    private String username;

    /** 新增时可选（默认 admin123），编辑时忽略 */
    @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
    private String password;

    @NotBlank(message = "请输入昵称")
    private String nickname;

    private String avatar;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    @NotNull(message = "请选择状态")
    private Integer status;

    @NotEmpty(message = "请至少分配一个角色")
    private List<Long> roleIds;
}
