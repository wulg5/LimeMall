package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.security.CryptoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录传输加密公钥接口（公开；公钥非机密，仅用于加密登录密码）。
 */
@Tag(name = "公共-登录加密")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthCryptoController {

    private final CryptoService cryptoService;

    @GetMapping("/public-key")
    @Operation(summary = "获取登录加密公钥（RSA-OAEP-SHA256）")
    public Result<Map<String, String>> publicKey() {
        return Result.ok(Map.of(
                "publicKey", cryptoService.publicKeyBase64(),
                "algorithm", "RSA-OAEP-256"));
    }
}
