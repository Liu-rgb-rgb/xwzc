package com.xiuwen.framework.config;

import com.xiuwen.framework.security.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：跨域、静态资源映射、JWT 拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final FileUploadProperties fileUploadProperties;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor, FileUploadProperties fileUploadProperties) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.fileUploadProperties = fileUploadProperties;
    }

    /**
     * 跨域配置。第一版前端本地开发可直接放开，后期上线建议限制具体域名。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 本地上传文件访问映射。
     * 例如：xiuwen.file.upload-path=./uploads，xiuwen.file.access-prefix=/uploads
     * 则浏览器可访问 /uploads/xxx.png。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String accessPrefix = normalizeAccessPrefix(fileUploadProperties.getAccessPrefix());
        String uploadPath = fileUploadProperties.getUploadPath();
        File uploadDir = new File(uploadPath);
        String location = "file:" + uploadDir.getAbsolutePath() + File.separator;
        registry.addResourceHandler(accessPrefix + "/**").addResourceLocations(location);
    }

    /**
     * JWT 拦截配置。
     * 注意：这里负责“必须登录”的基础判断，具体业务权限仍建议在业务方法中二次校验。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/home/**",
                        "/api/search/**",
                        "/api/shop/info",
                        "/api/patterns/options",
                        "/api/products/**",
                        "/api/courses/**",
                        "/api/resources",
                        "/api/resources/*",
                        normalizeAccessPrefix(fileUploadProperties.getAccessPrefix()) + "/**"
                );
    }

    private String normalizeAccessPrefix(String accessPrefix) {
        if (accessPrefix == null || accessPrefix.trim().isEmpty()) {
            return "/uploads";
        }
        String prefix = accessPrefix.trim();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
