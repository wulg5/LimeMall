package com.limemall.controller;

import com.limemall.common.Result;
import com.limemall.dto.PayConfigSaveDTO;
import com.limemall.service.AlipayService;
import com.limemall.vo.PayConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台支付配置接口（仅超级管理员）。
 * 密钥仅返回脱敏预览，保存后立即生效（无需重启）。
 */
@Tag(name = "后台-支付配置")
@RestController
@RequestMapping("/admin/pay-config")
@RequiredArgsConstructor
public class AdminPayConfigController {

    private final AlipayService alipayService;

    @GetMapping
    @PreAuthorize("hasAuthority('sys:config:list')")
    @Operation(summary = "支付配置视图（密钥脱敏）")
    public Result<PayConfigVO> detail() {
        return Result.ok(alipayService.describe());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @Operation(summary = "保存支付配置（密钥留空保持不变）")
    public Result<Void> save(@Valid @RequestBody PayConfigSaveDTO dto) {
        alipayService.saveConfig(dto);
        return Result.ok();
    }
}
