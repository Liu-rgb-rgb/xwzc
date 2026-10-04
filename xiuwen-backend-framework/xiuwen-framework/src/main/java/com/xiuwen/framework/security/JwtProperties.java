package com.xiuwen.framework.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 * 对应 application.yml：xiuwen.jwt.secret / xiuwen.jwt.expire-seconds
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiuwen.jwt")
public class JwtProperties {
    /** JWT 签名密钥。上线时必须换成更长、更随机的字符串。 */
    private String secret = "xiuwen-secret-change-me";

    /** Token 过期时间，单位秒。默认 1 天。 */
    private Long expireSeconds = 86400L;
}
