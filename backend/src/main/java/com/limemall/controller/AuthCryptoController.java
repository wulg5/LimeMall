package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.security.CryptoService;
import com.limemall.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录安全辅助接口（公开）：传输加密公钥 + 图形验证码。
 */
@Tag(name = "公共-登录安全")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthCryptoController {

    private final CryptoService cryptoService;
    private final CaptchaService captchaService;

    @GetMapping("/public-key")
    @Operation(summary = "获取登录加密公钥（RSA-OAEP-SHA256）")
    public Result<Map<String, String>> publicKey() {
        return Result.ok(Map.of(
                "publicKey", cryptoService.publicKeyBase64(),
                "algorithm", "RSA-OAEP-256"));
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码（失败 2 次后登录需携带）")
    public Result<Map<String, String>> captcha() {
        return Result.ok(captchaService.generate());
    }
}
