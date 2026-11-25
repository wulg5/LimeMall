package com.limemall.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户信息（认证主体）。
 * 同时承载后台管理员与商城会员两种身份，以 type 区分。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    public static final String TYPE_ADMIN = "ADMIN";
    public static final String TYPE_MEMBER = "MEMBER";

    /** 用户ID（管理员ID或会员ID） */
    private Long id;
    /** 用户名 */
    private String username;
    /** 用户类型：ADMIN / MEMBER */
    private String type;
}
