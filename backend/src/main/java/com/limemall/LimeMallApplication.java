package com.limemall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 青柠商城管理系统（LimeMall）启动类。
 *
 * @author LimeMall Team
 */
@EnableScheduling
@SpringBootApplication
public class LimeMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(LimeMallApplication.class, args);
    }
}
