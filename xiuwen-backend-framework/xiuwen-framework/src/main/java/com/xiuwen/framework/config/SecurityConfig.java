package com.xiuwen.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置。
 * 只注册 BCrypt 编码器 Bean（来自 spring-security-crypto 纯算法包），
 * 不启用 Spring Security 自动配置，不影响现有 JWT 拦截器鉴权体系。
 */
@Configuration
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
