package com.limemall.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功响应。
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 账号 */
    private String username;

    /** 昵称 */
    private String nickname;
}
