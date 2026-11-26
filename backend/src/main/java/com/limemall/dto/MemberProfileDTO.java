package com.limemall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 会员个人资料编辑参数（含手机号改绑，服务层做唯一性校验）。
 */
@Data
public class MemberProfileDTO {

    @Size(max = 64, message = "昵称不能超过 64 字")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Min(value = 0, message = "性别参数不正确")
    @Max(value = 2, message = "性别参数不正确")
    private Integer gender;

    private LocalDate birthday;
}
