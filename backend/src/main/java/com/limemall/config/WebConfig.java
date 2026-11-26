package com.limemall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Web 层配置。
 * 1. 跨域策略：默认放行本机与私有网段（10/8、172.16/12、192.168/16）来源，
 *    以及 CORS_ALLOWED_ORIGINS 配置的额外域名；其余来源拒绝。
 *    注意：开发代理（Vite）与生产反代（Nginx）均剥离 Origin，同源部署不受影响；
 * 2. 静态资源映射：上传的图片通过 /uploads/** 直接访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${limemall.upload-dir}")
    private String uploadDir;

    /** 额外允许的来源（逗号分隔完整 origin，用于公网前端独立部署场景） */
    @Value("${limemall.cors.allowed-origins:}")
    private String extraOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> extras = Arrays.stream(extraOrigins.split(","))
                .map(String::trim).filter(item -> !item.isEmpty()).toList();
        return (HttpServletRequest request) -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            String origin = request.getHeader("Origin");
            if (origin != null && (extras.contains(origin) || isTrustedOrigin(origin))) {
                config.setAllowedOrigins(List.of(origin));
            } else {
                config.setAllowedOrigins(List.of());
            }
            return config;
        };
    }

    /** 本机与私有网段来源放行（开发/局域网演示场景），其余拒绝 */
    private boolean isTrustedOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
                return true;
            }
            return host.startsWith("10.")
                    || host.startsWith("192.168.")
                    || is172Private(host);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean is172Private(String host) {
        if (!host.startsWith("172.")) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 相对路径统一解析为基于启动工作目录的绝对 URI，避免 file: 相对位置歧义
        String location = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
