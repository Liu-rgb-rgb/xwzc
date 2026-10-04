package com.xiuwen.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置。
 * 对应 application.yml：xiuwen.file.upload-path / xiuwen.file.access-prefix
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiuwen.file")
public class FileUploadProperties {
    /** 本地文件上传目录 */
    private String uploadPath = "./uploads";

    /** 浏览器访问前缀，例如 /uploads */
    private String accessPrefix = "/uploads";
}
