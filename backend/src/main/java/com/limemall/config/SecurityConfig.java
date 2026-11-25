package com.limemall.config;

import com.limemall.security.JwtAuthFilter;
import com.limemall.security.RestAccessDeniedHandler;
import com.limemall.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置。
 *
 * 设计说明：
 * 1. 前后端分离 + JWT 无状态认证，关闭 Session 与 CSRF；
 * 2. 接口级权限通过 @PreAuthorize("hasAuthority('模块:资源:动作')") 控制，
 *    权限标识与 ums_menu.perm 字段一一对应（RBAC）；
 * 3. 后台接口 /admin/** 需要 ROLE_ADMIN，商城前台接口 /portal/** 需要 ROLE_MEMBER；
 * 4. 未登录返回 401、无权限返回 403，均以统一 JSON 结构响应。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 认证接口与静态资源
                        .requestMatchers("/admin/auth/login", "/portal/auth/login", "/portal/auth/register",
                        "/auth/public-key", "/auth/captcha", "/portal/pay/alipay/notify").permitAll()
                        .requestMatchers("/portal/public/**", "/uploads/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/favicon.ico").permitAll()
                        // 业务接口按角色隔离
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/portal/**").hasRole("MEMBER")
                        .anyRequest().permitAll())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
