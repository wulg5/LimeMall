package com.limemall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 青柠商城管理系统（LimeMall）启动类。
 *
 * 排除 UserDetailsServiceAutoConfiguration：本项目使用自有 JWT 过滤器鉴权，
 * 不需要 Spring Boot 自动生成的默认内存用户（避免生成随机密码与无效认证源）。
 *
 * @author LimeMall Team
 */
@EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LimeMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(LimeMallApplication.class, args);
    }
}
