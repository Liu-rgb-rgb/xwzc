package com.xiuwen.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 团队共享演示素材映射。
 *
 * 固定测试图片放在：
 * xiuwen-web/src/main/resources/static/uploads/demo/
 *
 * 浏览器访问地址：
 * /api/uploads/demo/**
 *
 * 真实用户上传文件仍由 xiuwen-framework 中的 WebMvcConfig
 * 按 xiuwen.file.upload-path 映射，不与演示素材混用。
 */
@Configuration
public class DemoStaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/uploads/demo/**")
                .addResourceLocations("classpath:/static/uploads/demo/");
    }
}
