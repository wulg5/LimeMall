package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.ChangePasswordDTO;
import com.limemall.dto.LoginDTO;
import com.limemall.security.SecurityUtils;
import com.limemall.service.AuthService;
import com.limemall.vo.AdminInfoVO;
import com.limemall.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台认证接口：登录、当前用户信息、退出、修改密码。
 */
@Tag(name = "后台-认证")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/info")
    @Operation(summary = "当前登录管理员信息（角色/权限/菜单）")
    public Result<AdminInfoVO> info() {
        return Result.ok(authService.getInfo(SecurityUtils.getAdminId()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录（JWT 无状态，前端丢弃令牌即可）")
    public Result<Void> logout() {
        return Result.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改当前管理员密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(SecurityUtils.getAdminId(), dto);
        return Result.ok();
    }
}
