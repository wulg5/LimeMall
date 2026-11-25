package com.limemall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Web 层配置。
 * 1. 跨域配置：默认仅允许本机开发来源（环境变量 CORS_ALLOWED_ORIGINS 可覆盖，
 *    逗号分隔）；生产环境经 Nginx 同源部署时无需跨域；
 * 2. 静态资源映射：上传的图片通过 /uploads/** 直接访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${limemall.upload-dir}")
    private String uploadDir;

    /** 允许的跨域来源（默认本机前端开发地址；同源部署不受影响） */
    @Value("${limemall.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(item -> !item.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
