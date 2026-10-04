package com.xiuwen.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置。
 * 对应 application.yml：xiuwen.oss.endpoint / access-key-id / access-key-secret / bucket-name
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiuwen.oss")
public class OssProperties {
    /** OSS Endpoint，例如 oss-cn-guangzhou.aliyuncs.com */
    private String endpoint;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** Bucket 名称 */
    private String bucketName;

    /** Bucket 对应的对外访问域名，例如 https://bucket-name.oss-cn-guangzhou.aliyuncs.com */
    private String domain;
}
