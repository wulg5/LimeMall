package com.limemall.security;

import com.limemall.common.BusinessException;
import com.limemall.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类。
 * 提供从 Spring Security 上下文中获取当前登录用户的便捷方法。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未登录返回 null */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 获取当前登录管理员ID，未登录抛 401 业务异常 */
    public static Long getAdminId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || !LoginUser.TYPE_ADMIN.equals(loginUser.getType())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return loginUser.getId();
    }

    /** 获取当前登录会员ID，未登录抛 401 业务异常 */
    public static Long getMemberId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || !LoginUser.TYPE_MEMBER.equals(loginUser.getType())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return loginUser.getId();
    }
}
