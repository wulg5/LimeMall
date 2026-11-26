package com.limemall.controller;

import com.limemall.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（免登录）：部署排障 / cloudflared 连通性验证。
 */
@Tag(name = "公共-健康检查")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    @Operation(summary = "健康检查（应用与数据库状态）")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "limemall");
        data.put("status", "UP");
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            data.put("db", "UP");
        } catch (Exception e) {
            data.put("db", "DOWN");
            data.put("status", "DEGRADED");
        }
        return Result.ok(data);
    }
}
