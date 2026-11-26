package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.ChangePasswordDTO;
import com.limemall.dto.MemberProfileDTO;
import com.limemall.security.SecurityUtils;
import com.limemall.service.PortalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城前台会员个人中心接口：资料编辑与修改密码。
 */
@Tag(name = "前台-个人中心")
@RestController
@RequestMapping("/portal/member")
@RequiredArgsConstructor
public class PortalMemberController {

    private final PortalAuthService portalAuthService;

    @PutMapping("/profile")
    @Operation(summary = "编辑资料（昵称/性别/生日/手机号）")
    public Result<Void> updateProfile(@Valid @RequestBody MemberProfileDTO dto) {
        portalAuthService.updateProfile(SecurityUtils.getMemberId(), dto);
        return Result.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码（新旧密码均需加密）")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        portalAuthService.changePassword(SecurityUtils.getMemberId(), dto);
        return Result.ok();
    }
}
