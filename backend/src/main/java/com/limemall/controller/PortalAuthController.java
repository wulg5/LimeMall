package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.LoginDTO;
import com.limemall.dto.MemberRegisterDTO;
import com.limemall.security.SecurityUtils;
import com.limemall.service.PortalAuthService;
import com.limemall.vo.LoginVO;
import com.limemall.vo.MemberInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城前台会员认证接口。
 */
@Tag(name = "前台-认证")
@RestController
@RequestMapping("/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    @PostMapping("/register")
    @Operation(summary = "会员注册")
    public Result<Void> register(@Valid @RequestBody MemberRegisterDTO dto) {
        portalAuthService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "会员登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(portalAuthService.login(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "当前会员信息")
    public Result<MemberInfoVO> me() {
        return Result.ok(portalAuthService.me(SecurityUtils.getMemberId()));
    }
}
